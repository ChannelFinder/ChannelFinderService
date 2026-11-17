package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyNames;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.tagNames;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Tests PUT-replace and POST-merge on channels, rename through POST, and how owners are set. */
class ChannelWriteApiIT extends ApiTestBase {

  // Longer than the 32766 bytes Elasticsearch accepts for a keyword term.
  private static final String IMMENSE_VALUE = "x".repeat(40_000);

  @BeforeEach
  void createDefinitions() {
    createProperties("propOwner", "area", "number");
    createTags("tagOwner", "archived", "beam");
  }

  private static ObjectNode sampleChannel(String name) {
    return withTags(
        withProperties(
            channel(name, "admin"),
            property("area", "propOwner", "north"),
            property("number", "propOwner", "001")),
        tag("archived", "tagOwner"));
  }

  /** A batch with one channel whose value is too long for Elasticsearch to index. */
  private static String unindexableBatch() {
    return array(
            withProperties(channel("ch1", "admin"), property("area", "propOwner", IMMENSE_VALUE)))
        .toString();
  }

  private JsonNode read(String name) {
    ApiClient.Response response = cf.get("/resources/channels/" + name);
    assertEquals(200, response.status(), response.body());
    return response.json();
  }

  @Test
  void putReplacesTheWholeChannel() {
    putChannels(sampleChannel("ch1"));

    String replacement =
        withProperties(channel("ch1", "admin"), property("area", "propOwner", "south")).toString();
    assertEquals(200, cf.put("/resources/channels/ch1", replacement, ApiClient.ADMIN).status());

    JsonNode ch1 = read("ch1");
    assertEquals(List.of("area"), propertyNames(ch1));
    assertEquals("south", propertyValue(ch1, "area"));
    assertEquals(List.of(), tagNames(ch1));
  }

  @Test
  void batchPutReplacesEachChannel() {
    putChannels(sampleChannel("ch1"), sampleChannel("ch2"));

    putChannels(
        withProperties(channel("ch1", "admin"), property("area", "propOwner", "south")),
        withTags(channel("ch2", "admin"), tag("beam", "tagOwner")));

    assertEquals(List.of("area"), propertyNames(read("ch1")));
    assertEquals(List.of(), tagNames(read("ch1")));
    assertEquals(List.of(), propertyNames(read("ch2")));
    assertEquals(List.of("beam"), tagNames(read("ch2")));
  }

  @Test
  void postMergesIntoTheChannel() {
    putChannels(
        withTags(
            withProperties(channel("ch1", "admin"), property("area", "propOwner", "north")),
            tag("archived", "tagOwner")));

    String update =
        withTags(
                withProperties(
                    channel("ch1", "admin"),
                    property("area", "propOwner", "south"),
                    property("number", "propOwner", "001")),
                tag("beam", "tagOwner"))
            .toString();
    assertEquals(200, cf.post("/resources/channels/ch1", update, ApiClient.ADMIN).status());

    JsonNode ch1 = read("ch1");
    assertEquals("south", propertyValue(ch1, "area"));
    assertEquals("001", propertyValue(ch1, "number"));
    assertEquals(List.of("archived", "beam"), tagNames(ch1).stream().sorted().toList());
  }

  @Test
  void batchPostMergesIntoEachChannel() {
    putChannels(sampleChannel("ch1"), channel("ch2", "admin"));

    String update =
        array(
                withTags(channel("ch1", "admin"), tag("beam", "tagOwner")),
                withProperties(channel("ch2", "admin"), property("number", "propOwner", "002")))
            .toString();
    assertEquals(200, cf.post("/resources/channels", update, ApiClient.ADMIN).status());

    assertEquals(List.of("area", "number"), propertyNames(read("ch1")).stream().sorted().toList());
    assertEquals(List.of("archived", "beam"), tagNames(read("ch1")).stream().sorted().toList());
    assertEquals("002", propertyValue(read("ch2"), "number"));
  }

