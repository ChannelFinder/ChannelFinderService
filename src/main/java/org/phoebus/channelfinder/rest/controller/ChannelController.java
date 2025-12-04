package org.phoebus.channelfinder.rest.controller;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Parameter;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.ServletContext;
import org.phoebus.channelfinder.common.TextUtil;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.PropertyRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.rest.api.IChannel;
import org.phoebus.channelfinder.service.AuthorizationService;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.phoebus.channelfinder.service.ChannelProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin
@RestController
@EnableAutoConfiguration
public class ChannelController implements IChannel {

  private static final Logger channelManagerAudit =
      Logger.getLogger(ChannelController.class.getName() + ".audit");
  private static final Logger logger = Logger.getLogger(ChannelController.class.getName());

  @Autowired private ServletContext servletContext;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ChannelRepository channelRepository;

  @Autowired AuthorizationService authorizationService;

  @Autowired ChannelProcessorService channelProcessorService;

  @Override
  public List<Channel> query(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams) {
    return channelRepository.search(allRequestParams).channels();
  }

  @Override
  public SearchResult combinedQuery(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams) {
    return channelRepository.search(allRequestParams);
  }

  @Override
  public long queryCount(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams) {
    return channelRepository.count(allRequestParams);
  }

  @Override
  public Channel read(@PathVariable("channelName") String channelName) {
    channelManagerAudit.log(
        Level.INFO, () -> MessageFormat.format(TextUtil.FIND_CHANNEL, channelName));

    Optional<Channel> foundChannel = channelRepository.findById(channelName);
    if (foundChannel.isPresent()) return foundChannel.get();
    else {
      String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }

  @Override
  public Channel create(
      @PathVariable("channelName") String channelName, @RequestBody Channel channel) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
      channelManagerAudit.log(
          Level.INFO, () -> MessageFormat.format(TextUtil.CREATE_CHANNEL, channel.toLog()));
      // Validate request parameters
      validateChannelRequest(channel);

      // check if authorized owner
      checkAndThrow(
          !authorizationService.isAuthorizedOwner(
              SecurityContextHolder.getContext().getAuthentication(), channel),
          TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
          channel,
          HttpStatus.UNAUTHORIZED);
      Optional<Channel> existingChannel = channelRepository.findById(channelName);
      boolean present = existingChannel.isPresent();
      if (present) {
        checkAndThrow(
            !authorizationService.isAuthorizedOwner(
                SecurityContextHolder.getContext().getAuthentication(), existingChannel.get()),
            TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
            existingChannel.get(),
            HttpStatus.UNAUTHORIZED);
        // delete existing channel
        channelRepository.deleteById(channelName);
      }

      // reset owners of attached tags/props back to existing owners
      channel
          .getProperties()
          .forEach(
              prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));
      channel
          .getTags()
          .forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));

      Channel createdChannel = channelRepository.index(channel);
      // process the results
      channelProcessorService.sendToProcessors(List.of(createdChannel));
      // create new channel
      return createdChannel;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public Iterable<Channel> create(@RequestBody Iterable<Channel> channels) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
      // check if authorized owner
      long start = System.currentTimeMillis();
      Map<String, Channel> existingChannels =
          channelRepository
              .findAllById(
                  StreamSupport.stream(channels.spliterator(), true).map(Channel::getName).toList())
              .stream()
              .collect(Collectors.toMap(Channel::getName, channel -> channel));
      for (Channel channel : channels) {
        boolean present = existingChannels.containsKey(channel.getName());
        if (present) {
          Channel existingChannel = existingChannels.get(channel.getName());
          checkAndThrow(
              !authorizationService.isAuthorizedOwner(
                  SecurityContextHolder.getContext().getAuthentication(), existingChannel),
              TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
              existingChannel,
              HttpStatus.UNAUTHORIZED);
          channel.setOwner(existingChannel.getOwner());
        } else {
          checkAndThrow(
              !authorizationService.isAuthorizedOwner(
                  SecurityContextHolder.getContext().getAuthentication(), channel),
              TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
              channel,
              HttpStatus.UNAUTHORIZED);
        }
      }
      logger.log(
          Level.INFO,
          "Completed Authorization check : " + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      // Validate request parameters
      validateChannelRequest(channels);
      logger.log(
          Level.INFO,
          "Completed validation check : " + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();

      // delete existing channels
      channelRepository.deleteAll(channels);
      logger.log(
          Level.INFO,
          "Completed replacement of Channels : " + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();

      // reset owners of attached tags/props back to existing owners
      resetOwnersToExisting(channels);

      logger.log(
          Level.INFO,
          "Completed reset tag and property ownership : "
              + (System.currentTimeMillis() - start)
              + "ms");
      start = System.currentTimeMillis();

      logger.log(Level.INFO, "Completed logging : " + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      List<Channel> createdChannels = channelRepository.indexAll(Lists.newArrayList(channels));

      logger.log(Level.INFO, "Completed indexing : " + (System.currentTimeMillis() - start) + "ms");
      // process the results
      channelProcessorService.sendToProcessors(createdChannels);
      // created new channel
      return createdChannels;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNELS, channels);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
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

  @Override
  public Channel update(
      @PathVariable("channelName") String channelName, @RequestBody Channel channel) {
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
      long start = System.currentTimeMillis();

      // Validate request parameters
      validateChannelRequest(channel);

      final long time = System.currentTimeMillis() - start;
      channelManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.PATH_POST_VALIDATION_TIME, servletContext.getContextPath(), time));

      // check if authorized owner
      checkAndThrow(
          !authorizationService.isAuthorizedOwner(
              SecurityContextHolder.getContext().getAuthentication(), channel),
          TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
          channel,
          HttpStatus.UNAUTHORIZED);
      Optional<Channel> existingChannel = channelRepository.findById(channelName);
      boolean present = existingChannel.isPresent();

      Channel newChannel;
      if (present) {
        checkAndThrow(
            !authorizationService.isAuthorizedOwner(
                SecurityContextHolder.getContext().getAuthentication(), existingChannel.get()),
            TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
            existingChannel.get(),
            HttpStatus.UNAUTHORIZED);
        newChannel = existingChannel.get();
        newChannel.setOwner(channel.getOwner());
        newChannel.addProperties(channel.getProperties());
        newChannel.addTags(channel.getTags());
        // Is an existing channel being renamed
        if (!channel.getName().equalsIgnoreCase(existingChannel.get().getName())) {
          // Since this is a rename operation we will need to remove the old channel.
          channelRepository.deleteById(existingChannel.get().getName());
          newChannel.setName(channel.getName());
        }
      } else {
        newChannel = channel;
      }

      // reset owners of attached tags/props back to existing owners
      channel
          .getProperties()
          .forEach(
              prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));
      channel
          .getTags()
          .forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));

      Channel updatedChannels = channelRepository.save(newChannel);
      // process the results
      channelProcessorService.sendToProcessors(List.of(updatedChannels));
      // created new channel
      return updatedChannels;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public Iterable<Channel> update(@RequestBody Iterable<Channel> channels) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
      long start = System.currentTimeMillis();

      for (Channel channel : channels) {
        Optional<Channel> existingChannel = channelRepository.findById(channel.getName());
        boolean present = existingChannel.isPresent();
        if (present) {
          checkAndThrow(
              !authorizationService.isAuthorizedOwner(
                  SecurityContextHolder.getContext().getAuthentication(), existingChannel.get()),
              TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
              existingChannel.get(),
              HttpStatus.UNAUTHORIZED);
          channel.setOwner(existingChannel.get().getOwner());
        } else {
          checkAndThrow(
              !authorizationService.isAuthorizedOwner(
                  SecurityContextHolder.getContext().getAuthentication(), channel),
              TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL,
              channel,
              HttpStatus.UNAUTHORIZED);
        }
      }

      // Validate request parameters
      validateChannelRequest(channels);

      final long time = System.currentTimeMillis() - start;
      channelManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.PATH_POST_PREPERATION_TIME, servletContext.getContextPath(), time));

      // reset owners of attached tags/props back to existing owners
      resetOwnersToExisting(channels);
      channelManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.PATH_POST_PREPERATION_TIME, servletContext.getContextPath(), time));

      // update channels
      List<Channel> updatedChannels =
          FluentIterable.from(channelRepository.saveAll(channels)).toList();
      // process the results
      channelProcessorService.sendToProcessors(updatedChannels);
      // created new channel
      return updatedChannels;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNELS, channels);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public void remove(@PathVariable("channelName") String channelName) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
      channelManagerAudit.log(
          Level.INFO, () -> MessageFormat.format(TextUtil.DELETE_CHANNEL, channelName));
      Optional<Channel> existingChannel = channelRepository.findById(channelName);
      if (existingChannel.isPresent()) {
        // check if authorized owner
        if (authorizationService.isAuthorizedOwner(
            SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
          // delete channel
          channelRepository.deleteById(channelName);
        } else {
          String message =
              MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
      } else {
        String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  /**
   * Checks if 1. the channel name is not null and matches the name in the body 2. the channel owner
   * is not null or empty 3. all the listed tags/props exist and prop value is not null or empty
   *
   * @param channel channel to be validated
   */
  @Override
  public void validateChannelRequest(Channel channel) {
    // 1
    checkAndThrow(
        channel.getName() == null || channel.getName().isEmpty(),
        TextUtil.CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY,
        channel,
        HttpStatus.BAD_REQUEST);
    // 2
    checkAndThrow(
        channel.getOwner() == null || channel.getOwner().isEmpty(),
        TextUtil.CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY,
        channel,
        HttpStatus.BAD_REQUEST);
    // 3
    checkTags(channel);
    // 3
    checkProperties(channel);
  }

  private void checkProperties(Channel channel) {
    List<String> propertyNames = channel.getProperties().stream().map(Property::getName).toList();
    List<String> propertyValues = channel.getProperties().stream().map(Property::getValue).toList();
    for (String propertyName : propertyNames) {
      if (!propertyRepository.existsById(propertyName)) {
        String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    }
    checkValues(propertyNames, propertyValues);
  }

  private void checkValues(List<String> propertyNames, List<String> propertyValues) {
    for (String propertyValue : propertyValues) {
      if (propertyValue == null || propertyValue.isEmpty()) {
        String message =
            MessageFormat.format(
                TextUtil.PROPERTY_VALUE_NULL_OR_EMPTY,
                propertyNames.get(propertyValues.indexOf(propertyValue)),
                propertyValue);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
      }
    }
  }

  private void checkTags(Channel channel) {
    List<String> tagNames = channel.getTags().stream().map(Tag::getName).toList();
    for (String tagName : tagNames) {
      if (!tagRepository.existsById(tagName)) {
        String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    }
  }

  private static void checkAndThrow(
      boolean channel,
      String channelNameCannotBeNullOrEmpty,
      Channel channel1,
      HttpStatus badRequest) {
    if (channel) {
      String message = MessageFormat.format(channelNameCannotBeNullOrEmpty, channel1.toLog());
      logger.log(Level.SEVERE, message, new ResponseStatusException(badRequest));
      throw new ResponseStatusException(badRequest, message, null);
    }
  }

  /**
   * Checks if 1. the tag names are not null 2. the tag owners are not null or empty 3. all the
   * channels exist
   *
   * @param channels list of channels to be validated
   */
  @Override
  public void validateChannelRequest(Iterable<Channel> channels) {
    List<String> existingProperties =
        StreamSupport.stream(propertyRepository.findAll().spliterator(), true)
            .map(Property::getName)
            .toList();
    List<String> existingTags =
        StreamSupport.stream(tagRepository.findAll().spliterator(), true)
            .map(Tag::getName)
            .toList();
    for (Channel channel : channels) {
      // 1
      checkAndThrow(
          channel.getName() == null || channel.getName().isEmpty(),
          TextUtil.CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY,
          channel,
          HttpStatus.BAD_REQUEST);
      // 2
      checkAndThrow(
          channel.getOwner() == null || channel.getOwner().isEmpty(),
          TextUtil.CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY,
          channel,
          HttpStatus.BAD_REQUEST);
      // 3
      List<String> tagNames = channel.getTags().stream().map(Tag::getName).toList();
      for (String tagName : tagNames) {
        if (!existingTags.contains(tagName)) {
          String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
      }
      // 3
      List<String> propertyNames = channel.getProperties().stream().map(Property::getName).toList();
      List<String> propertyValues =
          channel.getProperties().stream().map(Property::getValue).toList();
      for (String propertyName : propertyNames) {
        if (!existingProperties.contains(propertyName)) {
          String message =
              MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
      }
      checkValues(propertyNames, propertyValues);
    }
  }
}
