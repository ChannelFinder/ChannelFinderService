package org.phoebus.channelfinder.rest.controller;

import static org.phoebus.channelfinder.CFResourceDescriptors.SCROLL_RESOURCE_URI;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.phoebus.channelfinder.CFResourceDescriptors;
import org.phoebus.channelfinder.TextUtil;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin
@RestController
@RequestMapping(SCROLL_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelScroll {

  private static final Logger logger = Logger.getLogger(ChannelScroll.class.getName());

  @Autowired ElasticConfig esService;

  @Autowired
  @Qualifier("indexClient")
  ElasticsearchClient client;

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
  public Scroll query(
      @Parameter(description = CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> allRequestParams) {
    return search(null, allRequestParams);
  }

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
  public Scroll query(
      @Parameter(description = "Scroll ID from previous query") @PathVariable("scrollId")
          String scrollId,
      @Parameter(description = CFResourceDescriptors.SEARCH_PARAM_DESCRIPTION) @RequestParam
          MultiValueMap<String, String> searchParameters) {
    return search(scrollId, searchParameters);
  }

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
  public Scroll search(String scrollId, MultiValueMap<String, String> searchParameters) {
    BoolQuery.Builder boolQuery = new BoolQuery.Builder();
    int size = esService.getES_QUERY_SIZE();
    int from = 0;

    for (Map.Entry<String, List<String>> parameter : searchParameters.entrySet()) {
      String key = parameter.getKey().trim();
      boolean isNot = key.endsWith("!");
      if (isNot) {
        key = key.substring(0, key.length() - 1);
      }
      switch (key) {
        case "~name":
          for (String value : parameter.getValue()) {
            DisMaxQuery.Builder nameQuery = new DisMaxQuery.Builder();
            for (String pattern : value.split("[\\|,;]")) {
              nameQuery.queries(
                  WildcardQuery.of(w -> w.field("name").value(pattern.trim()))._toQuery());
            }
            boolQuery.must(nameQuery.build()._toQuery());
          }
          break;
        case "~tag":
          for (String value : parameter.getValue()) {
            DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
            for (String pattern : value.split("[\\|,;]")) {
              tagQuery.queries(
                  NestedQuery.of(
                          n ->
                              n.path("tags")
                                  .query(
                                      WildcardQuery.of(
                                              w -> w.field("tags.name").value(pattern.trim()))
                                          ._toQuery()))
                      ._toQuery());
            }
            if (isNot) {
              boolQuery.mustNot(tagQuery.build()._toQuery());
            } else {
              boolQuery.must(tagQuery.build()._toQuery());
            }
          }
          break;
        case "~size":
          Optional<String> maxSize =
              parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
          if (maxSize.isPresent()) {
            size = Integer.parseInt(maxSize.get());
          }
          break;
        case "~from":
          Optional<String> maxFrom =
              parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
          if (maxFrom.isPresent()) {
            from = Integer.parseInt(maxFrom.get());
          }
          break;

        default:
          DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
          for (String value : parameter.getValue()) {
            for (String pattern : value.split("[\\|,;]")) {
              String finalKey = key;
              BoolQuery bq;
              if (isNot) {
                bq =
                    BoolQuery.of(
                        p ->
                            p.must(
                                    MatchQuery.of(
                                            name -> name.field("properties.name").query(finalKey))
                                        ._toQuery())
                                .mustNot(
                                    WildcardQuery.of(
                                            val ->
                                                val.field("properties.value").value(pattern.trim()))
                                        ._toQuery()));
              } else {
                bq =
                    BoolQuery.of(
                        p ->
                            p.must(
                                    MatchQuery.of(
                                            name -> name.field("properties.name").query(finalKey))
                                        ._toQuery())
                                .must(
                                    WildcardQuery.of(
                                            val ->
                                                val.field("properties.value").value(pattern.trim()))
                                        ._toQuery()));
              }
              propertyQuery.queries(
                  NestedQuery.of(n -> n.path("properties").query(bq._toQuery()))._toQuery());
            }
          }
          boolQuery.must(propertyQuery.build()._toQuery());
          break;
      }
    }

    try {
      SearchRequest.Builder builder = new SearchRequest.Builder();
      builder
          .index(esService.getES_CHANNEL_INDEX())
          .query(boolQuery.build()._toQuery())
          .from(from)
          .size(size)
          .sort(SortOptions.of(o -> o.field(FieldSort.of(f -> f.field("name")))));
      if (scrollId != null && !scrollId.isEmpty()) {
        builder.searchAfter(FieldValue.of(scrollId));
      }
      SearchResponse<Channel> response = client.search(builder.build(), Channel.class);
      List<Hit<Channel>> hits = response.hits().hits();
      return new Scroll(
          !hits.isEmpty() ? hits.get(hits.size() - 1).id() : null,
          hits.stream().map(Hit::source).collect(Collectors.toList()));
    } catch (Exception e) {
      String message =
          MessageFormat.format(TextUtil.SEARCH_FAILED_CAUSE, searchParameters, e.getMessage());
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
    }
  }
}
