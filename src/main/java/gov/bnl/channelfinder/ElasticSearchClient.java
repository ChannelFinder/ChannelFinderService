/**
 * 
 */
package gov.bnl.channelfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Kunal Shroff {@literal <shroffk@bnl.gov>}
 *
 */

@Configuration
@PropertySource("classpath:/application.properties")
public class ElasticSearchClient implements ServletContextListener {

    private static Logger log = Logger.getLogger(ElasticSearchClient.class.getCanonicalName());

    private RestHighLevelClient searchClient;
    private RestHighLevelClient indexClient;

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

    public RestHighLevelClient getSearchClient() {
        if(searchClient == null) {
            searchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            elasticIndexValidation(searchClient);
        }
        return searchClient;
    }

    public RestHighLevelClient getIndexClient() {
        if(indexClient == null) {
            indexClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            elasticIndexValidation(indexClient);
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
            elasticIndexValidation(client);
            return client;
        } catch (ElasticsearchException e) {
            log.log(Level.SEVERE, "failed to create elastic client", e.getDetailedMessage());
            return null;
        }
    }

    /**
     * Checks for the existence of the elastic indices needed for channelfinder and creates
     * them with the appropriate mapping is they are missing.
     * 
     * @param indexClient the elastic client instance used to validate and create
     *                    channelfinder indices
     */
	private synchronized void elasticIndexValidation(RestHighLevelClient indexClient) {
		
		// Create/migrate the tag index
		try {
			if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_TAG_INDEX), RequestOptions.DEFAULT)) {
				CreateIndexRequest createRequest = new CreateIndexRequest(ES_TAG_INDEX);
				ObjectMapper mapper = new ObjectMapper();
				InputStream is = ElasticSearchClient.class.getResourceAsStream("/tag_mapping.json");
				Map<String, String> jsonMap = mapper.readValue(is, Map.class);
				createRequest.mapping(ES_TAG_TYPE, jsonMap);

				indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
				log.info("Successfully created index: " + ES_TAG_INDEX);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to create index " + ES_TAG_INDEX, e);
		}
		// Create/migrate the properties index
		try {
			if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_PROPERTY_INDEX),
					RequestOptions.DEFAULT)) {
				CreateIndexRequest createRequest = new CreateIndexRequest(ES_PROPERTY_INDEX);
				ObjectMapper mapper = new ObjectMapper();
				InputStream is = ElasticSearchClient.class.getResourceAsStream("/properties_mapping.json");
				Map<String, String> jsonMap = mapper.readValue(is, Map.class);
				createRequest.mapping(ES_PROPERTY_TYPE, jsonMap);

				indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
				log.info("Successfully created index: " + ES_PROPERTY_INDEX);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to create index " + ES_PROPERTY_INDEX, e);
		}
		// Create/migrate the channel index
		try {
			if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_CHANNEL_INDEX),
					RequestOptions.DEFAULT)) {
				CreateIndexRequest createRequest = new CreateIndexRequest(ES_CHANNEL_INDEX);
				ObjectMapper mapper = new ObjectMapper();
				InputStream is = ElasticSearchClient.class.getResourceAsStream("/channel_mapping.json");
				Map<String, String> jsonMap = mapper.readValue(is, Map.class);
				createRequest.mapping(ES_CHANNEL_TYPE, jsonMap);

				indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
				log.info("Successfully created index: " + ES_CHANNEL_INDEX);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to create index " + ES_CHANNEL_INDEX, e);
		}
		

		Map<String, Object> map = new HashMap<>();
		map.put("index.max_result_window", ES_QUERY_SIZE);

		try {
			UpdateSettingsRequest updateSettings = new UpdateSettingsRequest(ES_TAG_INDEX, ES_PROPERTY_INDEX,
					ES_CHANNEL_INDEX);
			updateSettings.settings(map);
			AcknowledgedResponse updateSettingsResponse = indexClient.indices().putSettings(updateSettings,
					RequestOptions.DEFAULT);
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to set max_result_window setting on indices", e);
		}
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing a new Transport clients.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Closing the default Transport clients.");
        try {
            searchClient.close();
            indexClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
