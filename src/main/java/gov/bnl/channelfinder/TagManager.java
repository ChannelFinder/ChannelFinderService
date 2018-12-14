package gov.bnl.channelfinder;

/*
 * #%L
 * ChannelFinder Directory Service
 * %%
 * Copyright (C) 2010 - 2018 Brookhaven National Laboratory / National Synchrotron Light Source II
 * %%
 * Copyright (C) 2010 - 2012 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 * #L%
 */

import static gov.bnl.channelfinder.ElasticSearchClient.getNewClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import com.google.common.base.Function;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.SecurityContext;
import com.google.common.collect.Collections2;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaDataDeleteIndexService.Response;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.search.SearchHit;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.json.JsonParseException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tags")
@EnableAutoConfiguration
public class TagManager {

//	private SecurityContext securityContext;
	private Logger audit = Logger.getLogger(this.getClass().getPackage().getName() + ".audit");
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	/**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of tags
     */
	@GetMapping
	public List<XmlTag> listTags(@RequestParam Map<String,String> allRequestParams) {
		Client client = getNewClient();
		final ObjectMapper mapper = new ObjectMapper();
		mapper.addMixIn(XmlTag.class, OnlyXmlTag.class);
		try {
			Map<String, String> parameters = allRequestParams;
			int size = 10000;
			if (parameters.containsKey("~size")) {
				String maxSize = parameters.get("~size");
				if (maxSize!=null) {
					size = Integer.valueOf(maxSize);
				}
			}
			final SearchResponse response = client.prepareSearch("tags").setTypes("tag")
					.setQuery(new MatchAllQueryBuilder()).setSize(size).get();
			List<XmlTag> hits = new ArrayList<>();
			if (response != null) {
				for (SearchHit hit : response.getHits()) {
					hits.add(mapper.readValue(hit.source(), XmlTag.class));
				}
			}
			return hits;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			client.close();
		}
	}

	/**
	 * GET method for retrieving the tag with the
	 * path parameter <tt>tagName</tt> 
	 * 
	 * To get all its channels use the parameter "withChannels"
	 *
	 * @param tag URI path parameter: tag name to search for
	 * @return list of channels with their properties and tags that match
	 */
	@GetMapping("/tag")
	public XmlTag read(@RequestParam("tag") String tag, @RequestParam("withChannels") boolean withChannels) {
		long start = System.currentTimeMillis();
		Client client = getNewClient();
		audit.info("client initialization: "+ (System.currentTimeMillis() - start));
		//String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
		XmlTag result = null;        
		try {
			GetResponse response = client.prepareGet("tags", "tag", tag).get();
			if (response.isExists()) {
				ObjectMapper mapper = new ObjectMapper();
				result = mapper.readValue(response.getSourceAsBytes(), XmlTag.class);
				XmlTag r;
				if (result == null) {
					r = result;
				} else {
					if (withChannels) {
						// TODO iterator or scrolling needed
						final SearchResponse channelResult = client.prepareSearch("channelfinder")
								.setQuery(matchQuery("tags.name",  tag.trim())).setSize(10000).get();
						List<XmlChannel> channels = new ArrayList<XmlChannel>();
						if (channelResult != null) {
							for (SearchHit hit : channelResult.getHits()) {
								channels.add(mapper.readValue(hit.source(), XmlChannel.class));
							}
						}
						result.setChannels(channels);
					}
					r = result;
				}
				return r;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			client.close();
		}
	}
	
	/**
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <tt>name</tt> to all channels identified in the payload
     * structure <tt>data</tt>.
     * Setting the owner attribute in the XML root element is mandatory.
     * 
     * @param tag URI path parameter: tag name
     * @param data XmlTag structure containing the list of channels to be tagged
     * @return HTTP Response
     */
    @PutMapping("/tag/{tag}/{data}")
    public XmlTag create(@PathVariable("tag") String tag, @PathVariable("data") XmlTag data) {
        long start = System.currentTimeMillis();
        Client client = getNewClient();
        audit.info("client initialization: "+ (System.currentTimeMillis() - start));
        try {
            if (tag.equals(data.getName())) {
                BulkRequestBuilder bulkRequest = client.prepareBulk();
                IndexRequest indexRequest = new IndexRequest("tags", "tag", tag).source(jsonBuilder().startObject()
                        .field("name", data.getName()).field("owner", data.getOwner()).endObject());
                UpdateRequest updateRequest = new UpdateRequest("tags", "tag", tag).doc(jsonBuilder().startObject()
                        .field("name", data.getName()).field("owner", data.getOwner()).endObject())
                        .upsert(indexRequest);
                bulkRequest.add(updateRequest);
                SearchResponse qbResult = client.prepareSearch("channelfinder")
                        .setQuery(QueryBuilders.matchQuery("tags.name", tag)).addField("name").setSize(10000).execute()
                        .actionGet();

                Set<String> existingChannels = new HashSet<String>();
                for (SearchHit hit : qbResult.getHits()) {
                    existingChannels.add(hit.field("name").getValue().toString());
                }

                Set<String> newChannels = new HashSet<String>();
                if (data.getChannels() != null) {
                    newChannels.addAll(Collections2.transform(data.getChannels(), new Function<XmlChannel, String>() {
                        @Override
                        public String apply(XmlChannel channel) {
                            return channel.getName();
                        }
                    }));
                }

                Set<String> remove = new HashSet<String>(existingChannels);
                remove.removeAll(newChannels);

                Set<String> add = new HashSet<String>(newChannels);
                add.removeAll(existingChannels);

                HashMap<String, String> param = new HashMap<String, String>();
                param.put("name", data.getName());
                param.put("owner", data.getOwner());
                for (String ch : remove) {
                    bulkRequest.add(new UpdateRequest("channelfinder", "channel", ch).refresh(true)
                            .script("removeTag = new Object();" + "for (xmltag in ctx._source.tags) "
                                    + "{ if (xmltag.name == tag.name) { removeTag = xmltag} }; "
                                    + "ctx._source.tags.remove(removeTag);")
                            .addScriptParam("tag", param));
                }
                for (String ch : add) {
                    bulkRequest.add(new UpdateRequest("channelfinder", "channel", ch).refresh(true)
                            .script("ctx._source.tags.add(tag)").addScriptParam("tag", param));
                }

                bulkRequest.setRefresh(true);
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures()) {
                    audit.severe(bulkResponse.buildFailureMessage());
                    if (bulkResponse.buildFailureMessage().contains("DocumentMissingException")) {
                        return null;
                    } else {
                        return null;
                    }
                } else {
                    GetResponse response = client.prepareGet("tags", "tag", tag).execute().actionGet();
                    ObjectMapper mapper = new ObjectMapper();
                    XmlTag result = mapper.readValue(response.getSourceAsBytes(), XmlTag.class);
                    XmlTag r;
                    if (result == null) {
                        r = null;
                    } else {
                        r = result;
                    }
                    return r;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        } finally {
            client.close();
        }
    }


	abstract class OnlyXmlTag {
		@JsonIgnore
		private List<XmlChannel> channels;
	}
	
	}
