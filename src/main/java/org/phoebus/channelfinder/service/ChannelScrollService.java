package org.phoebus.channelfinder.service;

import java.util.List;
import java.util.Map;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.springframework.stereotype.Service;

@Service
public class ChannelScrollService {

  private final ChannelRepository channelRepository;

  public ChannelScrollService(ChannelRepository channelRepository) {
    this.channelRepository = channelRepository;
  }

  public Scroll search(String scrollId, Map<String, List<String>> searchParameters) {
    return channelRepository.scroll(scrollId, searchParameters);
  }
}
