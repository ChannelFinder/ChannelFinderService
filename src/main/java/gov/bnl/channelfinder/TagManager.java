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
import static gov.bnl.channelfinder.ElasticSearchClient.getSearchClient;


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
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;

import com.google.common.base.Function;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.SecurityContext;
import com.google.common.collect.Collections2;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MetaDataDeleteIndexService.Response;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.json.JsonParseException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tags")
@EnableAutoConfiguration
public class TagManager {

	//	private SecurityContext securityContext;
	private Logger audit = Logger.getLogger(this.getClass().getPackage().getName() + ".audit");
	private Logger log = Logger.getLogger(this.getClass().getName());
<<<<<<< HEAD

=======
	
	@Autowired
	Client client;
>>>>>>> 75115c24f1bb7c39b1d0ee2fbca8bcd21d875819
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
<<<<<<< HEAD
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
=======
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <tt>name</tt> to all channels identified in the payload
     * structure <tt>data</tt>.
     * Setting the owner attribute in the XML root element is mandatory.
     * 
     * @param tag URI path parameter: tag name
     * @param data XmlTag structure containing the list of channels to be tagged
     * @return HTTP Response
     */
    @PutMapping("/tag/{tag}")
    public XmlTag create(@PathVariable("tag") String tag, @RequestBody XmlTag data) {
        long start = System.currentTimeMillis();
        //Client client = getNewClient();
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
>>>>>>> 75115c24f1bb7c39b1d0ee2fbca8bcd21d875819

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

	/**
	 * PUT method to create and <b>exclusively</b> update the tag identified by the
	 * path parameter <tt>name</tt> to all channels identified in the payload
	 * structure <tt>data</tt>.
	 * Setting the owner attribute in the XML root element is mandatory.
	 * 
	 * @param data XmlTag structure containing the list of channels to be tagged
	 * @return HTTP Response
	 */
	@PutMapping("/tag/{data}")
	public List<XmlTag> createTags(@PathVariable("data") List<XmlTag> data) {
		long start = System.currentTimeMillis();
		Client client = getNewClient();
		audit.info("client initialization: "+ (System.currentTimeMillis() - start));
		try {
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			for (XmlTag xmlTag : data) {
				IndexRequest indexRequest = new IndexRequest("tags", "tag", xmlTag.getName()).source(jsonBuilder().startObject()
						.field("name", xmlTag.getName()).field("owner", xmlTag.getOwner()).endObject());
				UpdateRequest updateRequest = new UpdateRequest("tags", "tag", xmlTag.getName()).doc(
						jsonBuilder().startObject()
						.field("name", xmlTag.getName()).field("owner", xmlTag.getOwner()).endObject()).upsert(indexRequest);
				bulkRequest.add(updateRequest);
				SearchResponse qbResult = client.prepareSearch("channelfinder")
						.setQuery(QueryBuilders.matchQuery("tags.name", xmlTag.getName())).addField("name").setSize(10000).execute()
						.actionGet();

				Set<String> existingChannels = new HashSet<String>();
				for (SearchHit hit : qbResult.getHits()) {
					existingChannels.add(hit.getId());
				}

				Set<String> newChannels = new HashSet<>();
				if (xmlTag.getChannels() != null) {
					newChannels.addAll(
							Collections2.transform(xmlTag.getChannels(), new Function<XmlChannel, String>() {
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
				param.put("name", xmlTag.getName());
				param.put("owner", xmlTag.getOwner());
				HashMap<String, Object> params = new HashMap<>();
				params.put("tag", param);
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
				return data;
			}
		} catch (Exception e) {
			return null;
		} finally {
			client.close();
		}
	}

	/**
	 * POST method to update the the tag identified by the path parameter <tt>name</tt>,
	 * adding it to all channels identified by the channels inside the payload
	 * structure <tt>data</tt>.
	 * Setting the owner attribute in the XML root element is mandatory.
	 * 
	 * TODO: Optimize the bulk channel update
	 *
	 * @param tag URI path parameter: tag name
	 * @param data list of channels to addSingle the tag <tt>name</tt> to
	 * @return HTTP Response
	 */
	@PostMapping("/tag/{tag}/{data}")
	public XmlTag update(@PathVariable("tag") String tag, @PathVariable("data") XmlTag data) {
		long start = System.currentTimeMillis();
		Client client = getNewClient();
		audit.info("client initialization: "+ (System.currentTimeMillis() - start));
		try {
			GetResponse response = client.prepareGet("tags", "tag", tag).get();
			if(!response.isExists()){
				return null;
			}
			ObjectMapper mapper = new ObjectMapper();
			XmlTag original = mapper.readValue(response.getSourceAsBytes(), XmlTag.class);
			// rename a tag
			if(!original.getName().equals(data.getName())){
				return renameTag(client, original, data);
			}
			String tagOwner = data.getOwner() != null && !data.getOwner().isEmpty()? data.getOwner() : original.getOwner();

			BulkRequestBuilder bulkRequest = client.prepareBulk();
			UpdateRequest updateRequest = new UpdateRequest("tags", "tag", tag)
					.doc(jsonBuilder().startObject()
							.field("name", data.getName())
							.field("owner", tagOwner)
							.endObject());
			// New owner
			HashMap<String, String> param = new HashMap<String, String>(); 
			param.put("name", data.getName());
			param.put("owner", tagOwner);
			HashMap<String, Object> params = new HashMap<>();
			params.put("tag", param);
			if(!original.getOwner().equals(data.getOwner())){
				SearchResponse queryResponse = client.prepareSearch("channelfinder")
						.setQuery(new WildcardQueryBuilder("tags.name", original.getName().trim()))
						.addField("name")
						.setSize(10000).execute()
						.actionGet();

				for (SearchHit hit : queryResponse.getHits()) {
					bulkRequest.add(new UpdateRequest("channelfinder", "channel", hit.getId())
							.script("ctx._source.tags.add(tag)").addScriptParam("tag", param));
				}
			}
			bulkRequest.add(updateRequest);
			if (data.getChannels() != null) {                
				for (XmlChannel channel : data.getChannels()) {
					bulkRequest.add(new UpdateRequest("channelfinder", "channel", channel.getName())
							.script("ctx._source.tags.add(tag)").addScriptParam("tag", param));
				}
			}
			bulkRequest.setRefresh(true);
			BulkResponse bulkResponse = bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				audit.severe(bulkResponse.buildFailureMessage());
				if (bulkResponse.buildFailureMessage().contains("DocumentMissingException")) {
					return null;
				} else {
					return null;
				}
			} else {
				//                Response r = Response.ok().build();
				//                audit.info("|POST|OK|" +  "|data="  + XmlTag.toLog(data));
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
		}
	}

	/**
	 * Utility method to rename an existing tag
	 * @param data 
	 * @param original 
	 * @param client 
	 * @param um 
	 * @param client
	 * @param original
	 * @param data
	 * @return
	 */
	private XmlTag renameTag(Client client, XmlTag original, XmlTag data) {
		try {
			SearchResponse queryResponse = client.prepareSearch("channelfinder")
					.setQuery(new WildcardQueryBuilder("tags.name", original.getName().trim()))
					.addField("name")
					.setSize(10000).get();
			List<String> channelNames = new ArrayList<String>();
			for (SearchHit hit : queryResponse.getHits()) {
				channelNames.add(hit.getId());
			}
			String tagOwner = data.getOwner() != null && !data.getOwner().isEmpty()? data.getOwner() : original.getOwner();

			BulkRequestBuilder bulkRequest = client.prepareBulk();
			bulkRequest.add(new DeleteRequest("tags", "tag", original.getName()));
			IndexRequest indexRequest = new IndexRequest("tags", "tag", data.getName()).source(jsonBuilder()
					.startObject().field("name", data.getName()).field("owner", data.getOwner()).endObject());
			UpdateRequest updateRequest;
			updateRequest = new UpdateRequest("tags", "tag", data.getName()).doc(jsonBuilder().startObject()
					.field("name", data.getName()).field("owner", data.getOwner()).endObject()).upsert(indexRequest);
			bulkRequest.add(updateRequest);
			if (!channelNames.isEmpty()) {
				HashMap<String, String> originalParam = new HashMap<>(); 
				originalParam.put("name", original.getName());
				HashMap<String, String> param = new HashMap<>(); 
				param.put("name", data.getName());
				param.put("owner", tagOwner);
				HashMap<String, Object> params = new HashMap<>();
				params.put("originalTag", originalParam);
				params.put("tag", param);
				for (String channel : channelNames) {
					bulkRequest.add(new UpdateRequest("channelfinder", "channel", channel)
							.script("ctx._source.tags.add(tag)").addScriptParam("tag", param));

				}
			}
			bulkRequest.setRefresh(true);
			BulkResponse bulkResponse = bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				audit.severe(bulkResponse.buildFailureMessage());
				if (bulkResponse.buildFailureMessage().contains("DocumentMissingException")) {
					return null;
				} else {
					return null;
				}
			} else {
				//                Response r = Response.ok().build();
				//                audit.info("|POST|OK|" + "|data="+ XmlTag.toLog(data));
				return null;
			}
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * POST method for creating multiple tags and updating all the appropriate channels
	 * If the channels don't exist it will fail
	 *
	 * @param data XmlTags data (from payload)
	 * @return HTTP Response
	 * @throws IOException when audit or log fail
	 */
	@PostMapping("/tag/{data}")
	public List<XmlTag> updateTags(@PathVariable("data") List<XmlTag> data) throws IOException {
		Client client = getNewClient();
		try {
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			for (XmlTag tag : data) {
				bulkRequest.add(new UpdateRequest("tags", "tag", tag.getName())
						.doc(jsonBuilder().startObject()
								.field("name", tag.getName())
								.field("owner", tag.getOwner())
								.endObject()));
				if (tag.getChannels() != null) {
					HashMap<String, String> param = new HashMap<String, String>(); 
					param.put("name", tag.getName());
					param.put("owner", tag.getOwner());
					for (XmlChannel channel : tag.getChannels()) {
						bulkRequest.add(new UpdateRequest("channelfinder", "channel", channel.getName())
								.refresh(true)
								.script("removeTag = new Object();"
										+ "for (xmltag in ctx._source.tags) "
										+ "{ if (xmltag.name == tag.name) { removeTag = xmltag} }; "
										+ "ctx._source.tags.remove(removeTag);"
										+ "ctx._source.tags.add(tag)")
								.addScriptParam("tag", param));
					}
				}
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
				//                Response r = Response.noContent().build();
				//                audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|POST|OK|" + r.getStatus() + "|data="
				//                        + (data));
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
			client.close();
		}
	}

	/**
	 * DELETE method for deleting the tag identified by the path parameter <tt>name</tt>
	 * from all channels.
	 *
	 * @param tag URI path parameter: tag name to remove
	 * @return HTTP Response
	 */
	@DeleteMapping
	public String remove(@RequestParam("tagName") String tag) {
		Client client = getNewClient();
		try {
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			bulkRequest.add(new DeleteRequest("tags", "tag", tag));
			SearchResponse qbResult = client.prepareSearch("channelfinder")
					.setQuery(QueryBuilders.matchQuery("tags.name", tag)).addField("name").setSize(10000).execute().actionGet();
			if (qbResult != null) {
				for (SearchHit hit : qbResult.getHits()) {
					String channelName = hit.field("name").getValue().toString();
					bulkRequest.add(new UpdateRequest("channelfinder", "channel", channelName).refresh(true)
							.script("removeTag = new Object();" 
									+ "for (xmltag in ctx._source.tags) "
									+ "{ if (xmltag.name == tag) { removeTag = xmltag} }; "
									+ "ctx._source.tags.remove(removeTag);")
							.addScriptParam("tag", tag));
				}
			}
			bulkRequest.setRefresh(true);
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				audit.severe(bulkResponse.buildFailureMessage());
				return null;
			} else {
				DeleteResponse deleteResponse = bulkResponse.getItems()[0].getResponse();
				if (deleteResponse.isFound()) {
					//					Response r = Response.ok().build();
					//					audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|DELETE|OK|" + r.getStatus());
					return "Deleted successfully";
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			return null;
		} finally {
			client.close();
		}
	}

	/**
     * PUT method for adding the tag identified by <tt>tag</tt> to the single
     * channel <tt>chan</tt> (both path parameters). 
     * 
     * TODO: could be simplified
     * with multi index update and script which can use wildcards thus removing
     * the need to explicitly define the entire tag
     * 
     * @param tag
     *            URI path parameter: tag name
     * @param chan
     *            URI path parameter: channel to update <tt>tag</tt> to
     * @param data
     *            tag data (ignored)
     * @return HTTP Response
     */
    @PutMapping("/{tagName}/{chName}")
    public String addSingle(@PathVariable ("tagName") String tag, @PathVariable ("chName") String chan, XmlTag data) {
        Client client = getNewClient();
        XmlTag result = null;
        try {
            GetResponse response = client.prepareGet("tags", "tag", tag).execute().actionGet();
            ObjectMapper mapper = new ObjectMapper();
            mapper.addMixIn(XmlChannel.class, MyMixInForXmlChannels.class);
            result = mapper.readValue(response.getSourceAsBytes(), XmlTag.class);
            
            if (result != null) {
//                if(um.userHasAdminRole() || um.userIsInGroup(result.getOwner())){
                if (validateTag(result, data)) {
                    HashMap<String, String> param = new HashMap<String, String>();
                    param.put("name", result.getName());
                    param.put("owner", result.getOwner());
                    UpdateResponse updateResponse = client
                            .update(new UpdateRequest("channelfinder", "channel", chan).refresh(true)
                                    .refresh(true)
                                    .script("removeTags = new java.util.ArrayList();" + "for (tag in ctx._source.tags) "
                                            + "{ if (tag.name == tag.name) { removeTags.add(tag)} }; "
                                            + "for (removeTag in removeTags) {ctx._source.tags.remove(removeTag)};"
                                            + "ctx._source.tags.add(tag)")
                                    .addScriptParam("tag", param))
                                    .actionGet();
                    //Response r = Response.ok().build();
                    return "added single";
                } else {
                    return null;
                }
//                }else{
//                    return Response.status(Status.FORBIDDEN)
//                            .entity("User '" + um.getUserName() + "' does not belong to owner group '"
//                                    + result.getOwner() + "' of tag '" + result.getName() + "'")
//                            .build();
//                }
            }else{
                return null;
            }
        } catch (DocumentMissingException e) {
            return null;
        }  catch (Exception e) {
            return null;
        } finally {
            client.close();
        }
    }

    /**
     * Check that the existing tag and the tag in the request body match 
     *  
     * @param existing
     * @param request
     * @return
     */
    private boolean validateTag(XmlTag existing, XmlTag request) {
        return existing.getName().equals(request.getName());
}
	
	abstract class OnlyXmlTag {
		@JsonIgnore
		private List<XmlChannel> channels;
	}
	/**
     * A filter to be used with the jackson mapper to ignore the embedded
     * xmlchannels in the tag object
     * 
     * @author Kunal Shroff
     *
     */
    @JsonIgnoreType
    public class MyMixInForXmlChannels {
        //
}

}
