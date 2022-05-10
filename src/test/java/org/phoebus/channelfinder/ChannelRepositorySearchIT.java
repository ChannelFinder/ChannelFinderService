package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.ElasticSearchClient;
import org.phoebus.channelfinder.PropertyRepository;
import org.phoebus.channelfinder.TagRepository;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
     * Test searching for channels based on name
     * @throws InterruptedException 
     */
    @Test
    public void searchNameTest() throws InterruptedException {
        List<String> channelNames = Arrays
                .asList(populateService.getChannelList().toArray(new String[populateService.getChannelList().size()]));

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        // Search for a single unique channel
        searchParameters.add("~name", channelNames.get(0));
        List<XmlChannel> result = channelRepository.search(searchParameters);
        assertTrue(result.size() == 1 && result.get(0).getName().equals(channelNames.get(0)));

        // Search for all channels via wildcards
        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:2{BLA}Pos:?-RB");
        result = channelRepository.search(searchParameters);
        assertTrue("Expected 2 but got " + result.size(), result.size() == 2);

        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:?{BLA}Pos:*");
        result = channelRepository.search(searchParameters);
        assertTrue("Expected 4 but got " + result.size(), result.size() == 4);

        // Search for all 1000 channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*");
        result = channelRepository.search(searchParameters);
        assertTrue("Expected 1000 but got " + result.size(), result.size() == 1000);

        // Search for all 1000 SR channels and all 500 booster channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        result = channelRepository.search(searchParameters);
        assertTrue("Expected 1500 but got " + result.size(), result.size() == 1500);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        result = channelRepository.search(searchParameters);
        assertTrue("Expected 1500 but got " + result.size(), result.size() == 1500);
        
        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("~tag", "group"+id+"_"+val_bucket.get(index));

            result = channelRepository.search(searchParameters);
            assertTrue("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result.size(), result.size() == val_bucket.get(index));
        }
        
        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("group"+id, String.valueOf(val_bucket.get(index)));

            result = channelRepository.search(searchParameters);
            assertTrue("Search: "+ maptoString(searchParameters) +" Failed Expected "+val_bucket.get(index)+" but got " + result.size(), result.size() == val_bucket.get(index));
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
