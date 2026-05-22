package org.phoebus.channelfinder.web.v0.api;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;
import org.phoebus.channelfinder.web.v0.dto.SearchResultDto;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

public interface IChannel {

  @Operation(
      summary = "Query channels",
      description =
          "Query a collection of Channel instances based on tags, property values, and channel names.",
      operationId = "queryChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of channels",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ChannelDto.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to find all channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping
  List<ChannelDto> query(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams);

  @Operation(
      summary = "Combined query for channels",
      description =
          "Query for a collection of Channel instances and get a count and the first 10k hits.",
      operationId = "combinedQueryChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The number of matches for the query, and the first 10k channels",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = SearchResultDto.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - response size exceeded",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to find all channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/combined")
  SearchResultDto combinedQuery(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams);

  @Operation(
      summary = "Count channels matching query",
      description = "Get the number of channels matching the given query parameters.",
      operationId = "countChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The number of channels matching the query",
            content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to count the result for channel-query",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/count")
  long queryCount(
      @Parameter(description = SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams);

  @Operation(
      summary = "Get channel by name",
      description = "Retrieve a Channel instance by its name.",
      operationId = "getChannelByName",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Channel with the specified name",
            content = @Content(schema = @Schema(implementation = ChannelDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Channel not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/{channelName}")
  ChannelDto read(@PathVariable("channelName") String channelName);

  @Operation(
      summary = "Create or replace a channel",
      description = "Create or replace a channel instance identified by the payload.",
      operationId = "createOrReplaceChannel",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The created/replaced channel",
            content = @Content(schema = @Schema(implementation = ChannelDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Channel, Tag, or property not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create channel",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/{channelName}")
  ChannelDto create(
      @PathVariable("channelName") String channelName, @RequestBody ChannelDto channel);

  @Operation(
      summary = "Create or replace multiple channels",
      description = "Create or replace multiple channel instances.",
      operationId = "createOrReplaceChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The created/replaced channels",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ChannelDto.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Tag, or property not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping
  Iterable<ChannelDto> create(@RequestBody Iterable<ChannelDto> channels);

  @Operation(
      summary = "Update a channel",
      description =
          "Merge properties and tags of the channel identified by the payload into an existing channel.",
      operationId = "updateChannel",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The updated channel",
            content = @Content(schema = @Schema(implementation = ChannelDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Channel, Tag, or property not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to update channel",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping("/{channelName}")
  ChannelDto update(
      @PathVariable("channelName") String channelName, @RequestBody ChannelDto channel);

  @Operation(
      summary = "Update multiple channels",
      description =
          "Merge properties and tags of the channels identified by the payload into existing channels.",
      operationId = "updateChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "The updated channels",
            content = @Content(schema = @Schema(implementation = ChannelDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Channel not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to update channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping()
  Iterable<ChannelDto> update(@RequestBody Iterable<ChannelDto> channels);

  @Operation(
      summary = "Delete a channel",
      description = "Delete a channel instance identified by its name.",
      operationId = "deleteChannel",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Channel deleted"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Channel not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to delete channel",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping("/{channelName}")
  void remove(@PathVariable("channelName") String channelName);

  @Operation(
      summary = "Delete multiple channels",
      description = "Delete multiple channel instances identified by a request-body list of names.",
      operationId = "deleteChannels",
      tags = {"Channel"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Number of channels deleted"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to delete channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping
  long remove(@RequestBody List<String> channelNames);
}
