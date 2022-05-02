package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.phoebus.channelfinder.XmlProperty.OnlyNameOwnerXmlProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
@Configuration
public class PropertyRepository implements CrudRepository<XmlProperty, String> {
    static Logger log = Logger.getLogger(PropertyRepository.class.getName());

    @Value("${elasticsearch.property.index:cf_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:cf_property}")
    private String ES_PROPERTY_TYPE;

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    ChannelRepository channelRepository;

    ObjectMapper objectMapper = new ObjectMapper().addMixIn(XmlProperty.class, OnlyNameOwnerXmlProperty.class);

    /**
     * create a new property using the given XmlProperty
     * 
     * @param property - property to be created
     * @return the created property
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> S index(XmlProperty property) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                    .id(property.getName())
                    .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the property
            Result result = indexResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                return (S) findById(property.getName()).get();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index property: " + property.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index property: " + property, null);
        }
        return null;
    }

    /**
     * create new properties using the given XmlProperties
     * 
     * @param properties - properties to be created
     * @return the created properties
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> Iterable<S> indexAll(Iterable<XmlProperty> properties) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlProperty property : properties) {
                IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                        .id(property.getName())
                        .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            /// verify the creation of the properties
            if (bulkResponse.hasFailures()) {
                // Failed to create all the properties
            } else {
                List<String> createdPropertyIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                        createdPropertyIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdPropertyIds);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index properties: " + properties, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index properties: " + properties, null);      
        }
        return null;
    }

    /**
     * update/save property using the given XmlProperty
     * 
     * @param property - property to be created
     * @return the updated/saved property
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> S save(String propertyName, S property) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            UpdateRequest updateRequest;
            Optional<XmlProperty> existingProperty = findById(propertyName);
            boolean present = existingProperty.isPresent();
            if(present) {
                deleteById(propertyName);
            }
            updateRequest = new UpdateRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName());
            IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                    .id(property.getName())
                    .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
            updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON).upsert(indexRequest);
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            /// verify the updating/saving of the property
            Result result = updateResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                // client.get(, options)
                return (S) findById(property.getName()).get();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update/save property: " + property.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save property: " + property, null);
        }
        return null;
    }

    @Override
    public <S extends XmlProperty> S save(S property) {
        return save(property.getName(),property);
    }

    /**
     * update/save properties using the given XmlProperties
     * 
     * @param properties - properties to be created
     * @return the updated/saved properties
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlProperty> Iterable<S> saveAll(Iterable<S> properties) {
        RestHighLevelClient client = esService.getIndexClient();
        BulkRequest bulkRequest = new BulkRequest();
        try {
            for (XmlProperty property : properties) {
                UpdateRequest updateRequest = new UpdateRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName());

                Optional<XmlProperty> existingProperty = findById(property.getName());
                if (existingProperty.isPresent()) {
                    updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                } else {
                    IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                            .id(property.getName())
                            .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                    updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON).upsert(indexRequest);
                }
                bulkRequest.add(updateRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                // Failed to create/update all the properties
                throw new Exception();
            } else {
                List<String> createdPropertyIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                        createdPropertyIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdPropertyIds);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update/save properties: " + properties, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save properties: " + properties, null);
        }
    }

    /**
     * find property using the given property id
     * 
     * @param propertyId - id of property to be found
     * @return the found property
     */
    @Override
    public Optional<XmlProperty> findById(String propertyId) {
        return findById(propertyId, false);
    }

    /**
     * find property using the given property id
     * 
     * @param propertyId - id of property to be found
     * @param withChannels - whether channels should be included
     * @return the found property
     */
    public Optional<XmlProperty> findById(String propertyId, boolean withChannels) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyId);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlProperty property = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(), XmlProperty.class);
                if(withChannels) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
                    params.add(propertyId,"*");
                    List<XmlChannel> chans = channelRepository.search(params);
                    chans.forEach(chan -> chan.setTags(new ArrayList<XmlTag>()));
                    XmlProperty p = null;
                    for(XmlChannel chan: chans) {
                        for(XmlProperty prop: chan.getProperties())
                        {
                            if(prop.getName().equals(propertyId))
                                p = prop;
                        }
                        chan.setProperties(Arrays.asList(p));
                    }
                    property.setChannels(chans);
                }
                return Optional.of(property);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to find property: " + propertyId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find property: " + propertyId, null);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {

        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, id);
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");
        try {
            return client.exists(getRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to check if property exists by id: " + id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check if property exists by id: " + id, null); 
        }
    }

    /**
     * find all properties 
     * 
     * @return the found properties
     */
    @Override
    public Iterable<XmlProperty> findAll() {
        RestHighLevelClient client = esService.getSearchClient();

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_PROPERTY_INDEX);
        searchRequest.types(ES_PROPERTY_TYPE);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // TODO use of scroll will be necessary
        searchSourceBuilder.size(10000);
        searchSourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchAllQuery()));

        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            if (searchResponse.status().equals(RestStatus.OK)) {
                List<XmlProperty> result = new ArrayList<XmlProperty>();
                for (SearchHit hit : searchResponse.getHits()) {
                    result.add(objectMapper.readValue(hit.getSourceRef().streamInput(), XmlProperty.class));
                }
                return result;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to find all properties", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to find all properties", null);
        }
        return null;
    }

    /**
     * find properties using the given property ids
     * 
     * @param propertyIds - ids of properties to be found
     * @return the found properties
     */
    @Override
    public Iterable<XmlProperty> findAllById(Iterable<String> propertyIds) {
        MultiGetRequest request = new MultiGetRequest();

        for (String propertyId : propertyIds) {
            request.add(new MultiGetRequest.Item(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyId));
        }
        try {
            List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundProperties.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlProperty.class));
                } 
            }
            return foundProperties;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to find all properties: " + propertyIds, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find all properties: " + propertyIds, null);
        }
    }

    @Override
    public long count() {
        // NOT USED
        return 0;
    }

    /**
     * delete the given property by property name
     * 
     * @param property - property to be deleted
     */
    @Override
    public void deleteById(String propertyName) {
        RestHighLevelClient client = esService.getIndexClient();
        DeleteRequest request = new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyName);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            Result result = response.getResult();
            if (!result.equals(Result.DELETED)) 
                throw new Exception();
            // delete property from channels
            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add(propertyName,"*");
            List<XmlChannel> chans = channelRepository.search(params);
            if(!chans.isEmpty()) {
                chans.forEach(chan -> chan.removeProperty(new XmlProperty(propertyName, "")));
                channelRepository.indexAll(chans);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to delete property: " + propertyName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete property: " + propertyName, null);
        }
    }

    /**
     * delete the given property
     * 
     * @param property - property to be deleted
     */
    @Override
    public void delete(XmlProperty property) {
        deleteById(property.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlProperty> entities) {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

}
