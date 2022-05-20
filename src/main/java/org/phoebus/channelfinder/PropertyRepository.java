package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Configuration
public class PropertyRepository implements CrudRepository<XmlProperty, String> {
    static Logger log = Logger.getLogger(PropertyRepository.class.getName());

    @Value("${elasticsearch.property.index:cf_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

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
    public XmlProperty index(XmlProperty property) {
        try {
            IndexRequest request = IndexRequest.of(i -> i.index(ES_PROPERTY_INDEX)
                    .id(property.getName())
                    .document(property)
                    .refresh(Refresh.True));

            IndexResponse response = client.index(request);
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created property " + property);
                return property;
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
    public List<XmlProperty> indexAll(List<XmlProperty> properties) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlProperty property : properties) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(ES_PROPERTY_INDEX)
                            .id(property.getName())
                            .document(property)
                    )
            ).refresh(Refresh.True);
        }

        BulkResponse result = null;
        try {
            result = client.bulk(br.build());
            // Log errors, if any
            if (result.errors()) {
                log.severe("Bulk had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.severe(item.error().reason());
                    }
                }
                // TODO cleanup? or throw exception?
            } else {
                return findAllById(properties.stream().map(XmlProperty::getName).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to index properties " + properties, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index properties: " + properties, null);

        }
        return null;
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
    }

    /**
     * update/save property using the given XmlProperty
     *
     * @param <S>          extends XmlProperty
     * @param propertyName - name of property to be created
     * @param property     - property to be created
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
     * @param <S> extends XmlProperty
     */
    @Override
    public <S extends XmlProperty> S save(S property) {
        return save(property.getName(), property);
    }

    /**
     * update/save properties using the given XmlProperties
     *
     * @param <S>        extends XmlProperty
     * @param properties - properties to be created
     * @return the updated/saved properties
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlProperty> Iterable<S> saveAll(Iterable<S> properties) {
        List<String> ids = StreamSupport.stream(properties.spliterator(), false)
                .map(XmlProperty::getName).collect(Collectors.toList());

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (XmlProperty property : properties) {
            br.operations(op -> op.index(i -> i.index(ES_PROPERTY_INDEX).id(property.getName()).document(property)));
        }

        BulkResponse result = null;
        try {
            result = client.bulk(br.refresh(Refresh.True).build());
            // Log errors, if any
            if (result.errors()) {
                log.severe("Bulk had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.severe(item.error().reason());
                    }
                }
                // TODO cleanup? or throw exception?
            } else {
                return (Iterable<S>) findAllById(ids);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                if(withChannels) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
                    params.add(property.getName(), "*");
                    property.setChannels(channelRepository.search(params));
                }
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
        try {
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(ES_PROPERTY_INDEX)
                    .query(new MatchAllQuery.Builder().build()._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlProperty> response = client.search(searchBuilder.build(), XmlProperty.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all tags", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all tags", null);
        }
    }

    /**
     * find properties using the given property ids
     *
     * @param propertyIds - ids of properties to be found
     * @return the found properties
     */
    @Override
    public List<XmlProperty> findAllById(Iterable<String> propertyIds) {
        try {
            List<String> ids = StreamSupport.stream(propertyIds.spliterator(), false).collect(Collectors.toList());

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(ES_PROPERTY_INDEX)
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlProperty> response = client.search(searchBuilder.build(), XmlProperty.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all properties", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all properties", null);
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
     * @param propertyName - name of property to be deleted
     */
    @Override
    public void deleteById(String propertyName) {
        try {
            DeleteResponse response = client
                    .delete(i -> i.index(ES_PROPERTY_INDEX).id(propertyName).refresh(Refresh.True));
            BulkRequest.Builder br = new BulkRequest.Builder().refresh(Refresh.True);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add(propertyName, "*");
            List<XmlChannel> channels = channelRepository.search(params);
            while (channels.size() > 0) {
                for (XmlChannel channel : channels) {
                    channel.removeProperty(
                            channel.getProperties().stream().filter(prop -> propertyName.equalsIgnoreCase(prop.getName())).findAny().get());
                    br.operations(op -> op.update(
                            u -> u.index(ES_CHANNEL_INDEX)
                                    .id(channel.getName())
                                    .action(a -> a.doc(channel))));
                }
                try {
                    BulkResponse result = client.bulk(br.build());
                    // Log errors, if any
                    if (result.errors()) {
                        log.severe("Bulk had errors");
                        for (BulkResponseItem item : result.items()) {
                            if (item.error() != null) {
                                log.severe(item.error().reason());
                            }
                        }
                    } else {
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to delete property " + propertyName, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete property: " + propertyName, null);

                }
                params.set("~search_after", channels.get(channels.size() - 1).getName());
                channels = channelRepository.search(params);
            }

            // verify the deletion of the property
            if (response.result().equals(Result.Deleted)) {
                log.config("Deletes property " + propertyName);
            }

        } catch (ElasticsearchException | IOException e) {
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

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // TODO Auto-generated method stub

    }

}
