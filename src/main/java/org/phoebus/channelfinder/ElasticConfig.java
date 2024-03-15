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
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
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
    @Value("${elasticsearch.http.port:9200}")
    private int port;
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
                                                    String host, int port, String createIndices, ElasticConfig config) {
        ElasticsearchClient client;
        if (currentClient == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

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
    @Bean({ "searchClient" })
    public ElasticsearchClient getSearchClient() {
        searchClient = createClient(searchClient, objectMapper, host, port, createIndices, this);
        return searchClient;
    }

    @Bean({ "indexClient" })
    public ElasticsearchClient getIndexClient() {
        indexClient = createClient(indexClient, objectMapper, host, port, createIndices, this);
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
