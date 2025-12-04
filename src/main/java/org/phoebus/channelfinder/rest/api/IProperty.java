package org.phoebus.channelfinder.rest.api;

import static org.phoebus.channelfinder.common.CFResourceDescriptors.PROPERTY_RESOURCE_URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.phoebus.channelfinder.entity.Property;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping(PROPERTY_RESOURCE_URI)
public interface IProperty {

  @Operation(
      summary = "List all properties",
      description = "Retrieve the list of all properties in the database.",
      operationId = "listProperties",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of properties",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Property.class)))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while listing properties",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping
  Iterable<Property> list();

  @Operation(
      summary = "Get property by name",
      description = "Retrieve a property by its name. Optionally include its channels.",
      operationId = "getPropertyByName",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fetch property by propertyName",
            content = @Content(schema = @Schema(implementation = Property.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Property not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @GetMapping("/{propertyName}")
  Property read(
      @PathVariable("propertyName") String propertyName,
      @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels);

  @Operation(
      summary = "Create or update a property",
      description = "Create and exclusively update the property identified by the path parameter.",
      operationId = "createOrUpdateProperty",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Property created",
            content = @Content(schema = @Schema(implementation = Property.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Property not found",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create property",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/{propertyName}")
  Property create(
      @PathVariable("propertyName") String propertyName, @RequestBody Property property);

  @Operation(
      summary = "Create multiple properties",
      description = "Create multiple properties in a single request.",
      operationId = "createMultipleProperties",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Properties created",
            content = @Content(schema = @Schema(implementation = Property.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to create properties",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping()
  Iterable<Property> create(@RequestBody Iterable<Property> properties);

  @Operation(
      summary = "Add property to a single channel",
      description =
          "Add the property identified by propertyName to the channel identified by channelName.",
      operationId = "addPropertyToChannel",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Property added to the channel",
            content = @Content(schema = @Schema(implementation = Property.class))),
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
            description = "Property-, or Channel-name does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to add property",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PutMapping("/{propertyName}/{channelName}")
  Property addSingle(
      @PathVariable("propertyName") String propertyName,
      @PathVariable("channelName") String channelName,
      @RequestBody Property property);

  @Operation(
      summary = "Update a property",
      description =
          "Update the property identified by the path parameter, adding it to all channels in the payload.",
      operationId = "updateProperty",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Property updated",
            content = @Content(schema = @Schema(implementation = Property.class))),
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
            description = "Property does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to update property",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping("/{propertyName}")
  Property update(
      @PathVariable("propertyName") String propertyName, @RequestBody Property property);

  @Operation(
      summary = "Update multiple properties",
      description = "Update multiple properties and all appropriate channels.",
      operationId = "updateMultipleProperties",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Properties updated",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Property.class)))),
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
            description = "Property does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to update properties",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @PostMapping()
  Iterable<Property> update(@RequestBody Iterable<Property> properties);

  @Operation(
      summary = "Delete a property",
      description = "Delete the property identified by the path parameter from all channels.",
      operationId = "deleteProperty",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Property deleted"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Property does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to delete property",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping("/{propertyName}")
  void remove(@PathVariable("propertyName") String propertyName);

  @Operation(
      summary = "Delete property from a channel",
      description =
          "Delete the property identified by propertyName from the channel identified by channelName.",
      operationId = "deletePropertyFromChannel",
      tags = {"Property"})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Property deleted from the channel"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Property does not exist",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error while trying to delete property from a channel",
            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
      })
  @DeleteMapping("/{propertyName}/{channelName}")
  void removeSingle(
      @PathVariable("propertyName") String propertyName,
      @PathVariable("channelName") String channelName);

  /**
   * Checks if 1. the property name is not null and matches the name in the body 2. the property
   * owner is not null or empty 3. all the listed channels exist and have the property with a non
   * null and non empty value
   *
   * @param property validate property
   */
  void validatePropertyRequest(Property property);

  /**
   * Checks if 1. the property name is not null and matches the name in the body 2. the property
   * owner is not null or empty 3. the property value is not null or empty 4. all the listed
   * channels exist
   *
   * @param properties properties to be validated
   */
  void validatePropertyRequest(Iterable<Property> properties);

  /**
   * Checks if the channel exists
   *
   * @param channelName check channel exists
   */
  void validatePropertyRequest(String channelName);
}
