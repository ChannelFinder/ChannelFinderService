package org.phoebus.channelfinder.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  @Mock private ChannelPreProcessorService channelPreProcessorService;

  private ChannelService channelService;

  @BeforeEach
  void setUp() {
    channelService =
        new ChannelService(
            channelRepository,
            tagRepository,
            propertyRepository,
            authorizationService,
            channelProcessorService,
            channelPreProcessorService);
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

  @Test
  void createChannel_callsPreProcessorWithExistingChannel() {
    Channel existing = new Channel("ch", "owner");
    Channel channel = new Channel("ch", "owner");
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findById("ch")).thenReturn(Optional.of(existing));
    when(channelRepository.index(any())).thenReturn(channel);
    when(propertyRepository.findAll()).thenReturn(List.of());
    when(tagRepository.findAll()).thenReturn(List.of());

    channelService.create("ch", channel);

    verify(channelPreProcessorService).preProcess(List.of(channel), Map.of("ch", existing));
  }

  @Test
  void createChannels_callsPreProcessorWithExistingChannels() {
    Channel existing = new Channel("ch1", "owner");
    List<Channel> channels = List.of(new Channel("ch1", "owner"), new Channel("ch2", "owner"));
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findAllById(any())).thenReturn(List.of(existing));
    when(channelRepository.indexAll(any())).thenReturn(channels);
    when(propertyRepository.findAll()).thenReturn(List.of());
    when(tagRepository.findAll()).thenReturn(List.of());

    channelService.create(channels);

    verify(channelPreProcessorService).preProcess(channels, Map.of("ch1", existing));
  }

  @Test
  void updateChannel_preProcessorSeesExistingChannelBeforeMerge() {
    Property known = new Property("hostName", "owner");
    Channel existing =
        new Channel("ch", "owner", List.of(new Property("hostName", "owner", "ioc-a")), List.of());
    Channel channel =
        new Channel("ch", "owner", List.of(new Property("hostName", "owner", "ioc-b")), List.of());
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findById("ch")).thenReturn(Optional.of(existing));
    when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(propertyRepository.findAll()).thenReturn(List.of(known));
    when(tagRepository.findAll()).thenReturn(List.of());
    List<String> seenHostNames = new ArrayList<>();
    when(channelPreProcessorService.preProcess(any(), any()))
        .thenAnswer(
            inv -> {
              Map<String, Channel> existingChannels = inv.getArgument(1);
              seenHostNames.add(existingChannels.get("ch").getProperties().get(0).getValue());
              return null;
            });

    channelService.update("ch", channel);

    assertEquals(List.of("ioc-a"), seenHostNames);
  }

  @Test
  void updateChannels_callsPreProcessorWithExistingChannels() {
    Channel existing = new Channel("ch1", "owner");
    List<Channel> channels = List.of(new Channel("ch1", "owner"), new Channel("ch2", "owner"));
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findAllById(any())).thenReturn(List.of(existing));
    when(channelRepository.saveAll(any())).thenReturn(channels);
    when(propertyRepository.findAll()).thenReturn(List.of());
    when(tagRepository.findAll()).thenReturn(List.of());

    channelService.update(channels);

    verify(channelPreProcessorService).preProcess(channels, Map.of("ch1", existing));
  }

  @Test
  void removeMultipleChannels_validChannels_returnsDeletedCount() {
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findAllById(any()))
        .thenReturn(
            List.of(
                new Channel("ch1", "owner"),
                new Channel("ch2", "owner"),
                new Channel("ch3", "owner")));
    when(channelRepository.deleteAllByIdBestEffort(any())).thenReturn(3L);

    long deleted = channelService.remove(List.of("ch1", "ch2", "ch3"));

    assertEquals(3L, deleted);
    verify(channelRepository, times(1)).deleteAllByIdBestEffort(any());
    verify(channelRepository, never()).deleteAllById(any());
    verify(channelRepository, never()).deleteById(anyString());
  }

  @Test
  void removeMultipleChannels_whenOneMissing_deletesExistingAndReturnsDeletedCount() {
    when(authorizationService.isAuthorizedOwner(any(), any(Channel.class))).thenReturn(true);
    when(channelRepository.findAllById(any()))
        .thenReturn(List.of(new Channel("ch1", "owner"), new Channel("ch3", "owner")));
    when(channelRepository.deleteAllByIdBestEffort(eq(List.of("ch1", "ch3")))).thenReturn(2L);

    long deleted = channelService.remove(List.of("ch1", "missing", "ch3"));

    assertEquals(2L, deleted);
    verify(channelRepository, times(1)).deleteAllByIdBestEffort(eq(List.of("ch1", "ch3")));
    verify(channelRepository, never()).deleteAllById(any());
    verify(channelRepository, never()).deleteById(anyString());
  }
}
