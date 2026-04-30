package org.phoebus.channelfinder.performance;

import com.google.common.collect.Lists;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
class ExistsPerformanceIT {

  @Autowired PopulateDBConfiguration service;

  @Autowired ChannelRepository channelRepository;

  @Test
  void channelExists() throws IOException {
    service.createDB(1);
    Assertions.assertTrue(
        channelRepository.existsByIds(Lists.newArrayList(service.getChannelList())));
    service.cleanupDB();
  }
}
