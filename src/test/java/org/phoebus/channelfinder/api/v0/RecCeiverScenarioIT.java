package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.withChannels;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Replays the calls that recCeiver makes to ChannelFinder and checks what ChannelFinder does with
 * each. The sequences follow {@code server/recceiver/cf} in recsync and the pyCFClient calls it
 * makes.
 *
 * <p>The writes go as {@code user}, which has a channel group and no admin group, as a recCeiver
 * service account would. A channel group also allows property writes. The owner is the same name,
 * as recCeiver's {@code username} setting would set it.
 */
class RecCeiverScenarioIT extends ApiTestBase {

  private static final String OWNER = "user";
  private static final String CREDENTIALS = ApiClient.USER;
  private static final String RECCEIVER_ID = "recceiver1";
  // `findSizeLimit` in the recCeiver configuration. It is small here, so that the clean loop takes
  // two rounds and a commit of three records takes two PUTs.
  private static final int QUERY_LIMIT = 2;
  private static final List<String> MANAGED_PROPERTIES =
      List.of("hostName", "iocName", "iocid", "iocIP", "pvStatus", "time", "recceiverID");

  /** {@code _setup_cf_properties}: define every required property that is not defined yet. */
  private void provisionProperties() {
    Set<String> missing = new LinkedHashSet<>(MANAGED_PROPERTIES);
    names(cf.get("/resources/properties").json()).forEach(missing::remove);
    for (String name : missing) {
      assertEquals(
          200,
          cf.put("/resources/properties/" + name, property(name, OWNER).toString(), CREDENTIALS)
              .status());
    }
  }

  private static ObjectNode recordChannel(String name, String iocid, String time) {
    return withProperties(
        channel(name, OWNER),
        property("hostName", OWNER, "host1"),
        property("iocName", OWNER, "IOC1"),
        property("iocid", OWNER, iocid),
        property("iocIP", OWNER, "10.0.0.1"),
        property("pvStatus", OWNER, "Active"),
        property("time", OWNER, time),
        property("recceiverID", OWNER, RECCEIVER_ID));
  }

  /** {@code PyCFClientAdapter._find}: every lookup carries the query limit as {@code ~size}. */
  private List<JsonNode> find(String query) {
    ApiClient.Response response = cf.get("/resources/channels?" + query + "&~size=" + QUERY_LIMIT);
    assertEquals(200, response.status(), response.body());
    List<JsonNode> channels = new ArrayList<>();
    response.json().forEach(channels::add);
    return channels;
  }

  /** {@code _merge_property_lists}: add the old properties that recCeiver does not manage. */
  private static ObjectNode keepUnmanaged(ObjectNode channel, JsonNode old) {
    old.get("properties")
        .valueStream()
        .filter(p -> !MANAGED_PROPERTIES.contains(p.get("name").asString()))
        .forEach(p -> ((ArrayNode) channel.get("properties")).add(p));
    return channel;
  }

  /**
   * {@code _update_channelfinder} for an IOC whose records are all connected.
   *
   * <p>It looks the channels of the IOC up by {@code iocid}. A record found there gets {@code
   * pvStatus} and {@code time} and keeps only the properties recCeiver does not manage, as {@code
   * _handle_channel_old_and_new} does. It looks the other records up by name. This lookup is left
   * out if no records remain. A record found by name gets the full set of managed properties and
   * keeps the others. Then it PUTs the channels in batches of the query limit.
   */
  private void commit(String iocid, String time, String... records) {
    Set<String> remaining = new LinkedHashSet<>(List.of(records));
    List<ObjectNode> channels = new ArrayList<>();
    for (JsonNode old : find("iocid=" + iocid)) {
      String name = old.get("name").asString();
      if (remaining.remove(name)) {
        channels.add(
            keepUnmanaged(
                withProperties(
                    channel(name, old.get("owner").asString()),
                    property("pvStatus", OWNER, "Active"),
                    property("time", OWNER, time)),
                old));
      }
    }
    Map<String, JsonNode> existing = new HashMap<>();
    if (!remaining.isEmpty()) {
      find("~name=" + String.join("|", remaining))
          .forEach(old -> existing.put(old.get("name").asString(), old));
    }
    for (String name : remaining) {
      ObjectNode channel = recordChannel(name, iocid, time);
      channels.add(
          existing.containsKey(name) ? keepUnmanaged(channel, existing.get(name)) : channel);
    }
    for (int i = 0; i < channels.size(); i += QUERY_LIMIT) {
      List<ObjectNode> batch = channels.subList(i, Math.min(i + QUERY_LIMIT, channels.size()));
      assertEquals(
          200,
          cf.put(
                  "/resources/channels",
                  array(batch.toArray(ObjectNode[]::new)).toString(),
                  CREDENTIALS)
              .status());
    }
  }

