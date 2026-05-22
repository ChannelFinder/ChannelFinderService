package org.phoebus.channelfinder.web.v0.mapper;

import java.util.List;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;

public final class ChannelMapper {

  private ChannelMapper() {}

  public static ChannelDto toDto(Channel channel) {
    return new ChannelDto(
        channel.getName(),
        channel.getOwner(),
        channel.getProperties().stream().map(PropertyMapper::toDto).toList(),
        channel.getTags().stream().map(TagMapper::toDto).toList());
  }

  public static Channel toDomain(ChannelDto dto) {
    Channel channel = new Channel();
    channel.setName(dto.name());
    channel.setOwner(dto.owner());
    channel.setProperties(dto.properties().stream().map(PropertyMapper::toDomain).toList());
    channel.setTags(dto.tags().stream().map(TagMapper::toDomain).toList());
    return channel;
  }

  public static List<ChannelDto> toDtos(Iterable<Channel> channels) {
    return StreamSupport.stream(channels.spliterator(), false).map(ChannelMapper::toDto).toList();
  }

  public static List<Channel> toDomains(Iterable<ChannelDto> dtos) {
    return StreamSupport.stream(dtos.spliterator(), false).map(ChannelMapper::toDomain).toList();
  }
}