  @Test
  void postCreatesAMissingChannel() {
    String ch1 = sampleChannel("ch1").toString();

    assertEquals(200, cf.post("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
    assertEquals("north", propertyValue(read("ch1"), "area"));
  }

  @Test
  void postWithANewNameRenamesTheChannel() {
    putChannels(sampleChannel("ch1"));

    String renamed =
        withProperties(channel("ch1-renamed", "admin"), property("number", "propOwner", "002"))
            .toString();
    ApiClient.Response response = cf.post("/resources/channels/ch1", renamed, ApiClient.ADMIN);

    assertEquals(200, response.status());
    assertEquals("ch1-renamed", response.json().get("name").asString());
    assertEquals(404, cf.get("/resources/channels/ch1").status());
    JsonNode ch1Renamed = read("ch1-renamed");
    assertEquals("north", propertyValue(ch1Renamed, "area"));
    assertEquals("002", propertyValue(ch1Renamed, "number"));
    assertEquals(List.of("archived"), tagNames(ch1Renamed));
  }

  @Test
  void postThatOnlyChangesTheCaseOfTheNameDoesNotRename() {
    // api.rst documents that names ignore case, so CH1 is the same channel and nothing is
    // renamed. It is still worth a test, because the request looks like a rename.
    putChannels(sampleChannel("ch1"));

    assertEquals(
        200,
        cf.post("/resources/channels/ch1", channel("CH1", "admin").toString(), ApiClient.ADMIN)
            .status());

    assertEquals(List.of("ch1"), names(cf.get("/resources/channels").json()));
  }

  @Test
  void putUsesTheNameInThePayloadNotThePath() {
    // One would expect ch1 to be written, or the request to be rejected. ch2 is written instead.
    String ch2 = sampleChannel("ch2").toString();

    assertEquals(200, cf.put("/resources/channels/ch1", ch2, ApiClient.ADMIN).status());

    assertEquals(404, cf.get("/resources/channels/ch1").status());
    assertEquals(200, cf.get("/resources/channels/ch2").status());
  }

  @Test
  void propertyAndTagOwnersAreResetToTheDefinitionOwners() {
    putChannels(
        withTags(
            withProperties(channel("ch1", "admin"), property("area", "someoneElse", "north")),
            tag("archived", "someoneElse")));

    JsonNode ch1 = read("ch1");
    assertEquals("propOwner", ch1.get("properties").get(0).get("owner").asString());
    assertEquals("tagOwner", ch1.get("tags").get(0).get("owner").asString());
  }

  @Test
  void singlePutAndPostTakeTheChannelOwnerFromThePayload() {
    putChannels(channel("ch1", "admin"));

    cf.put("/resources/channels/ch1", channel("ch1", "newOwner").toString(), ApiClient.ADMIN);
    assertEquals("newOwner", read("ch1").get("owner").asString());

    cf.post("/resources/channels/ch1", channel("ch1", "otherOwner").toString(), ApiClient.ADMIN);
    assertEquals("otherOwner", read("ch1").get("owner").asString());
  }

  @Test
  void batchPutAndPostKeepTheExistingChannelOwner() {
    // Single writes take the owner from the payload, but batch writes keep the existing owner.
    // api.rst documents this for batch PUT, and batch POST does the same. It is still worth a
    // test, because the two kinds of write are easy to confuse.
    putChannels(channel("ch1", "admin"));

    putChannels(channel("ch1", "newOwner"));
    assertEquals("admin", read("ch1").get("owner").asString());

    cf.post("/resources/channels", array(channel("ch1", "otherOwner")).toString(), ApiClient.ADMIN);
    assertEquals("admin", read("ch1").get("owner").asString());
  }

  @Test
  void batchDeleteReturnsTheNumberOfDeletedChannels() {
    putChannels(channel("ch1", "admin"), channel("ch2", "admin"));

    ApiClient.Response response =
        cf.delete("/resources/channels", "[\"ch1\",\"ch2\",\"missing\"]", ApiClient.ADMIN);

    assertEquals(new ApiClient.Response(200, "2"), response);
    assertEquals(List.of(), names(cf.get("/resources/channels").json()));
  }

  @Test
  void emptyPropertyValueIsRejectedWith400() {
    String ch1 =
        withProperties(channel("ch1", "admin"), property("area", "propOwner", "")).toString();

    assertEquals(400, cf.put("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
  }

  @Test
  void missingOwnerIsRejectedWith400() {
    assertEquals(
        400, cf.put("/resources/channels/ch1", "{\"name\":\"ch1\"}", ApiClient.ADMIN).status());
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value =
          "PUT deletes the channel before it writes the new one, so a failed write loses the"
              + " channel (#168)",
      withExceptions = AssertionFailedError.class)
  void failedSinglePutKeepsTheExistingChannel() {
    putChannels(sampleChannel("ch1"));

    String unindexable =
        withProperties(channel("ch1", "admin"), property("area", "propOwner", IMMENSE_VALUE))
            .toString();

    // today: returns 500 and the channel is gone
    assertNotEquals(200, cf.put("/resources/channels/ch1", unindexable, ApiClient.ADMIN).status());
    assertEquals("north", propertyValue(read("ch1"), "area"));
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value = "a failed batch PUT returns 200, because bulk errors are only logged",
      withExceptions = AssertionFailedError.class)
  void failedBatchPutReportsAnError() {
    putChannels(sampleChannel("ch1"));

    ApiClient.Response response =
        cf.put("/resources/channels", unindexableBatch(), ApiClient.ADMIN);

    // today: returns 200 with []
    // Which error status a failed bulk write returns is not decided yet, only that it is not 200.
    assertNotEquals(200, response.status());
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value = "a failed batch PUT loses the channel, because PUT deletes before it writes (#168)",
      withExceptions = AssertionFailedError.class)
  void failedBatchPutKeepsTheExistingChannel() {
    putChannels(sampleChannel("ch1"));

    cf.put("/resources/channels", unindexableBatch(), ApiClient.ADMIN);

    // today: the channel is gone
    assertEquals("north", propertyValue(read("ch1"), "area"));
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value = "a failed batch POST returns 200, because bulk errors are only logged",
      withExceptions = AssertionFailedError.class)
  void failedBatchPostReportsAnError() {
    putChannels(sampleChannel("ch1"));

    ApiClient.Response response =
        cf.post("/resources/channels", unindexableBatch(), ApiClient.ADMIN);

    // today: returns 200 with [] and leaves the channel unchanged
    // Which error status a failed bulk write returns is not decided yet, only that it is not 200.
    assertNotEquals(200, response.status());
    assertEquals("north", propertyValue(read("ch1"), "area"));
  }
}
