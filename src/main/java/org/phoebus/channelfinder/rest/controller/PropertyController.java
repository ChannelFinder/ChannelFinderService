package org.phoebus.channelfinder.rest.controller;

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
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.PropertyRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.rest.api.IProperty;
import org.phoebus.channelfinder.service.AuthorizationService;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin
@RestController
@EnableAutoConfiguration
public class PropertyController implements IProperty {

  private static final Logger propertyManagerAudit =
      Logger.getLogger(PropertyController.class.getName() + ".audit");
  private static final Logger logger = Logger.getLogger(PropertyController.class.getName());

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ChannelRepository channelRepository;

  @Autowired AuthorizationService authorizationService;

  @Override
  public Iterable<Property> list() {
    return propertyRepository.findAll();
  }

  @Override
  public Property read(String propertyName, boolean withChannels) {
    propertyManagerAudit.log(
        Level.INFO, () -> MessageFormat.format(TextUtil.FIND_PROPERTY, propertyName));

    Optional<Property> foundProperty;
    if (withChannels) {
      foundProperty = propertyRepository.findById(propertyName, true);
    } else {
      foundProperty = propertyRepository.findById(propertyName);
    }
    if (foundProperty.isPresent()) {
      return foundProperty.get();
    } else {
      String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }

  @Override
  public Property create(String propertyName, Property property) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      long start = System.currentTimeMillis();
      propertyManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
      // Validate request parameters
      validatePropertyRequest(property);

      // check if authorized owner
      if (!authorizationService.isAuthorizedOwner(
          SecurityContextHolder.getContext().getAuthentication(), property)) {
        String message =
            MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, property.toLog());
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
      }
      Optional<Property> existingProperty = propertyRepository.findById(propertyName);
      boolean present = existingProperty.isPresent();
      if (present) {
        checkPropertyAuthorization(existingProperty);
        // delete existing property
        propertyRepository.deleteById(propertyName);
      }

      // create new property
      Property createdProperty = propertyRepository.index(property);

