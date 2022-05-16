package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.web.server.ResponseStatusException;

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
    @Qualifier("indexClient")
    ElasticsearchClient client;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * create a new channel using the given XmlChannel
     * 
     * @param <S> extends XmlChannel
     * @param channel - channel to be created
     * @return the created channel
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> S index(XmlChannel channel) {
        try {
            IndexRequest request = IndexRequest.of(i -> i.index(ES_CHANNEL_INDEX)
                    .id(channel.getName())
                    .document(channel)
                    .refresh(Refresh.True));

            // TODO validation
            IndexResponse response = client.index(request);
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created channel " + channel);
                return (S) channel;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index channel " + channel.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index channel: " + channel, null);
        }
        return null;
    }

    /**
     * create new channels using the given XmlChannels
     * 
     * @param <S> extends XmlChannel
     * @param channels - channels to be created
     * @return the created channels
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> Iterable<S> indexAll(Iterable<XmlChannel> channels) {
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (XmlChannel channel : channels) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(ES_CHANNEL_INDEX)
                            .id(channel.getName())
                            .document(channel)
                    )
            );
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
                return (Iterable<S>) channels;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to index channels " + channels, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index channels: " + channels, null);

        }
        return null;
    }

    /**
     * update/save channel using the given XmlChannel
     * 
     * @param <S> extends XmlChannel
     * @param channelName - name of channel to be saved
     * @param channel - channel to be saved
     * @return the updated/saved channel
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlChannel> S save(String channelName, S channel) {
//        RestHighLevelClient client = esService.getNewClient();
//
//        try {
//            UpdateRequest updateRequest;
//            Optional<XmlChannel> existingChannel = findById(channelName);
//            boolean present = existingChannel.isPresent();
//            if(present) {
//                List<String> tagNames = channel.getTags().stream().map(XmlTag::getName).collect(Collectors.toList());
//                // Add the old tags on the channel update request to ensure that old tags are preserved
//                for (XmlTag oldTag : existingChannel.get().getTags()) {
//                    if (!tagNames.contains(oldTag.getName()))
//                        channel.addTag(oldTag);
//                }
//
//                // Add the old properties on the channel update request to ensure that old properties are preserved
//                List<String> propNames = channel.getProperties().stream().map(XmlProperty::getName).collect(Collectors.toList());
//                for(XmlProperty oldProp: existingChannel.get().getProperties()) {
//                    if(!propNames.contains(oldProp.getName())) {
//                        channel.addProperty(oldProp);
//                    }
//                }
//
//                // If there are properties with null or empty values, they are to be removed from the channel
//                List<XmlProperty> properties = channel.getProperties();
//                properties.removeIf(prop -> prop.getValue() == null);
//                properties.removeIf(prop -> prop.getValue().isEmpty());
//                channel.setProperties(properties);
//
//                // In case of a rename, the old channel should be removed
//                deleteById(channelName);
//            } 
//            updateRequest = new UpdateRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channel.getName());
//            IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
//                    .id(channel.getName())
//                    .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
//            updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON).upsert(indexRequest);
//            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
//            // verify the creation of the channel
//            Result result = updateResponse.getResult();
//            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
//                // client.get(, options)
//                return (S) findById(channel.getName()).get();
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to update/save channel: " + channel.toLog(), e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to update/save channel: " + channel, null);
//        }
        return null;
    }

    /**
     * 
     * @param <S> extends XmlChannel
     */
    @Override
    public <S extends XmlChannel> S save(S channel) {
        return save(channel.getName(),channel);
    }

    /**
     * update/save channels using the given XmlChannels
     * 
     * @param <S> extends XmlChannel
     * @param channels - channels to be saved
     * @return the updated/saved channels
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlChannel> Iterable<S> saveAll(Iterable<S> channels) {
        return null;
//        RestHighLevelClient client = esService.getNewClient();
//
//        BulkRequest bulkRequest = new BulkRequest();
//        try {
//            for (XmlChannel channel : channels) {
//                UpdateRequest updateRequest = new UpdateRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channel.getName());
//
//                Optional<XmlChannel> existingChannel = findById(channel.getName());
//                if (existingChannel.isPresent()) {
//                    List<String> tagNames = channel.getTags().stream().map(XmlTag::getName).collect(Collectors.toList());
//                    // Add the old tags on the channel update request to ensure that old tags are preserved
//                    for (XmlTag oldTag : existingChannel.get().getTags()) {
//                        if (!tagNames.contains(oldTag.getName()))
//                            channel.addTag(oldTag);
//                    }
//
//                    // Add the old properties on the channel update request to ensure that old properties are preserved
//                    List<String> propNames = channel.getProperties().stream().map(XmlProperty::getName).collect(Collectors.toList());
//                    for(XmlProperty oldProp: existingChannel.get().getProperties()) {
//                        if(!propNames.contains(oldProp.getName())) {
//                            channel.addProperty(oldProp);
//                        }
//                    }
//
//                    // If there are properties with null or empty values, they are to be removed from the channel
//                    List<XmlProperty> properties = channel.getProperties();
//                    properties.removeIf(prop -> prop.getValue() == null);
//                    properties.removeIf(prop -> prop.getValue().isEmpty());
//                    channel.setProperties(properties);
//
//                    updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
//                } else {
//                    IndexRequest indexRequest = new IndexRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE)
//                            .id(channel.getName())
//                            .source(objectMapper.writeValueAsBytes(channel), XContentType.JSON);
//                    updateRequest.doc(objectMapper.writeValueAsBytes(channel), XContentType.JSON).upsert(indexRequest);
//                }
//                bulkRequest.add(updateRequest);
//            }
//
//            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                // Failed to create/update all the channels
//                throw new Exception();
//            } else {
//                List<String> createdChannelIds = new ArrayList<String>();
//                for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                    Result result = bulkItemResponse.getResponse().getResult();
//                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
//                        createdChannelIds.add(bulkItemResponse.getId());
//                    }
//                }
//                return (Iterable<S>) findAllById(createdChannelIds);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to update/save channels: " + channels, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to update/save channels: " + channels, null);
//        }
    }

    /**
     * find channel using the given channel id
     * 
     * @param channelName - name of channel to be found
     * @return the found channel
     */
    @Override
    public Optional<XmlChannel> findById(String channelName) {
        GetResponse<XmlChannel> response;
        try {
            response = client.get(g -> g.index(ES_CHANNEL_INDEX).id(channelName), XmlChannel.class);

            if (response.found()) {
                XmlChannel channel = response.source();
                log.info("Channel name " + channel.getName());
                return Optional.of(channel);
            } else {
                log.info("Channel not found");
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find Channel " + channelName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find Channel: " + channelName, null);
        }
    }

    /**
     * Find all channels with given ids
     * @param channelIds - list of channel ids to verify exists
     * @return true if all the channel id's exist
     */
    public boolean existsByIds(List<String> channelIds) {
        try {
            List<String> ids = StreamSupport.stream(channelIds.spliterator(), false).collect(Collectors.toList());

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(ES_CHANNEL_INDEX)
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlChannel> response = client.search(searchBuilder.build(), XmlChannel.class);
            return response.hits()
                    .hits().stream().map(h -> h.source().getName()).collect(Collectors.toList())
                    .containsAll(channelIds);
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all channels", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all channels", null);
        }
    }

    /**
     * Check is channel with name 'channelName' exists
     * @param channelName
     * @return true if channel exists
     */
    @Override
    public boolean existsById(String channelName) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_CHANNEL_INDEX).id(channelName);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to check if channel " + channelName + " exists", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check if channel exists by channelName: " + channelName, null);
        }
    }

    /**
     * find all channels 
     * 
     * @return the found channels
     */
    @Override
    public Iterable<XmlChannel> findAll() {
//        RestHighLevelClient client = esService.getSearchClient();
//
//        SearchRequest searchRequest = new SearchRequest();
//        searchRequest.indices(ES_CHANNEL_INDEX);
//        searchRequest.types(ES_CHANNEL_TYPE);
//
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        // TODO use of scroll will be necessary
//        searchSourceBuilder.size(10000);
//        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchAllQuery()));
//
//        try {
//            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//            if (searchResponse.status().equals(RestStatus.OK)) {
//                List<XmlChannel> result = new ArrayList<XmlChannel>();
//                for (SearchHit hit : searchResponse.getHits()) {
//                    result.add(objectMapper.readValue(hit.getSourceRef().streamInput(), XmlChannel.class));
//                }
//                return result;
//            }
//        } catch (IOException e) {
//            log.log(Level.SEVERE, "Failed to find all channels", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to find all channels", null);
//        }
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
        try {
            List<String> ids = StreamSupport.stream(channelIds.spliterator(), false).collect(Collectors.toList());

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(ES_CHANNEL_INDEX)
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlChannel> response = client.search(searchBuilder.build(), XmlChannel.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all channels", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all channels", null);
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
//        RestHighLevelClient client = esService.getNewClient();
//
//        DeleteRequest request = new DeleteRequest(ES_CHANNEL_INDEX, ES_CHANNEL_TYPE, channelName);
//        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//
//        try {
//            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
//            Result result = response.getResult();
//            if (!result.equals(Result.DELETED)) {
//                throw new Exception();
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to delete channel: " + channelName, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to delete channel: " + channelName, null);
//        }
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
     * 
     * @param searchParameters channel search parameters
     * @return matching channels
     */
    public List<XmlChannel> search(MultiValueMap<String, String> searchParameters) {
        return null;
//        StringBuffer performance = new StringBuffer();
//        long start = System.currentTimeMillis();
//        long totalStart = System.currentTimeMillis();
//
//        RestHighLevelClient client = esService.getSearchClient();
//        start = System.currentTimeMillis();
//        Integer size = defaultMaxSize;
//
//        try {
//            BoolQueryBuilder qb = boolQuery();
//            int from = 0;
//            for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
//                String key = parameter.getKey().trim();
//
//                boolean isNot = key.endsWith("!");
//                if (isNot) {
//                    key = key.substring(0, key.length() - 1);
//                }
//
//                switch (key) {
//                case "~name":
//                    for (String value : parameter.getValue()) {
//                        DisMaxQueryBuilder nameQuery = disMaxQuery();
//                        for (String pattern : value.split("[\\|,;]")) {
//                            nameQuery.add(wildcardQuery("name", pattern.trim()));
//                        }
//                        qb.must(nameQuery);
//                    }
//                    break;
//                case "~tag":
//                    for (String value : parameter.getValue()) {
//                        DisMaxQueryBuilder tagQuery = disMaxQuery();
//                        for (String pattern : value.split("[\\|,;]")) {
//                            tagQuery.add(wildcardQuery("tags.name", pattern.trim()));
//                        }
//
//                        if (isNot) {
//                            qb.mustNot(nestedQuery("tags", tagQuery, ScoreMode.None));
//                        } else {
//                            qb.must(nestedQuery("tags", tagQuery, ScoreMode.None));
//                        }
//                    }
//                    break;
//                case "~size":
//                    Optional<String> maxSize = parameter.getValue().stream().max((o1, o2) -> {
//                        return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
//                    });
//                    if (maxSize.isPresent()) {
//                        size = Integer.valueOf(maxSize.get());
//                    }
//                    break;
//                case "~from":
//                    Optional<String> maxFrom = parameter.getValue().stream().max((o1, o2) -> {
//                        return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
//                    });
//                    if (maxFrom.isPresent()) {
//                        from = Integer.valueOf(maxFrom.get());
//                    }
//                    break;
//                default:
//                    DisMaxQueryBuilder propertyQuery = disMaxQuery();
//                    for (String value : parameter.getValue()) {
//                        for (String pattern : value.split("[\\|,;]")) {
//                            propertyQuery
//                            .add(nestedQuery("properties",
//                                    isNot ? boolQuery().must(matchQuery("properties.name", key)).mustNot(wildcardQuery("properties.value", pattern.trim()))
//                                            : boolQuery().must(matchQuery("properties.name", key)).must(wildcardQuery("properties.value", pattern.trim())),
//                                    ScoreMode.None));
//                        }
//                    }
//                    qb.must(propertyQuery);
//                    break;
//                }
//            }            
//            
//            performance.append("|prepare: " + (System.currentTimeMillis() - start));
//            start = System.currentTimeMillis();
//            SearchRequest searchRequest = new SearchRequest(ES_CHANNEL_INDEX);
//            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//            searchSourceBuilder.size(size);
//            if (from >= 0) {
//                searchSourceBuilder.from(from);
//            }
//            searchSourceBuilder.query(qb);
//            searchSourceBuilder.sort(SortBuilders.fieldSort("name").order(SortOrder.ASC));
//            searchRequest.types(ES_CHANNEL_TYPE);
//            searchRequest.source(searchSourceBuilder);
//
//            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//            performance.append(
//                    "|query:(" + searchResponse.getHits().getTotalHits() + ")" + (System.currentTimeMillis() - start));
//            start = System.currentTimeMillis();
//            final ObjectMapper mapper = new ObjectMapper();
//            mapper.addMixIn(XmlProperty.class, OnlyXmlProperty.class);
//            mapper.addMixIn(XmlTag.class, OnlyXmlTag.class);
//            start = System.currentTimeMillis();
//            List<XmlChannel> result = new ArrayList<XmlChannel>();
//            searchResponse.getHits().forEach(hit -> {
//                try {
//                    result.add(mapper.readValue(hit.getSourceAsString(), XmlChannel.class));
//                } catch (IOException e) {
//                    log.log(Level.SEVERE, "Failed to parse result for search : " + searchParameters, e);
//                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                            "Failed to parse result for search : " + searchParameters + ", CAUSE: " + e.getMessage(), e);
//                }
//            });
//
//            performance.append("|parse:" + (System.currentTimeMillis() - start));
//            //            log.info(user + "|" + uriInfo.getPath() + "|GET|OK" + performance.toString() + "|total:"
//            //                    + (System.currentTimeMillis() - totalStart) + "|" + r.getStatus() + "|returns "
//            //                    + qbResult.getHits().getTotalHits() + " channels");
//            return result;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Search failed for: " + searchParameters, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Search failed for: " + searchParameters + ", CAUSE: " + e.getMessage(), e);
//        }
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // TODO Auto-generated method stub
        
    }
}
