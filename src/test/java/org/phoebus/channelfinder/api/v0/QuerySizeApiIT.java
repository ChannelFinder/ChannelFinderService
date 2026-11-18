package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests what depends on {@code elasticsearch.query.size}. The size is lowered to 5 so that more
 * than one page of channels or definitions takes a handful of documents instead of 10000.
 */
@TestPropertySource(
    properties = {
      "elasticsearch.query.size=5",
      "elasticsearch.channel.index=v0_api_small_channelfinder",
      "elasticsearch.tag.index=v0_api_small_cf_tags",
      "elasticsearch.property.index=v0_api_small_cf_properties"
    })
class QuerySizeApiIT extends ApiTestBase {

  private static final List<String> SEVEN_CHANNELS =
      List.of("ch1", "ch2", "ch3", "ch4", "ch5", "ch6", "ch7");

  private void putSevenChannelsWith(ObjectNode property, ObjectNode tag) {
    putChannels(
        SEVEN_CHANNELS.stream()
            .map(name -> withTags(withProperties(channel(name, "admin"), property), tag))
            .toArray(ObjectNode[]::new));
  }

  @Test
  void defaultSizeAndMaxResultWindowFollowTheQuerySize() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));

    assertEquals(
        List.of("ch1", "ch2", "ch3", "ch4", "ch5"), names(cf.get("/resources/channels").json()));
    assertEquals("7", cf.get("/resources/channels/count").body());
    assertEquals(400, cf.get("/resources/channels?~size=6").status());
  }

  @Test
  void batchPostOfMoreChannelsThanTheQuerySizeMergesIntoEachChannel() {
    createProperties("admin", "area", "number");
    createTags("admin", "archived");
    putSevenChannelsWith(property("number", "admin", "001"), tag("archived", "admin"));

    String update =
        array(
                SEVEN_CHANNELS.stream()
                    .map(
                        name ->
                            withProperties(
                                channel(name, "admin"), property("area", "admin", "south")))
                    .toArray(ObjectNode[]::new))
            .toString();
    assertEquals(200, cf.post("/resources/channels", update, ApiClient.ADMIN).status());

    for (String name : SEVEN_CHANNELS) {
      JsonNode channel = cf.get("/resources/channels/" + name).json();
      assertEquals("south", propertyValue(channel, "area"), name);
      assertEquals("001", propertyValue(channel, "number"), name);
    }
  }

  @Test
  void scrollWithAQueryAndNoSizePagesPastTheQuerySize() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));

    JsonNode first = cf.get("/resources/scroll?area=north").json();
    assertEquals(List.of("ch1", "ch2", "ch3", "ch4", "ch5"), names(first.get("channels")));

    JsonNode second =
        cf.get("/resources/scroll/" + first.get("id").asString() + "?area=north").json();
    assertEquals(List.of("ch6", "ch7"), names(second.get("channels")));
  }

  @Test
  void renamingAPropertyOnMoreThanOnePageRenamesItEverywhere() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));
    String renamed = property("zone", "admin").toString();

    ApiClient.Response response = cf.post("/resources/properties/area", renamed, ApiClient.ADMIN);

    assertEquals(200, response.status());
    // Unlike GET, the POST response lists only the channels in the payload.
    assertEquals(List.of(), names(response.json().get("channels")));
    assertEquals("7", cf.get("/resources/channels/count?zone=north").body());
    assertEquals("0", cf.get("/resources/channels/count?area=*").body());
  }

  @Test
  void renamingATagOnMoreThanOnePageRenamesItEverywhere() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));
    String renamed = tag("saved", "admin").toString();

    ApiClient.Response response = cf.post("/resources/tags/archived", renamed, ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals(
        names(cf.get("/resources/tags/saved").json().get("channels")),
        names(response.json().get("channels")));
    assertEquals("7", cf.get("/resources/channels/count?~tag=saved").body());
    assertEquals("0", cf.get("/resources/channels/count?~tag=archived").body());
  }

  @Test
  void postingATagWithoutARenameAnswersWithOnePageAndKeepsEveryChannel() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));
    String unchanged = tag("archived", "admin").toString();

    ApiClient.Response response = cf.post("/resources/tags/archived", unchanged, ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals(
        names(cf.get("/resources/tags/archived").json().get("channels")),
        names(response.json().get("channels")));
    assertEquals("7", cf.get("/resources/channels/count?~tag=archived").body());
  }

  @Test
  void listingTheDefinitionsReturnsEveryDefinition() {
    createProperties("admin", "p1", "p2", "p3", "p4", "p5", "p6");

    assertEquals(
        List.of("p1", "p2", "p3", "p4", "p5", "p6"), names(cf.get("/resources/properties").json()));
  }

  @Test
  void listingTheTagsReturnsEveryTag() {
    createTags("admin", "t1", "t2", "t3", "t4", "t5", "t6");

    assertEquals(
        List.of("t1", "t2", "t3", "t4", "t5", "t6"), names(cf.get("/resources/tags").json()));
  }

  @Test
  void deletingAPropertyOnMoreThanOnePageRemovesItEverywhere() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));

    assertEquals(200, cf.delete("/resources/properties/area", ApiClient.ADMIN).status());
    assertEquals(404, cf.get("/resources/properties/area").status());
    assertEquals(List.of(), names(cf.get("/resources/channels?area=*").json()));
  }

  @Test
  void deletingATagOnMoreThanOnePageRemovesItEverywhere() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putSevenChannelsWith(property("area", "admin", "north"), tag("archived", "admin"));

    assertEquals(200, cf.delete("/resources/tags/archived", ApiClient.ADMIN).status());
    assertEquals(404, cf.get("/resources/tags/archived").status());
    assertEquals(List.of(), names(cf.get("/resources/channels?~tag=archived").json()));
  }

  @Test
  void aChannelUsingADefinitionPastTheQuerySizeIsAccepted() {
    createProperties("admin", "p1", "p2", "p3", "p4", "p5", "p6");
    String usesP6 =
        withProperties(channel("ch1", "admin"), property("p6", "admin", "v")).toString();

    assertEquals(200, cf.put("/resources/channels/ch1", usesP6, ApiClient.ADMIN).status());
  }

  @Test
  void aChannelUsingATagPastTheQuerySizeIsAccepted() {
    createTags("admin", "t1", "t2", "t3", "t4", "t5", "t6");
    String usesT6 = withTags(channel("ch1", "admin"), tag("t6", "admin")).toString();

    assertEquals(200, cf.put("/resources/channels/ch1", usesT6, ApiClient.ADMIN).status());
  }
}
