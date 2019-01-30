package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import gov.bnl.channelfinder.example.PopulateService;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
public class ChannelRepositorySearchIT {

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    PopulateService populateService;

    @Before
    public void setup() {
        populateService.createDB(1);
    }

    @After
    public void cleanup() {
        populateService.cleanupDB();
    }

    /**
     * Test searching for channels based on name
     */
    @Test
    public void searchNameTest() {
        populateService.getChannelList().stream().forEach(System.out::println);
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap();
        searchParameters.add("~name", "SR*");
        List<XmlChannel> result = channelRepository.search(searchParameters);
        assertTrue(result.size() == 1000);
    }
}
