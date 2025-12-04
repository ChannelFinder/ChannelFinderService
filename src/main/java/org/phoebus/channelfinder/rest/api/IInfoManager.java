package org.phoebus.channelfinder.rest.api;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.CF_SERVICE_INFO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(CF_SERVICE_INFO)
public interface IInfoManager {

  @Operation(
      summary = "Get ChannelFinder service info",
      description =
          "Returns information about the ChannelFinder service and its Elasticsearch backend.",
      operationId = "getServiceInfo",
      tags = {"Info"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "ChannelFinder info",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  @GetMapping
  String info();
}