      if (!property.getChannels().isEmpty()) {
        // update the listed channels in the property's payload with the new property
        Iterable<Channel> chans = channelRepository.saveAll(property.getChannels());
        // TODO validate the above result
        List<Channel> chanList = new ArrayList<>();
        for (Channel chan : chans) {
          chanList.add(chan);
        }
        createdProperty.setChannels(chanList);
      }
      return createdProperty;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public Iterable<Property> create(Iterable<Property> properties) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      long start = System.currentTimeMillis();
      propertyManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));

      // check if authorized owner
      checkPropertiesAuthorization(properties);

      // Validate request parameters
      validatePropertyRequest(properties);

      // delete existing property
      for (Property property : properties) {
        if (propertyRepository.existsById(property.getName())) {
          // delete existing property
          propertyRepository.deleteById(property.getName());
        }
      }

      // create new properties
      propertyRepository.indexAll(Lists.newArrayList(properties));

      // update the listed channels in the properties' payloads with the new
      // properties
      Map<String, Channel> channels = new HashMap<>();
      for (Property property : properties) {
        for (Channel ch : property.getChannels()) {
          if (channels.containsKey(ch.getName())) {
            channels.get(ch.getName()).addProperties(ch.getProperties());
          } else {
            channels.put(ch.getName(), ch);
          }
        }
      }

      if (!channels.isEmpty()) {
        Iterable<Channel> chans = channelRepository.saveAll(channels.values());
      }
      // TODO should return created props with properly organized saved channels, but it would be
      // very complicated...
      return properties;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTIES, properties);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public Property addSingle(String propertyName, String channelName, Property property) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      long start = System.currentTimeMillis();
      propertyManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
      // Validate request parameters
      validatePropertyRequest(channelName);
      if (!propertyName.equals(property.getName())
          || property.getValue().isEmpty()
          || property.getValue() == null) {
        String message =
            MessageFormat.format(
                TextUtil.PAYLOAD_PROPERTY_DOES_NOT_MATCH_URI_OR_HAS_BAD_VALUE, property.toLog());
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
      }

      // check if authorized owner
      Optional<Property> existingProperty = propertyRepository.findById(propertyName);
      boolean present = existingProperty.isPresent();
      if (present) {
        checkPropertyAuthorization(existingProperty);
        // add property to channel
        Channel channel = channelRepository.findById(channelName).get();
        Property prop = existingProperty.get();
        channel.addProperty(new Property(prop.getName(), prop.getOwner(), property.getValue()));
        Channel taggedChannel = channelRepository.save(channel);
        Property addedProperty = new Property(prop.getName(), prop.getOwner(), property.getValue());
        taggedChannel.setTags(new ArrayList<>());
        taggedChannel.setProperties(new ArrayList<>());
        addedProperty.setChannels(Arrays.asList(taggedChannel));
        return addedProperty;
      } else {
        String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public Property update(String propertyName, Property property) {
    // check if authorized role
    if (!authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }

    long start = System.currentTimeMillis();
    propertyManagerAudit.log(
        Level.INFO,
        () ->
            MessageFormat.format(
                TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
    // Validate request parameters
    validatePropertyRequest(property);

    // check if authorized owner
    if (!authorizationService.isAuthorizedOwner(
        SecurityContextHolder.getContext().getAuthentication(), property)) {
      String message =
          MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, property.toLog());
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }

    List<Channel> chans = new ArrayList<>();
    Optional<Property> existingProperty = propertyRepository.findById(propertyName, true);
    Property newProperty;
    if (existingProperty.isPresent()) {
      checkPropertyAuthorization(existingProperty);
      chans = existingProperty.get().getChannels();
      newProperty = existingProperty.get();
      newProperty.setOwner(property.getOwner());
      // Is an existing channel being renamed
      if (!property.getName().equalsIgnoreCase(existingProperty.get().getName())) {
        // Since this is a rename operation we will need to remove the old channel.
        propertyRepository.deleteById(existingProperty.get().getName());
        newProperty.setName(property.getName());
      }
    } else {
      newProperty = property;
    }

    // update property
    Property updatedProperty = propertyRepository.save(newProperty);

    // update channels of existing property
    if (!chans.isEmpty()) {
      List<Channel> chanList = new ArrayList<>();
      for (Channel chan : chans) {
        boolean alreadyUpdated =
            updatedProperty.getChannels().stream()
                .anyMatch(c -> c.getName().equals(chan.getName()));

        if (!alreadyUpdated) {
          Optional<String> value =
              chan.getProperties().stream()
                  .filter(p -> p.getName().equals(propertyName))
                  .findFirst()
                  .map(Property::getValue);
          if (value.isPresent()) {
            Property prop = new Property(property.getName(), property.getOwner(), value.get());
            chan.setProperties(List.of(prop));
            chanList.add(chan);
          }
        }
      }
      if (!chanList.isEmpty()) channelRepository.saveAll(chanList);
    }

    if (!property.getChannels().isEmpty()) {
      // update the listed channels in the property's payloads with the new property
      Iterable<Channel> channels = channelRepository.saveAll(property.getChannels());
      List<Channel> chanList = new ArrayList<>();
      Property p = null;
      for (Channel chan : channels) {
        chan.setTags(new ArrayList<>());
        for (Property prop : chan.getProperties()) {
          if (prop.getName().equals(propertyName)) p = prop;
        }
        chan.setProperties(Collections.singletonList(p));
        chanList.add(chan);
      }
      if (!chanList.isEmpty()) updatedProperty.setChannels(chanList);
    }

    return updatedProperty;
  }

  @Override
  public Iterable<Property> update(Iterable<Property> properties) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      long start = System.currentTimeMillis();
      propertyManagerAudit.log(
          Level.INFO,
          () ->
              MessageFormat.format(
                  TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));

      // check if authorized owner
      checkPropertiesAuthorization(properties);

      // Validate request parameters
      validatePropertyRequest(properties);

      // prepare the list of channels which need to be updated with the new properties
      Map<String, Channel> channels = new HashMap<>();

      // import the old properties
      for (Property property : properties) {
        if (propertyRepository.existsById(property.getName())) {
          for (Channel ch :
              propertyRepository.findById(property.getName(), true).get().getChannels()) {
            if (channels.containsKey(ch.getName())) {
              channels.get(ch.getName()).addProperties(ch.getProperties());
            } else {
              channels.put(ch.getName(), ch);
            }
          }
        }
      }
      // set the new properties
      for (Property property : properties) {
        for (Channel ch : property.getChannels()) {
          if (channels.containsKey(ch.getName())) {
            channels.get(ch.getName()).addProperties(ch.getProperties());
          } else {
            channels.put(ch.getName(), ch);
          }
        }
      }

      // update properties
      Iterable<Property> updatedProperties = propertyRepository.saveAll(properties);

      // update channels
      if (!channels.isEmpty()) {
        channelRepository.saveAll(channels.values());
      }
      // TODO should return updated props with properly organized saved channels, but it would be
      // very complicated...
      return properties;
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTIES, properties);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  private void checkPropertiesAuthorization(Iterable<Property> properties) {
    for (Property property : properties) {
      Optional<Property> existingProperty = propertyRepository.findById(property.getName());
      boolean present = existingProperty.isPresent();
      if (present) {
        checkPropertyAuthorization(existingProperty);
        property.setOwner(existingProperty.get().getOwner());
        property
            .getChannels()
            .forEach(
                chan -> chan.getProperties().get(0).setOwner(existingProperty.get().getOwner()));
      } else {
        if (!authorizationService.isAuthorizedOwner(
            SecurityContextHolder.getContext().getAuthentication(), property)) {
          String message =
              MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, property.toLog());
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
      }
    }
  }

  private void checkPropertyAuthorization(Optional<Property> existingProperty) {
    if (!authorizationService.isAuthorizedOwner(
        SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
      String message =
          MessageFormat.format(
              TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, existingProperty.get().toLog());
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public void remove(String propertyName) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      Optional<Property> existingProperty = propertyRepository.findById(propertyName);
      if (existingProperty.isPresent()) {
        // check if authorized owner
        if (authorizationService.isAuthorizedOwner(
            SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
          // delete property
          propertyRepository.deleteById(propertyName);
        } else {
          String message =
              MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
      } else {
        String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  @Override
  public void removeSingle(final String propertyName, String channelName) {
    // check if authorized role
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
      Optional<Property> existingProperty = propertyRepository.findById(propertyName);
      if (existingProperty.isPresent()) {
        // check if authorized owner
        if (authorizationService.isAuthorizedOwner(
            SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
          Optional<Channel> ch = channelRepository.findById(channelName);
          if (ch.isPresent()) {
            // remove property from channel
            Channel channel = ch.get();
            channel.removeProperty(new Property(propertyName, ""));
            channelRepository.index(channel);
          } else {
            String message =
                MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
          }
        } else {
          String message =
              MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
          logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
      } else {
        String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      }
    } else {
      String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_PROPERTY, propertyName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
    }
  }

  /**
   * Checks if 1. the property name is not null and matches the name in the body 2. the property
   * owner is not null or empty 3. all the listed channels exist and have the property with a non
   * null and non empty value
   *
   * @param property validate property
   */
  @Override
  public void validatePropertyRequest(Property property) {
    // 1
    if (property.getName() == null || property.getName().isEmpty()) {
      String message =
          MessageFormat.format(TextUtil.PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY, property.toLog());
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
    }
    // 2
    if (property.getOwner() == null || property.getOwner().isEmpty()) {
      String message =
          MessageFormat.format(TextUtil.PROPERTY_OWNER_CANNOT_BE_NULL_OR_EMPTY, property.toLog());
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
    }
    // 3
    property.getChannels().stream()
        .forEach(
            (channel) -> {
              // Check if all the channels exists
              if (!channelRepository.existsById(channel.getName())) {
                String message =
                    MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channel.getName());
                logger.log(
                    Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
              }
              // Check if the channel data has the requested property attached with a non null - non
              // empty value
              if (!channel.getProperties().stream()
                  .anyMatch(
                      p ->
                          p.getName().equals(property.getName())
                              && p.getValue() != null
                              && !p.getValue().isEmpty())) {
                String message =
                    MessageFormat.format(
                        TextUtil.CHANNEL_NAME_NO_VALID_INSTANCE_PROPERTY,
                        channel.getName(),
                        property.toLog());
                logger.log(
                    Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
              }
            });
  }

  /**
   * Checks if 1. the property name is not null and matches the name in the body 2. the property
   * owner is not null or empty 3. the property value is not null or empty 4. all the listed
   * channels exist
   *
   * @param properties properties to be validated
   */
  @Override
  public void validatePropertyRequest(Iterable<Property> properties) {
    for (Property property : properties) {
      validatePropertyRequest(property);
    }
  }

  /**
   * Checks if the channel exists
   *
   * @param channelName check channel exists
   */
  @Override
  public void validatePropertyRequest(String channelName) {
    if (!channelRepository.existsById(channelName)) {
      String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
      logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }
}
