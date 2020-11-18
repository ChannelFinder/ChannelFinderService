package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.CF_SERVICE_INFO;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.elasticsearch.Version;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@RestController
@RequestMapping(CF_SERVICE_INFO)
@EnableAutoConfiguration
public class InfoManager {

    @Value("${channelfinder.version:4.0.0}")
    private String version;

    @Autowired
    ElasticSearchClient esService;

    private final static ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 
     * @return Information about the ChannelFinder service
     */
    @GetMapping
    public String info() {

        Map<String, Object> cfServiceInfo = new LinkedHashMap<String, Object>();
        cfServiceInfo.put("name", "ChannelFinder Service");
        cfServiceInfo.put("version", version);

        RestHighLevelClient client = esService.getSearchClient();
        Map<String, String> elasticInfo = new LinkedHashMap<String, String>();
        try {
            MainResponse response = client.info(RequestOptions.DEFAULT);

            elasticInfo.put("clusterName", response.getClusterName().value());
            elasticInfo.put("clusterUuid", response.getClusterUuid());
            Version version = response.getVersion();
            elasticInfo.put("version", version.toString());
            cfServiceInfo.put("elastic", elasticInfo);
        } catch (IOException e) {
            Application.logger.log(Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
        }
        try {
            return objectMapper.writeValueAsString(cfServiceInfo);
        } catch (JsonProcessingException e) {
            Application.logger.log(Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
            return "Failed to gather ChannelFinder service info";
        }
    }
}
