package org.phoebus.channelfinder.rest.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.phoebus.channelfinder.Application;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.rest.api.IInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@EnableAutoConfiguration
public class InfoController implements IInfo {

  @Value("${channelfinder.version:4.7.0}")
  private String version;

  @Autowired private ElasticConfig esService;

  private static final ObjectMapper objectMapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Override
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
      ElasticsearchVersionInfo elasticVersion = response.version();
      elasticInfo.put("version", elasticVersion.number());
    } catch (IOException e) {
      Application.logger.log(
          Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
      elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
    }
    cfServiceInfo.put("elastic", elasticInfo);
    try {
      return objectMapper.writeValueAsString(cfServiceInfo);
    } catch (JsonProcessingException e) {
      Application.logger.log(
          Level.WARNING, "Failed to create ChannelFinder service info resource.", e);
      return "Failed to gather ChannelFinder service info";
    }
  }
}
