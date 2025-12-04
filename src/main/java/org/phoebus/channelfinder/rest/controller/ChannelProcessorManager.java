package org.phoebus.channelfinder.rest.controller;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.processors.ChannelProcessorInfo;
import org.phoebus.channelfinder.rest.api.IChannelScroll;
import org.phoebus.channelfinder.service.AuthorizationService;
import org.phoebus.channelfinder.service.ChannelProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@EnableAutoConfiguration
public class ChannelProcessorManager
    implements org.phoebus.channelfinder.rest.api.IChannelProcessorManager {

  private static final Logger logger = Logger.getLogger(ChannelProcessorManager.class.getName());

  @Autowired ChannelProcessorService channelProcessorService;
  @Autowired AuthorizationService authorizationService;

  // TODO replace with PIT and search_after
  @Autowired IChannelScroll channelScroll;

  @Value("${elasticsearch.query.size:10000}")
  private int defaultMaxSize;

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
    logger.log(Level.INFO, "Calling processor on ALL channels in ChannelFinder");
    // Only allow authorized users to trigger this operation
    if (authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(),
        AuthorizationService.ROLES.CF_ADMIN)) {
      MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
      searchParameters.add("~name", "*");
      return processChannels(searchParameters);
    } else {
      logger.log(
          Level.SEVERE,
          "User does not have the proper authorization to perform this operation: /process/all",
          new ResponseStatusException(HttpStatus.UNAUTHORIZED));
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "User does not have the proper authorization to perform this operation: /process/all");
    }
  }

  @Override
  public long processChannels(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams) {
    long channelCount = 0;
    Scroll scrollResult = channelScroll.query(allRequestParams);
    channelCount += scrollResult.getChannels().size();
    processChannels(scrollResult.getChannels());
    while (scrollResult.getChannels().size() == defaultMaxSize) {
      scrollResult = channelScroll.search(scrollResult.getId(), allRequestParams);
      channelCount += scrollResult.getChannels().size();
      processChannels(scrollResult.getChannels());
    }
    return channelCount;
  }

  @Override
  public void processChannels(List<Channel> channels) {
    channelProcessorService.sendToProcessors(channels);
  }

  @Override
  public void setProcessorEnabled(
      @PathVariable("processorName") String processorName,
      @Parameter(description = "Value of enabled to set, default value: true")
          @RequestParam(required = false, name = "enabled", defaultValue = "true")
          Boolean enabled) {
    channelProcessorService.setProcessorEnabled(processorName, enabled);
  }
}
