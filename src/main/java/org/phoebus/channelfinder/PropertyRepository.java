package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.phoebus.channelfinder.XmlProperty.OnlyNameOwnerXmlProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Configuration
public class PropertyRepository implements CrudRepository<XmlProperty, String> {
    static Logger log = Logger.getLogger(PropertyRepository.class.getName());

    @Value("${elasticsearch.property.index:cf_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:cf_property}")
    private String ES_PROPERTY_TYPE;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Autowired
    ChannelRepository channelRepository;

    ObjectMapper objectMapper = new ObjectMapper().addMixIn(XmlProperty.class, OnlyNameOwnerXmlProperty.class);

    /**
     * create a new property using the given XmlProperty
     * 
     * @param <S> extends XmlProperty
     * @param property - property to be created
     * @return the created property
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> S index(XmlProperty property) {
//        try {
//            IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
//                    .id(property.getName())
//                    .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
//            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//            /// verify the creation of the property
//            Result result = indexResponse.getResult();
//            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
//                return (S) findById(property.getName()).get();
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to index property: " + property.toLog(), e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to index property: " + property, null);
//        }
        return null;
    }

    /**
     * create new properties using the given XmlProperties
     * 
     * @param <S> extends XmlProperty
     * @param properties - properties to be created
     * @return the created properties
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> Iterable<S> indexAll(Iterable<XmlProperty> properties) {
//        try {
//            BulkRequest bulkRequest = new BulkRequest();
//            for (XmlProperty property : properties) {
//                IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
//                        .id(property.getName())
//                        .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
//                bulkRequest.add(indexRequest);
//            }
//
//            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            /// verify the creation of the properties
//            if (bulkResponse.hasFailures()) {
//                // Failed to create all the properties
//            } else {
//                List<String> createdPropertyIds = new ArrayList<String>();
//                for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                    Result result = bulkItemResponse.getResponse().getResult();
//                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
//                        createdPropertyIds.add(bulkItemResponse.getId());
//                    }
//                }
//                return (Iterable<S>) findAllById(createdPropertyIds);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to index properties: " + properties, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to index properties: " + properties, null);      
//        }
        return null;
    }

    /**
     * update/save property using the given XmlProperty
     * 
     * @param <S> extends XmlProperty
     * @param propertyName - name of property to be created
     * @param property - property to be created
     * @return the updated/saved property
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlProperty> S save(String propertyName, S property) {

        try {
            IndexRequest request = IndexRequest.of(i -> i.index(ES_PROPERTY_INDEX)
                                                         .id(propertyName)
                                                         .document(property)
                                                         .refresh(Refresh.True));

            IndexResponse response = client.index(request);
            /// verify the creation of the property
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created property " + property);
                return property;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index property " + property.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index property: " + property, null);
        }
        return null;
    }

    /**
     * 
     * @param <S> extends XmlProperty
     */
    @Override
    public <S extends XmlProperty> S save(S property) {
        return save(property.getName(),property);
    }

    /**
     * update/save properties using the given XmlProperties
     * 
     * @param <S> extends XmlProperty
     * @param properties - properties to be created
     * @return the updated/saved properties
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlProperty> Iterable<S> saveAll(Iterable<S> properties) {
//        RestHighLevelClient client = esService.getNewClient();
//        BulkRequest bulkRequest = new BulkRequest();
//        try {
//            for (XmlProperty property : properties) {
//                UpdateRequest updateRequest = new UpdateRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName());
//
//                Optional<XmlProperty> existingProperty = findById(property.getName());
//                if (existingProperty.isPresent()) {
//                    updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON);
//                } else {
//                    IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
//                            .id(property.getName())
//                            .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
//                    updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON).upsert(indexRequest);
//                }
//                bulkRequest.add(updateRequest);
//            }
//
//            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                // Failed to create/update all the properties
//                throw new Exception();
//            } else {
//                List<String> createdPropertyIds = new ArrayList<String>();
//                for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                    Result result = bulkItemResponse.getResponse().getResult();
//                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
//                        createdPropertyIds.add(bulkItemResponse.getId());
//                    }
//                }
//                return (Iterable<S>) findAllById(createdPropertyIds);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to update/save properties: " + properties, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to update/save properties: " + properties, null);
//        }
        return null;
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
     * @param propertyName - id of property to be found
     * @param withChannels - whether channels should be included
     * @return the found property
     */
    public Optional<XmlProperty> findById(String propertyName, boolean withChannels) {
        GetResponse<XmlProperty> response;
        try {
            response = client.get(g -> g.index(ES_PROPERTY_INDEX).id(propertyName), XmlProperty.class);

            if (response.found()) {
                XmlProperty property = response.source();
                log.info("property name " + property.getName());
                // TODO if (withChannels)
                return Optional.of(property);
            } else {
                log.info("property not found");
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find property " + propertyName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find property: " + propertyName, null);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_PROPERTY_INDEX).id(id);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to check if property " + id + " exists", e);
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
//        RestHighLevelClient client = esService.getSearchClient();
//
//        SearchRequest searchRequest = new SearchRequest();
//        searchRequest.indices(ES_PROPERTY_INDEX);
//        searchRequest.types(ES_PROPERTY_TYPE);
//
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        // TODO use of scroll will be necessary
//        searchSourceBuilder.size(10000);
//        searchSourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));
//        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchAllQuery()));
//
//        try {
//            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//            if (searchResponse.status().equals(RestStatus.OK)) {
//                List<XmlProperty> result = new ArrayList<XmlProperty>();
//                for (SearchHit hit : searchResponse.getHits()) {
//                    result.add(objectMapper.readValue(hit.getSourceRef().streamInput(), XmlProperty.class));
//                }
//                return result;
//            }
//        } catch (IOException e) {
//            log.log(Level.SEVERE, "Failed to find all properties", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to find all properties", null);
//        }
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
//        MultiGetRequest request = new MultiGetRequest();
//
//        for (String propertyId : propertyIds) {
//            request.add(new MultiGetRequest.Item(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyId));
//        }
//        try {
//            List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
//            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
//            for (MultiGetItemResponse multiGetItemResponse : response) {
//                if (!multiGetItemResponse.isFailed()) {
//                    foundProperties.add(objectMapper.readValue(
//                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlProperty.class));
//                } 
//            }
//            return foundProperties;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to find all properties: " + propertyIds, e);
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
//                    "Failed to find all properties: " + propertyIds, null);

        return null;
    }

    @Override
    public long count() {
        // NOT USED
        return 0;
    }

    /**
     * delete the given property by property name
     * 
     * @param propertyName - name of property to be deleted
     */
    @Override
    public void deleteById(String propertyName) {

//        RestHighLevelClient client = esService.getNewClient();
//        DeleteRequest request = new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyName);
//        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//
//        try {
//            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
//            Result result = response.getResult();
//            if (!result.equals(Result.DELETED)) 
//                throw new Exception();
//            // delete property from channels
//            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
//            params.add(propertyName,"*");
//            List<XmlChannel> chans = channelRepository.search(params);
//            if(!chans.isEmpty()) {
//                chans.forEach(chan -> chan.removeProperty(new XmlProperty(propertyName, "")));
//                channelRepository.indexAll(chans);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to delete property: " + propertyName, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to delete property: " + propertyName, null);
//        }
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

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // TODO Auto-generated method stub
        
    }

}
