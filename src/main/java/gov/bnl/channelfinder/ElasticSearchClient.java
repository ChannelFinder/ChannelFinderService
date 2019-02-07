/**
 * 
 */
package gov.bnl.channelfinder;

import java.io.IOException;

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
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Kunal Shroff {@literal <shroffk@bnl.gov>}
 *
 */

@Configuration
@PropertySource("classpath:/elasticsearch.properties")
public class ElasticSearchClient implements ServletContextListener {

    private static Logger log = Logger.getLogger(ElasticSearchClient.class.getCanonicalName());

    private static Settings settings;

    private RestHighLevelClient searchClient;
    private RestHighLevelClient indexClient;

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
    @Value("${network.host:localhost}")
    private String host;
    @Value("${http.port:9200}")
    private int port;

    public RestHighLevelClient getSearchClient() {
        if(searchClient == null) {
            searchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
        }
        return searchClient;
    }

    public RestHighLevelClient getIndexClient() {
        if(indexClient == null) {
            indexClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
        }
        return indexClient;
    }

    /**
     * Returns a new {@link TransportClient} using the default settings
     * **IMPORTANT** it is the responsibility of the caller to close this client
     * 
     * @return es transport client
     */
    @SuppressWarnings("resource")
    public RestHighLevelClient getNewClient() {
        try {
            RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            return client;
        } catch (ElasticsearchException e) {
            log.severe(e.getDetailedMessage());
            return null;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing a new Transport clients.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Closeing the default Transport clients.");
        try {
            searchClient.close();
            indexClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
