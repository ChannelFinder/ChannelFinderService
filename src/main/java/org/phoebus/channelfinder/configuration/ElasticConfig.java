/** */
package org.phoebus.channelfinder.configuration;

/*
 * #%L
 * ChannelFinder Directory Service
 * %%
 * Copyright (C) 2010 - 2015 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * %%
 * Copyright (C) 2010 - 2012 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 * #L%
 */

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.phoebus.channelfinder.common.TextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Kunal Shroff {@literal <shroffk@bnl.gov>}
 */
@Configuration
@ComponentScan(basePackages = {"org.phoebus.channelfinder"})
@PropertySource(value = "classpath:application.properties")
public class ElasticConfig {

  private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

  // Used to retrieve the auto-configured ElasticsearchClient lazily in @PostConstruct,
  // avoiding a circular dependency (this bean provides RestClient → auto-config builds
  // ElasticsearchClient from it).
  @Autowired private ApplicationContext applicationContext;

  @Value("${elasticsearch.network.host:localhost}")
  private String host;

  @Value("${elasticsearch.host_urls:http://localhost:9200}")
  private String[] httpHostUrls;

  @Value("${elasticsearch.http.port:9200}")
  private int port;

  @Value("${elasticsearch.authorization.header:}")
  private String authorizationHeader;

  @Value("${elasticsearch.authorization.username:}")
  private String username;

  @Value("${elasticsearch.authorization.password:}")
  private String password;

  @Value("${elasticsearch.create.indices:true}")
  private String createIndices;

  @Value("${elasticsearch.tag.index:cf_tags}")
  private String ES_TAG_INDEX;

  @Value("${elasticsearch.property.index:cf_properties}")
  private String ES_PROPERTY_INDEX;

  @Value("${elasticsearch.channel.index:channelfinder}")
  private String ES_CHANNEL_INDEX;

  @Value("${elasticsearch.query.size:10000}")
  private int ES_QUERY_SIZE;

  public String getES_TAG_INDEX() {
    return this.ES_TAG_INDEX;
  }

  public String getES_PROPERTY_INDEX() {
    return this.ES_PROPERTY_INDEX;
  }

  public String getES_CHANNEL_INDEX() {
    return this.ES_CHANNEL_INDEX;
  }

  public int getES_QUERY_SIZE() {
    return this.ES_QUERY_SIZE;
  }

  public int getES_MAX_RESULT_WINDOW_SIZE() {
    return ES_QUERY_SIZE;
  }

  public ElasticsearchClient getElasticsearchClient() {
    return applicationContext.getBean(ElasticsearchClient.class);
  }

  /**
   * Provides the low-level Elasticsearch {@link RestClient} built from the {@code elasticsearch.*}
   * connection properties. Spring Boot's {@code ElasticsearchClientAutoConfiguration} detects this
   * bean and uses it to auto-configure {@code ElasticsearchTransport} and {@code
   * ElasticsearchClient}, which in turn activates the {@code /actuator/health} Elasticsearch
   * indicator.
   */
  @Bean
  public RestClient restClient() {
    RestClientBuilder clientBuilder = RestClient.builder(getHttpHosts());
    if (!authorizationHeader.isEmpty()) {
      clientBuilder.setDefaultHeaders(
          new Header[] {new BasicHeader("Authorization", authorizationHeader)});
      if (!username.isEmpty() || !password.isEmpty()) {
        logger.warning(
            "elasticsearch.authorization.header is set, ignoring"
                + " elasticsearch.authorization.username and elasticsearch.authorization.password.");
      }
    } else if (!username.isEmpty() || !password.isEmpty()) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(username, password));
      clientBuilder.setHttpClientConfigCallback(
          httpClientBuilder ->
              httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }
    return clientBuilder.build();
  }

  private HttpHost[] getHttpHosts() {
    boolean hostIsDefault = host.equals("localhost");
    boolean hostUrlsIsDefault =
        httpHostUrls.length == 1 && httpHostUrls[0].equals("http://localhost:9200");
    boolean portIsDefault = (port == 9200);
    if (hostUrlsIsDefault && (!hostIsDefault || !portIsDefault)) {
      logger.warning(
          "Specifying elasticsearch.network.host and elasticsearch.http.port is deprecated,"
              + " please consider using elasticsearch.host_urls instead.");
      return new HttpHost[] {new HttpHost(host, port)};
    } else {
      if (!hostIsDefault) {
        logger.warning(
            "Only one of elasticsearch.host_urls and elasticsearch.network.host can be set,"
                + " ignoring elasticsearch.network.host.");
      }
      if (!portIsDefault) {
        logger.warning(
            "Only one of elasticsearch.host_urls and elasticsearch.http.port can be set,"
                + " ignoring elasticsearch.http.port.");
      }
      return Arrays.stream(httpHostUrls).map(HttpHost::create).toArray(HttpHost[]::new);
    }
  }

  @PostConstruct
  public void init() {
    if (Boolean.parseBoolean(createIndices)) {
      elasticIndexValidation(applicationContext.getBean(ElasticsearchClient.class));
    }
  }

  /**
   * Create the ChannelFinder indices and templates if they don't exist
   *
   * @param client client connected to elasticsearch
   */
  public void elasticIndexValidation(ElasticsearchClient client) {
    validateIndex(client, ES_CHANNEL_INDEX, "/channel_mapping.json");
    validateIndex(client, ES_TAG_INDEX, "/tag_mapping.json");
    validateIndex(client, ES_PROPERTY_INDEX, "/properties_mapping.json");
  }

  private void validateIndex(ElasticsearchClient client, String esIndex, String mapping) {

    // ChannelFinder Index
    try (InputStream is = ElasticConfig.class.getResourceAsStream(mapping)) {
      BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(esIndex)));
      if (!exits.value()) {
        CreateIndexResponse result =
            client
                .indices()
                .create(
                    CreateIndexRequest.of(
                        c ->
                            c.index(esIndex)
                                .withJson(is)
                                .settings(
                                    IndexSettings.of(
                                        builder ->
                                            builder.maxResultWindow(
                                                getES_MAX_RESULT_WINDOW_SIZE())))));
        logger.log(
            Level.INFO,
            () ->
                MessageFormat.format(
                    TextUtil.CREATED_INDEX_ACKNOWLEDGED, esIndex, result.acknowledged()));
      }
      PutIndicesSettingsResponse response =
          client
              .indices()
              .putSettings(
                  PutIndicesSettingsRequest.of(
                      builder ->
                          builder
                              .index(esIndex)
                              .settings(
                                  IndexSettings.of(
                                      i -> i.maxResultWindow(getES_MAX_RESULT_WINDOW_SIZE())))));
      logger.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.UPDATE_INDEX_ACKNOWLEDGED, esIndex, response.acknowledged()));
    } catch (IOException e) {
      logger.log(Level.WARNING, MessageFormat.format(TextUtil.FAILED_TO_CREATE_INDEX, esIndex), e);
    }
  }
}
