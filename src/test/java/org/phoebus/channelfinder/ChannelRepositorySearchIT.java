package org.phoebus.channelfinder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.min;
import static org.phoebus.channelfinder.example.PopulateService.valBucket;
import static org.phoebus.channelfinder.example.PopulateService.valBucketSize;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelRepository.class)
@TestPropertySource(locations = "classpath:application_test.properties")
class ChannelRepositorySearchIT {
    private static final Logger logger = Logger.getLogger(ChannelRepositorySearchIT.class.getName());

    // Need at least 10 000 channels to test Elastic search beyond the 10 000 default result limit
    // So needs to be a minimum of 7
    private final int CELLS = 100;
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

    @BeforeAll
    void setupAll() {
        ElasticConfigIT.setUp(esService);
    }

    @BeforeEach
    public void setup() throws InterruptedException {
        populateService.cleanupDB();
        populateService.createDB(CELLS);
        Thread.sleep(5000);
    }

    @AfterEach
    public void cleanup() throws InterruptedException {
        populateService.cleanupDB();
    }

    /**
     * Test searching for channel Note: the search tests are merged into a single test as an optimization. The
     * population of the data cannot be performed in the @beforeClass since the springboot beans are not initialized
     * hence the need to use @Before
     */
    @Test
    void searchTest() {
        List<String> channelNames = Arrays
                .asList(populateService.getChannelList().toArray(new String[0]));

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        //logger.log(Level.INFO, "Search for a single unique channel " + channelNames.get(0));

        searchParameters.add("~name", channelNames.get(0));
        SearchResult result = channelRepository.search(searchParameters);
        long countResult = channelRepository.count(searchParameters);
        Assertions.assertEquals(1, result.getCount());
        Assertions.assertEquals(1, result.getChannels().size());
        Assertions.assertEquals(1, countResult);
        Assertions.assertEquals(result.getChannels().get(0).getName(), channelNames.get(0));

        logger.log(Level.INFO, "Search for all channels via wildcards");
        searchName(2,2, "BR:C001-BI:2{BLA}Pos:?-RB");

        searchName(4, 4, "BR:C001-BI:?{BLA}Pos:*");

        logger.log(Level.INFO, "Search for all 1000 channels");
        searchName(min(1000 * CELLS, ELASTIC_LIMIT), 1000 * CELLS, "SR*");

        logger.log(Level.INFO, "Search for all 1000 SR channels and all 500 booster channels");
        long allCount = 1500 * CELLS;
        long elasticDefaultCount = min(allCount, ELASTIC_LIMIT);
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        assertSearchCount(elasticDefaultCount, (int) elasticDefaultCount, allCount, searchParameters);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        assertSearchCount(elasticDefaultCount, (int) elasticDefaultCount, allCount, searchParameters);

        logger.log(Level.INFO, "Search for all 1000 SR channels and all 500 booster channels");
        searchParameters.clear();
        searchParameters.add("~name", "SR*|BR*");
        searchParameters.add("~track_total_hits", "true");
        assertSearchCount(allCount, (int) elasticDefaultCount, allCount, searchParameters);

        searchParameters.clear();
        searchParameters.add("~name", "SR*,BR*");
        searchParameters.add("~track_total_hits", "true");
        assertSearchCount(allCount, (int) elasticDefaultCount, allCount, searchParameters);

        logger.log(Level.INFO, "Search for channels based on a tag");
        for (long id = 1; id < valBucket.size(); id++) {

            for (int bucket_index = 0; bucket_index < valBucket.size(); bucket_index++) {
                checkGroup(valBucketSize.get(bucket_index), "~tag", "group" + id + "_" + valBucket.get(bucket_index));
                checkGroup(valBucketSize.get(bucket_index), "group" + id, String.valueOf(valBucket.get(bucket_index)));
            }

        }

    }

    private void searchName(int expectedChannels, int expectedQueryCount, String name) {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add("~name", name);
        assertSearchCount(expectedChannels, expectedChannels, expectedQueryCount, searchParameters);
    }

    private void assertSearchCount(long expectedResultCount, int expectedChannelsCount, long expectedQueryCount, MultiValueMap<String, String> searchParameters) {
        logger.log(Level.INFO, "Search for " + searchParameters + " expected " + expectedResultCount + " results " + expectedChannelsCount + " channels " + expectedQueryCount + " queries");
        // Act
        SearchResult result = channelRepository.search(searchParameters);

        // Assert
        Assertions.assertEquals(expectedResultCount, result.getCount());
        Assertions.assertEquals(expectedChannelsCount, result.getChannels().size());
        Assertions.assertEquals(expectedQueryCount, channelRepository.count(searchParameters));
    }

    private void checkGroup(int bucket, String key, String value) {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();

        searchParameters.add("~name", "SR*");
        searchParameters.add(key, value);

        SearchResult result = channelRepository.search(searchParameters);
        Integer expectedCount = CELLS * bucket;
        logger.log(Level.INFO, "Search for " + maptoString(searchParameters) + " expected " + expectedCount + " results");
        Assertions.assertEquals(min(expectedCount, ELASTIC_LIMIT), Integer.valueOf(result.getChannels().size()), "Search: " + maptoString(searchParameters));
        Assertions.assertEquals(expectedCount, Integer.valueOf((int) channelRepository.count(searchParameters)));
    }

    private String maptoString(MultiValueMap<String, String> searchParameters) {
        StringBuffer sb = new StringBuffer();
        searchParameters.forEach((key, value) -> sb.append(key).append(" ").append(value));
        return sb.toString();
    }
    @AfterAll
    void tearDown() throws IOException {
        ElasticConfigIT.teardown(esService);
    }
}
