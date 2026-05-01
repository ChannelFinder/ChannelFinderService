package org.phoebus.channelfinder.web.v0.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Map;
import org.phoebus.channelfinder.common.CFResourceDescriptors;
import org.phoebus.channelfinder.entity.Scroll;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

public interface IChannelScroll {

  @Operation(
      summary = "Scroll query for channels",
      description =
          "Retrieve a collection of Channel instances based on multi-parameter search. "
              + "Search parameters can be provided via URL query string or JSON request body, or both. "
              + "URL parameters take precedence for control parameters (~size, ~from, ~search_after, ~track_total_hits). "
              + "Regular search parameters from URL and body are combined as separate values in the query.",
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
          MultiValueMap<String, String> allRequestParams,
      @Parameter(
              description =
                  "Optional JSON request body containing search parameters. Used to bypass URL length limitations.")
          @RequestBody(required = false)
          Map<String, String> searchParamsBody);

  // Backward-compatible overload when no request body is provided.
  default Scroll query(MultiValueMap<String, String> allRequestParams) {
    return query(allRequestParams, null);
  }

  @Operation(
      summary = "Scroll query by scrollId",
      description =
          "Retrieve a collection of Channel instances using a scrollId and search parameters. "
              + "Search parameters can be provided via URL query string or JSON request body, or both. "
              + "URL parameters take precedence for control parameters (~size, ~from, ~search_after, ~track_total_hits). "
              + "Regular search parameters from URL and body are combined as separate values in the query.",
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
          MultiValueMap<String, String> searchParameters,
      @Parameter(
              description =
                  "Optional JSON request body containing search parameters. Used to bypass URL length limitations.")
          @RequestBody(required = false)
          Map<String, String> searchParamsBody);

  // Backward-compatible overload when no request body is provided.
  default Scroll query(String scrollId, MultiValueMap<String, String> searchParameters) {
    return query(scrollId, searchParameters, null);
  }
}
