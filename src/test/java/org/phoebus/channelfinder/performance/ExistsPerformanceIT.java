package org.phoebus.channelfinder.performance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;


import com.google.common.collect.Lists;

/**
 * Performance tests for "exists" calls
 * 
 * @author Kunal Shroff
 *
 */


@WebMvcTest(ChannelRepository.class)
public class ExistsPerformanceIT {

    @Autowired
    PopulateService service;

    @Autowired
    ChannelRepository channelRepository;

    @Test
    public void channelExists() {
        service.createDB(1);
        Assertions.assertTrue(channelRepository.existsByIds(Lists.newArrayList(service.getChannelList())));
        service.cleanupDB();
    }
}
