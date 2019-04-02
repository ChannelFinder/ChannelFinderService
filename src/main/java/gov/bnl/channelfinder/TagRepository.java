package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_CHANNEL_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class TagRepository implements CrudRepository<XmlTag, String> {

    @Autowired
    ElasticSearchClient esService;

    ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unused")
    public <S extends XmlTag> S index(S entity) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(entity.getName())
                    .source(objectMapper.writeValueAsBytes(entity), XContentType.JSON);
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            IndexResponse indexRespone = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            Result result = indexRespone.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                client.indices().refresh(new RefreshRequest(ES_TAG_INDEX), RequestOptions.DEFAULT);
                return (S) findById(entity.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <S extends XmlTag> Iterable<S> indexAll(Iterable<S> entities) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlTag tag : entities) {
                IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(tag.getName())
                        .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            if (bulkResponse.hasFailures()) {
                // Failed to create all the tags

            } else {
                List<String> createdTagIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.getResponse().getResult().equals(Result.CREATED)) {
                        createdTagIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdTagIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public <S extends XmlTag> S save(S tag) {
        RestHighLevelClient client = esService.getIndexClient();
        try {

            UpdateRequest updateRequest = new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName());

            Optional<XmlTag> existingTag = findById(tag.getName());
            if(existingTag.isPresent()) {
                XmlTag newTag = existingTag.get();
                updateRequest.doc(objectMapper.writeValueAsBytes(newTag), XContentType.JSON);
            } else {
                IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(tag.getName())
                        .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
                updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON).upsert(indexRequest);
            }
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            UpdateResponse updateRespone = client.update(updateRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            Result result = updateRespone.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                // client.get(, options)
                return (S) findById(tag.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S extends XmlTag> Iterable<S> saveAll(Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<XmlTag> findById(String id) {
        return findById(id, false);
    }

    public Optional<XmlTag> findById(String id, boolean withChannels) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, id);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlTag tag = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(), XmlTag.class);
                return Optional.of(tag);
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
        GetRequest getRequest = new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, id);
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
    public Iterable<XmlTag> findAll() {

        RestHighLevelClient client = esService.getSearchClient();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_TAG_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // TODO use of scroll will be necessary
        searchSourceBuilder.size(10000);
        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchAllQuery()));
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            if (searchResponse.status().equals(RestStatus.OK)) {
                List<XmlTag> result = new ArrayList<XmlTag>();
                for (SearchHit hit : searchResponse.getHits()) {
                    result.add(objectMapper.readValue(hit.getSourceRef().streamInput(), XmlTag.class));
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
    public Iterable<XmlTag> findAllById(Iterable<String> ids) {

        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            request.add(new MultiGetRequest.Item(ES_TAG_INDEX, ES_TAG_TYPE, id));
        }
        try {
            List<XmlTag> foundTags = new ArrayList<XmlTag>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundTags.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlTag.class));
                } else {
                    // failed to fetch all the listed tags
                }
            }
            return foundTags;
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
    public void deleteById(String tag) {
        RestHighLevelClient client = esService.getIndexClient();
        DeleteRequest request = new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag);
        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            Result result = response.getResult();
            if (!result.equals(Result.DELETED)) {
                // Failed to delete the requested tag
            	System.out.println("failed to delete the tag, but no error");
//            	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            	String currentPrincipalName = authentication.getName();
//            	//MyUser user = (MyUser)authentication.getPrincipal();
//            	User user = (User) authentication.getPrincipal();
//            	System.out.println(currentPrincipalName);
            	//System.out.println(user.getGroups());
            }
            else
            	System.out.println("tag deleted! yay!");
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	System.out.println("an unexpected error has occurred while trying to delete the tag");
            e.printStackTrace();
        }
    }

    @Override
    public void delete(XmlTag entity) {
        deleteById(entity.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlTag> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    /**
     * Utility method to rename an existing tag
     * 
     * @param original
     * @param data
     * @return
     */
    XmlTag renameTag(XmlTag original, XmlTag data) {

        return null;
    }

}
