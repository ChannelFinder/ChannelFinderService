package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests {@code /scroll} and its {@code {id, channels}} response. Clients use it to page past the
 * 10000 result cap.
 */
class ScrollApiIT extends ApiTestBase {

  @BeforeEach
  void createChannels() {
    createProperties("admin", "area");
    putChannels(
        withProperties(channel("ch1", "admin"), property("area", "admin", "north")),
        withProperties(channel("ch2", "admin"), property("area", "admin", "south")),
        withProperties(channel("ch3", "admin"), property("area", "admin", "north")),
        withProperties(channel("ch4", "admin"), property("area", "admin", "north")),
        withProperties(channel("ch5", "admin"), property("area", "admin", "north")));
  }

  private JsonNode scroll(String pathAndQuery) {
    ApiClient.Response response = cf.get("/resources/scroll" + pathAndQuery);
    assertEquals(200, response.status(), response.body());
    return response.json();
  }

  @Test
  void firstPageReturnsTheLastChannelNameAsTheId() {
    JsonNode page = scroll("?~size=2");

    assertEquals("ch2", page.get("id").asString());
    assertEquals(List.of("ch1", "ch2"), names(page.get("channels")));
  }

  @Test
  void followingTheIdPagesThroughEveryChannelAndEndsWithANullId() {
    JsonNode second = scroll("/ch2?~size=2");
    JsonNode third = scroll("/ch4?~size=2");
    JsonNode end = scroll("/ch5?~size=2");

    assertEquals(List.of("ch3", "ch4"), names(second.get("channels")));
    assertEquals("ch4", second.get("id").asString());
    assertEquals(List.of("ch5"), names(third.get("channels")));
    assertEquals("ch5", third.get("id").asString());
    assertEquals(List.of(), names(end.get("channels")));
    assertTrue(end.get("id").isNull());
  }

  @Test
  void idNeedNotBeTheNameOfAChannel() {
    // The id is the name to continue after, so any name works, and no id is unknown.
    JsonNode page = scroll("/ch25?~size=2");

    assertEquals(List.of("ch3", "ch4"), names(page.get("channels")));
  }

  @Test
  void sizeZeroReturnsNoChannelsAndANullId() {
    JsonNode page = scroll("?~size=0");

    assertEquals(List.of(), names(page.get("channels")));
    assertTrue(page.get("id").isNull());
  }

  @Test
  void scrollAcceptsTheChannelQueryLanguage() {
    JsonNode first = scroll("?area=north&~size=2");
    JsonNode second = scroll("/" + first.get("id").asString() + "?area=north&~size=2");

    assertEquals(List.of("ch1", "ch3"), names(first.get("channels")));
    assertEquals(List.of("ch4", "ch5"), names(second.get("channels")));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"~size=-1", "~from=-1"})
  void negativeSizeOrFromIsRejectedWith400(String query) {
    assertEquals(400, cf.get("/resources/scroll?" + query).status());
  }

  @Test
  void defaultPageSizeIsTheQuerySize() {
    // api.rst gives 100 as the current default page size. The code uses elasticsearch.query.size,
    // which is 10000 here, and this test records the code.
    putChannels(
        IntStream.range(0, 100)
            .mapToObj(i -> channel(String.format("zz%03d", i), "admin"))
            .toArray(ObjectNode[]::new));

    JsonNode page = scroll("");

    assertEquals(105, page.get("channels").size());
    assertEquals("zz099", page.get("id").asString());
  }
}
