package org.phoebus.channelfinder.web.v0.controller;

import org.phoebus.channelfinder.service.ChannelScrollService;
import org.phoebus.channelfinder.web.v0.api.IChannelScroll;
import org.phoebus.channelfinder.web.v0.dto.ScrollDto;
import org.phoebus.channelfinder.web.v0.mapper.ChannelMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/scroll")
public class ChannelScrollController implements IChannelScroll {

  private final ChannelScrollService channelScrollService;

  public ChannelScrollController(ChannelScrollService channelScrollService) {
    this.channelScrollService = channelScrollService;
  }

  @Override
  public ScrollDto query(MultiValueMap<String, String> allRequestParams) {
    return toScrollDto(channelScrollService.search(null, allRequestParams));
  }

  @Override
  public ScrollDto query(String scrollId, MultiValueMap<String, String> searchParameters) {
    return toScrollDto(channelScrollService.search(scrollId, searchParameters));
  }

  private static ScrollDto toScrollDto(org.phoebus.channelfinder.entity.Scroll scroll) {
    return new ScrollDto(
        scroll.getId(), scroll.getChannels().stream().map(ChannelMapper::toDto).toList());
  }
}
