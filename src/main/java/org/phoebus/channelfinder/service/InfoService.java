package org.phoebus.channelfinder.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InfoService {

  private static final Logger logger = Logger.getLogger(InfoService.class.getName());

  private static final ObjectMapper objectMapper =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private final ElasticConfig esService;

  @Value("${channelfinder.version:unknown}")
  private String version;

  public InfoService(ElasticConfig esService) {
    this.esService = esService;
  }

  public String info() {
    Map<String, Object> cfServiceInfo = new LinkedHashMap<>();
    cfServiceInfo.put("name", "ChannelFinder Service");
    cfServiceInfo.put("version", version);

    Map<String, String> elasticInfo = new LinkedHashMap<>();
    try {
      var client = esService.getSearchClient();
      var response = client.info();
      elasticInfo.put("status", "Connected");
      elasticInfo.put("clusterName", response.clusterName());
      elasticInfo.put("clusterUuid", response.clusterUuid());
      elasticInfo.put("version", response.version().number());
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to retrieve Elasticsearch info", e);
      elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
    }
    cfServiceInfo.put("elastic", elasticInfo);

    try {
      return objectMapper.writeValueAsString(cfServiceInfo);
    } catch (JacksonException e) {
      logger.log(Level.WARNING, "Failed to serialize ChannelFinder service info", e);
      return "Failed to gather ChannelFinder service info";
    }
  }
}
