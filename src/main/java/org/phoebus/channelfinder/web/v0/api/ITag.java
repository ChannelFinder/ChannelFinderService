package org.phoebus.channelfinder.web.v0.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.phoebus.channelfinder.web.v0.dto.TagDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

public interface ITag {

  @Operation(
      summary = "List all tags",
      description = "Retrieve the list of all tags in the database.",
      operationId = "listTags",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List all Tags",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = TagDto.class)))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to list all Tags",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping
  Iterable<TagDto> list();

  @Operation(
      summary = "Get tag by name",
      description = "Retrieve a tag by its name. Optionally include its channels.",
      operationId = "getTagByName",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Finding Tag by tagName",
            content = @Content(schema = @Schema(implementation = TagDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Tag not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/{tagName}")
  TagDto read(
      @PathVariable("tagName") String tagName,
      @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels);

  @Operation(
      summary = "Create or update a tag",
      description = "Create and exclusively update the tag identified by the path parameter.",
      operationId = "createOrUpdateTag",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tag created and updated",
            content = @Content(schema = @Schema(implementation = TagDto.class))),
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
            description = "Tag-, or Channel-name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create/update Tag",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/{tagName}")
  TagDto create(@PathVariable("tagName") String tagName, @RequestBody TagDto tag);

  @Operation(
      summary = "Create multiple tags",
      description = "Create multiple tags in a single request.",
      operationId = "createMultipleTags",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tags created",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = TagDto.class)))),
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
            description = "Tag-, or Channel-name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create Tags",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping()
  Iterable<TagDto> create(@RequestBody Iterable<TagDto> tags);

  @Operation(
      summary = "Add tag to a single channel",
      description = "Add the tag identified by tagName to the channel identified by channelName.",
      operationId = "addTagToChannel",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tags added to a single channel",
            content = @Content(schema = @Schema(implementation = TagDto.class))),
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
            description = "Tag-, or Channel-name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Tag creational error",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/{tagName}/{channelName}")
  TagDto addSingle(
      @PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName);

  @Operation(
      summary = "Update a tag",
      description =
          "Update the tag identified by the path parameter, adding it to all channels in the payload.",
      operationId = "updateTag",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tag updated",
            content = @Content(schema = @Schema(implementation = TagDto.class))),
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
            description = "Tag name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Tag update error",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping("/{tagName}")
  TagDto update(@PathVariable("tagName") String tagName, @RequestBody TagDto tag);

  @Operation(
      summary = "Update multiple tags",
      description =
          "Update multiple tags and all appropriate channels. The operation will fail if any of the specified channels do not exist.",
      operationId = "updateMultipleTags",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tags updated",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = TagDto.class)))),
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
            description = "Tag-, or Channel-name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while updating tags",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping()
  Iterable<TagDto> update(@RequestBody Iterable<TagDto> tags);

  @Operation(
      summary = "Delete a tag",
      description = "Delete the tag identified by the path parameter from all channels.",
      operationId = "deleteTag",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Tag deleted"),
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
            description = "Tag does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Tag creational error",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping("/{tagName}")
  void remove(@PathVariable("tagName") String tagName);

  @Operation(
      summary = "Delete tag from a channel",
      description =
          "Delete the tag identified by tagName from the channel identified by channelName.",
      operationId = "deleteTagFromChannel",
      tags = {"Tag"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Tag deleted from the desired channel"),
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
            description = "Tag does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Tag creational error",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping("/{tagName}/{channelName}")
  void removeSingle(
      @PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName);
}
