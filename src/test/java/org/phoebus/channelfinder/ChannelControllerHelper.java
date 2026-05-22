package org.phoebus.channelfinder;

import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.web.v0.api.IChannel;
import org.phoebus.channelfinder.web.v0.mapper.ChannelMapper;

class ChannelControllerHelper {

  private final IChannel manager;

  ChannelControllerHelper(IChannel manager) {
    this.manager = manager;
  }

  Channel apiCreate(String name, Channel channel) {
    return ChannelMapper.toDomain(manager.create(name, ChannelMapper.toDto(channel)));
  }

  Iterable<Channel> apiCreate(Iterable<Channel> channels) {
    return ChannelMapper.toDomains(manager.create(ChannelMapper.toDtos(channels)));
  }

  Channel apiRead(String name) {
    return ChannelMapper.toDomain(manager.read(name));
  }

  Channel apiUpdate(String name, Channel channel) {
    return ChannelMapper.toDomain(manager.update(name, ChannelMapper.toDto(channel)));
  }

  Iterable<Channel> apiUpdate(Iterable<Channel> channels) {
    return ChannelMapper.toDomains(manager.update(ChannelMapper.toDtos(channels)));
  }
}
