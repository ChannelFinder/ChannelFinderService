package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_PROPERTY_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.channelfinder.XmlProperty.OnlyNameOwnerXmlProperty;

@Repository
public class PropertyRepository implements CrudRepository<XmlProperty, String> {

    @Autowired
    ElasticSearchClient esService;

    ObjectMapper objectMapper = new ObjectMapper();
 
    /**
     * create a new property using the given XmlProperty
     * 
     * @param testProperty
     * @return the created property
     */
    public <S extends XmlProperty> S index(XmlProperty property) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            objectMapper.addMixIn(XmlProperty.class, OnlyNameOwnerXmlProperty.class);
            IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                    .id(property.getName())
                    .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the property
            Result result = indexResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                client.indices().refresh(new RefreshRequest(ES_PROPERTY_INDEX), RequestOptions.DEFAULT);
                return (S) findById(property.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <S extends XmlProperty> Iterable<S> indexAll(List<XmlProperty> properties) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlProperty property : properties) {
                IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE)
                        .id(property.getName())
                        .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            if (bulkResponse.hasFailures()) {
                // Failed to create all the tags

            } else {
                List<String> createdPropertiesIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                        createdPropertiesIds.add(bulkItemResponse.getId());
                    }
                }
                client.indices().refresh(new RefreshRequest(ES_PROPERTY_INDEX), RequestOptions.DEFAULT);
                return (Iterable<S>) findAllById(createdPropertiesIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S extends XmlProperty> S save(S property) {
    	RestHighLevelClient client = esService.getIndexClient();
        try {

            UpdateRequest updateRequest = new UpdateRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName());

            Optional<XmlProperty> existingProperty = findById(property.getName());
            if(existingProperty.isPresent()) {
            	XmlProperty newProperty = existingProperty.get();
                updateRequest.doc(objectMapper.writeValueAsBytes(newProperty), XContentType.JSON);
            } else {
                IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE).id(property.getName())
                        .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON).upsert(indexRequest);
            }
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            /// verify the creation of the property
            Result result = updateResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                // client.get(, options)
                return (S) findById(property.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save property" + property, null);
        }
        return null;
    }

    @Override
    public <S extends XmlProperty> Iterable<S> saveAll(Iterable<S> properties) {
        
    	RestHighLevelClient client = esService.getIndexClient();
        BulkRequest bulkRequest = new BulkRequest();
        try {
            for (XmlProperty property : properties) {
                UpdateRequest updateRequest = new UpdateRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName());

                Optional<XmlProperty> existingProperty = findById(property.getName());
                if (existingProperty.isPresent()) {
                	XmlProperty newProperty = existingProperty.get();
                    updateRequest.doc(objectMapper.writeValueAsBytes(newProperty), XContentType.JSON);
                } else {
                	IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(property.getName())
                            .source(objectMapper.writeValueAsBytes(property), XContentType.JSON);
                    updateRequest.doc(objectMapper.writeValueAsBytes(property), XContentType.JSON).upsert(indexRequest);
                }
                bulkRequest.add(updateRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                // Failed to create/update all the tags

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
        	e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save properties" + properties, null);
        }
        return null;
    }

    @Override
    public Optional<XmlProperty> findById(String id) {
        return findById(id, false);
    }
   
    public Optional<XmlProperty> findById(String id, boolean withChannels) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, id);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlProperty property = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(),
                        XmlProperty.class);
                return Optional.of(property);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Iterable<XmlProperty> findAll() {

        RestHighLevelClient client = esService.getSearchClient();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_PROPERTY_INDEX);
        searchRequest.types(ES_PROPERTY_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // TODO use of scroll will be necessary
        searchSourceBuilder.size(10000);
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Iterable<XmlProperty> findAllById(Iterable<String> ids) {
        MultiGetRequest request = new MultiGetRequest();
        
        for (String id : ids) {
            request.add(new MultiGetRequest.Item(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, id));
        }
        try {
            List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundProperties.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlProperty.class));
                } else {
                    // failed to fetch all the listed tags
                }
            }
            return foundProperties;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String id) {
        RestHighLevelClient client = esService.getIndexClient();
        DeleteRequest request = new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, id);
        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            Result result = response.getResult();
            if (!result.equals(Result.DELETED)) {
                // Failed to delete the requested tag
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void delete(XmlProperty entity) {
        deleteById(entity.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlProperty> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

}
