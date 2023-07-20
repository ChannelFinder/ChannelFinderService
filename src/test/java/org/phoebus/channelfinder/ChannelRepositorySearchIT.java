package org.phoebus.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
public class ChannelRepositorySearchIT {

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

    @Before
    public void setup() throws InterruptedException {
        populateService.createDB(1);
        Thread.sleep(10000);
    }

    @After
    public void cleanup() throws InterruptedException {
        populateService.cleanupDB();
        Thread.sleep(10000);
    }

    final List<Integer> val_bucket = Arrays.asList(1, 2, 5, 10, 20, 50, 100, 200, 500);

    /**
     * Test searching for channel
     * Note: the search tests are merged into a single test as an optimization. The population of the data cannot
     * be performed in the @beforeClass since the springboot beans are not initialized hence the need to use @Before
     * @throws InterruptedException 
     */
    @Test
    public void searchTest() throws InterruptedException {
        List<String> channelNames = Arrays
                .asList(populateService.getChannelList().toArray(new String[populateService.getChannelList().size()]));

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        // Search for a single unique channel
        searchParameters.add("~name", channelNames.get(0));
        List<Channel> result = channelRepository.search(searchParameters).getChannels();
        assertTrue(result.size() == 1 && result.get(0).getName().equals(channelNames.get(0)));

        // Search for all channels via wildcards
        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:2{BLA}Pos:?-RB");
        result = channelRepository.search(searchParameters).getChannels();
        assertSame("Expected 2 but got " + result.size(), 2, result.size());

        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:?{BLA}Pos:*");
        result = channelRepository.search(searchParameters).getChannels();
        assertSame("Expected 4 but got " + result.size(), 4, result.size());

        // Search for all 1000 channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*");
        result = channelRepository.search(searchParameters).getChannels();
        assertEquals("Expected 1000 but got " + result.size(), 1000, result.size());

        // Search for all 1000 SR channels and all 500 booster channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        result = channelRepository.search(searchParameters).getChannels();
        assertEquals("Expected 1500 but got " + result.size(), 1500, result.size());

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        result = channelRepository.search(searchParameters).getChannels();
        assertEquals("Expected 1500 but got " + result.size(), 1500, result.size());
        
        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("~tag", "group"+id+"_"+val_bucket.get(index));

            result = channelRepository.search(searchParameters).getChannels();
            assertEquals("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result.size(), val_bucket.get(index), Integer.valueOf(result.size()));
        }
        
        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("group"+id, String.valueOf(val_bucket.get(index)));

            result = channelRepository.search(searchParameters).getChannels();
            assertEquals("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result.size(), val_bucket.get(index), Integer.valueOf(result.size()));
        }
    }

    @Test
    public void countTest() throws InterruptedException {
        List<String> channelNames = Arrays
                .asList(populateService.getChannelList().toArray(new String[populateService.getChannelList().size()]));

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        // Search for a single unique channel
        searchParameters.add("~name", channelNames.get(0));
        long result = channelRepository.count(searchParameters);
        assertTrue(result == 1);

        // Search for all channels via wildcards
        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:2{BLA}Pos:?-RB");
        result = channelRepository.count(searchParameters);
        assertEquals("Expected 2 but got " + result, 2, result);

        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:?{BLA}Pos:*");
        result = channelRepository.count(searchParameters);
        assertEquals("Expected 4 but got " + result, 4, result);

        // Search for all 1000 channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*");
        result = channelRepository.count(searchParameters);
        assertEquals("Expected 1000 but got " + result, 1000, result);

        // Search for all 1000 SR channels and all 500 booster channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        result = channelRepository.count(searchParameters);
        assertEquals("Expected 1500 but got " + result, 1500, result);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        result = channelRepository.count(searchParameters);
        assertEquals("Expected 1500 but got " + result, 1500, result);

        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("~tag", "group"+id+"_"+val_bucket.get(index));

            result = channelRepository.count(searchParameters);
            assertEquals("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result, val_bucket.get(index), Integer.valueOf((int) result));
        }

        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("group"+id, String.valueOf(val_bucket.get(index)));

            result = channelRepository.count(searchParameters);
            assertEquals("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result, val_bucket.get(index), Integer.valueOf((int) result));
        }
    }

    private String maptoString(MultiValueMap<String, String> searchParameters) {
        StringBuffer sb = new StringBuffer();
        searchParameters.entrySet().forEach(e -> {
            sb.append(e.getKey() + " " + e.getValue());
        });
        return sb.toString();
    }
}
