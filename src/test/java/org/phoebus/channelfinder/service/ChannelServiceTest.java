package org.phoebus.channelfinder.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.exceptions.ChannelValidationException;
import org.phoebus.channelfinder.exceptions.PropertyNotFoundException;
import org.phoebus.channelfinder.exceptions.TagNotFoundException;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

  @Mock private ChannelRepository channelRepository;
  @Mock private TagRepository tagRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private AuthorizationService authorizationService;
  @Mock private ChannelProcessorService channelProcessorService;

  private ChannelService channelService;

  @BeforeEach
  void setUp() {
    channelService =
        new ChannelService(
            channelRepository,
            tagRepository,
            propertyRepository,
            authorizationService,
            channelProcessorService);
    when(authorizationService.isAuthorizedRole(any(), eq(ROLES.CF_CHANNEL))).thenReturn(true);
  }

  @Test
  void createChannel_nullName_throwsValidationException() {
    Channel channel = new Channel(null, "owner");

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_emptyName_throwsValidationException() {
    Channel channel = new Channel("", "owner");

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_nullOwner_throwsValidationException() {
    Channel channel = new Channel("ch", null);

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_emptyOwner_throwsValidationException() {
    Channel channel = new Channel("ch", "");

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_nonExistentTag_throwsTagNotFoundException() {
    Tag tag = new Tag("missing-tag", "owner");
    Channel channel = new Channel("ch", "owner", List.of(), List.of(tag));
    when(tagRepository.findAll()).thenReturn(List.of());

    assertThrows(TagNotFoundException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_nonExistentProperty_throwsPropertyNotFoundException() {
    Property prop = new Property("missing-prop", "owner", "val");
    Channel channel = new Channel("ch", "owner", List.of(prop), List.of());
    when(tagRepository.findAll()).thenReturn(List.of());
    when(propertyRepository.findAll()).thenReturn(List.of());

    assertThrows(PropertyNotFoundException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_nullPropertyValue_throwsValidationException() {
    Property prop = new Property("prop1", "owner", null);
    Channel channel = new Channel("ch", "owner", List.of(prop), List.of());
    when(tagRepository.findAll()).thenReturn(List.of());
    when(propertyRepository.findAll()).thenReturn(List.of(prop));

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_emptyPropertyValue_throwsValidationException() {
    Property prop = new Property("prop1", "owner", "");
    Channel channel = new Channel("ch", "owner", List.of(prop), List.of());
    when(tagRepository.findAll()).thenReturn(List.of());
    when(propertyRepository.findAll()).thenReturn(List.of(prop));

    assertThrows(ChannelValidationException.class, () -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_validChannel_noException() {
    Channel channel = new Channel("ch", "owner");
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findById("ch")).thenReturn(Optional.empty());
    when(channelRepository.index(any())).thenReturn(channel);
    when(propertyRepository.findAll()).thenReturn(List.of());
    when(tagRepository.findAll()).thenReturn(List.of());

    assertDoesNotThrow(() -> channelService.create("ch", channel));
  }

  @Test
  void createChannel_validChannelWithTagAndProperty_noException() {
    Tag tag = new Tag("tag1", "owner");
    Property prop = new Property("prop1", "owner", "value");
    Channel channel = new Channel("ch", "owner", List.of(prop), List.of(tag));
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findById("ch")).thenReturn(Optional.empty());
    when(channelRepository.index(any())).thenReturn(channel);
    when(propertyRepository.findAll()).thenReturn(List.of(prop));
    when(tagRepository.findAll()).thenReturn(List.of(tag));

    assertDoesNotThrow(() -> channelService.create("ch", channel));
  }
}
