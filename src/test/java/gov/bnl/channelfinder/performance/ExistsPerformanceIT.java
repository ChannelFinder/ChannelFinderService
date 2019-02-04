package gov.bnl.channelfinder.performance;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import gov.bnl.channelfinder.ChannelRepository;
import gov.bnl.channelfinder.example.PopulateService;

/**
 * Performance tests for "exists" calls
 * 
 * @author Kunal Shroff
 *
 */

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
public class ExistsPerformanceIT {

    @Autowired
    PopulateService service;

    @Autowired
    ChannelRepository channelRepository;

    @Test
    public void channelExists() {
        service.createDB(1);
        assertTrue(channelRepository.existsByIds(Lists.newArrayList(service.getChannelList())));
        service.cleanupDB();
    }
}
