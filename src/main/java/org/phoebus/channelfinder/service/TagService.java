package org.phoebus.channelfinder.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.common.TextUtil;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.exceptions.ChannelNotFoundException;
import org.phoebus.channelfinder.exceptions.TagNotFoundException;
import org.phoebus.channelfinder.exceptions.TagValidationException;
import org.phoebus.channelfinder.exceptions.UnauthorizedException;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TagService {

  private static final Logger audit = Logger.getLogger(TagService.class.getName() + ".audit");
  private static final Logger logger = Logger.getLogger(TagService.class.getName());

  private final TagRepository tagRepository;
  private final ChannelRepository channelRepository;
  private final AuthorizationService authorizationService;

  public TagService(
      TagRepository tagRepository,
      ChannelRepository channelRepository,
      AuthorizationService authorizationService) {
    this.tagRepository = tagRepository;
    this.channelRepository = channelRepository;
    this.authorizationService = authorizationService;
  }

  public Iterable<Tag> list() {
    return tagRepository.findAll();
  }

  public Tag read(String tagName, boolean withChannels) {
    audit.log(Level.INFO, () -> MessageFormat.format(TextUtil.FIND_TAG, tagName));
    Optional<Tag> found =
        withChannels ? tagRepository.findById(tagName, true) : tagRepository.findById(tagName);
    return found.orElseThrow(() -> new TagNotFoundException(tagName));
  }

  public Tag create(String tagName, Tag tag) {
    requireRole(ROLES.CF_TAG, tagName);
    validateTag(tag);
    requireOwner(tag);

    Optional<Tag> existingTag = tagRepository.findById(tagName);
    if (existingTag.isPresent()) {
      requireOwner(existingTag.get());
      tagRepository.deleteById(tagName);
    }

    Tag created = tagRepository.index(tag);

    if (!tag.getChannels().isEmpty()) {
      tag.getChannels().forEach(chan -> chan.addTag(created));
      Iterable<Channel> chans = channelRepository.saveAll(tag.getChannels());
      List<Channel> chanList = new ArrayList<>();
      for (Channel chan : chans) chanList.add(chan);
      created.setChannels(chanList);
    }
    return created;
  }

  public Iterable<Tag> create(Iterable<Tag> tags) {
    requireRole(ROLES.CF_TAG, "tags batch");

    for (Tag tag : tags) {
      Optional<Tag> existing = tagRepository.findById(tag.getName());
      if (existing.isPresent()) {
        requireOwner(existing.get());
        tag.setOwner(existing.get().getOwner());
      } else {
        requireOwner(tag);
      }
    }

    validateTags(tags);

    for (Tag tag : tags) {
      if (tagRepository.existsById(tag.getName())) {
        tagRepository.deleteById(tag.getName());
      }
    }

    tagRepository.indexAll(StreamSupport.stream(tags.spliterator(), false).toList());

    Map<String, Channel> channels = new HashMap<>();
    for (Tag tag : tags) {
      for (Channel channel : tag.getChannels()) {
        if (channels.containsKey(channel.getName())) {
          channels.get(channel.getName()).addTag(new Tag(tag.getName(), tag.getOwner()));
        } else {
          channel.addTag(new Tag(tag.getName(), tag.getOwner()));
          channels.put(channel.getName(), channel);
        }
      }
    }

    if (!channels.isEmpty()) {
      channelRepository.saveAll(channels.values());
    }
    return tags;
  }

  public Tag addSingle(String tagName, String channelName) {
    requireRole(ROLES.CF_TAG, tagName);
    requireChannelExists(channelName);

    Tag existing =
        tagRepository.findById(tagName).orElseThrow(() -> new TagNotFoundException(tagName));
    requireOwner(existing);

    Channel channel = channelRepository.findById(channelName).get();
    channel.addTag(existing);
    Channel saved = channelRepository.save(channel);
    existing.setChannels(Arrays.asList(saved));
    return existing;
  }

  public Tag update(String tagName, Tag tag) {
    requireRole(ROLES.CF_TAG, tagName);
    validateTag(tag);
    requireOwner(tag);

    List<Channel> channels = new ArrayList<>();
    Optional<Tag> existingTag = tagRepository.findById(tagName, true);

    Tag newTag;
    if (existingTag.isPresent()) {
      requireOwner(existingTag.get());
      channels = existingTag.get().getChannels();
      newTag = existingTag.get();
      newTag.setOwner(tag.getOwner());
      if (!tag.getName().equalsIgnoreCase(existingTag.get().getName())) {
        tagRepository.deleteById(existingTag.get().getName());
        newTag.setName(tag.getName());
      }
    } else {
      newTag = tag;
    }

    Tag updated = tagRepository.save(newTag);

    if (!channels.isEmpty()) {
      channels.forEach(chan -> chan.addTag(updated));
    }
    if (!tag.getChannels().isEmpty()) {
      tag.getChannels().forEach(c -> c.addTag(updated));
      channels.addAll(tag.getChannels());
    }
    if (!channels.isEmpty()) {
      Iterable<Channel> updatedChannels = channelRepository.saveAll(channels);
      updated.setChannels(StreamSupport.stream(updatedChannels.spliterator(), false).toList());
    }

    return updated;
  }

  public Iterable<Tag> update(Iterable<Tag> tags) {
    requireRole(ROLES.CF_TAG, "tags batch");

    for (Tag tag : tags) {
      Optional<Tag> existing = tagRepository.findById(tag.getName());
      if (existing.isPresent()) {
        requireOwner(existing.get());
        tag.setOwner(existing.get().getOwner());
      } else {
        requireOwner(tag);
      }
    }

    validateTags(tags);

    Map<String, Channel> channels = new HashMap<>();
    for (Tag tag : tags) {
      for (Channel channel : tag.getChannels()) {
        if (channels.containsKey(channel.getName())) {
          channels.get(channel.getName()).addTag(new Tag(tag.getName(), tag.getOwner()));
        } else {
          channel.addTag(new Tag(tag.getName(), tag.getOwner()));
          channels.put(channel.getName(), channel);
        }
      }
    }

    tagRepository.saveAll(tags);

    if (!channels.isEmpty()) {
      channelRepository.saveAll(channels.values());
    }
    return tags;
  }

  public void remove(String tagName) {
    requireRole(ROLES.CF_TAG, tagName);

    Tag existing =
        tagRepository.findById(tagName).orElseThrow(() -> new TagNotFoundException(tagName));
    requireOwner(existing);
    tagRepository.deleteById(tagName);
  }

  public void removeSingle(String tagName, String channelName) {
    requireRole(ROLES.CF_TAG, tagName);

    Tag existingTag =
        tagRepository.findById(tagName).orElseThrow(() -> new TagNotFoundException(tagName));
    requireOwner(existingTag);

    Channel channel =
        channelRepository
            .findById(channelName)
            .orElseThrow(() -> new ChannelNotFoundException(channelName));
    channel.removeTag(new Tag(tagName, ""));
    channelRepository.index(channel);
  }

  private void validateTag(Tag tag) {
    if (tag.getName() == null || tag.getName().isEmpty()) {
      throw new TagValidationException(
          MessageFormat.format(TextUtil.TAG_NAME_CANNOT_BE_NULL_OR_EMPTY, tag.toLog()));
    }
    if (tag.getOwner() == null || tag.getOwner().isEmpty()) {
      throw new TagValidationException(
          MessageFormat.format(TextUtil.TAG_OWNER_CANNOT_BE_NULL_OR_EMPTY, tag.toLog()));
    }
    for (Channel channel : tag.getChannels()) {
      requireChannelExists(channel.getName());
    }
  }

  private void validateTags(Iterable<Tag> tags) {
    for (Tag tag : tags) {
      validateTag(tag);
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
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, subject));
    }
  }

  private void requireOwner(Tag tag) {
    if (!authorizationService.isAuthorizedOwner(
        SecurityContextHolder.getContext().getAuthentication(), tag)) {
      throw new UnauthorizedException(
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tag.toLog()));
    }
  }
}
