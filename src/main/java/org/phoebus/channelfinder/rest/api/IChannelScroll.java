package org.phoebus.channelfinder.rest.api;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.SCROLL_RESOURCE_URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.phoebus.channelfinder.common.CFResourceDescriptors;
import org.phoebus.channelfinder.entity.Scroll;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping(SCROLL_RESOURCE_URI)
public interface IChannelScroll {

  @Operation(
      summary = "Scroll query for channels",
      description = "Retrieve a collection of Channel instances based on multi-parameter search.",
      operationId = "scrollQueryChannels",
      tags = {"ChannelScroll"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Scroll that contains a collection of channel instances",
            content = @Content(schema = @Schema(implementation = Scroll.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to list channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping
  Scroll query(
      @Parameter(description = CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams);

  @Operation(
      summary = "Scroll query by scrollId",
      description =
          "Retrieve a collection of Channel instances using a scrollId and search parameters.",
      operationId = "scrollQueryById",
      tags = {"ChannelScroll"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Scroll List of channels",
            content = @Content(schema = @Schema(implementation = Scroll.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to list channels",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/{scrollId}")
  Scroll query(
      @Parameter(description = "Scroll ID from previous query") @PathVariable("scrollId")
          String scrollId,
      @Parameter(description = CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> searchParameters);

  /**
   * Search for a list of channels based on their name, tags, and/or properties. Search parameters
   * ~name - The name of the channel ~tags - A list of comma separated values
   * ${propertyName}:${propertyValue} -
   *
   * <p>The query result is sorted based on the channel name ~size - The number of channels to be
   * returned ~from - The starting index of the channel list
   *
   * <p>TODO combine with ChannelRepository code.
   *
   * @param scrollId scroll ID
   * @param searchParameters - search parameters for scrolling searches
   * @return search scroll
   */
  Scroll search(String scrollId, MultiValueMap<String, String> searchParameters);
}
