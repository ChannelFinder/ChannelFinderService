package org.phoebus.channelfinder;

import static org.phoebus.channelfinder.CFResourceDescriptors.CF_SERVICE_INFO;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.core.InfoResponse;

@CrossOrigin
@RestController
@RequestMapping(CF_SERVICE_INFO)
@EnableAutoConfiguration
public class InfoManager {

    @Value("${channelfinder.version:4.7.0}")
    private String version;
    
    @Autowired
    private ElasticConfig esService;

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 
     * @return Information about the ChannelFinder service
     */
    @GetMapping
    public String info() {

        Map<String, Object> cfServiceInfo = new LinkedHashMap<>();
        cfServiceInfo.put("name", "ChannelFinder Service");
        cfServiceInfo.put("version", version);

        Map<String, String> elasticInfo = new LinkedHashMap<>();
        try {

            ElasticsearchClient client = esService.getSearchClient();
            InfoResponse response = client.info();
            
            elasticInfo.put("status", "Connected");
            elasticInfo.put("clusterName", response.clusterName());
            elasticInfo.put("clusterUuid", response.clusterUuid());
            ElasticsearchVersionInfo version = response.version();
            elasticInfo.put("version", version.toString());
        } catch (IOException e) {
            Application.logger.log(Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
            elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
        }
        cfServiceInfo.put("elastic", elasticInfo);
        try {
            return objectMapper.writeValueAsString(cfServiceInfo);
        } catch (JsonProcessingException e) {
            Application.logger.log(Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
            return "Failed to gather ChannelFinder service info";
        }
    }
}
