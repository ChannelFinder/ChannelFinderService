package org.phoebus.channelfinder.performance;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.ElasticsearchTestBase;
import org.phoebus.channelfinder.configuration.PopulateDBConfiguration;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Performance tests for "exists" calls
 *
 * @author Kunal Shroff
 */
@WebMvcTest(ChannelRepository.class)
@TestPropertySource(properties = "aa.enabled=false")
class ExistsPerformanceIT extends ElasticsearchTestBase {

  @Autowired PopulateDBConfiguration service;

  @Autowired ChannelRepository channelRepository;

  @Test
  void channelExists() throws IOException {
    service.createDB(1);
    Assertions.assertTrue(channelRepository.existsByIds(new ArrayList<>(service.getChannelList())));
    service.cleanupDB();
  }
}
