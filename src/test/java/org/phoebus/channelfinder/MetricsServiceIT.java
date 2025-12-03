package org.phoebus.channelfinder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.configuration.PopulateDBConfiguration;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.PropertyRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("metrics")
@TestPropertySource(
    locations = "classpath:application_test.properties",
    properties = {
      "metrics.tags=testTag0, testTag1",
      "metrics.properties=testProperty0: value0, value1; testProperty1: value0, !*",
      "metrics.updateInterval=1"
    })
class MetricsServiceIT {

  public static final String METRICS_ENDPOINT = "/actuator/metrics/";
  public static final String PROPERTY_NAME = "testProperty";
  public static final String OWNER = "testOwner";
  public static final String TAG_NAME = "testTag";
  public static final String METRICS_PARAM_KEY = "tag";
  public static final String PROPERTY_VALUE = "value";

  @Autowired ChannelRepository channelRepository;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ElasticConfig esService;

  @Autowired PopulateDBConfiguration populateDBConfiguration;

  @Autowired MetricsService metricsService;

  @Autowired private MockMvc mockMvc;

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  @AfterEach
  public void cleanup() {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.set("~name", "*");
    channelRepository
        .search(map)
        .channels()
        .forEach(c -> channelRepository.deleteById(c.getName()));
    tagRepository.findAll().forEach(t -> tagRepository.deleteById(t.getName()));
    propertyRepository.findAll().forEach(p -> propertyRepository.deleteById(p.getName()));
  }

  private void getAndExpectMetric(String paramValue, String endpoint, int expectedValue)
      throws Exception {
    mockMvc
        .perform(get(METRICS_ENDPOINT + endpoint).param(METRICS_PARAM_KEY, paramValue))
        .andExpect(jsonPath("$.measurements[0].value").value(expectedValue));
  }

  private void getAndExpectMetricParent(String endpoint, int expectedValue) throws Exception {
    mockMvc
        .perform(get(METRICS_ENDPOINT + endpoint))
        .andExpect(jsonPath("$.measurements[0].value").value(expectedValue));
  }

  @Test
  void testGaugeMetrics() throws Exception {
    mockMvc.perform(get(METRICS_ENDPOINT)).andExpect(status().is(200));
    getAndExpectMetricParent(MetricsService.CF_TOTAL_CHANNEL_COUNT, 0);
    getAndExpectMetricParent(MetricsService.CF_PROPERTY_COUNT, 0);
    getAndExpectMetricParent(MetricsService.CF_TAG_COUNT, 0);

    Channel testChannel = new Channel("testChannel", "testOwner");
    channelRepository.save(testChannel);
    propertyRepository.saveAll(propertyList(3));
    tagRepository.saveAll(tagList(2));

    getAndExpectMetricParent(MetricsService.CF_TOTAL_CHANNEL_COUNT, 1);
    getAndExpectMetricParent(MetricsService.CF_PROPERTY_COUNT, 3);
    getAndExpectMetricParent(MetricsService.CF_TAG_COUNT, 2);
  }

  private List<Tag> tagList(int count) {
    return IntStream.range(0, count).mapToObj(i -> new Tag(TAG_NAME + i, OWNER)).toList();
  }

  private String tagParamValue(Tag tag) {
    return String.format("tag:%s", tag.getName());
  }

  private void getAndExpectTagMetric(Tag tag, int expectedValue) throws Exception {
    getAndExpectMetric(tagParamValue(tag), MetricsService.CF_TAG_ON_CHANNELS_COUNT, expectedValue);
  }

  @Test
  void testTagMultiGaugeMetrics() throws Exception {
    List<Tag> testTags = tagList(3);
    getAndExpectMetricParent(MetricsService.CF_TAG_ON_CHANNELS_COUNT, 0);
    getAndExpectTagMetric(testTags.get(0), 0);
    getAndExpectTagMetric(testTags.get(1), 0);

    tagRepository.saveAll(testTags);

    Channel testChannel = new Channel("testChannelTag", "testOwner", List.of(), testTags);
    channelRepository.save(testChannel);
    Channel testChannel1 =
        new Channel("testChannelTag1", "testOwner", List.of(), List.of(testTags.get(0)));
    channelRepository.save(testChannel1);

    Thread.sleep(2000); // Update interval is 1 second
    getAndExpectTagMetric(testTags.get(0), 2);
    getAndExpectTagMetric(testTags.get(1), 1);
    getAndExpectMetricParent(MetricsService.CF_TAG_ON_CHANNELS_COUNT, 3);
  }

  private List<Property> propertyList(int count) {
    return IntStream.range(0, count).mapToObj(i -> new Property(PROPERTY_NAME + i, OWNER)).toList();
  }

  private String propertyParamValue(Property property) {
    return String.format("%s:%s", property.getName(), property.getValue());
  }

  private void getAndExpectPropertyMetric(Property property, int expectedValue) throws Exception {
    getAndExpectMetric(
        propertyParamValue(property), MetricsService.CF_CHANNEL_COUNT, expectedValue);
  }

  @Test
  void testPropertyMultiGaugeMetrics() throws Exception {
    List<Property> testProperties = propertyList(2);

    Channel testChannel =
        new Channel(
            "testChannelProp",
            "testOwner",
            List.of(
                new Property(testProperties.get(0).getName(), OWNER, PROPERTY_VALUE + 0),
                new Property(testProperties.get(1).getName(), OWNER, PROPERTY_VALUE + 0)),
            List.of());

    Channel testChannel1 =
        new Channel(
            "testChannelProp1",
            "testOwner",
            List.of(new Property(testProperties.get(0).getName(), OWNER, PROPERTY_VALUE + 1)),
            List.of());

    getAndExpectMetricParent(MetricsService.CF_CHANNEL_COUNT, 0);
    getAndExpectPropertyMetric(testChannel.getProperties().get(0), 0);
    getAndExpectPropertyMetric(testChannel1.getProperties().get(0), 0);

    getAndExpectPropertyMetric(testChannel.getProperties().get(1), 0);
    getAndExpectPropertyMetric(
        new Property(testProperties.get(1).getName(), OWNER, MetricsService.NOT_SET), 0);

    propertyRepository.saveAll(testProperties);

    channelRepository.save(testChannel);
    channelRepository.save(testChannel1);

    Thread.sleep(2000); // Update interval is 1 second

    getAndExpectMetricParent(MetricsService.CF_CHANNEL_COUNT, 2);
    getAndExpectPropertyMetric(testChannel.getProperties().get(0), 1);
    getAndExpectPropertyMetric(testChannel1.getProperties().get(0), 1);

    getAndExpectPropertyMetric(testChannel.getProperties().get(1), 1);
    getAndExpectPropertyMetric(
        new Property(testProperties.get(1).getName(), OWNER, MetricsService.NOT_SET), 1);
  }
}
