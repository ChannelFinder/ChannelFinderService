package org.phoebus.channelfinder;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.entity.Tag.OnlyTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.search.Hit;

@Repository
@Configuration
public class TagRepository implements CrudRepository<Tag, String> {

    private static final Logger logger = Logger.getLogger(TagRepository.class.getName());

    @Autowired
    ElasticConfig esService;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Autowired
    ChannelRepository channelRepository;

    ObjectMapper objectMapper = new ObjectMapper().addMixIn(Tag.class, OnlyTag.class);

    /**
     * create a new tag using the given Tag
     * 
     * @param <S> extends Tag
     * @param tag - tag to be created
     * @return the created tag
     */
    public <S extends Tag> S index(S tag) {
        return save(tag.getName(), tag);
    }

    /**
     * create new tags using the given XmlTags
     *
     * @param tags - tags to be created
     * @return the created tags
     */
    public List<Tag> indexAll(List<Tag> tags) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Tag tag : tags) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(esService.getES_TAG_INDEX())
                            .id(tag.getName())
                            .document(JsonData.of(tag, new JacksonJsonpMapper(objectMapper)))));
        }
        try {
            BulkResponse result  = client.bulk(br.refresh(Refresh.True).build());
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
                return findAllById(tags.stream().map(Tag::getName).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_TAGS, tags);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
        return null;
    }

    /**
     * update/save tag using the given Tag
     * 
     * @param <S>     extends Tag
     * @param tagName - name of tag to be created
     * @param tag     - tag to be created
     * @return the updated/saved tag
     */
    @SuppressWarnings("unchecked")
    public <S extends Tag> S save(String tagName, S tag) {
        try{
            IndexResponse response = client
                    .index(i -> i.index(esService.getES_TAG_INDEX())
                            .id(tagName)
                            .document(JsonData.of(tag, new JacksonJsonpMapper(objectMapper)))
                            .refresh(Refresh.True));
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.CREATE_TAG, tag.toLog()));
                return (S) findById(tagName).get();
            }
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_UPDATE_SAVE_TAG, tag.toLog());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
        return null;
    }

    @Override
    public <S extends Tag> S save(S tag) {
        return save(tag.getName(), tag);
    }

    /**
     * update/save tags using the given XmlTags
     * 
     * @param <S>  extends Tag
     * @param tags - tags to be created
     * @return the updated/saved tags
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends Tag> Iterable<S> saveAll(Iterable<S> tags) {

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Tag tag : tags) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(esService.getES_TAG_INDEX())
                            .id(tag.getName())
                            .document(JsonData.of(tag, new JacksonJsonpMapper(objectMapper)))
                    )
            );
        }

        BulkResponse result = null;
        try {
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
                return (Iterable<S>) findAllById(
                        StreamSupport.stream(tags.spliterator(), false)
                                .map(Tag::getName)
                                .collect(Collectors.toList()));
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_TAGS, tags);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);

        }
        return null;
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId - id of tag to be found
     * @return the found tag
     */
    @Override
    public Optional<Tag> findById(String tagId) {
        return findById(tagId, false);
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId        - id of tag to be found
     * @param withChannels - whether channels should be included
     * @return the found tag
     */
    public Optional<Tag> findById(String tagId, boolean withChannels) {
        GetResponse<Tag> response;
        try {
            response = client.get(g -> g.index(esService.getES_TAG_INDEX()).id(tagId), Tag.class);

            if (response.found()) {
                Tag tag = response.source();
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.TAG_FOUND, tag.getName()));
                if(withChannels) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("~tag", tag.getName());
                    tag.setChannels(channelRepository.search(params).getChannels());
                }
                return Optional.of(tag);
            } else {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.TAG_NOT_FOUND, tagId));
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_FIND_TAG, tagId);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message, null);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(esService.getES_TAG_INDEX()).id(id);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_CHECK_IF_TAG_EXISTS, id);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    /**
     * find all tags
     * 
     * @return the found tags
     */
    @Override
    public Iterable<Tag> findAll() {
        try {
            SearchRequest.Builder searchBuilder = new Builder()
                    .index(esService.getES_TAG_INDEX())
                    .query(new MatchAllQuery.Builder().build()._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<Tag> response = client.search(searchBuilder.build(), Tag.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_TAGS, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_TAGS, null);
        }
    }

    /**
     * find tags using the given tags ids
     * 
     * @param tagIds - ids of tags to be found
     * @return the found tags
     */
    @Override
    public List<Tag> findAllById(Iterable<String> tagIds) {
        try {
            List<String> ids = StreamSupport.stream(tagIds.spliterator(), false).collect(Collectors.toList());
            SearchRequest.Builder searchBuilder = new Builder()
                    .index(esService.getES_TAG_INDEX())
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<Tag> response = client.search(searchBuilder.build(), Tag.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_TAGS, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_TAGS, null);
        }
    }

    @Override
    public long count() {
        try {
            CountRequest countRequest = new CountRequest.Builder().index(esService.getES_TAG_INDEX()).build();
            CountResponse countResponse = client.count(countRequest);
            return countResponse.count();
        } catch (ElasticsearchException | IOException e) {

            String message = MessageFormat.format(TextUtil.COUNT_FAILED_CAUSE, "", e.getMessage());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    /**
     * delete the given tag by tag name
     * 
     * @param tagName - tag to be deleted
     */
    @Override
    public void deleteById(String tagName) {
        try {

            DeleteResponse response = client
                    .delete(i -> i.index(esService.getES_TAG_INDEX()).id(tagName).refresh(Refresh.True));
            // verify the deletion of the tag
            if (response.result().equals(Result.Deleted)) {
                logger.log(Level.CONFIG, () -> MessageFormat.format(TextUtil.DELETE_TAG, tagName));
            }
            BulkRequest.Builder br = new BulkRequest.Builder().refresh(Refresh.True);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("~tag", tagName);
            List<Channel> channels = channelRepository.search(params).getChannels();
            while (channels.size() > 0) {

                for (Channel channel : channels) {
//                    br.operations(op -> op.update(
//                            u -> u.index(esService.getES_CHANNEL_INDEX())
//                                    .id(channel.getName())
//                                    .action(a -> a.script(
//                                                    Script.of(script -> script.inline(
//                                                            InlineScript.of(
//                                                                    i -> i.source("ctx._source.tags.removeIf(list_item -> list_item.name == params.remove_tag);")
//                                                                          .params("remove_tag", JsonData.of(tagName)))))))));
                    // Or
                    channel.removeTag(channel.getTags().stream().filter(tag -> tagName.equalsIgnoreCase(tag.getName())).findAny().get());
                    br.operations(op -> op.update(
                            u -> u.index(esService.getES_CHANNEL_INDEX())
                                    .id(channel.getName())
                                    .action(a -> a.doc(channel))));
                }
                try {
                    BulkResponse result = client.bulk(br.build());
                    // Log errors, if any
                    if (result.errors()) {
                        logger.log(Level.SEVERE, TextUtil.BULK_HAD_ERRORS);
                        for (BulkResponseItem item : result.items()) {
                            if (item.error() != null) {
                                logger.log(Level.SEVERE, () -> item.error().reason());
                            }
                        }
                    }
                } catch (IOException e) {
                    String message = MessageFormat.format(TextUtil.FAILED_TO_DELETE_TAG, tagName);
                    logger.log(Level.SEVERE, message, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);

                }
                params.set("~search_after", channels.get(channels.size() - 1).getName());
                channels = channelRepository.search(params).getChannels();
            }
            
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.FAILED_TO_DELETE_TAG, tagName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    /**
     * delete the given tag
     * 
     * @param tag - tag to be deleted
     */
    @Override
    public void delete(Tag tag) {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Tag> entities) {
        throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
    }

}
