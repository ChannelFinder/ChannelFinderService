package org.phoebus.channelfinder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Configuration
public class ChannelRepository implements CrudRepository<Channel, String> {

    private static final Logger logger = Logger.getLogger(ChannelRepository.class.getName());

    @Autowired
    ElasticConfig esService;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Value("${index.chunk.size:1}")
    private int chunkSize;

    // Object mapper to ignore properties we don't want to index
    final ObjectMapper objectMapper = new ObjectMapper()
            .addMixIn(Tag.class, Tag.OnlyTag.class)
            .addMixIn(Property.class, Property.OnlyProperty.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * create a new channel using the given Channel
     *
     * @param channel - channel to be created
     * @return the created channel
     */
    @SuppressWarnings("unchecked")
    public Channel index(Channel channel) {
        try {
            IndexRequest request = IndexRequest.of(i -> i.index(esService.getES_CHANNEL_INDEX())
                    .id(channel.getName())
                    .document(JsonData.of(channel, new JacksonJsonpMapper(objectMapper)))
                    .refresh(Refresh.True));
            IndexResponse response = client.index(request);
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.CREATE_CHANNEL, channel.toLog()));
                return findById(channel.getName()).get();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_CHANNEL, channel.toLog());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
        return null;
    }


    /**
     * create new channels using the given XmlChannels
     *
     * @param channels - channels to be created
     * @return the created channels
     */
    public List<Channel> indexAll(List<Channel> channels) {

        List<Future<List<Channel>>> futures = new ArrayList<>();

        for (int i = 0; i < channels.size(); i += chunkSize) {
            List<Channel> chunk = channels.stream().skip(i).limit(chunkSize).collect(Collectors.toList());
            futures.add(executor.submit(() -> {
                BulkRequest.Builder br = new BulkRequest.Builder();
                for (Channel channel : chunk) {
                    br.operations(op -> op
                            .index(idx -> idx
                                    .index(esService.getES_CHANNEL_INDEX())
                                    .id(channel.getName())
                                    .document(JsonData.of(channel, new JacksonJsonpMapper(objectMapper)))
                            )
                    ).refresh(Refresh.True);
                }
                BulkResponse result;
                try {
                    result = client.bulk(br.build());
                    // Log errors, if any
                    if (result.errors()) {
                        logger.log(Level.SEVERE, TextUtil.BULK_HAD_ERRORS);
                        for (BulkResponseItem item : result.items()) {
                            if (item.error() != null) {
                                logger.log(Level.SEVERE, () -> item.error().reason());
                            }
                        }
                        // TODO cleanup? or throw exception?
                    } else {
                        return chunk;
                    }
                } catch (IOException e) {
                    String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_CHANNELS, chunk);
                    logger.log(Level.SEVERE, message, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);

                }
                return Collections.emptyList();
            }));
        }
        List<Channel> allIndexed = new ArrayList<>();
        for (Future<List<Channel>> future : futures) {
            try {
                allIndexed.addAll(future.get());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Bulk indexing failed", e);
            }
        }
        return allIndexed;
    }



    /**
     * update/save channel using the given Channel
     *
     * @param channelName - name of channel to be saved
     * @param channel - channel to be saved
     * @return the updated/saved channel
     */
    public Channel save(String channelName, Channel channel) {
        try {
            IndexResponse response = client.index(i -> i.index(esService.getES_CHANNEL_INDEX())
                    .id(channel.getName())
                    .document(JsonData.of(channel, new JacksonJsonpMapper(objectMapper)))
                    .refresh(Refresh.True));
            // verify the creation of the channel
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.CREATE_CHANNEL, channel.toLog()));
                return findById(channel.getName()).get();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_CHANNEL, channel.toLog());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
        return null;
    }

    /**
     *
     */
    @Override
    public Channel save(Channel channel) {
        return save(channel.getName(),channel);
    }

    /**
     * update/save channels using the given XmlChannels
     *
     * @param <S> extends Channel
     * @param channels - channels to be saved
     * @return the updated/saved channels
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends Channel> Iterable<S> saveAll(Iterable<S> channels) {
        // Create a list of all channel names
        List<String> ids = StreamSupport.stream(channels.spliterator(), false).map(Channel::getName).collect(Collectors.toList());

        try {
            Map<String, Channel> existingChannels = findAllById(ids).stream().collect(Collectors.toMap(Channel::getName, c -> c));

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Channel channel : channels) {
                if (existingChannels.containsKey(channel.getName())) {
                    // merge with existing channel
                    Channel updatedChannel = existingChannels.get(channel.getName());
                    if (channel.getOwner() != null && !channel.getOwner().isEmpty())
                        updatedChannel.setOwner(channel.getOwner());
                    updatedChannel.addProperties(channel.getProperties());
                    updatedChannel.addTags(channel.getTags());
                    br.operations(op -> op.index(i -> i.index(esService.getES_CHANNEL_INDEX())
                            .id(updatedChannel.getName())
                            .document(JsonData.of(updatedChannel, new JacksonJsonpMapper(objectMapper)))));
                } else {
                    br.operations(op -> op.index(i -> i.index(esService.getES_CHANNEL_INDEX())
                            .id(channel.getName())
                            .document(JsonData.of(channel, new JacksonJsonpMapper(objectMapper)))));
                }

            }
            BulkResponse result = null;
            result = client.bulk(br.refresh(Refresh.True).build());
            // Log errors, if any
            if (result.errors()) {
                logger.log(Level.SEVERE, TextUtil.BULK_HAD_ERRORS);
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        logger.log(Level.SEVERE, () -> item.error().reason());
                    }
                }
                // TODO cleanup? or throw exception?
            } else {
                return (Iterable<S>) findAllById(ids);
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_CHANNELS, channels);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);

        }
        return null;
    }

    /**
     * find channel using the given channel id
     *
     * @param channelName - name of channel to be found
     * @return the found channel
     */
    @Override
    public Optional<Channel> findById(String channelName) {
        GetResponse<Channel> response;
        try {
            response = client.get(g -> g.index(esService.getES_CHANNEL_INDEX()).id(channelName), Channel.class);

            if (response.found()) {
                Channel channel = response.source();
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.CHANNEL_FOUND, channel.getName()));
                return Optional.of(channel);
            } else {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.CHANNEL_NOT_FOUND, channelName));
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_FIND_CHANNEL, channelName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message, null);
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
                    .index(esService.getES_CHANNEL_INDEX())
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(esService.getES_QUERY_SIZE())
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<Channel> response = client.search(searchBuilder.build(), Channel.class);
            return new HashSet<>(response.hits()
                    .hits().stream().map(h -> h.source().getName()).collect(Collectors.toList()))
                    .containsAll(channelIds);
        } catch (ElasticsearchException | IOException e) {
            logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_CHANNELS, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_CHANNELS, null);
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
            builder.index(esService.getES_CHANNEL_INDEX()).id(channelName);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_CHECK_IF_CHANNEL_EXISTS, channelName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    /**
     * find all channels
     *
     * @return the found channels
     */
    @Override
    public Iterable<Channel> findAll() {
        throw new UnsupportedOperationException(TextUtil.FIND_ALL_CHANNELS_NOT_SUPPORTED);
    }

    /**
     * find channels using the given channels ids
     *
     * @param channelIds - ids of channels to be found
     * @return the found channels
     */
    @Override
    public List<Channel> findAllById(Iterable<String> channelIds) {
        try {
            List<String> ids = StreamSupport.stream(channelIds.spliterator(), false).collect(Collectors.toList());

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(esService.getES_CHANNEL_INDEX())
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(esService.getES_QUERY_SIZE())
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<Channel> response = client.search(searchBuilder.build(), Channel.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_CHANNELS, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_CHANNELS, null);
        }
    }

    @Override
    public long count() {
        return this.count(new LinkedMultiValueMap<>());
    }

    /**
     * delete the given channel by channel name
     *
     * @param channelName - channel to be deleted
     */
    @Override
    public void deleteById(String channelName) {
        try {
            DeleteResponse response = client
                    .delete(i -> i.index(esService.getES_CHANNEL_INDEX()).id(channelName).refresh(Refresh.True));
            // verify the deletion of the channel
            if (response.result().equals(Result.Deleted)) {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.DELETE_CHANNEL, channelName));
            }
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_DELETE_CHANNEL, channelName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    /**
     * delete the given channel
     *
     * @param channel - channel to be deleted
     */
    @Override
    public void delete(Channel channel) {
        deleteById(channel.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Channel> channels) {

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Channel channel : channels) {
            br.operations(op -> op
                    . delete(idx -> idx
                            .index(esService.getES_CHANNEL_INDEX())
                            .id(channel.getName()))
                    ).refresh(Refresh.True);
        }
        try {
            BulkResponse result = client.bulk(br.build());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
    }


    /**
     * Search for a list of channels based on their name, tags, and/or properties.
     * Search parameters ~name - The name of the channel ~tags - A list of comma
     * separated values ${propertyName}:${propertyValue} -
     * <p>
     * The query result is sorted based on the channel name ~size - The number of
     * channels to be returned ~from - The starting index of the channel list
     *
     * @param searchParameters channel search parameters
     * @return matching channels
     */
    public SearchResult search(MultiValueMap<String, String> searchParameters) {
        BuiltQuery builtQuery = getBuiltQuery(searchParameters);
        Integer finalSize = builtQuery.size;
        Integer finalFrom = builtQuery.from;

        if(builtQuery.size + builtQuery.from > esService.getES_MAX_RESULT_WINDOW_SIZE()) {
            String message = MessageFormat.format(TextUtil.SEARCH_FAILED_CAUSE,
                    searchParameters,
                    "Max search window exceeded, use the " + CFResourceDescriptors.SCROLL_RESOURCE_URI + " api.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        try {
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder();
            searchBuilder.index(esService.getES_CHANNEL_INDEX())
                            .query(builtQuery.boolQuery.build()._toQuery())
                            .from(finalFrom)
                            .size(finalSize)
                            .trackTotalHits(builder -> builder.enabled(builtQuery.trackTotalHits))
                            .sort(SortOptions.of(o -> o.field(FieldSort.of(f -> f.field("name")))));
            builtQuery.searchAfter.ifPresent(s -> searchBuilder.searchAfter(FieldValue.of(s)));

            SearchResponse<Channel> response = client.search(searchBuilder.build(),
                                                                Channel.class
            );

            List<Hit<Channel>> hits = response.hits().hits();
            long count = hits.size();
            if (builtQuery.trackTotalHits) {
                assert response.hits().total() != null;
                count = response.hits().total().value();
            }
            return new SearchResult(hits.stream().map(Hit::source).collect(Collectors.toList()), count);
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.SEARCH_FAILED_CAUSE, searchParameters, e.getMessage());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    private BuiltQuery getBuiltQuery(MultiValueMap<String, String> searchParameters) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        int size = esService.getES_QUERY_SIZE();
        int from = 0;
        boolean trackTotalHits = false;
        Optional<String> searchAfter = Optional.empty();
        String valueSplitPattern = "[|,;]";
        for (Map.Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            String key = parameter.getKey().trim();
            boolean isNot = key.endsWith("!");
                if (isNot) {
                    key = key.substring(0, key.length() - 1);
                }
            switch (key) {
                case "~name":
                    addNameQuery(parameter, valueSplitPattern, boolQuery);
                    break;
                case "~tag":
                    addTagsQuery(parameter, valueSplitPattern, isNot, boolQuery);
                    break;
                case "~size":
                    size = parseCountParameter(parameter, size);
                    break;
                case "~from":
                    from = parseCountParameter(parameter, from);
                    break;
                case "~search_after":
                    searchAfter = parameter.getValue().stream().findFirst();
                    break;

                case "~track_total_hits":
                    trackTotalHits = isTrackTotalHits(parameter, trackTotalHits);
                    break;
                default:
                    DisMaxQuery.Builder propertyQuery = calculatePropertiesQuery(parameter, valueSplitPattern, key, isNot);
                    boolQuery.must(propertyQuery.build()._toQuery());
                    break;
            }
        }
        return new BuiltQuery(boolQuery, size, from, searchAfter, trackTotalHits);
    }

    private static DisMaxQuery.Builder calculatePropertiesQuery(Map.Entry<String, List<String>> parameter, String valueSplitPattern, String key, boolean isNot) {
        DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
        for (String value : parameter.getValue()) {
            for (String pattern : value.split(valueSplitPattern)) {
                BoolQuery bq;
                bq = calculatePropertyQuery(key, isNot, pattern);
                addPropertyQuery(isNot, pattern, propertyQuery, bq);
            }
        }
        return propertyQuery;
    }

    private static void addPropertyQuery(boolean isNot, String pattern, DisMaxQuery.Builder propertyQuery, BoolQuery bq) {
        if (isNot && pattern.trim().equals("*")) {

            propertyQuery.queries(
                BoolQuery.of( p -> p.mustNot(
                    NestedQuery.of(n -> n.path("properties").query(bq._toQuery()))._toQuery()
                ))._toQuery()
            );
        } else {

            propertyQuery.queries(
                NestedQuery.of(n -> n.path("properties").query(bq._toQuery()))._toQuery()
            );
        }
    }

    private static BoolQuery calculatePropertyQuery(String key, boolean isNot, String pattern) {
        BoolQuery bq;
        if (isNot) {
            if (pattern.trim().equals("*")) {
                bq = BoolQuery.of(p -> p.must(getSingleValueQuery("properties.name", key)));
            } else {
                bq = BoolQuery.of(p -> p.must(getSingleValueQuery("properties.name", key))
                    .mustNot(getSingleValueQuery("properties.value", pattern.trim())));
            }
        } else {
            bq = BoolQuery.of(p -> p.must(getSingleValueQuery("properties.name", key))
                    .must(getSingleValueQuery("properties.value", pattern.trim())));
        }
        return bq;
    }

    private static boolean isTrackTotalHits(Map.Entry<String, List<String>> parameter, boolean trackTotalHits) {
        Optional<String> firstTrackTotalHits = parameter.getValue().stream().findFirst();
        if (firstTrackTotalHits.isPresent()) {
            trackTotalHits = Boolean.parseBoolean(firstTrackTotalHits.get());
        }
        return trackTotalHits;
    }

    private static int parseCountParameter(Map.Entry<String, List<String>> parameter, int size) {
        Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
        if (maxSize.isPresent()) {
            size = Integer.parseInt(maxSize.get());
        }
        return size;
    }

    private static void addTagsQuery(Map.Entry<String, List<String>> parameter, String valueSplitPattern, boolean isNot, BoolQuery.Builder boolQuery) {
        for (String value : parameter.getValue()) {
            DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
            for (String pattern : value.split(valueSplitPattern)) {
                tagQuery.queries(
                        NestedQuery.of(n -> n.path("tags").query(
                                getSingleValueQuery("tags.name", pattern.trim())))._toQuery());
            }
            if (isNot) {
                boolQuery.mustNot(tagQuery.build()._toQuery());
            } else {
                boolQuery.must(tagQuery.build()._toQuery());
            }

        }
    }

    private static void addNameQuery(Map.Entry<String, List<String>> parameter, String valueSplitPattern, BoolQuery.Builder boolQuery) {
        for (String value : parameter.getValue()) {
            DisMaxQuery.Builder nameQuery = new DisMaxQuery.Builder();
            for (String pattern : value.split(valueSplitPattern)) {
                nameQuery.queries(getSingleValueQuery("name", pattern.trim()));
            }
            boolQuery.must(nameQuery.build()._toQuery());
        }
    }

    private static Query getSingleValueQuery(String name, String pattern) {
        return WildcardQuery.of(w -> w.field(name).caseInsensitive(true).value(pattern))._toQuery();
    }

    private record BuiltQuery(BoolQuery.Builder boolQuery, Integer size, Integer from, Optional<String> searchAfter,
                              boolean trackTotalHits) {
    }

    /**
     * Match count
     * @param searchParameters channel search parameters
     * @return count of the number of matches to the provided query
     */
    public long count(MultiValueMap<String, String> searchParameters) {
        BuiltQuery builtQuery = getBuiltQuery(searchParameters);

        try {

            CountRequest.Builder countBuilder = new CountRequest.Builder();
            countBuilder.index(esService.getES_CHANNEL_INDEX()).query(builtQuery.boolQuery.build()._toQuery());
            CountResponse response = client.count(countBuilder.build());

            return response.count();
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.COUNT_FAILED_CAUSE, searchParameters, e.getMessage());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }


    /**
     * Match count
     * @param propertyName channel search property name
     * @param propertyValue channel search property value
     * @return count of the number of matches to the provided query
     */
    public long countByProperty(String propertyName, String propertyValue) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(propertyName, propertyValue == null? "*" : propertyValue);
        return this.count(params);
    }

    /**
     * Match count
     * @param tagName channel search tag
     * @return count of the number of matches to the provided query
     */
    public long countByTag(String tagName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("~tag", tagName);
        return this.count(params);
    }



    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // TODO Auto-generated method stub
        
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
