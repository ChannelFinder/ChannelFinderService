package org.phoebus.channelfinder;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;
import static org.phoebus.channelfinder.ChannelManager.channelManagerAudit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.lucene.search.join.ScoreMode;
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
import org.elasticsearch.action.get.MultiGetRequest.Item;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.phoebus.channelfinder.XmlProperty.OnlyXmlProperty;
import org.phoebus.channelfinder.XmlTag.OnlyXmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
@Configuration
public class ChannelRepository implements CrudRepository<XmlChannel, String> {
    static Logger log = Logger.getLogger(ChannelRepository.class.getName());

    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;
    @Value("${elasticsearch.channel.type:cf_channel}")
    private String ES_CHANNEL_TYPE;
    
    @Value("${elasticsearch.query.size:10000}")
    private int defaultMaxSize;


    @Autowired
    ElasticSearchClient esService;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * create a new channel using the given XmlChannel
     * 
     * @param channel - channel to be created
     * @return the created channel
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> S index(XmlChannel channel) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                    .id(channel.getName())
                    .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            /// verify the creation of the channel
            Result result = indexResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                return (S) findById(channel.getName()).get();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index channel: " + channel.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index channel: " + channel, null);
        }
        return null;
    }

    /**
     * create new channels using the given XmlChannels
     * 
     * @param channels - channels to be created
     * @return the created channels
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> Iterable<S> indexAll(Iterable<XmlChannel> channels) {
        RestHighLevelClient client = esService.getIndexClient();
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (XmlChannel channel : channels) {
                IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                        .id(channel.getName())
                        .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            // verify the creation of the channels
            if (bulkResponse.hasFailures()) {
                // Failed to create all the channels
            } else {
                List<String> createdChannelIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
                        createdChannelIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdChannelIds);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index channels: " + channels, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index channels: " + channels, null);
        }
        return null;
    }

    /**
     * update/save channel using the given XmlChannel
     * 
     * @param channel - channel to be saved
     * @return the updated/saved channel
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> S save(String channelName, S channel) {
        RestHighLevelClient client = esService.getIndexClient();

        try {
            UpdateRequest updateRequest;
            Optional<XmlChannel> existingChannel = findById(channelName);
            boolean present = existingChannel.isPresent();
            if(present) {
                List<String> tagNames = channel.getTags().stream().map(XmlTag::getName).collect(Collectors.toList());
                // Add the old tags on the channel update request to ensure that old tags are preserved
                for (XmlTag oldTag : existingChannel.get().getTags()) {
                    if (!tagNames.contains(oldTag.getName()))
                        channel.addTag(oldTag);
                }

                // Add the old properties on the channel update request to ensure that old properties are preserved
                List<String> propNames = channel.getProperties().stream().map(XmlProperty::getName).collect(Collectors.toList());
                for(XmlProperty oldProp: existingChannel.get().getProperties()) {
                    if(!propNames.contains(oldProp.getName())) {
                        channel.addProperty(oldProp);
                    }
                }

                // If there are properties with null or empty values, they are to be removed from the channel
                List<XmlProperty> properties = channel.getProperties();
                properties.removeIf(prop -> prop.getValue() == null);
                properties.removeIf(prop -> prop.getValue().isEmpty());
                channel.setProperties(properties);

                // In case of a rename, the old channel should be removed
                deleteById(channelName);
            } 
            updateRequest = new UpdateRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channel.getName());
            IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                    .id(channel.getName())
                    .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
            updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON).upsert(indexRequest);
            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            // verify the creation of the channel
            Result result = updateResponse.getResult();
            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                // client.get(, options)
                return (S) findById(channel.getName()).get();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update/save channel: " + channel.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save channel: " + channel, null);
        }
        return null;
    }

    @Override
    public <S extends XmlChannel> S save(S channel) {
        return save(channel.getName(),channel);
    }

    /**
     * update/save channels using the given XmlChannels
     * 
     * @param channels - channels to be saved
     * @return the updated/saved channels
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlChannel> Iterable<S> saveAll(Iterable<S> channels) {
        RestHighLevelClient client = esService.getIndexClient();

        BulkRequest bulkRequest = new BulkRequest();
        try {
            for (XmlChannel channel : channels) {
                UpdateRequest updateRequest = new UpdateRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channel.getName());

                Optional<XmlChannel> existingChannel = findById(channel.getName());
                if (existingChannel.isPresent()) {
                    List<String> tagNames = channel.getTags().stream().map(XmlTag::getName).collect(Collectors.toList());
                    // Add the old tags on the channel update request to ensure that old tags are preserved
                    for (XmlTag oldTag : existingChannel.get().getTags()) {
                        if (!tagNames.contains(oldTag.getName()))
                            channel.addTag(oldTag);
                    }

                    // Add the old properties on the channel update request to ensure that old properties are preserved
                    List<String> propNames = channel.getProperties().stream().map(XmlProperty::getName).collect(Collectors.toList());
                    for(XmlProperty oldProp: existingChannel.get().getProperties()) {
                        if(!propNames.contains(oldProp.getName())) {
                            channel.addProperty(oldProp);
                        }
                    }

                    // If there are properties with null or empty values, they are to be removed from the channel
                    List<XmlProperty> properties = channel.getProperties();
                    properties.removeIf(prop -> prop.getValue() == null);
                    properties.removeIf(prop -> prop.getValue().isEmpty());
                    channel.setProperties(properties);

                    updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
                } else {
                    IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
                            .id(channel.getName())
                            .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
                    updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON).upsert(indexRequest);
                }
                bulkRequest.add(updateRequest);
            }

            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                // Failed to create/update all the channels
                throw new Exception();
            } else {
                List<String> createdChannelIds = new ArrayList<String>();
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    Result result = bulkItemResponse.getResponse().getResult();
                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
                        createdChannelIds.add(bulkItemResponse.getId());
                    }
                }
                return (Iterable<S>) findAllById(createdChannelIds);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update/save channels: " + channels, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update/save channels: " + channels, null);
        }
    }

    /**
     * find channel using the given channel id
     * 
     * @param channelId - id of channel to be found
     * @return the found channel
     */
    @Override
    public Optional<XmlChannel> findById(String channelId) {
        RestHighLevelClient client = esService.getSearchClient();
        GetRequest getRequest = new GetRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channelId);
        try {
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                XmlChannel channel = objectMapper.readValue(response.getSourceAsBytesRef().streamInput(), XmlChannel.class);
                return Optional.of(channel);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to find channel: " + channelId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find channel: " + channelId, null);
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
            long start = System.currentTimeMillis();
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.getResponse().isExists())
                    return false;
            }
            channelManagerAudit.info(
                    "Completed existance check for " + ids.size() + " in: " + (System.currentTimeMillis() - start));
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

    /**
     * find all channels 
     * 
     * @return the found channels
     */
    @Override
    public Iterable<XmlChannel> findAll() {
        RestHighLevelClient client = esService.getSearchClient();

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(ES_CHANNEL_INDEX);
        searchRequest.types(ES_CHANNEL_TYPE);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // TODO use of scroll will be necessary
        searchSourceBuilder.size(10000);
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
            log.log(Level.SEVERE, "Failed to find all channels", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to find all channels", null);
        }
        return null;
    }

    /**
     * find channels using the given channels ids
     * 
     * @param channelIds - ids of channels to be found
     * @return the found channels
     */
    @Override
    public Iterable<XmlChannel> findAllById(Iterable<String> channelIds) {
        MultiGetRequest request = new MultiGetRequest();

        for (String channelId : channelIds) {
            request.add(new MultiGetRequest.Item(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channelId));
        }
        try {
            List<XmlChannel> foundChannels = new ArrayList<XmlChannel>();
            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response) {
                if (!multiGetItemResponse.isFailed()) {
                    foundChannels.add(objectMapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlChannel.class));
                } 
            }
            return foundChannels;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to find all channels: " + channelIds, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Failed to find all channels: " + channelIds, null);
        }
    }

    @Override
    public long count() {
        // NOT USED
        return 0;
    }

    /**
     * delete the given channel by channel name
     * 
     * @param channelName - channel to be deleted
     */
    @Override
    public void deleteById(String channelName) {
        RestHighLevelClient client = esService.getIndexClient();

        DeleteRequest request = new DeleteRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channelName);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            Result result = response.getResult();
            if (!result.equals(Result.DELETED)) {
                throw new Exception();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to delete channel: " + channelName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete channel: " + channelName, null);
        }
    }

    /**
     * delete the given channel
     * 
     * @param channel - channel to be deleted
     */
    @Override
    public void delete(XmlChannel channel) {
        deleteById(channel.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlChannel> entities) {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    /**
     * Search for a list of channels based on their name, tags, and/or properties.
     * Search parameters ~name - The name of the channel ~tags - A list of comma
     * separated values ${propertyName}:${propertyValue} -
     * 
     * The query result is sorted based on the channel name ~size - The number of
     * channels to be returned ~from - The starting index of the channel list
     */
    public List<XmlChannel> search(MultiValueMap<String, String> searchParameters) {

        StringBuffer performance = new StringBuffer();
        long start = System.currentTimeMillis();
        long totalStart = System.currentTimeMillis();

        RestHighLevelClient client = esService.getSearchClient();
        start = System.currentTimeMillis();
        Integer size = defaultMaxSize;

        try {
            BoolQueryBuilder qb = boolQuery();
            int from = 0;
            for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
                String key = parameter.getKey().trim();

                boolean isNot = key.endsWith("!");
                if (isNot) {
                    key = key.substring(0, key.length() - 1);
                }

                switch (key) {
                case "~name":
                    for (String value : parameter.getValue()) {
                        DisMaxQueryBuilder nameQuery = disMaxQuery();
                        for (String pattern : value.split("[\\|,;]")) {
                            nameQuery.add(wildcardQuery("name", pattern.trim()));
                        }
                        qb.must(nameQuery);
                    }
                    break;
                case "~tag":
                    for (String value : parameter.getValue()) {
                        DisMaxQueryBuilder tagQuery = disMaxQuery();
                        for (String pattern : value.split("[\\|,;]")) {
                            tagQuery.add(wildcardQuery("tags.name", pattern.trim()));
                        }

                        if (isNot) {
                            qb.mustNot(nestedQuery("tags", tagQuery, ScoreMode.None));
                        } else {
                            qb.must(nestedQuery("tags", tagQuery, ScoreMode.None));
                        }
                    }
                    break;
                case "~size":
                    Optional<String> maxSize = parameter.getValue().stream().max((o1, o2) -> {
                        return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                    });
                    if (maxSize.isPresent()) {
                        size = Integer.valueOf(maxSize.get());
                    }
                    break;
                case "~from":
                    Optional<String> maxFrom = parameter.getValue().stream().max((o1, o2) -> {
                        return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                    });
                    if (maxFrom.isPresent()) {
                        from = Integer.valueOf(maxFrom.get());
                    }
                    break;
                default:
                    DisMaxQueryBuilder propertyQuery = disMaxQuery();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            propertyQuery
                            .add(nestedQuery("properties",
                                    isNot ? boolQuery().must(matchQuery("properties.name", key)).mustNot(wildcardQuery("properties.value", pattern.trim()))
                                            : boolQuery().must(matchQuery("properties.name", key)).must(wildcardQuery("properties.value", pattern.trim())),
                                    ScoreMode.None));
                        }
                    }
                    qb.must(propertyQuery);
                    break;
                }
            }            
            
            performance.append("|prepare: " + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            SearchRequest searchRequest = new SearchRequest(ES_CHANNEL_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.size(size);
            if (from >= 0) {
                searchSourceBuilder.from(from);
            }
            searchSourceBuilder.query(qb);
            searchSourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));
            searchRequest.types(ES_CHANNEL_TYPE);
            searchRequest.source(searchSourceBuilder);

            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            performance.append(
                    "|query:(" + searchResponse.getHits().getTotalHits() + ")" + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.addMixIn(XmlProperty.class, OnlyXmlProperty.class);
            mapper.addMixIn(XmlTag.class, OnlyXmlTag.class);
            start = System.currentTimeMillis();
            List<XmlChannel> result = new ArrayList<XmlChannel>();
            searchResponse.getHits().forEach(hit -> {
                try {
                    result.add(mapper.readValue(hit.getSourceAsString(), XmlChannel.class));
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to parse result for search : " + searchParameters, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to parse result for search : " + searchParameters + ", CAUSE: " + e.getMessage(), e);
                }
            });

            performance.append("|parse:" + (System.currentTimeMillis() - start));
            //            log.info(user + "|" + uriInfo.getPath() + "|GET|OK" + performance.toString() + "|total:"
            //                    + (System.currentTimeMillis() - totalStart) + "|" + r.getStatus() + "|returns "
            //                    + qbResult.getHits().getTotalHits() + " channels");
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Search failed for: " + searchParameters, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Search failed for: " + searchParameters + ", CAUSE: " + e.getMessage(), e);
        }
    }
}
