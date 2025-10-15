package org.phoebus.channelfinder.performance;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

/**
 * Performance tests for "exists" calls
 *
 * @author Kunal Shroff
 */
@WebMvcTest(ChannelRepository.class)
class ExistsPerformanceIT {

  @Autowired PopulateService service;

  @Autowired ChannelRepository channelRepository;

  @Test
  void channelExists() {
    service.createDB(1);
    Assertions.assertTrue(
        channelRepository.existsByIds(Lists.newArrayList(service.getChannelList())));
    service.cleanupDB();
  }
}
