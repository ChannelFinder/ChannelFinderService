package gov.bnl.channelfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import gov.bnl.channelfinder.XmlTag.OnlyXmlTag;

@Repository
@Configuration
public class TagRepository implements CrudRepository<XmlTag, String> {

    @Value("${elasticsearch.tag.index:cf_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:cf_tag}")
    private String ES_TAG_TYPE;

    @Autowired
    ElasticSearchClient esService;
    
    @Autowired
    ChannelRepository channelRepository;
    
    ObjectMapper objectMapper = new ObjectMapper().addMixIn(XmlTag.class, OnlyXmlTag.class);

    /**
     * create a new tag using the given XmlTag
     * 
     * @param tag - tag to be created
     * @return the created tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S index(S tag) {
        RestHighLevelClient client = esService.getIndexClient(); 
        try {
            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE)
                    .id(tag.getName())
                    .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tag
            Result result = indexResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                return (S) findById(tag.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index tag: " + tag, null);
        }
        return null;
    }

    /**
     * create new tags using the given XmlTags
     * 
     * @param tags - tags to be created
     * @return the created tags
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> Iterable<S> indexAll(Iterable<S> tags) {
        RestHighLevelClient client = esService.getIndexClient();      
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlTag tag : tags) {
                IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE)
                        .id(tag.getName())
                        .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            /// verify the creation of the tags
            if (bulkResponse.hasFailures()) {
                // Failed to create all the tags
            } else {
                List<String> createdTagIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                        createdTagIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdTagIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index tags: " + tags, null);      
        }
        return null;
    }

    /**
     * update/save tag using the given XmlTag
     * 
     * @param tag - tag to be created
     * @return the updated/saved tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S save(String tagName, S tag) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            UpdateRequest updateRequest;
            Optional<XmlTag> existingTag = findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                deleteById(tagName);
            } 
            updateRequest = new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName());
            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE)
                    .id(tag.getName())
                    .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
            updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON).upsert(indexRequest);
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            /// verify the updating/saving of the tag
            Result result = updateResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                // client.get(, options)
                return (S) findById(tag.getName()).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save tag: " + tag, null);
        }
        return null;
    }

    @Override
    public <S extends XmlTag> S save(S tag) {
        return save(tag.getName(),tag);
    }

    /**
     * update/save tags using the given XmlTags
     * 
     * @param tags - tags to be created
     * @return the updated/saved tags
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlTag> Iterable<S> saveAll(Iterable<S> tags) {
        RestHighLevelClient client = esService.getIndexClient();
        BulkRequest bulkRequest = new BulkRequest();
        try {
            for (XmlTag tag : tags) {
                UpdateRequest updateRequest = new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName());

                Optional<XmlTag> existingTag = findById(tag.getName());
                if (existingTag.isPresent()) {
                    updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
                } else {
                    IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE)
                            .id(tag.getName())
                            .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
                    updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON).upsert(indexRequest);
                }
                bulkRequest.add(updateRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                // Failed to create/update all the tags
                throw new Exception();
            } else {
                List<String> createdTagIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                        createdTagIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdTagIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save tags: " + tags, null);
        }
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId - id of tag to be found
     * @return the found tag
     */
    @Override
    public Optional<XmlTag> findById(String tagId) {
        return findById(tagId, false);
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId - id of tag to be found
     * @param withChannels - whether channels should be included
     * @return the found tag
     */
    public Optional<XmlTag> findById(String tagId, boolean withChannels) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagId);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlTag tag = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(), XmlTag.class);
                if(withChannels) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
                    params.add("~tag",tagId);
                    List<XmlChannel> chans = channelRepository.search(params);
                    chans.forEach(chan -> chan.setTags(new ArrayList<XmlTag>()));
                    chans.forEach(chan -> chan.setProperties(new ArrayList<XmlProperty>()));
                    tag.setChannels(chans);
                }
                return Optional.of(tag);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find tag: " + tagId, null);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check if tag exists by id: " + id, null); 
        }
    }

    /**
     * find all tags 
     * 
     * @return the found tags
     */
    @Override
    public Iterable<XmlTag> findAll() {
        RestHighLevelClient client = esService.getSearchClient();

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_TAG_INDEX);
        searchRequest.types(ES_TAG_TYPE);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // TODO use of scroll will be necessary
        searchSourceBuilder.size(10000);
        searchSourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));
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
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to find all tags", null);
        }
        return null;
    }

    /**
     * find tags using the given tags ids
     * 
     * @param tagIds - ids of tags to be found
     * @return the found tags
     */
    @Override
    public Iterable<XmlTag> findAllById(Iterable<String> tagIds) {
        MultiGetRequest request = new MultiGetRequest();

        for (String tagId : tagIds) {
            request.add(new MultiGetRequest.Item(ES_TAG_INDEX, ES_TAG_TYPE, tagId));
        }
        try {
            List<XmlTag> foundTags = new ArrayList<XmlTag>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundTags.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlTag.class));
                } 
            }
            return foundTags;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find all tags: " + tagIds, null);
        }
    }

    @Override
    public long count() {
        // NOT USED
        return 0;
    }

    /**
     * delete the given tag by tag name
     * 
     * @param tagName - tag to be deleted
     */
    @Override
    public void deleteById(String tagName) {
        RestHighLevelClient client = esService.getIndexClient();
        DeleteRequest request = new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            Result result = response.getResult();
            if (!result.equals(Result.DELETED)) {
                throw new Exception();
            }
            // delete tag from channels
            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add("~tag",tagName);
            List<XmlChannel> chans = channelRepository.search(params);
            if(!chans.isEmpty()) {
                chans.forEach(chan -> chan.removeTag(new XmlTag(tagName, "")));
                channelRepository.indexAll(chans);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete tag: " + tagName, null);
        }

    }

    /**
     * delete the given tag
     * 
     * @param tag - tag to be deleted
     */
    @Override
    public void delete(XmlTag tag) {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlTag> entities) {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }
}
