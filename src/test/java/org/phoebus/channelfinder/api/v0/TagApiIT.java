package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.tagNames;
import static org.phoebus.channelfinder.api.v0.ApiJson.withChannels;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests the tag endpoints: the exclusive PUT, the merging POST, rename and single-channel writes,
 * as {@code src/site/sphinx/api.rst} documents them.
 */
class TagApiIT extends ApiTestBase {

  @BeforeEach
  void createChannels() {
    createTags("admin", "archived", "beam");
    putChannels(
        withTags(channel("ch1", "admin"), tag("archived", "admin"), tag("beam", "admin")),
        withTags(channel("ch2", "admin"), tag("archived", "admin")),
        channel("ch3", "admin"));
  }

  /** A tag body that adds the tag to each named channel. */
  private static String onChannels(String name, String... channelNames) {
    ObjectNode[] channels = new ObjectNode[channelNames.length];
    for (int i = 0; i < channelNames.length; i++) {
      channels[i] = withTags(channel(channelNames[i], "admin"), tag(name, "admin"));
    }
    return withChannels(tag(name, "admin"), channels).toString();
  }

  private List<String> channelsWith(String tag) {
    return names(cf.get("/resources/channels?~tag=" + tag).json());
  }

  @Test
  void getListsTheChannelsThatHaveTheTag() {
    ApiClient.Response response = cf.get("/resources/tags/beam");

    assertEquals(200, response.status());
    assertEquals(List.of("ch1"), names(response.json().get("channels")));
    assertEquals(
        0, cf.get("/resources/tags/beam?withChannels=false").json().get("channels").size());
  }

  @Test
  void putWithChannelsTagsThoseChannels() {
    assertEquals(
        200, cf.put("/resources/tags/beam", onChannels("beam", "ch3"), ApiClient.ADMIN).status());

    assertEquals(List.of("ch3"), channelsWith("beam"));
  }

  @Test
  void putIsExclusiveAndUntagsChannelsNotInThePayload() {
    cf.put("/resources/tags/archived", onChannels("archived", "ch3"), ApiClient.ADMIN);

    assertEquals(List.of("ch3"), channelsWith("archived"));
    assertEquals(List.of("beam"), tagNames(cf.get("/resources/channels/ch1").json()));
  }

  @Test
  void putWithoutChannelsUntagsEveryChannel() {
    // A PUT of the definition alone untags every channel. api.rst documents this, because the PUT
    // sets the tag exclusively on the channels in the payload. It is still worth a test, because a
    // PUT that only means to create the definition is easy to send.
    cf.put("/resources/tags/archived", tag("archived", "admin").toString(), ApiClient.ADMIN);

    assertEquals(200, cf.get("/resources/tags/archived").status());
    assertEquals(List.of(), channelsWith("archived"));
  }

  @Test
  void batchPutCreatesEachDefinition() {
    String tags = array(tag("alarm", "admin"), tag("vacuum", "admin")).toString();

    assertEquals(200, cf.put("/resources/tags", tags, ApiClient.ADMIN).status());
    assertEquals(
        List.of("alarm", "archived", "beam", "vacuum"), names(cf.get("/resources/tags").json()));
  }

  @Test
  void postWithChannelsMergesAndKeepsTheOtherChannels() {
    assertEquals(
        200, cf.post("/resources/tags/beam", onChannels("beam", "ch3"), ApiClient.ADMIN).status());

    assertEquals(List.of("ch1", "ch3"), channelsWith("beam"));
  }

  @Test
  void postWithoutChannelsCreatesAMissingDefinition() {
    assertEquals(
        200,
        cf.post("/resources/tags/alarm", tag("alarm", "admin").toString(), ApiClient.ADMIN)
            .status());
    assertEquals(200, cf.get("/resources/tags/alarm").status());
  }

  @Test
  void batchPostKeepsTheExistingOwner() {
    String tags = array(tag("archived", "newOwner")).toString();

    assertEquals(200, cf.post("/resources/tags", tags, ApiClient.ADMIN).status());
    assertEquals("admin", cf.get("/resources/tags/archived").json().get("owner").asString());
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value =
          "a tag rename leaves the old tag on its channels, because the channels read before the"
              + " delete are written back",
      withExceptions = AssertionFailedError.class)
  void postWithANewNameRenamesTheTagEverywhere() {
    assertEquals(
        200,
        cf.post("/resources/tags/archived", tag("saved", "admin").toString(), ApiClient.ADMIN)
            .status());

    assertEquals(404, cf.get("/resources/tags/archived").status());
    assertEquals(List.of("ch1", "ch2"), channelsWith("saved"));
    // today: ch1 and ch2 keep the old tag
    assertEquals(List.of(), channelsWith("archived"));
  }

  @Test
  void putOnASingleChannelAddsTheTag() {
    assertEquals(200, cf.put("/resources/tags/beam/ch3", "", ApiClient.ADMIN).status());

    assertEquals(List.of("ch1", "ch3"), channelsWith("beam"));
    assertEquals(404, cf.put("/resources/tags/beam/missing", "", ApiClient.ADMIN).status());
  }

  @Test
  void deleteRemovesTheDefinitionAndUntagsEveryChannel() {
    assertEquals(200, cf.delete("/resources/tags/archived", ApiClient.ADMIN).status());

    assertEquals(404, cf.get("/resources/tags/archived").status());
    assertEquals(List.of("beam"), tagNames(cf.get("/resources/channels/ch1").json()));
    assertEquals(List.of(), tagNames(cf.get("/resources/channels/ch2").json()));
  }

  @Test
  void deleteFromASingleChannelLeavesTheOthers() {
    assertEquals(200, cf.delete("/resources/tags/archived/ch1", ApiClient.ADMIN).status());

    assertEquals(List.of("ch2"), channelsWith("archived"));
  }
}
