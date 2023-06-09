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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
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
@ComponentScan(basePackages = { "org.phoebus.channelfinder" })
@PropertySource(value = "classpath:application.properties")
public class ElasticConfig implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    private ElasticsearchClient searchClient;
    private ElasticsearchClient indexClient;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
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
    private String ES_QUERY_SIZE;

    ObjectMapper objectMapper = new ObjectMapper()
            .addMixIn(XmlTag.class, XmlTag.OnlyXmlTag.class)
            .addMixIn(XmlProperty.class, XmlProperty.OnlyXmlProperty.class);


    @Bean({ "searchClient" })
    public ElasticsearchClient getSearchClient() {
        if (searchClient == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper(objectMapper));

            searchClient = new ElasticsearchClient(transport);
        }
        esInitialized.set(!Boolean.parseBoolean(createIndices));
        if (esInitialized.compareAndSet(false, true)) {
            elasticIndexValidation(searchClient);
        }
        return searchClient;
    }

    @Bean({ "indexClient" })
    public ElasticsearchClient getIndexClient() {
        if (indexClient == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper(objectMapper));
            indexClient = new ElasticsearchClient(transport);
        }
        esInitialized.set(!Boolean.parseBoolean(createIndices));
        if (esInitialized.compareAndSet(false, true)) {
            elasticIndexValidation(indexClient);
        }
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
     * @param client
     */
    void elasticIndexValidation(ElasticsearchClient client) {
        // ChannelFinder Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/channel_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_CHANNEL_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_CHANNEL_INDEX).withJson(is)));
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.CREATED_INDEX_ACKNOWLEDGED, ES_CHANNEL_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.FAILED_TO_CREATE_INDEX, ES_CHANNEL_INDEX), e);
        }

        // ChannelFinder tag Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/tag_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_TAG_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_TAG_INDEX).withJson(is)));
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.CREATED_INDEX_ACKNOWLEDGED, ES_TAG_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.FAILED_TO_CREATE_INDEX, ES_TAG_INDEX), e);
        }

        // ChannelFinder property Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/properties_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_PROPERTY_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_PROPERTY_INDEX).withJson(is)));
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.CREATED_INDEX_ACKNOWLEDGED, ES_PROPERTY_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.FAILED_TO_CREATE_INDEX, ES_PROPERTY_INDEX), e);
        }
    }
}
