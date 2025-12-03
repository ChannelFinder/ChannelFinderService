package org.phoebus.channelfinder.processors;

import static org.phoebus.channelfinder.CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI;
import static org.phoebus.channelfinder.CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.ChannelScroll;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.service.AuthorizationService;
import org.phoebus.channelfinder.service.ChannelProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(CHANNEL_PROCESSOR_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelProcessorManager {

  private static final Logger logger = Logger.getLogger(ChannelProcessorManager.class.getName());

  @Autowired ChannelProcessorService channelProcessorService;
  @Autowired AuthorizationService authorizationService;

  // TODO replace with PIT and search_after
  @Autowired ChannelScroll channelScroll;

  @Value("${elasticsearch.query.size:10000}")
  private int defaultMaxSize;

  @Operation(
      summary = "Get processor count",
      description = "Returns the number of channel processors.",
      operationId = "getProcessorCount",
      tags = {"ChannelProcessor"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Number of channel-processors",
            content = @Content(schema = @Schema(implementation = Long.class)))
      })
  @GetMapping("/count")
  public long processorCount() {
    return channelProcessorService.getProcessorCount();
  }

  @Operation(
      summary = "Get processor info",
      description = "Returns information about all channel processors.",
      operationId = "getProcessorInfo",
      tags = {"ChannelProcessor"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of processor-info",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ChannelProcessorInfo.class))))
      })
  @GetMapping("/processors")
  public List<ChannelProcessorInfo> processorInfo() {
    return channelProcessorService.getProcessorsInfo();
  }

  @Operation(
      summary = "Process all channels",
      description = "Manually trigger processing on all channels in ChannelFinder.",
      operationId = "processAllChannels",
      tags = {"ChannelProcessor"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Number of channels where processor was called",
            content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/process/all")
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

  @Operation(
      summary = "Process channels by query",
      description = "Manually trigger processing on channels matching the given query.",
      operationId = "processChannelsByQuery",
      tags = {"ChannelProcessor"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Number of channels where processor was called",
            content = @Content(schema = @Schema(implementation = Long.class)))
      })
  @PutMapping("/process/query")
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

  @PutMapping("/process/channels")
  public void processChannels(List<Channel> channels) {
    channelProcessorService.sendToProcessors(channels);
  }

  @Operation(summary = "Set if the processor is enabled or not")
  @PutMapping(
      value = "/processor/{processorName}/enabled",
      produces = {"application/json"},
      consumes = {"application/json"})
  public void setProcessorEnabled(
      @PathVariable("processorName") String processorName,
      @Parameter(description = "Value of enabled to set, default value: true")
          @RequestParam(required = false, name = "enabled", defaultValue = "true")
          Boolean enabled) {
    channelProcessorService.setProcessorEnabled(processorName, enabled);
  }
}
