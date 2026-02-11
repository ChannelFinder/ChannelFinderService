package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.*;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.configuration.PopulateDBConfiguration;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.phoebus.channelfinder.rest.api.IChannelScroll;
import org.phoebus.channelfinder.rest.controller.ChannelScrollController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelScrollController.class)
@TestPropertySource(value = "classpath:application_test.properties")
@ContextConfiguration(classes = {ChannelScrollController.class, ElasticConfig.class})
class ChannelScrollControllerIT {

  @Autowired IChannelScroll channelScroll;

  @Autowired ChannelRepository channelRepository;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ElasticConfig esService;

  @Autowired PopulateDBConfiguration populateDBConfiguration;

  @BeforeEach
  public void setup() throws InterruptedException {
    populateDBConfiguration.createDB(1);
    Thread.sleep(10000);
  }

  @AfterEach
  public void cleanup() throws InterruptedException {
    populateDBConfiguration.cleanupDB();
  }

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  final List<Integer> val_bucket = Arrays.asList(1, 2, 5, 10, 20, 50, 100, 200, 500);

  /**
   * Test searching for channels based on name
   *
   * @throws InterruptedException
   */
  @Test
  void searchNameTest() throws InterruptedException {
    List<String> channelNames =
        Arrays.asList(
            populateDBConfiguration
                .getChannelList()
                .toArray(new String[populateDBConfiguration.getChannelList().size()]));

    MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
    // Search for a single unique channel
    searchParameters.add("~name", channelNames.get(0));
    Scroll scrollResult = channelScroll.search(null, searchParameters);
    List<Channel> result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertTrue(
        result.size() == 1 && result.get(0).getName().equals(channelNames.get(0)));

    // Search for all channels via wildcards
    searchParameters.clear();
    searchParameters.add("~name", "BR:C001-BI:2{BLA}Pos:?-RB");
    scrollResult = channelScroll.search(null, searchParameters);
    result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertSame(2, result.size(), "Expected 2 but got " + result.size());

    searchParameters.clear();
    searchParameters.add("~name", "BR:C001-BI:?{BLA}Pos:*");
    scrollResult = channelScroll.search(null, searchParameters);
    result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertSame(4, result.size(), "Expected 4 but got " + result.size());

    // Search for all 1000 channels
    searchParameters.clear();
    searchParameters.add("~name", "SR*");
    scrollResult = channelScroll.search(null, searchParameters);
    result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertEquals(1000, result.size(), "Expected 1000 but got " + result.size());

    // Search for all 1000 SR channels and all 500 booster channels
    searchParameters.clear();
    searchParameters.add("~name", "SR*|BR*");
    scrollResult = channelScroll.search(null, searchParameters);
    result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertEquals(1500, result.size(), "Expected 1500 but got " + result.size());

    searchParameters.clear();
    searchParameters.add("~name", "SR*,BR*");
    scrollResult = channelScroll.search(null, searchParameters);
    result = scrollResult.getChannels();
    while (scrollResult.getChannels().size() == 100) {
      scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
      result.addAll(scrollResult.getChannels());
    }
    Assertions.assertEquals(1500, result.size(), "Expected 1500 but got " + result.size());

    // search for channels based on a tag
    for (int i = 0; i < 5; i++) {

      long id = new Random().nextInt(10);
      int index = new Random().nextInt(9);
      searchParameters.clear();
      searchParameters.add("~name", "SR*");
      searchParameters.add("~tag", "group" + id + "_" + val_bucket.get(index));

      scrollResult = channelScroll.search(null, searchParameters);
      result = scrollResult.getChannels();
      while (scrollResult.getChannels().size() == 100) {
        scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
        result.addAll(scrollResult.getChannels());
      }
      Assertions.assertEquals(
          val_bucket.get(index),
          Integer.valueOf(result.size()),
          "Search: "
              + maptoString(searchParameters)
              + " Failed Expected "
              + val_bucket.get(index)
              + " but got "
              + result.size());
    }

    // search for channels based on a tag
    for (int i = 0; i < 5; i++) {

      long id = new Random().nextInt(10);
      int index = new Random().nextInt(9);
      searchParameters.clear();
      searchParameters.add("~name", "SR*");
      searchParameters.add("group" + id, String.valueOf(val_bucket.get(index)));

      scrollResult = channelScroll.search(null, searchParameters);
      result = scrollResult.getChannels();
      while (scrollResult.getChannels().size() == 100) {
        scrollResult = channelScroll.search(scrollResult.getId(), searchParameters);
        result.addAll(scrollResult.getChannels());
      }
      Assertions.assertEquals(
          val_bucket.get(index),
          Integer.valueOf(result.size()),
          "Search: "
              + maptoString(searchParameters)
              + " Failed Expected "
              + val_bucket.get(index)
              + " but got "
              + result.size());
    }
  }

  private String maptoString(MultiValueMap<String, String> searchParameters) {
    StringBuffer sb = new StringBuffer();
    searchParameters.forEach((key, value) -> sb.append(key).append(" ").append(value));
    return sb.toString();
  }
}
