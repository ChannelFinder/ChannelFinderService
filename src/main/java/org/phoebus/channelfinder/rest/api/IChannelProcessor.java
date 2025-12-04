package org.phoebus.channelfinder.rest.api;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI;
import static org.phoebus.channelfinder.common.CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping(CHANNEL_PROCESSOR_RESOURCE_URI)
public interface IChannelProcessor {

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
  long processorCount();

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
  List<ChannelProcessorInfo> processorInfo();

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
  long processAllChannels();

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
  long processChannels(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams);

  @PutMapping("/process/channels")
  void processChannels(List<Channel> channels);

  @Operation(summary = "Set if the processor is enabled or not")
  @PutMapping(
      value = "/processor/{processorName}/enabled",
      produces = {"application/json"},
      consumes = {"application/json"})
  void setProcessorEnabled(
      @PathVariable("processorName") String processorName,
      @Parameter(description = "Value of enabled to set, default value: true")
          @RequestParam(required = false, name = "enabled", defaultValue = "true")
          Boolean enabled);
}
