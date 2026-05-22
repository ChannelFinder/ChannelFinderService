package org.phoebus.channelfinder.service;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.common.TextUtil;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.exceptions.ChannelNotFoundException;
import org.phoebus.channelfinder.exceptions.PropertyNotFoundException;
import org.phoebus.channelfinder.exceptions.PropertyValidationException;
import org.phoebus.channelfinder.exceptions.UnauthorizedException;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

  private static final Logger audit = Logger.getLogger(PropertyService.class.getName() + ".audit");
  private static final Logger logger = Logger.getLogger(PropertyService.class.getName());

  private final PropertyRepository propertyRepository;
  private final ChannelRepository channelRepository;
  private final AuthorizationService authorizationService;

  public PropertyService(
      PropertyRepository propertyRepository,
      ChannelRepository channelRepository,
      AuthorizationService authorizationService) {
    this.propertyRepository = propertyRepository;
    this.channelRepository = channelRepository;
    this.authorizationService = authorizationService;
  }

  public Iterable<Property> list() {
    return propertyRepository.findAll();
  }

  public Property read(String propertyName, boolean withChannels) {
    audit.log(Level.INFO, () -> MessageFormat.format(TextUtil.FIND_PROPERTY, propertyName));
    Optional<Property> found =
        withChannels
            ? propertyRepository.findById(propertyName, true)
            : propertyRepository.findById(propertyName);
    return found.orElseThrow(() -> new PropertyNotFoundException(propertyName));
  }

  public Property create(String propertyName, Property property) {
    requireRole(ROLES.CF_PROPERTY, propertyName);
    validateProperty(property);
    requireOwner(property);

    Optional<Property> existing = propertyRepository.findById(propertyName);
    if (existing.isPresent()) {
      requireOwner(existing.get());
      propertyRepository.deleteById(propertyName);
    }

    Property created = propertyRepository.index(property);

    if (!property.getChannels().isEmpty()) {
      Iterable<Channel> chans = channelRepository.saveAll(property.getChannels());
      List<Channel> chanList = new ArrayList<>();
      for (Channel chan : chans) chanList.add(chan);
      created.setChannels(chanList);
    }
    return created;
  }

  public Iterable<Property> create(Iterable<Property> properties) {
    requireRole(ROLES.CF_PROPERTY, "properties batch");

    checkPropertiesAuthorization(properties);
    validateProperties(properties);

    for (Property property : properties) {
      if (propertyRepository.existsById(property.getName())) {
        propertyRepository.deleteById(property.getName());
      }
    }

    propertyRepository.indexAll(Lists.newArrayList(properties));

    Map<String, Channel> channels = new HashMap<>();
    for (Property property : properties) {
      mergeChannelsIntoMap(property.getChannels(), channels);
    }

    if (!channels.isEmpty()) {
      channelRepository.saveAll(channels.values());
    }
    return properties;
  }

  public Property addSingle(String propertyName, String channelName, Property property) {
    requireRole(ROLES.CF_PROPERTY, propertyName);
    requireChannelExists(channelName);

    if (!propertyName.equals(property.getName())
        || property.getValue() == null
        || property.getValue().isEmpty()) {
      throw new PropertyValidationException(
          MessageFormat.format(
              TextUtil.PAYLOAD_PROPERTY_DOES_NOT_MATCH_URI_OR_HAS_BAD_VALUE, property.toLog()));
    }

    Property existing =
        propertyRepository
            .findById(propertyName)
            .orElseThrow(() -> new PropertyNotFoundException(propertyName));
    requireOwner(existing);

    Channel channel = channelRepository.findById(channelName).get();
    channel.addProperty(new Property(existing.getName(), existing.getOwner(), property.getValue()));
    Channel saved = channelRepository.save(channel);
    Property added = new Property(existing.getName(), existing.getOwner(), property.getValue());
    saved.setTags(new ArrayList<>());
    saved.setProperties(new ArrayList<>());
    added.setChannels(Arrays.asList(saved));
    return added;
  }

  public Property update(String propertyName, Property property) {
    requireRole(ROLES.CF_PROPERTY, propertyName);
    validateProperty(property);
    requireOwner(property);

    List<Channel> chans = new ArrayList<>();
    Optional<Property> existingOpt = propertyRepository.findById(propertyName, true);
    Property newProperty;
    if (existingOpt.isPresent()) {
      requireOwner(existingOpt.get());
      chans = existingOpt.get().getChannels();
      newProperty = existingOpt.get();
      newProperty.setOwner(property.getOwner());
      if (!property.getName().equalsIgnoreCase(existingOpt.get().getName())) {
        propertyRepository.deleteById(existingOpt.get().getName());
        newProperty.setName(property.getName());
      }
    } else {
      newProperty = property;
    }

    Property updated = propertyRepository.save(newProperty);

    propagateRenameToChannels(propertyName, updated, chans);

    if (!property.getChannels().isEmpty()) {
      List<Channel> chanList = saveAndRetainProperty(property.getChannels(), updated.getName());
      if (!chanList.isEmpty()) updated.setChannels(chanList);
    }

    return updated;
  }

  private List<Channel> saveAndRetainProperty(Iterable<Channel> channels, String propertyName) {
    List<Channel> result = new ArrayList<>();
    for (Channel chan : channelRepository.saveAll(channels)) {
      chan.setTags(new ArrayList<>());
      chan.setProperties(
          Collections.singletonList(
              chan.getProperties().stream()
                  .filter(p -> p.getName().equals(propertyName))
                  .findFirst()
                  .orElse(null)));
      result.add(chan);
    }
    return result;
  }

  public Iterable<Property> update(Iterable<Property> properties) {
    requireRole(ROLES.CF_PROPERTY, "properties batch");

    checkPropertiesAuthorization(properties);
    validateProperties(properties);

    Map<String, Channel> channels = new HashMap<>();
    for (Property property : properties) {
      if (propertyRepository.existsById(property.getName())) {
        mergeChannelsIntoMap(
            propertyRepository.findById(property.getName(), true).get().getChannels(), channels);
      }
    }
    for (Property property : properties) {
      mergeChannelsIntoMap(property.getChannels(), channels);
    }

    propertyRepository.saveAll(properties);

    if (!channels.isEmpty()) {
      channelRepository.saveAll(channels.values());
    }
    return properties;
  }

  public void remove(String propertyName) {
    requireRole(ROLES.CF_PROPERTY, propertyName);

    Property existing =
        propertyRepository
            .findById(propertyName)
            .orElseThrow(() -> new PropertyNotFoundException(propertyName));
    requireOwner(existing);
    propertyRepository.deleteById(propertyName);
  }

  public void removeSingle(String propertyName, String channelName) {
    requireRole(ROLES.CF_PROPERTY, propertyName);

    Property existing =
        propertyRepository
            .findById(propertyName)
            .orElseThrow(() -> new PropertyNotFoundException(propertyName));
    requireOwner(existing);

    Channel channel =
        channelRepository
            .findById(channelName)
            .orElseThrow(() -> new ChannelNotFoundException(channelName));
    channel.removeProperty(new Property(propertyName, ""));
    channelRepository.index(channel);
  }

  private void propagateRenameToChannels(
      String oldPropertyName, Property updated, List<Channel> existingChannels) {
    if (existingChannels.isEmpty()) return;
    List<Channel> toUpdate = new ArrayList<>();
    for (Channel chan : existingChannels) {
      boolean alreadyUpdated =
          updated.getChannels().stream().anyMatch(c -> c.getName().equals(chan.getName()));
      if (!alreadyUpdated) {
        chan.getProperties().stream()
            .filter(p -> p.getName().equals(oldPropertyName))
            .findFirst()
            .map(Property::getValue)
            .ifPresent(
                val -> {
                  chan.setProperties(
                      List.of(new Property(updated.getName(), updated.getOwner(), val)));
                  toUpdate.add(chan);
                });
      }
    }
    if (!toUpdate.isEmpty()) channelRepository.saveAll(toUpdate);
  }

  private void mergeChannelsIntoMap(Iterable<Channel> channels, Map<String, Channel> target) {
    for (Channel ch : channels) {
      if (target.containsKey(ch.getName())) {
        target.get(ch.getName()).addProperties(ch.getProperties());
      } else {
        target.put(ch.getName(), ch);
      }
    }
  }

  private void validateProperty(Property property) {
    if (property.getName() == null || property.getName().isEmpty()) {
      throw new PropertyValidationException(
          MessageFormat.format(TextUtil.PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY, property.toLog()));
    }
    if (property.getOwner() == null || property.getOwner().isEmpty()) {
      throw new PropertyValidationException(
          MessageFormat.format(TextUtil.PROPERTY_OWNER_CANNOT_BE_NULL_OR_EMPTY, property.toLog()));
    }
    for (Channel channel : property.getChannels()) {
      if (!channelRepository.existsById(channel.getName())) {
        throw new PropertyValidationException(
            MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channel.getName()));
      }
      boolean hasValidValue =
          channel.getProperties().stream()
              .anyMatch(
                  p ->
                      p.getName().equals(property.getName())
                          && p.getValue() != null
                          && !p.getValue().isEmpty());
      if (!hasValidValue) {
        throw new PropertyValidationException(
            MessageFormat.format(
                TextUtil.CHANNEL_NAME_NO_VALID_INSTANCE_PROPERTY,
                channel.getName(),
                property.getName()));
      }
    }
  }

  private void validateProperties(Iterable<Property> properties) {
    for (Property property : properties) {
      validateProperty(property);
    }
  }

  private void requireChannelExists(String channelName) {
    if (!channelRepository.existsById(channelName)) {
      throw new ChannelNotFoundException(channelName);
    }
  }

  private void requireRole(ROLES role, Object subject) {
    if (!authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), role)) {
      throw new UnauthorizedException(
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, subject));
    }
  }

  private void requireOwner(Property property) {
    if (!authorizationService.isAuthorizedOwner(
        SecurityContextHolder.getContext().getAuthentication(), property)) {
      throw new UnauthorizedException(
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, property.toLog()));
    }
  }

  private void checkPropertiesAuthorization(Iterable<Property> properties) {
    for (Property property : properties) {
      Optional<Property> existing = propertyRepository.findById(property.getName());
      if (existing.isPresent()) {
        requireOwner(existing.get());
        property.setOwner(existing.get().getOwner());
        property
            .getChannels()
            .forEach(chan -> chan.getProperties().get(0).setOwner(existing.get().getOwner()));
      } else {
        requireOwner(property);
      }
    }
  }
}
