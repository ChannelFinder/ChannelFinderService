package org.phoebus.channelfinder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("metrics")
@TestPropertySource(
        locations = "classpath:application_test.properties",
        properties = {
            "metrics.tags=testTag0, testTag1",
            "metrics.properties=testProperty0: testProperty0Value; testProperty1: testProperty1Value"
        })
class MetricsServiceIT {

    public static final String METRICS_ENDPOINT = "/actuator/metrics/";
    public static final String METRICS_TAG_LABEL = "tag";
    public static final String METRICS_PROPERTY_NAME = "testProperty";
    public static final String PROPERTY_0_LABEL = METRICS_PROPERTY_NAME + "0:testProperty0Value";
    public static final String PROPERTY_1_LABEL = METRICS_PROPERTY_NAME + "1:testProperty1Value";
    public static final String TAG_0_LABEL = "tag:testTag0";
    public static final String TAG_1_LABEL = "tag:testTag1";
    private final List<Tag> testTags =
            Arrays.asList(new Tag("testTag0", "testTagOwner0"), new Tag("testTag1", "testTagOwner1"));
    private final List<Property> testProperties = Arrays.asList(
            new Property(METRICS_PROPERTY_NAME + "0", "testPropertyOwner0"),
            new Property(METRICS_PROPERTY_NAME + "1", "testPropertyOwner1"),
            new Property(METRICS_PROPERTY_NAME + "2", "testPropertyOwner2"));

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ElasticConfig esService;

    @Autowired
    PopulateService populateService;

    @Autowired
    MetricsService metricsService;

    @Autowired
    private MockMvc mockMvc;

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
        channelRepository.search(map).channels().forEach(c -> channelRepository.deleteById(c.getName()));
        tagRepository.findAll().forEach(t -> tagRepository.deleteById(t.getName()));
        propertyRepository.findAll().forEach(p -> propertyRepository.deleteById(p.getName()));
    }
    @Test
    void testGaugeMetrics() throws Exception {
        mockMvc.perform(get(METRICS_ENDPOINT)).andExpect(status().is(200));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_TOTAL_CHANNEL_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(0));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_PROPERTY_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(0));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_TAG_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(0));

        Channel testChannel = new Channel("testChannel", "testOwner");
        channelRepository.save(testChannel);
        propertyRepository.saveAll(testProperties);
        tagRepository.saveAll(testTags);

        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_TOTAL_CHANNEL_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(1));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_PROPERTY_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(3));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_TAG_COUNT))
                .andExpect(jsonPath("$.measurements[0].value").value(2));
    }

    @Test
    void testTagMultiGaugeMetrics() throws Exception {
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_CHANNEL_COUNT)
                        .param(METRICS_TAG_LABEL, TAG_0_LABEL))
                .andExpect(jsonPath("$.measurements[0].value").value(0));
        mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_CHANNEL_COUNT)
                        .param(METRICS_TAG_LABEL, TAG_1_LABEL))
                .andExpect(jsonPath("$.measurements[0].value").value(0));

        tagRepository.saveAll(testTags);

        Channel testChannel = new Channel("testChannelTag", "testOwner", List.of(), testTags);
        channelRepository.save(testChannel);

        await().untilAsserted(() -> {
            mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_CHANNEL_COUNT)
                            .param(METRICS_TAG_LABEL, TAG_0_LABEL))
                    .andExpect(jsonPath("$.measurements[0].value").value(1));
            mockMvc.perform(get(METRICS_ENDPOINT + MetricsService.CF_CHANNEL_COUNT)
                            .param(METRICS_TAG_LABEL, TAG_1_LABEL))
                    .andExpect(jsonPath("$.measurements[0].value").value(1));
        });
    }

    @Test
    void testPropertyMultiGaugeMetrics() throws Exception {
    mockMvc.perform(get(METRICS_ENDPOINT + String.format(MetricsService.CF_PROPERTY_FORMAT_STRING, METRICS_PROPERTY_NAME + "0"))
                        .param(METRICS_TAG_LABEL, PROPERTY_0_LABEL))
                .andExpect(jsonPath("$.measurements[0].value").value(0));
        mockMvc.perform(get(METRICS_ENDPOINT + String.format(MetricsService.CF_PROPERTY_FORMAT_STRING, METRICS_PROPERTY_NAME + "1"))
                        .param(METRICS_TAG_LABEL, PROPERTY_1_LABEL))
                .andExpect(jsonPath("$.measurements[0].value").value(0));


        propertyRepository.saveAll(testProperties);

        Channel testChannel = new Channel(
                "testChannelProp",
                "testOwner",
                testProperties.stream()
                        .map(p -> new Property(p.getName(), p.getOwner(), p.getName() + "Value"))
                        .toList(),
                testTags);
        channelRepository.save(testChannel);

        await().untilAsserted(() -> {
            mockMvc.perform(get(METRICS_ENDPOINT + String.format(MetricsService.CF_PROPERTY_FORMAT_STRING, METRICS_PROPERTY_NAME + "0"))
                            .param(METRICS_TAG_LABEL, PROPERTY_0_LABEL))
                    .andExpect(jsonPath("$.measurements[0].value").value(1));
            mockMvc.perform(get(METRICS_ENDPOINT + String.format(MetricsService.CF_PROPERTY_FORMAT_STRING, METRICS_PROPERTY_NAME + "1"))
                            .param(METRICS_TAG_LABEL, PROPERTY_1_LABEL))
                    .andExpect(jsonPath("$.measurements[0].value").value(1));
        });
    }
}
