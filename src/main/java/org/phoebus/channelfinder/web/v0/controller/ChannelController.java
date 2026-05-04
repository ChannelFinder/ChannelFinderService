package org.phoebus.channelfinder.web.v0.controller;

import java.util.List;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.service.ChannelService;
import org.phoebus.channelfinder.web.v0.api.IChannel;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/channels")
public class ChannelController implements IChannel {

  private final ChannelService channelService;

  public ChannelController(ChannelService channelService) {
    this.channelService = channelService;
  }

  @Override
  public List<Channel> query(MultiValueMap<String, String> allRequestParams) {
    return channelService.query(allRequestParams);
  }

  @Override
  public SearchResult combinedQuery(MultiValueMap<String, String> allRequestParams) {
    return channelService.combinedQuery(allRequestParams);
  }

  @Override
  public long queryCount(MultiValueMap<String, String> allRequestParams) {
    return channelService.queryCount(allRequestParams);
  }

  @Override
  public Channel read(String channelName) {
    return channelService.read(channelName);
  }

  @Override
  public Channel create(String channelName, Channel channel) {
    return channelService.create(channelName, channel);
  }

  @Override
  public Iterable<Channel> create(Iterable<Channel> channels) {
    return channelService.create(channels);
  }

  @Override
  public Channel update(String channelName, Channel channel) {
    return channelService.update(channelName, channel);
  }

  @Override
  public Iterable<Channel> update(Iterable<Channel> channels) {
    return channelService.update(channels);
  }

  @Override
  public void remove(String channelName) {
    channelService.remove(channelName);
  }

  @Override
  public long remove(List<String> channelNames) {
    return channelService.remove(channelNames);
  }
}
