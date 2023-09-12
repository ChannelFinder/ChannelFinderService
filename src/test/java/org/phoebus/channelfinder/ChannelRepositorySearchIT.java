package org.phoebus.channelfinder;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.lang.Math.min;
import static org.phoebus.channelfinder.example.PopulateService.val_bucket;


@WebMvcTest(ChannelRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelRepositorySearchIT {

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

    @Value("${elasticsearch.query.size:10000}")
    int ELASTIC_LIMIT;

    // Need at least 10 000 channels to test Elastic search beyond the 10 000 default result limit
    // So needs to be a minimum of 7
    private final int CELLS = 7;

    @BeforeEach
    public void setup() throws InterruptedException {
        populateService.cleanupDB();
        populateService.createDB(CELLS);
        Thread.sleep(5000);
    }

    @AfterEach
    public void cleanup() throws InterruptedException {
        populateService.cleanupDB();
        Thread.sleep(5000);
    }

    /**
     * Test searching for channel
     * Note: the search tests are merged into a single test as an optimization. The population of the data cannot
     * be performed in the @beforeClass since the springboot beans are not initialized hence the need to use @Before
     */
    @Test
    public void searchTest() {
        List<String> channelNames = Arrays
                .asList(populateService.getChannelList().toArray(new String[0]));

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        // Search for a single unique channel
        searchParameters.add("~name", channelNames.get(0));
        SearchResult result = channelRepository.search(searchParameters);
        long countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(1, result.getCount());
        Assertions.assertEquals(1, result.getChannels().size());
        Assertions.assertEquals(1, countResult);
        Assertions.assertEquals(result.getChannels().get(0).getName(), channelNames.get(0));

        // Search for all channels via wildcards
        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:2{BLA}Pos:?-RB");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(2, result.getCount());
        Assertions.assertEquals( 2, result.getChannels().size());
        Assertions.assertEquals(2, countResult);

        searchParameters.clear();
        searchParameters.add("~name", "BR:C001-BI:?{BLA}Pos:*");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(4, result.getCount());
        Assertions.assertEquals(4, result.getChannels().size());
        Assertions.assertEquals(4, countResult);

        // Search for all 1000 channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(1000 * CELLS, result.getCount());
        Assertions.assertEquals(1000 * CELLS, result.getChannels().size());
        Assertions.assertEquals(1000 * CELLS, countResult);

        // Search for all 1000 SR channels and all 500 booster channels
        long allCount = 1500 * CELLS;
        long elasticDefaultCount = min(allCount, ELASTIC_LIMIT);
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(elasticDefaultCount, result.getCount());
        Assertions.assertEquals(elasticDefaultCount, result.getChannels().size());
        Assertions.assertEquals(allCount, countResult);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(elasticDefaultCount, result.getCount());
        Assertions.assertEquals(elasticDefaultCount, result.getChannels().size());
        Assertions.assertEquals(allCount, countResult);

        // Search for all 1000 SR channels and all 500 booster channels
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        searchParameters.add("~track_total_hits", "true");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(allCount, result.getCount());
        Assertions.assertEquals(elasticDefaultCount, result.getChannels().size());
        Assertions.assertEquals(allCount, countResult);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        searchParameters.add("~track_total_hits", "true");
        result = channelRepository.search(searchParameters);
        countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(allCount, result.getCount());
        Assertions.assertEquals(elasticDefaultCount, result.getChannels().size());
        Assertions.assertEquals(allCount, countResult);

        Integer expectedCount;
        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("~tag", "group"+id+"_"+val_bucket.get(index));

            result = channelRepository.search(searchParameters);
            countResult = channelRepository.count(searchParameters);
            expectedCount = CELLS * val_bucket.get(index);
            Assertions.assertEquals(expectedCount, Integer.valueOf(result.getChannels().size()), "Search: "+ maptoString(searchParameters));
            Assertions.assertEquals(expectedCount, Integer.valueOf((int) countResult));
        }

        // search for channels based on a tag
        for (int i = 0; i < 5; i++) {

            long id = new Random().nextInt(10);
            int index = new Random().nextInt(9);
            searchParameters.clear();
            searchParameters.add("~name", "SR*");
            searchParameters.add("group"+id, String.valueOf(val_bucket.get(index)));

            result = channelRepository.search(searchParameters);
            countResult = channelRepository.count(searchParameters);
            expectedCount = CELLS * val_bucket.get(index);
            Assertions.assertEquals(expectedCount, Integer.valueOf(result.getChannels().size()), "Search: "+ maptoString(searchParameters));
            Assertions.assertEquals(expectedCount, Integer.valueOf((int) countResult));
        }
    }

    private String maptoString(MultiValueMap<String, String> searchParameters) {
        StringBuffer sb = new StringBuffer();
        searchParameters.forEach((key, value) -> sb.append(key).append(" ").append(value));
        return sb.toString();
    }
}
