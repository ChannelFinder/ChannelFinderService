package org.phoebus.channelfinder.service;

import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class ChannelScrollService {

  private final ChannelRepository channelRepository;

  public ChannelScrollService(ChannelRepository channelRepository) {
    this.channelRepository = channelRepository;
  }

  public Scroll search(String scrollId, MultiValueMap<String, String> searchParameters) {
    return channelRepository.scroll(scrollId, searchParameters);
  }
}