  /**
   * {@code clean_service}: mark every Active channel of this recCeiver Inactive, page by page. The
   * body is what pyCFClient builds for {@code update(property=..., channelNames=...)}.
   */
  private int clean() {
    int rounds = 0;
    List<JsonNode> active = find("pvStatus=Active&recceiverID=" + RECCEIVER_ID);
    while (!active.isEmpty()) {
      ObjectNode[] channels =
          active.stream()
              .map(
                  ch ->
                      withProperties(
                          channel(ch.get("name").asString(), OWNER),
                          property("pvStatus", OWNER, "Inactive")))
              .toArray(ObjectNode[]::new);
      String body = withChannels(property("pvStatus", OWNER, "Inactive"), channels).toString();
      assertEquals(200, cf.post("/resources/properties/pvStatus", body, CREDENTIALS).status());
      rounds++;
      assertTrue(rounds < 10, "clean loop does not terminate");
      active = find("pvStatus=Active&recceiverID=" + RECCEIVER_ID);
    }
    return rounds;
  }

  @Test
  void firstLookupsOfANewIocReturnEmptyLists() {
    provisionProperties();

    assertEquals(List.of(), find("iocid=host1:5064"));
    assertEquals(List.of(), find("~name=rec1|rec2"));
  }

  @Test
  void commitKeepsCuratedPropertiesThroughReadMergeWrite() {
    provisionProperties();
    createProperties("curator", "archive");
    putChannels(
        withProperties(channel("rec1", OWNER), property("archive", "curator", "monitor 1.0")));

    commit("host1:5064", "2026-11-17 10:00:00", "rec1", "rec2");

    assertEquals(
        List.of("rec1", "rec2"), names(cf.get("/resources/channels?iocid=host1:5064").json()));
    JsonNode rec1 = cf.get("/resources/channels/rec1").json();
    assertEquals("monitor 1.0", propertyValue(rec1, "archive"));
    assertEquals("Active", propertyValue(rec1, "pvStatus"));
    assertEquals(RECCEIVER_ID, propertyValue(rec1, "recceiverID"));
  }

  @Test
  void recommitWithTheSameIocidUpdatesTheTime() {
    provisionProperties();
    commit("host1:5064", "2026-11-17 10:00:00", "rec1");

    commit("host1:5064", "2026-11-17 11:00:00", "rec1");

    JsonNode rec1 = cf.get("/resources/channels/rec1").json();
    assertEquals("2026-11-17 11:00:00", propertyValue(rec1, "time"));
    assertEquals("Active", propertyValue(rec1, "pvStatus"));
  }

  @Test
  void cleanLoopTerminatesAndOnlyTouchesThisRecCeiversChannels() {
    provisionProperties();
    commit("host1:5064", "2026-11-17 10:00:00", "rec1", "rec2", "rec3");
    putChannels(
        withProperties(
            channel("other", OWNER),
            property("pvStatus", OWNER, "Active"),
            property("recceiverID", OWNER, "recceiver2")));

    int rounds = clean();

    assertEquals(2, rounds);
    for (String name : List.of("rec1", "rec2", "rec3")) {
      JsonNode channel = cf.get("/resources/channels/" + name).json();
      assertEquals("Inactive", propertyValue(channel, "pvStatus"));
      assertEquals("host1:5064", propertyValue(channel, "iocid"));
    }
    assertEquals("Active", propertyValue(cf.get("/resources/channels/other").json(), "pvStatus"));
  }
}
