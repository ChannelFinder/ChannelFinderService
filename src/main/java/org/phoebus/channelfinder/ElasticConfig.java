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

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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

    private static Logger log = Logger.getLogger(ElasticConfig.class.getCanonicalName());

    private ElasticsearchClient searchClient;
    private ElasticsearchClient indexClient;

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    @Value("${elasticsearch.tag.index:cf_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:cf_tag}")
    private String ES_TAG_TYPE;
    @Value("${elasticsearch.property.index:cf_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:cf_property}")
    private String ES_PROPERTY_TYPE;
    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;
    @Value("${elasticsearch.channel.type:cf_channel}")
    private String ES_CHANNEL_TYPE;

    @Value("${elasticsearch.query.size}")
    private String ES_QUERY_SIZE;

    @Bean({ "searchClient" })
    public ElasticsearchClient getSearchClient() {
        if (searchClient == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper());

            searchClient = new ElasticsearchClient(transport);
        }
        return searchClient;
    }

    @Bean({ "indexClient" })
    public ElasticsearchClient getIndexClient() {
        if (indexClient == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(httpClient, new JacksonJsonpMapper());
            indexClient = new ElasticsearchClient(transport);
        }
        return indexClient;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing a new Transport clients.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Closing the default Transport clients.");
        if (searchClient != null)
            searchClient.shutdown();
        if (indexClient != null)
            indexClient.shutdown();
    }

}
