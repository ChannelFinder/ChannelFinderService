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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaDataDeleteIndexService.Response;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import org.elasticsearch.search.SearchHit;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.json.JsonParseException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
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
	
//	private Response handleException(String user, Response.Status status, Exception e) {
//        return handleException(user, status, e.getMessage());
//}
//	private Response handleException(String user, Response.Status status, String message) {
//        return new CFException(status, message).toResponse();
//}

	abstract class OnlyXmlTag {
		@JsonIgnore
		private List<XmlChannel> channels;
	}
	
	}
