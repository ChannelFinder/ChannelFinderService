package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.withChannels;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests the property endpoints: the exclusive PUT, the merging POST, rename, create-if-absent and
 * single-channel writes, as {@code src/site/sphinx/api.rst} documents them.
 */
class PropertyApiIT extends ApiTestBase {

  @BeforeEach
  void createChannels() {
    createProperties("admin", "area", "number");
    putChannels(
        withProperties(
            channel("ch1", "admin"),
            property("area", "admin", "north"),
            property("number", "admin", "001")),
        withProperties(channel("ch2", "admin"), property("area", "admin", "north")),
        channel("ch3", "admin"));
  }

  /** A property body that sets {@code value} on each named channel, as pyCFClient builds it. */
  private static String onChannels(String name, String value, String... channelNames) {
    ObjectNode[] channels = new ObjectNode[channelNames.length];
    for (int i = 0; i < channelNames.length; i++) {
      channels[i] =
          withProperties(channel(channelNames[i], "admin"), property(name, "admin", value));
    }
    return withChannels(property(name, "admin"), channels).toString();
  }

  private JsonNode channelNamed(String name) {
    return cf.get("/resources/channels/" + name).json();
  }

  private List<String> channelsWith(String query) {
    return names(cf.get("/resources/channels?" + query).json());
  }

  @Test
  void getListsTheChannelsThatHaveTheProperty() throws Exception {
    ApiClient.Response response = cf.get("/resources/properties/number");

    assertEquals(200, response.status());
    JSONAssert.assertEquals(
        """
        {"name":"number","owner":"admin","value":null,
         "channels":[{"name":"ch1","owner":"admin",
                      "properties":[{"name":"area","owner":"admin","value":"north","channels":[]},
                                    {"name":"number","owner":"admin","value":"001","channels":[]}],
                      "tags":[]}]}
        """,
        response.body(),
        JSONCompareMode.STRICT);
  }

  @Test
  void getWithoutChannelsOmitsThem() {
    ApiClient.Response response = cf.get("/resources/properties/number?withChannels=false");

    assertEquals(200, response.status());
    assertEquals(0, response.json().get("channels").size());
  }

  @Test
  void putWithChannelsSetsTheValueOnThoseChannels() {
    ApiClient.Response response =
        cf.put("/resources/properties/number", onChannels("number", "003", "ch3"), ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals("003", propertyValue(channelNamed("ch3"), "number"));
  }

  @Test
  void putIsExclusiveAndRemovesThePropertyFromChannelsNotInThePayload() {
    cf.put("/resources/properties/area", onChannels("area", "south", "ch3"), ApiClient.ADMIN);

    assertEquals(List.of("ch3"), channelsWith("area=*"));
    assertEquals("001", propertyValue(channelNamed("ch1"), "number"));
  }

  @Test
  void putWithoutChannelsRemovesThePropertyFromEveryChannel() {
    // A PUT of the definition alone removes the property from every channel. api.rst documents
    // this, because the PUT sets the property exclusively on the channels in the payload. It is
    // still worth a test, because a PUT that only means to create the definition is easy to send.
    cf.put("/resources/properties/area", property("area", "admin").toString(), ApiClient.ADMIN);

    assertEquals(200, cf.get("/resources/properties/area").status());
    assertEquals(List.of(), channelsWith("area=*"));
  }

  @Test
  void batchPutCreatesEachDefinition() {
    String properties =
        array(property("hostName", "admin"), property("iocName", "admin")).toString();

    assertEquals(200, cf.put("/resources/properties", properties, ApiClient.ADMIN).status());
    assertEquals(
        List.of("area", "hostName", "iocName", "number"),
        names(cf.get("/resources/properties").json()));
  }

  @Test
  void postWithChannelsMergesAndKeepsTheOtherChannels() {
    ApiClient.Response response =
        cf.post(
            "/resources/properties/area",
            onChannels("area", "south", "ch1", "ch3"),
            ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals("south", propertyValue(channelNamed("ch1"), "area"));
    assertEquals("north", propertyValue(channelNamed("ch2"), "area"));
    assertEquals("south", propertyValue(channelNamed("ch3"), "area"));
    assertEquals("001", propertyValue(channelNamed("ch1"), "number"));
  }

  @Test
  void batchPostKeepsTheExistingOwner() {
    String properties = array(property("area", "newOwner")).toString();

    assertEquals(200, cf.post("/resources/properties", properties, ApiClient.ADMIN).status());
    assertEquals("admin", cf.get("/resources/properties/area").json().get("owner").asString());
  }

  @Test
  void postWithoutChannelsCreatesAMissingDefinition() {
    String hostName = property("hostName", "admin").toString();

    assertEquals(
        200, cf.post("/resources/properties/hostName", hostName, ApiClient.ADMIN).status());
    assertEquals(200, cf.get("/resources/properties/hostName").status());
  }

  @Test
  void postWithoutChannelsLeavesAnExistingPropertyOnItsChannels() {
    String area = property("area", "newOwner").toString();

    assertEquals(200, cf.post("/resources/properties/area", area, ApiClient.ADMIN).status());
    assertEquals(List.of("ch1", "ch2"), channelsWith("area=north"));
    assertEquals("newOwner", cf.get("/resources/properties/area").json().get("owner").asString());
  }

  @Test
  void postWithANewNameRenamesThePropertyEverywhere() {
    String renamed = property("section", "admin").toString();

    assertEquals(200, cf.post("/resources/properties/area", renamed, ApiClient.ADMIN).status());

    assertEquals(404, cf.get("/resources/properties/area").status());
    assertEquals(200, cf.get("/resources/properties/section").status());
    assertEquals(List.of("ch1", "ch2"), channelsWith("section=north"));
    assertNull(propertyValue(channelNamed("ch1"), "area"));
    assertEquals("001", propertyValue(channelNamed("ch1"), "number"));
  }

  @Test
  void putOnASingleChannelAddsTheProperty() {
    ApiClient.Response response =
        cf.put(
            "/resources/properties/number/ch3",
            property("number", "admin", "003").toString(),
            ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals("003", propertyValue(channelNamed("ch3"), "number"));
  }

  @Test
  void putOnASingleChannelRejectsAMismatchedOrEmptyPayload() {
    assertEquals(
        400,
        cf.put(
                "/resources/properties/number/ch3",
                property("area", "admin", "003").toString(),
                ApiClient.ADMIN)
            .status());
    assertEquals(
        400,
        cf.put(
                "/resources/properties/number/ch3",
                property("number", "admin").toString(),
                ApiClient.ADMIN)
            .status());
    assertEquals(
        404,
        cf.put(
                "/resources/properties/number/missing",
                property("number", "admin", "003").toString(),
                ApiClient.ADMIN)
            .status());
  }

  @Test
  void deleteRemovesTheDefinitionAndTheValuesOnEveryChannel() {
    assertEquals(200, cf.delete("/resources/properties/area", ApiClient.ADMIN).status());

    assertEquals(404, cf.get("/resources/properties/area").status());
    assertNull(propertyValue(channelNamed("ch1"), "area"));
    assertNull(propertyValue(channelNamed("ch2"), "area"));
    assertEquals("001", propertyValue(channelNamed("ch1"), "number"));
  }

  @Test
  void deleteFromASingleChannelLeavesTheOthers() {
    assertEquals(200, cf.delete("/resources/properties/area/ch1", ApiClient.ADMIN).status());

    assertEquals(List.of("ch2"), channelsWith("area=*"));
  }
}
