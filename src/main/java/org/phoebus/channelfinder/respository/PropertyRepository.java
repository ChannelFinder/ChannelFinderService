package org.phoebus.channelfinder.respository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.ElasticConfig;
import org.phoebus.channelfinder.TextUtil;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Property.OnlyNameOwnerProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Configuration
public class PropertyRepository implements CrudRepository<Property, String> {

  private static final Logger logger = Logger.getLogger(PropertyRepository.class.getName());

  @Autowired
  @Qualifier("indexClient")
  ElasticsearchClient client;

  @Autowired ElasticConfig esService;

  @Autowired ChannelRepository channelRepository;

  ObjectMapper objectMapper =
      new ObjectMapper().addMixIn(Property.class, OnlyNameOwnerProperty.class);

  /**
   * create a new property using the given Property
   *
   * @param property - property to be created
   * @return the created property
   */
  public Property index(Property property) {
    return save(property.getName(), property);
  }

  /**
   * create new properties using the given XmlProperties
   *
   * @param properties - properties to be created
   * @return the created properties
   */
  public List<Property> indexAll(List<Property> properties) {
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (Property property : properties) {
      br.operations(
          op ->
              op.index(
                  idx ->
                      idx.index(esService.getES_PROPERTY_INDEX())
                          .id(property.getName())
                          .document(JsonData.of(property, new JacksonJsonpMapper(objectMapper)))));
    }
    try {
      BulkResponse result = client.bulk(br.refresh(Refresh.True).build());
      // Log errors, if any
      if (result.errors()) {
        logger.log(Level.SEVERE, TextUtil.BULK_HAD_ERRORS);
        for (BulkResponseItem item : result.items()) {
          if (item.error() != null) {
            logger.log(Level.SEVERE, () -> item.error().reason());
          }
        }
      } else {
        return findAllById(properties.stream().map(Property::getName).toList());
      }
    } catch (IOException e) {
      String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_PROPERTIES, properties);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
    return null;
  }

  /**
   * update/save property using the given Property
   *
   * @param <S> extends Property
   * @param propertyName - name of property to be created
   * @param property - property to be created
   * @return the updated/saved property
   */
  @SuppressWarnings("unchecked")
  public <S extends Property> S save(String propertyName, S property) {
    try {
      IndexRequest request =
          IndexRequest.of(
              i ->
                  i.index(esService.getES_PROPERTY_INDEX())
                      .id(propertyName)
                      .document(JsonData.of(property, new JacksonJsonpMapper(objectMapper)))
                      .refresh(Refresh.True));

      IndexResponse response = client.index(request);
      // verify the creation of the tag
      if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
        logger.log(
            Level.CONFIG, () -> MessageFormat.format(TextUtil.CREATE_PROPERTY, property.toLog()));
        return (S) findById(propertyName).get();
      }
    } catch (Exception e) {
      String message = MessageFormat.format(TextUtil.FAILED_TO_INDEX_PROPERTY, property.toLog());
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
    return null;
  }

  /**
   * @param <S> extends Property
   */
  @Override
  public <S extends Property> S save(S property) {
    return save(property.getName(), property);
  }

