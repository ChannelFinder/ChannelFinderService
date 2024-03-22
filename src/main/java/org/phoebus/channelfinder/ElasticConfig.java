/**
 * 
 */
package org.phoebus.channelfinder;

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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * @author Kunal Shroff {@literal <shroffk@bnl.gov>}
 *
 */

@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@ComponentScan(basePackages = { "org.phoebus.channelfinder" })
@PropertySource(value = "classpath:application.properties")
public class ElasticConfig implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    private ElasticsearchClient searchClient;
    private ElasticsearchClient indexClient;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.host_urls:http://localhost:9200}")
    private HttpHost[] httpHosts;
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
    @Value("${elasticsearch.query.size}")
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

    ObjectMapper objectMapper = new ObjectMapper()
            .addMixIn(Tag.class, Tag.OnlyTag.class)
            .addMixIn(Property.class, Property.OnlyProperty.class);

    private static ElasticsearchClient createClient(ElasticsearchClient currentClient, ObjectMapper objectMapper,
                                                    HttpHost[] httpHosts, String createIndices, ElasticConfig config) {
        ElasticsearchClient client;
        if (currentClient == null) {
            // Create the low-level client
            RestClientBuilder clientBuilder = RestClient.builder(httpHosts);
            // Configure authentication
            if (!config.authorizationHeader.isEmpty()) {
                clientBuilder.setDefaultHeaders(new Header[] {new BasicHeader("Authorization", config.authorizationHeader)});
                if (!config.username.isEmpty() || !config.password.isEmpty()) {
                    logger.warning("elasticsearch.authorization_header is set, ignoring elasticsearch.username and elasticsearch.password.");
                }
            } else if (!config.username.isEmpty() || !config.password.isEmpty()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(config.username, config.password));
                clientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
            }
            RestClient httpClient = clientBuilder.build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper(objectMapper));

            client = new ElasticsearchClient(transport);
        } else {
            client = currentClient;
        }
        if (Boolean.parseBoolean(createIndices) && esInitialized.compareAndSet(false, true)) {
            config.elasticIndexValidation(client);
        }
        return client;

    }

    private HttpHost[] getHttpHosts() {
        HttpHost[] localHttpHosts = this.httpHosts;
        boolean hostIsDefault = host.equals("localhost");
        boolean hostUrlsIsDefault = localHttpHosts.length == 1 && localHttpHosts[0].equals(new HttpHost("http://localhost:9200"));
        boolean portIsDefault = (port == 9200);
        if (hostUrlsIsDefault) {
            if (!hostIsDefault || !portIsDefault) {
                logger.warning("Specifying elasticsearch.network.host and elasticsearch.http.port is deprecated, please consider using elasticsearch.host_urls instead.");
                localHttpHosts = new HttpHost[] {new HttpHost("http://" + host + ":" + port)};
            }
        } else {
            if (!hostIsDefault) {
                logger.warning("Only one of elasticsearch.host_urls and elasticsearch.network.host can be set, ignoring elasticsearch.network.host.");
            }
            if (!portIsDefault) {
                logger.warning("Only one of elasticsearch.host_urls and elasticsearch.http.port can be set, ignoring elasticsearch.http.port.");
            }
        }
        return localHttpHosts;
    }

    @Bean({ "searchClient" })
    public ElasticsearchClient getSearchClient() {
        searchClient = createClient(searchClient, objectMapper, getHttpHosts(), createIndices, this);
        return searchClient;
    }

    @Bean({ "indexClient" })
    public ElasticsearchClient getIndexClient() {
        indexClient = createClient(indexClient, objectMapper, getHttpHosts(), createIndices, this);
        return indexClient;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.log(Level.INFO, "Initializing a new Transport clients.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.log(Level.INFO, "Closing the default Transport clients.");
        if (searchClient != null)
            searchClient.shutdown();
        if (indexClient != null)
            indexClient.shutdown();
    }

    /**
     * Create the olog indices and templates if they don't exist
     * @param client client connected to elasticsearch
     */
    void elasticIndexValidation(ElasticsearchClient client) {
        validateIndex(client, ES_CHANNEL_INDEX, "/channel_mapping.json");
        validateIndex(client, ES_TAG_INDEX, "/tag_mapping.json");
        validateIndex(client, ES_PROPERTY_INDEX, "/properties_mapping.json");
    }

    private void validateIndex(ElasticsearchClient client, String esIndex, String mapping) {

        // ChannelFinder Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream(mapping)) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(esIndex)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(esIndex).withJson(is)));
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.CREATED_INDEX_ACKNOWLEDGED, esIndex, result.acknowledged()));
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.FAILED_TO_CREATE_INDEX, esIndex), e);
        }
    }
}
