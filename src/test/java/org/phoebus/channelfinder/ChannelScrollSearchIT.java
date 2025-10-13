package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelScroll.class)
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelScrollSearchIT {

  private static final Logger logger = Logger.getLogger(ChannelScrollSearchIT.class.getName());

  @Autowired ChannelScroll channelScroll;

  @Autowired TagRepository tagRepository;
  @Autowired PropertyRepository propertyRepository;

  @Autowired ElasticConfig esService;
  @Autowired PopulateService populateService;

  @BeforeEach
  public void setup() throws InterruptedException {
    populateService.createDB(1);
    Thread.sleep(10000);
  }

  @AfterEach
  public void cleanup() throws InterruptedException {
    populateService.cleanupDB();
    Thread.sleep(10000);
  }

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  /** Test searching for channels based on name */
  @Test
  void searchNameTest() {

    MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
    searchParameters.add("~name", "*");
    searchParameters.add("~size", "100");

    long start = System.currentTimeMillis();
    Scroll scrollResult = channelScroll.query(searchParameters);
    logger.log(
        Level.INFO,
        "Completed the first scroll request in : " + (System.currentTimeMillis() - start) + "ms");
    int totalResult = 0;
    Double avg100 = 0.0;

    logger.log(
        Level.INFO,
        "Retrieved channels "
            + totalResult
            + " to "
            + (totalResult + scrollResult.getChannels().size())
            + " in : "
            + (System.currentTimeMillis() - start)
            + "ms");
    avg100 = (avg100 + (System.currentTimeMillis() - start)) / 2;
    totalResult += scrollResult.getChannels().size();
    start = System.currentTimeMillis();

    while (scrollResult.getChannels().size() == 100) {
      logger.log(Level.INFO, "Retireval id: " + scrollResult.getId());
      scrollResult = channelScroll.query(scrollResult.getId(), searchParameters);
      logger.log(
          Level.INFO,
          "Retrieved channels "
              + totalResult
              + " to "
              + (totalResult + scrollResult.getChannels().size())
              + " in : "
              + (System.currentTimeMillis() - start)
              + "ms");
      avg100 = (avg100 + (System.currentTimeMillis() - start)) / 2;
      totalResult += scrollResult.getChannels().size();
      start = System.currentTimeMillis();
    }
    logger.log(Level.INFO, "total result = " + totalResult);

    // Size = 1000
    logger.log(Level.INFO, "Rerun test with scroll size  = 1000");
    searchParameters.add("~size", "1000");
    start = System.currentTimeMillis();
    scrollResult = channelScroll.query(searchParameters);
    logger.log(
        Level.INFO,
        "Completed the first scroll request in : " + (System.currentTimeMillis() - start) + "ms");
    totalResult = 0;
    Double avg1000 = 0.0;

    logger.log(
        Level.INFO,
        "Retrieved channels "
            + totalResult
            + " to "
            + (totalResult + scrollResult.getChannels().size())
            + " in : "
            + (System.currentTimeMillis() - start)
            + "ms");
    avg1000 = (avg1000 + (System.currentTimeMillis() - start)) / 2;
    totalResult += scrollResult.getChannels().size();
    start = System.currentTimeMillis();

    while (scrollResult.getChannels().size() == 1000) {
      logger.log(Level.INFO, "Retireval id: " + scrollResult.getId());
      scrollResult = channelScroll.query(scrollResult.getId(), searchParameters);
      logger.log(
          Level.INFO,
          "Retrieved channels "
              + totalResult
              + " to "
              + (totalResult + scrollResult.getChannels().size())
              + " in : "
              + (System.currentTimeMillis() - start)
              + "ms");
      avg1000 = (avg1000 + (System.currentTimeMillis() - start)) / 2;
      totalResult += scrollResult.getChannels().size();
      start = System.currentTimeMillis();
    }
    logger.log(Level.INFO, "total result = " + totalResult);

    // Size = 10000
    logger.log(Level.INFO, "Rerun test with scroll size  = 10000");
    searchParameters.remove("~size");
    searchParameters.add("~size", "10000");
    start = System.currentTimeMillis();
    scrollResult = channelScroll.query(searchParameters);
    logger.log(
        Level.INFO,
        "Completed the first scroll request in : " + (System.currentTimeMillis() - start) + "ms");
    totalResult = 0;
    Double avg10000 = 0.0;

    logger.log(
        Level.FINE,
        "Retrieved channels "
            + totalResult
            + " to "
            + (totalResult + scrollResult.getChannels().size())
            + " in : "
            + (System.currentTimeMillis() - start)
            + "ms");
    avg10000 = (avg10000 + (System.currentTimeMillis() - start)) / 2;
    totalResult += scrollResult.getChannels().size();
    start = System.currentTimeMillis();

    while (scrollResult.getChannels().size() == 10000) {
      scrollResult = channelScroll.query(scrollResult.getId(), searchParameters);
      logger.log(
          Level.FINE,
          "Retrieved channels "
              + totalResult
              + " to "
              + (totalResult + scrollResult.getChannels().size())
              + " in : "
              + (System.currentTimeMillis() - start)
              + "ms");
      avg10000 = (avg10000 + (System.currentTimeMillis() - start)) / 2;
      totalResult += scrollResult.getChannels().size();
      start = System.currentTimeMillis();
    }
    logger.log(Level.INFO, "total result = " + totalResult);

    logger.log(
        Level.INFO,
        " compeleted scrolling db with \n"
            + " size 100 in avg: "
            + avg100
            + "\n"
            + " size 1000 in avg: "
            + avg1000
            + "\n"
            + " size 10000 in avg: "
            + avg10000
            + "\n");
  }
}