  /**
   * update/save properties using the given XmlProperties
   *
   * @param <S> extends Property
   * @param properties - properties to be created
   * @return the updated/saved properties
   */
  @SuppressWarnings("unchecked")
  @Override
  public <S extends Property> Iterable<S> saveAll(Iterable<S> properties) {
    List<String> ids =
        StreamSupport.stream(properties.spliterator(), false).map(Property::getName).toList();

    BulkRequest.Builder br = new BulkRequest.Builder();

    for (Property property : properties) {
      br.operations(
          op ->
              op.index(
                  i ->
                      i.index(esService.getES_PROPERTY_INDEX())
                          .id(property.getName())
                          .document(JsonData.of(property, new JacksonJsonpMapper(objectMapper)))));
    }

    try {
      BulkResponse result = client.bulk(br.refresh(Refresh.True).build());
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
      String message = MessageFormat.format(TextUtil.FAILED_TO_UPDATE_SAVE_PROPERTIES, properties);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
    return null;
  }

  /**
   * find property using the given property id
   *
   * @param propertyId - id of property to be found
   * @return the found property
   */
  @Override
  public Optional<Property> findById(String propertyId) {
    return findById(propertyId, false);
  }

  /**
   * find property using the given property id
   *
   * @param propertyName - id of property to be found
   * @param withChannels - whether channels should be included
   * @return the found property
   */
  public Optional<Property> findById(String propertyName, boolean withChannels) {
    GetResponse<Property> response;
    try {
      response =
          client.get(
              g -> g.index(esService.getES_PROPERTY_INDEX()).id(propertyName), Property.class);

      if (response.found()) {
        Property property = response.source();
        logger.log(
            Level.CONFIG, () -> MessageFormat.format(TextUtil.PROPERTY_FOUND, property.getName()));
        if (withChannels) {
          MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
          params.add(property.getName(), "*");
          property.setChannels(channelRepository.search(params).channels());
        }
        return Optional.of(property);
      } else {
        logger.log(
            Level.CONFIG, () -> MessageFormat.format(TextUtil.PROPERTY_NOT_FOUND, propertyName));
        return Optional.empty();
      }
    } catch (ElasticsearchException | IOException e) {
      String message = MessageFormat.format(TextUtil.FAILED_TO_FIND_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message, null);
    }
  }

  @Override
  public boolean existsById(String id) {
    try {
      ExistsRequest.Builder builder = new ExistsRequest.Builder();
      builder.index(esService.getES_PROPERTY_INDEX()).id(id);
      return client.exists(builder.build()).value();
    } catch (ElasticsearchException | IOException e) {
      String message = MessageFormat.format(TextUtil.FAILED_TO_CHECK_IF_PROPERTY_EXISTS, id);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
  }

  /**
   * find all properties
   *
   * @return the found properties
   */
  @Override
  public Iterable<Property> findAll() {
    try {
      SearchRequest.Builder searchBuilder =
          new SearchRequest.Builder()
              .index(esService.getES_PROPERTY_INDEX())
              .query(new MatchAllQuery.Builder().build()._toQuery())
              .size(esService.getES_QUERY_SIZE())
              .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
      SearchResponse<Property> response = client.search(searchBuilder.build(), Property.class);
      return response.hits().hits().stream().map(Hit::source).toList();
    } catch (ElasticsearchException | IOException e) {
      logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_PROPERTIES, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_PROPERTIES, null);
    }
  }

  /**
   * find properties using the given property ids
   *
   * @param propertyIds - ids of properties to be found
   * @return the found properties
   */
  @Override
  public List<Property> findAllById(Iterable<String> propertyIds) {
    try {
      List<String> ids = StreamSupport.stream(propertyIds.spliterator(), false).toList();

      SearchRequest.Builder searchBuilder =
          new SearchRequest.Builder()
              .index(esService.getES_PROPERTY_INDEX())
              .query(IdsQuery.of(q -> q.values(ids))._toQuery())
              .size(esService.getES_QUERY_SIZE())
              .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
      SearchResponse<Property> response = client.search(searchBuilder.build(), Property.class);
      return response.hits().hits().stream().map(Hit::source).toList();
    } catch (ElasticsearchException | IOException e) {
      logger.log(Level.SEVERE, TextUtil.FAILED_TO_FIND_ALL_PROPERTIES, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.FAILED_TO_FIND_ALL_PROPERTIES, null);
    }
  }

  @Override
  public long count() {
    try {
      CountRequest countRequest =
          new CountRequest.Builder().index(esService.getES_PROPERTY_INDEX()).build();
      CountResponse countResponse = client.count(countRequest);
      return countResponse.count();
    } catch (ElasticsearchException | IOException e) {

      String message = MessageFormat.format(TextUtil.COUNT_FAILED_CAUSE, "", e.getMessage());
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
    }
  }

  /**
   * delete the given property by property name
   *
   * @param propertyName - name of property to be deleted
   */
  @Override
  public void deleteById(String propertyName) {
    try {
      DeleteResponse response =
          client.delete(
              i ->
                  i.index(esService.getES_PROPERTY_INDEX()).id(propertyName).refresh(Refresh.True));
      // verify the deletion of the property
      if (response.result().equals(Result.Deleted)) {
        logger.log(
            Level.CONFIG, () -> MessageFormat.format(TextUtil.DELETE_PROPERTY, propertyName));
      }

      // Remove the Property from Channels
      BulkRequest.Builder br = new BulkRequest.Builder().refresh(Refresh.True);
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add(propertyName, "*");
      List<Channel> channels = channelRepository.search(params).channels();
      while (channels.size() > 0) {
        for (Channel channel : channels) {
          channel.removeProperty(
              channel.getProperties().stream()
                  .filter(prop -> propertyName.equalsIgnoreCase(prop.getName()))
                  .findAny()
                  .get());
          br.operations(
              op ->
                  op.update(
                      u ->
                          u.index(esService.getES_CHANNEL_INDEX())
                              .id(channel.getName())
                              .action(a -> a.doc(channel))));
        }
        try {
          br.refresh(Refresh.True);
          BulkResponse result = client.bulk(br.build());
          // Log errors, if any
          if (result.errors()) {
            logger.log(Level.SEVERE, TextUtil.BULK_HAD_ERRORS);
            for (BulkResponseItem item : result.items()) {
              if (item.error() != null) {
                logger.log(Level.SEVERE, () -> item.error().reason());
              }
            }
          } else {
          }
        } catch (IOException e) {
          String message = MessageFormat.format(TextUtil.FAILED_TO_DELETE_PROPERTY, propertyName);
          logger.log(Level.SEVERE, message, e);
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
        params.set("~search_after", channels.get(channels.size() - 1).getName());
        channels = channelRepository.search(params).channels();
      }
    } catch (ElasticsearchException | IOException e) {
      String message = MessageFormat.format(TextUtil.FAILED_TO_DELETE_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }
  }

  /**
   * delete the given property
   *
   * @param property - property to be deleted
   */
  @Override
  public void delete(Property property) {
    deleteById(property.getName());
  }

  @Override
  public void deleteAll(Iterable<? extends Property> entities) {
    throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
  }

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException(TextUtil.DELETE_ALL_NOT_SUPPORTED);
  }

  @Override
  public void deleteAllById(Iterable<? extends String> ids) {
    // TODO Auto-generated method stub
  }
}
