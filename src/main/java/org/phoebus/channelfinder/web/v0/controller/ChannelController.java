package org.phoebus.channelfinder.web.v0.controller;

import java.util.List;
import org.phoebus.channelfinder.service.ChannelService;
import org.phoebus.channelfinder.web.v0.api.IChannel;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;
import org.phoebus.channelfinder.web.v0.dto.SearchResultDto;
import org.phoebus.channelfinder.web.v0.mapper.ChannelMapper;
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
  public List<ChannelDto> query(MultiValueMap<String, String> allRequestParams) {
    return channelService.query(allRequestParams).stream().map(ChannelMapper::toDto).toList();
  }

  @Override
  public SearchResultDto combinedQuery(MultiValueMap<String, String> allRequestParams) {
    var result = channelService.combinedQuery(allRequestParams);
    return new SearchResultDto(
        result.channels().stream().map(ChannelMapper::toDto).toList(), result.count());
  }

  @Override
  public long queryCount(MultiValueMap<String, String> allRequestParams) {
    return channelService.queryCount(allRequestParams);
  }

  @Override
  public ChannelDto read(String channelName) {
    return ChannelMapper.toDto(channelService.read(channelName));
  }

  @Override
  public ChannelDto create(String channelName, ChannelDto channel) {
    return ChannelMapper.toDto(channelService.create(channelName, ChannelMapper.toDomain(channel)));
  }

  @Override
  public Iterable<ChannelDto> create(Iterable<ChannelDto> channels) {
    return ChannelMapper.toDtos(channelService.create(ChannelMapper.toDomains(channels)));
  }

  @Override
  public ChannelDto update(String channelName, ChannelDto channel) {
    return ChannelMapper.toDto(channelService.update(channelName, ChannelMapper.toDomain(channel)));
  }

  @Override
  public Iterable<ChannelDto> update(Iterable<ChannelDto> channels) {
    return ChannelMapper.toDtos(channelService.update(ChannelMapper.toDomains(channels)));
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
