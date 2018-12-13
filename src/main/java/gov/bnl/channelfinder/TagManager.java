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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tags")
@EnableAutoConfiguration
public class TagManager {

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
					.setQuery(new MatchAllQueryBuilder()).setSize(size).execute().actionGet();
			List<XmlTag> hits = new ArrayList<>();
			if (response != null) {
				for (SearchHit hit : response.getHits()) {
					hits.add(mapper.readValue(hit.source(), XmlTag.class));
				}
			}
			return hits;
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
