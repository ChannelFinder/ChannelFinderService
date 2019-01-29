package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_PROPERTY_INDEX;
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
import org.elasticsearch.action.get.MultiGetRequest.Item;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
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
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ChannelRepository implements CrudRepository<XmlChannel, String> {

    @Autowired
    ElasticSearchClient esService;

    ObjectMapper objectMapper = new ObjectMapper();
    /**
     * create a new property using the given XmlChannel
     * 
     * @param testProperty
     * @return 
     * @return the created property
     */
    public <S extends XmlChannel> S index(XmlChannel channel) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                    .id(channel.getName())
                    .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
            IndexResponse indexRespone = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            Result result = indexRespone.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                // client.get(, options)
                return (S) findById(channel.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <S extends XmlChannel> Iterable<S> indexAll(List<XmlChannel> channels) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlChannel property : channels) {
                IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
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
                    if (bulkItemResponse.getResponse().getResult().equals(Result.CREATED)) {
                        createdPropertiesIds.add(bulkItemResponse.getId());
                    }
                }
                client.indices().refresh(new RefreshRequest(ES_CHANNEL_INDEX), RequestOptions.DEFAULT);
                return (Iterable<S>) findAllById(createdPropertiesIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S extends XmlChannel> S save(S entity) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                    .id(entity.getName())
                    .source(objectMapper.writeValueAsBytes(entity), XContentType.JSON);
            UpdateRequest updateRequest = new UpdateRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, entity.getName())
                    .doc(objectMapper.writeValueAsBytes(entity), XContentType.JSON)
                    .upsert(indexRequest);
            UpdateResponse updateRespone = client.update(updateRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            Result result = updateRespone.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                // client.get(, options)
                return (S) findById(entity.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S extends XmlChannel> Iterable<S> saveAll(Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<XmlChannel> findById(String id) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, id);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlChannel property = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(), XmlChannel.class);
                return Optional.of(property);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean existsByIds(List<String> ids) {
        RestHighLevelClient client = esService.getSearchClient();
        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            Item getRequest = new MultiGetRequest.Item(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, id);
            getRequest.fetchSourceContext(new FetchSourceContext(false));
            getRequest.storedFields("_none_");
            request.add(getRequest);
        }
        try {
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if( !multiGetItemResponse.getResponse().isExists())
                    return false;
            }
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public boolean existsById(String id) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, id);
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
    public Iterable<XmlChannel> findAll() {

        RestHighLevelClient client = esService.getSearchClient();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_CHANNEL_INDEX);
        searchRequest.types(ES_CHANNEL_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchAllQuery()));
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            if (searchResponse.status().equals(RestStatus.OK)) {
                List<XmlChannel> result = new ArrayList<XmlChannel>();
                for (SearchHit hit : searchResponse.getHits()) {
                    result.add(objectMapper.readValue(hit.getSourceRef().streamInput(), XmlChannel.class));
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
    public Iterable<XmlChannel> findAllById(Iterable<String> ids) {
        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            request.add(new MultiGetRequest.Item(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, id));
        }
        try {
            List<XmlChannel> foundProperties = new ArrayList<XmlChannel>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundProperties.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlChannel.class));
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
        DeleteRequest request = new DeleteRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, id);
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
    public void delete(XmlChannel entity) {
        deleteById(entity.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlChannel> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

}
