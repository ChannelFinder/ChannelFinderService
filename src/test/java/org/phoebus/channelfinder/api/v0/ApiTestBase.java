package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.phoebus.channelfinder.Application;
import org.phoebus.channelfinder.ElasticsearchTestBase;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base for these tests: the whole application on a random port, the production {@code
 * application.properties}, and a real Elasticsearch.
 *
 * <p>Only what a random port and a shared test JVM require is overridden. TLS and the extra plain
 * HTTP connector are switched off because both bind fixed ports, the archiver processor is switched
 * off because it would call out to an archiver, and a third demo user is added whose role is in no
 * group. Every test starts from empty indices.
 */
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "server.ssl.enabled=false",
      "server.http.enable=false",
      "aa.enabled=false",
      "elasticsearch.channel.index=v0_api_channelfinder",
      "elasticsearch.tag.index=v0_api_cf_tags",
      "elasticsearch.property.index=v0_api_cf_properties",
      "demo_auth.users=admin,user,guest",
      "demo_auth.pwds=adminPass,userPass,guestPass",
      // AuthorizationService.ROLES takes the role groups from static fields once, at the first
      // authorisation check in the test JVM. Every Spring context sets those fields when it
      // starts, but ROLES keeps the lists it took. The groups are those of the context that set
      // the fields last before that first check, which can be another test's property file.
      // ADMIN is an admin group and CF-CHANNELS a channel group in both application.properties
      // and application_test.properties, so admin and user can write either way.
      "demo_auth.roles=ADMIN,USER:CF-CHANNELS,GUEST"
    })
abstract class ApiTestBase extends ElasticsearchTestBase {

  static final String ROOT = "/ChannelFinder";

  @LocalServerPort int port;

  @Autowired ElasticConfig elasticConfig;

  ApiClient cf;

  @BeforeEach
  void emptyIndicesAndConnect() throws IOException {
    ElasticsearchClient client = elasticConfig.getSearchClient();
    for (String index :
        new String[] {
          elasticConfig.getES_CHANNEL_INDEX(),
          elasticConfig.getES_PROPERTY_INDEX(),
          elasticConfig.getES_TAG_INDEX()
        }) {
      // Batches above the max result window, which follows the query size, are rejected.
      client.deleteByQuery(
          d ->
              d.index(index)
                  .query(q -> q.matchAll(m -> m))
                  .scrollSize((long) elasticConfig.getES_QUERY_SIZE())
                  .refresh(true));
    }
    cf = new ApiClient(serviceUrl());
  }

  String serviceUrl() {
    return "http://localhost:" + port + ROOT;
  }

  void createProperties(String owner, String... names) {
    for (String name : names) {
      assertEquals(
          200,
          cf.put(
                  "/resources/properties/" + name,
                  ApiJson.property(name, owner).toString(),
                  ApiClient.ADMIN)
              .status());
    }
  }

  void createTags(String owner, String... names) {
    for (String name : names) {
      assertEquals(
          200,
          cf.put("/resources/tags/" + name, ApiJson.tag(name, owner).toString(), ApiClient.ADMIN)
              .status());
    }
  }

  void putChannels(ObjectNode... channels) {
    assertEquals(
        200,
        cf.put("/resources/channels", ApiJson.array(channels).toString(), ApiClient.ADMIN)
            .status());
  }
}
