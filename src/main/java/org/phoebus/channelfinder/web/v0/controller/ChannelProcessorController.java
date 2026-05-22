package org.phoebus.channelfinder.web.v0.controller;

import java.util.List;
import org.phoebus.channelfinder.service.ChannelProcessorService;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.phoebus.channelfinder.web.v0.api.IChannelProcessor;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;
import org.phoebus.channelfinder.web.v0.mapper.ChannelMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/processors")
public class ChannelProcessorController implements IChannelProcessor {

  private final ChannelProcessorService channelProcessorService;

  public ChannelProcessorController(ChannelProcessorService channelProcessorService) {
    this.channelProcessorService = channelProcessorService;
  }

  @Override
  public long processorCount() {
    return channelProcessorService.getProcessorCount();
  }

  @Override
  public List<ChannelProcessorInfo> processorInfo() {
    return channelProcessorService.getProcessorsInfo();
  }

  @Override
  public long processAllChannels() {
    return channelProcessorService.processAllChannels();
  }

  @Override
  public long processChannels(MultiValueMap<String, String> allRequestParams) {
    return channelProcessorService.processChannelsByQuery(allRequestParams);
  }

  @Override
  public void processChannels(List<ChannelDto> channels) {
    channelProcessorService.sendToProcessors(ChannelMapper.toDomains(channels));
  }

  @Override
  public void setProcessorEnabled(String processorName, Boolean enabled) {
    channelProcessorService.setProcessorEnabled(processorName, enabled);
  }
}
