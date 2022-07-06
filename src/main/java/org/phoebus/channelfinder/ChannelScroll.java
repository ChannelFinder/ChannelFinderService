package org.phoebus.channelfinder;

import static org.phoebus.channelfinder.CFResourceDescriptors.SCROLL_RESOURCE_URI;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin
@RestController
@RequestMapping(SCROLL_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelScroll {
    static Logger log = Logger.getLogger(ChannelScroll.class.getName());

    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;

    @Value("${elasticsearch.query.size:10000}")
    private int defaultMaxSize;


    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    /**
     * GET method for retrieving a collection of Channel instances, based on a
     * multi-parameter query specifying patterns for tags, property values, and
     * channel names to match against.
     *
     * @param allRequestParams search parameters
     * @return list of all channels
     */
    @GetMapping
    public XmlScroll query(@RequestParam MultiValueMap<String, String> allRequestParams) {
        return search(null, allRequestParams);
    }

    /**
     * GET method for retrieving a collection of Channel instances, based on a
     * multi-parameter query specifying patterns for tags, property values, and
     * channel names to match against.
     *
     * @param scrollId scroll Id
     * @return list of all channels
     */
    @GetMapping("/{scrollId}")
    public XmlScroll query(@PathVariable("scrollId") String scrollId, @RequestParam MultiValueMap<String, String> searchParameters) {
        return search(scrollId, searchParameters);
    }

    /**
     * Search for a list of channels based on their name, tags, and/or properties.
     * Search parameters ~name - The name of the channel ~tags - A list of comma
     * separated values ${propertyName}:${propertyValue} -
     * <p>
     * The query result is sorted based on the channel name ~size - The number of
     * channels to be returned ~from - The starting index of the channel list
     *
     * @param scrollId         scroll ID
     * @param searchParameters - search parameters for scrolling searches
     * @return search scroll
     */
    public XmlScroll search(String scrollId, MultiValueMap<String, String> searchParameters) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        Integer size = defaultMaxSize;
        Integer from = 0;

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
                            nameQuery.queries(WildcardQuery.of(w -> w.field("name").value(pattern.trim()))._toQuery());
                        }
                        boolQuery.must(nameQuery.build()._toQuery());
                    }
                    break;
                case "~tag":
                    for (String value : parameter.getValue()) {
                        DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
                        for (String pattern : value.split("[\\|,;]")) {
                            tagQuery.queries(
                                    NestedQuery.of(n -> n.path("tags").query(
                                            WildcardQuery.of(w -> w.field("tags.name").value(pattern.trim()))._toQuery()))._toQuery());
                        }
                        if (isNot) {
                            boolQuery.mustNot(tagQuery.build()._toQuery());
                        } else {
                            boolQuery.must(tagQuery.build()._toQuery());
                        }

                    }
                    break;
                case "~size":
                    Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxSize.isPresent()) {
                        size = Integer.valueOf(maxSize.get());
                    }
                    break;
                case "~from":
                    Optional<String> maxFrom = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxFrom.isPresent()) {
                        from = Integer.valueOf(maxFrom.get());
                    }
                    break;

                default:
                    DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String finalKey = key;
                            BoolQuery bq;
                            if (isNot) {
                                bq = BoolQuery.of(p -> p.must(MatchQuery.of(name -> name.field("properties.name").query(finalKey))._toQuery())
                                        .mustNot(WildcardQuery.of(val -> val.field("properties.value").value(pattern.trim()))._toQuery()));
                            } else {
                                bq = BoolQuery.of(p -> p.must(MatchQuery.of(name -> name.field("properties.name").query(finalKey))._toQuery())
                                        .must(WildcardQuery.of(val -> val.field("properties.value").value(pattern.trim()))._toQuery()));
                            }
                            propertyQuery.queries(
                                    NestedQuery.of(n -> n.path("properties").query(bq._toQuery()))._toQuery()
                            );
                        }
                    }
                    boolQuery.must(propertyQuery.build()._toQuery());
                    break;
            }
        }

        try {
            Integer finalSize = size;
            Integer finalFrom = from;
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(ES_CHANNEL_INDEX)
                    .query(boolQuery.build()._toQuery())
                    .from(finalFrom)
                    .size(finalSize)
                    .sort(SortOptions.of(o -> o.field(FieldSort.of(f -> f.field("name")))));
            if(scrollId != null && !scrollId.isEmpty()) {
                builder.searchAfter(scrollId);
            }
            SearchResponse<XmlChannel> response = client.search(builder.build(),
                    XmlChannel.class
            );
            List<Hit<XmlChannel>> hits = response.hits().hits();
            return new XmlScroll(hits.size() > 0 ? hits.get(hits.size()-1).id() : null, hits.stream().map(Hit::source).collect(Collectors.toList()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Search failed for: " + searchParameters, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Search failed for: " + searchParameters + ", CAUSE: " + e.getMessage(), e);
        }
    }
}
