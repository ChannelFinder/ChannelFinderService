package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import org.phoebus.channelfinder.XmlTag.OnlyXmlTag;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.util.ObjectBuilder;

@Repository
@Configuration
public class TagRepository implements CrudRepository<XmlTag, String> {
    static Logger log = Logger.getLogger(TagRepository.class.getName());

    @Value("${elasticsearch.tag.index:cf_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Autowired
    ChannelRepository channelRepository;

    ObjectMapper objectMapper = new ObjectMapper().addMixIn(XmlTag.class, OnlyXmlTag.class);

    /**
     * create a new tag using the given XmlTag
     * 
     * @param <S> extends XmlTag
     * @param tag - tag to be created
     * @return the created tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S index(S tag) {
        try {
            IndexRequest request = IndexRequest.of(i -> i.index(ES_TAG_INDEX)
                    .id(tag.getName())
                    .document(tag)
                    .refresh(Refresh.True));

            IndexResponse response = client.index(request);
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created tag " + tag);
                return tag;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index tag " + tag.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index tag: " + tag, null);
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
    public List<XmlTag> indexAll(List<XmlTag> tags) {

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlTag tag : tags) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(ES_TAG_INDEX)
                            .id(tag.getName())
                            .document(tag)
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
                return findAllById(tags.stream().map(XmlTag::getName).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to index tags " + tags, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index tags: " + tags, null);

        }
        return null;
    }

    /**
     * update/save tag using the given XmlTag
     * 
     * @param <S>     extends XmlTag
     * @param tagName - name of tag to be created
     * @param tag     - tag to be created
     * @return the updated/saved tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S save(String tagName, S tag) {
        try {
            IndexResponse response = client
                    .index(i -> i.index(ES_TAG_INDEX).id(tagName).document(tag).refresh(Refresh.True));
            // verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created tag " + tag);
                return tag;
            }
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to update/save tag:" + tag.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update/save tag: " + tag,
                    null);
        }
        return null;
    }

    @Override
    public <S extends XmlTag> S save(S tag) {
        return save(tag.getName(), tag);
    }

    /**
     * update/save tags using the given XmlTags
     * 
     * @param <S>  extends XmlTag
     * @param tags - tags to be created
     * @return the updated/saved tags
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlTag> Iterable<S> saveAll(Iterable<S> tags) {

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlTag tag : tags) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(ES_TAG_INDEX)
                            .id(tag.getName())
                            .document(tag)
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
                return (Iterable<S>) findAllById(
                        StreamSupport.stream(tags.spliterator(), false)
                                .map(XmlTag::getName)
                                .collect(Collectors.toList()));
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to index tags " + tags, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index tags: " + tags, null);

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
    public Optional<XmlTag> findById(String tagId) {
        return findById(tagId, false);
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId        - id of tag to be found
     * @param withChannels - whether channels should be included
     * @return the found tag
     */
    public Optional<XmlTag> findById(String tagId, boolean withChannels) {
        GetResponse<XmlTag> response;
        try {
            response = client.get(g -> g.index(ES_TAG_INDEX).id(tagId), XmlTag.class);

            if (response.found()) {
                XmlTag tag = response.source();
                log.info("Tag name " + tag.getName());
                // TODO if (withChannels)
                if(withChannels) {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
                    params.add("~tag", tag.getName());
                    tag.setChannels(channelRepository.search(params));
                }
                return Optional.of(tag);
            } else {
                log.info("Tag not found");
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find tag " + tagId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tag: " + tagId, null);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_TAG_INDEX).id(id);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to check if tag " + id + " exists", e);
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
        try {
            SearchRequest.Builder searchBuilder = new Builder()
                    .index(ES_TAG_INDEX)
                    .query(new MatchAllQuery.Builder().build()._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlTag> response = client.search(searchBuilder.build(), XmlTag.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all tags", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all tags", null);
        }
    }

    /**
     * find tags using the given tags ids
     * 
     * @param tagIds - ids of tags to be found
     * @return the found tags
     */
    @Override
    public List<XmlTag> findAllById(Iterable<String> tagIds) {
        try {
            List<String> ids = StreamSupport.stream(tagIds.spliterator(), false).collect(Collectors.toList());

            SearchRequest.Builder searchBuilder = new Builder()
                    .index(ES_TAG_INDEX)
                    .query(IdsQuery.of(q -> q.values(ids))._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlTag> response = client.search(searchBuilder.build(), XmlTag.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all tags", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all tags", null);
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
        try {
            DeleteResponse response = client
                    .delete(i -> i.index(ES_TAG_INDEX).id(tagName).refresh(Refresh.True));
            // verify the deletion of the tag
            if (response.result().equals(Result.Deleted)) {
                log.config("Deletes tag " + tagName);
            }
            BulkRequest.Builder br = new BulkRequest.Builder();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add("~tag", tagName);
            List<XmlChannel> channels = channelRepository.search(params);
            while (channels.size() > 0) {

                for (XmlChannel channel : channels) {
//                    br.operations(op -> op.update(
//                            u -> u.index(ES_CHANNEL_INDEX)
//                                    .id(channel.getName())
//                                    .action(a -> a.script(
//                                                    Script.of(script -> script.inline(
//                                                            InlineScript.of(
//                                                                    i -> i.source("ctx._source.tags.removeIf(list_item -> list_item.name == params.remove_tag);")
//                                                                          .params("remove_tag", JsonData.of(tagName)))))))));
                    // Or
                    channel.removeTag(channel.getTags().stream().filter(tag -> tagName.equalsIgnoreCase(tag.getName())).findAny().get());
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
                    log.log(Level.SEVERE, "Failed to delete tag " + tagName, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete tag: " + tagName, null);

                }
                params.set("~search_after", channels.get(channels.size() - 1).getName());
                channels = channelRepository.search(params);
            }
            
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to delete tag:" + tagName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete tag: " + tagName,
                    null);
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

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }
}
