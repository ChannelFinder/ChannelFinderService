package org.phoebus.channelfinder.service;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.common.TextUtil;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.exceptions.ChannelNotFoundException;
import org.phoebus.channelfinder.exceptions.ChannelValidationException;
import org.phoebus.channelfinder.exceptions.PropertyNotFoundException;
import org.phoebus.channelfinder.exceptions.TagNotFoundException;
import org.phoebus.channelfinder.exceptions.UnauthorizedException;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class ChannelService {

  private static final Logger audit = Logger.getLogger(ChannelService.class.getName() + ".audit");
  private static final Logger logger = Logger.getLogger(ChannelService.class.getName());

  private final ChannelRepository channelRepository;
  private final TagRepository tagRepository;
  private final PropertyRepository propertyRepository;
  private final AuthorizationService authorizationService;
  private final ChannelProcessorService channelProcessorService;

  public ChannelService(
      ChannelRepository channelRepository,
      TagRepository tagRepository,
      PropertyRepository propertyRepository,
      AuthorizationService authorizationService,
      ChannelProcessorService channelProcessorService) {
    this.channelRepository = channelRepository;
    this.tagRepository = tagRepository;
    this.propertyRepository = propertyRepository;
    this.authorizationService = authorizationService;
    this.channelProcessorService = channelProcessorService;
  }

  public List<Channel> query(MultiValueMap<String, String> allRequestParams) {
    return channelRepository.search(allRequestParams).channels();
  }

  public SearchResult combinedQuery(MultiValueMap<String, String> allRequestParams) {
    return channelRepository.search(allRequestParams);
  }

  public long queryCount(MultiValueMap<String, String> allRequestParams) {
    return channelRepository.count(allRequestParams);
  }

  public Channel read(String channelName) {
    audit.log(Level.INFO, () -> MessageFormat.format(TextUtil.FIND_CHANNEL, channelName));
    return channelRepository
        .findById(channelName)
        .orElseThrow(() -> new ChannelNotFoundException(channelName));
  }

  public Channel create(String channelName, Channel channel) {
    requireRole(ROLES.CF_CHANNEL, channelName);
    audit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CREATE_CHANNEL, channel.toLog()));

    validateChannel(channel);
    requireOwner(channel);

    Optional<Channel> existingChannel = channelRepository.findById(channelName);
    if (existingChannel.isPresent()) {
      requireOwner(existingChannel.get());
      channelRepository.deleteById(channelName);
    }

    resetOwnersToExisting(List.of(channel));

    Channel created = channelRepository.index(channel);
    channelProcessorService.sendToProcessors(List.of(created));
    return created;
  }

  public Iterable<Channel> create(Iterable<Channel> channels) {
    requireRole(ROLES.CF_CHANNEL, "channels batch");

    List<Channel> channelList = Lists.newArrayList(channels);
    Map<String, Channel> existing = findExistingChannels(channelList);

    for (Channel channel : channelList) {
      if (existing.containsKey(channel.getName())) {
        requireOwner(existing.get(channel.getName()));
        channel.setOwner(existing.get(channel.getName()).getOwner());
      } else {
        requireOwner(channel);
      }
    }

    validateChannels(channelList);
    channelRepository.deleteAll(channelList);
    resetOwnersToExisting(channelList);

    List<Channel> created = channelRepository.indexAll(channelList);
    channelProcessorService.sendToProcessors(created);
    return created;
  }

  public Channel update(String channelName, Channel channel) {
    requireRole(ROLES.CF_CHANNEL, channelName);
    validateChannel(channel);
    requireOwner(channel);

    Optional<Channel> existingChannel = channelRepository.findById(channelName);

    Channel newChannel;
    if (existingChannel.isPresent()) {
      requireOwner(existingChannel.get());
      newChannel = existingChannel.get();
      newChannel.setOwner(channel.getOwner());
      newChannel.addProperties(channel.getProperties());
      newChannel.addTags(channel.getTags());
      if (!channel.getName().equalsIgnoreCase(existingChannel.get().getName())) {
        channelRepository.deleteById(existingChannel.get().getName());
        newChannel.setName(channel.getName());
      }
    } else {
      newChannel = channel;
    }

    resetOwnersToExisting(List.of(channel));

    Channel updated = channelRepository.save(newChannel);
    channelProcessorService.sendToProcessors(List.of(updated));
    return updated;
  }

  public Iterable<Channel> update(Iterable<Channel> channels) {
    requireRole(ROLES.CF_CHANNEL, "channels batch");

    List<Channel> channelList = Lists.newArrayList(channels);
    Map<String, Channel> existing = findExistingChannels(channelList);

    for (Channel channel : channelList) {
      if (existing.containsKey(channel.getName())) {
        requireOwner(existing.get(channel.getName()));
        channel.setOwner(existing.get(channel.getName()).getOwner());
      } else {
        requireOwner(channel);
      }
    }

    validateChannels(channelList);
    resetOwnersToExisting(channelList);

    List<Channel> updated = Lists.newArrayList(channelRepository.saveAll(channelList));
    channelProcessorService.sendToProcessors(updated);
    return updated;
  }

  public void remove(String channelName) {
    requireRole(ROLES.CF_CHANNEL, channelName);
    audit.log(Level.INFO, () -> MessageFormat.format(TextUtil.DELETE_CHANNEL, channelName));

    Channel existing =
        channelRepository
            .findById(channelName)
            .orElseThrow(() -> new ChannelNotFoundException(channelName));
    requireOwner(existing);
    channelRepository.deleteById(channelName);
  }

  public long remove(Iterable<String> channelNames) {
    requireRole(ROLES.CF_CHANNEL, "channels batch");

    List<String> distinctChannelNames =
        StreamSupport.stream(channelNames.spliterator(), false)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();

    if (distinctChannelNames.isEmpty()) {
      return 0;
    }

    List<Channel> existingChannels = channelRepository.findAllById(distinctChannelNames);

    for (Channel existing : existingChannels) {
      requireOwner(existing);
      audit.log(
          Level.INFO, () -> MessageFormat.format(TextUtil.DELETE_CHANNEL, existing.getName()));
    }

    if (existingChannels.isEmpty()) {
      return 0;
    }

    return channelRepository.deleteAllByIdBestEffort(
        existingChannels.stream().map(Channel::getName).toList());
  }

  private Map<String, Channel> findExistingChannels(List<Channel> channels) {
    return channelRepository.findAllById(channels.stream().map(Channel::getName).toList()).stream()
        .collect(Collectors.toMap(Channel::getName, c -> c));
  }

  private void validateChannel(Channel channel) {
    if (channel.getName() == null || channel.getName().isEmpty()) {
      throw new ChannelValidationException(
          MessageFormat.format(TextUtil.CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY, channel.toLog()));
    }
    if (channel.getOwner() == null || channel.getOwner().isEmpty()) {
      throw new ChannelValidationException(
          MessageFormat.format(TextUtil.CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY, channel.toLog()));
    }
    for (Tag tag : channel.getTags()) {
      if (!tagRepository.existsById(tag.getName())) {
        throw new TagNotFoundException(tag.getName());
      }
    }
    checkPropertyValues(channel);
  }

  private void validateChannels(Iterable<Channel> channels) {
    List<String> existingProperties =
        StreamSupport.stream(propertyRepository.findAll().spliterator(), true)
            .map(Property::getName)
            .toList();
    List<String> existingTags =
        StreamSupport.stream(tagRepository.findAll().spliterator(), true)
            .map(Tag::getName)
            .toList();
    for (Channel channel : channels) {
      validateChannelAgainst(channel, existingTags, existingProperties);
    }
  }

  private void validateChannelAgainst(
      Channel channel, List<String> existingTags, List<String> existingProperties) {
    if (channel.getName() == null || channel.getName().isEmpty()) {
      throw new ChannelValidationException(
          MessageFormat.format(TextUtil.CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY, channel.toLog()));
    }
    if (channel.getOwner() == null || channel.getOwner().isEmpty()) {
      throw new ChannelValidationException(
          MessageFormat.format(TextUtil.CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY, channel.toLog()));
    }
    for (Tag tag : channel.getTags()) {
      if (!existingTags.contains(tag.getName())) {
        throw new TagNotFoundException(tag.getName());
      }
    }
    List<String> propNames = channel.getProperties().stream().map(Property::getName).toList();
    List<String> propValues = channel.getProperties().stream().map(Property::getValue).toList();
    for (String propName : propNames) {
      if (!existingProperties.contains(propName)) {
        throw new PropertyNotFoundException(propName);
      }
    }
    checkValues(propNames, propValues);
  }

  private void checkPropertyValues(Channel channel) {
    List<String> propNames = channel.getProperties().stream().map(Property::getName).toList();
    List<String> propValues = channel.getProperties().stream().map(Property::getValue).toList();
    for (String propName : propNames) {
      if (!propertyRepository.existsById(propName)) {
        throw new PropertyNotFoundException(propName);
      }
    }
    checkValues(propNames, propValues);
  }

  private void checkValues(List<String> propNames, List<String> propValues) {
    for (int i = 0; i < propValues.size(); i++) {
      String value = propValues.get(i);
      if (value == null || value.isEmpty()) {
        throw new ChannelValidationException(
            MessageFormat.format(TextUtil.PROPERTY_VALUE_NULL_OR_EMPTY, propNames.get(i), value));
      }
    }
  }

  private void requireRole(ROLES role, Object subject) {
    if (!authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), role)) {
      throw new UnauthorizedException(
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, subject));
    }
  }

  private void requireOwner(Channel channel) {
    if (!authorizationService.isAuthorizedOwner(
        SecurityContextHolder.getContext().getAuthentication(), channel)) {
      throw new UnauthorizedException(
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channel.toLog()));
    }
  }

  private void resetOwnersToExisting(Iterable<Channel> channels) {
    Map<String, String> propOwners =
        StreamSupport.stream(propertyRepository.findAll().spliterator(), true)
            .collect(Collectors.toUnmodifiableMap(Property::getName, Property::getOwner));
    Map<String, String> tagOwners =
        StreamSupport.stream(tagRepository.findAll().spliterator(), true)
            .collect(Collectors.toUnmodifiableMap(Tag::getName, Tag::getOwner));

    for (Channel channel : channels) {
      channel.getProperties().forEach(prop -> prop.setOwner(propOwners.get(prop.getName())));
      channel.getTags().forEach(tag -> tag.setOwner(tagOwners.get(tag.getName())));
    }
  }
}
