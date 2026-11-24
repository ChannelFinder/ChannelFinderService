package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.JsonNode;

/**
 * Tests the endpoints under the service root, their status codes, their content type and the JSON
 * shape of their responses.
 */
class EndpointApiIT extends ApiTestBase {

  private static final List<String> SINGLE_WRITES =
      List.of(
          "PUT /channels/x",
          "POST /channels/x",
          "PUT /properties/x",
          "POST /properties/x",
          "PUT /tags/x",
          "POST /tags/x");

  private static final List<String> BATCH_WRITES =
      List.of(
          "PUT /channels",
          "POST /channels",
          "PUT /properties",
          "POST /properties",
          "PUT /tags",
          "POST /tags");

  static Stream<String> writes() {
    return Stream.concat(SINGLE_WRITES.stream(), BATCH_WRITES.stream());
  }

  static Stream<Arguments> singleWritesWithoutANameOrAnOwner() {
    List<String> bodies =
        List.of(
            "{\"owner\":\"admin\"}",
            "{\"name\":\"\",\"owner\":\"admin\"}",
            "{\"name\":\"x\"}",
            "{\"name\":\"x\",\"owner\":\"\"}");
    return SINGLE_WRITES.stream()
        .flatMap(request -> bodies.stream().map(body -> Arguments.of(request, body)));
  }

  static Stream<Arguments> batchWritesOfAChannelWithoutAName() {
    List<String> items = List.of("{\"owner\":\"admin\"}", "{\"name\":\"\",\"owner\":\"admin\"}");
    return Stream.of("PUT /channels", "POST /channels")
        .flatMap(request -> items.stream().map(item -> Arguments.of(request, item)));
  }

  /** Send a request written as {@code "PUT /channels/x"} under {@code /resources}, as admin. */
  private ApiClient.Response send(String request, String body) {
    String[] methodAndPath = request.split(" ");
    String path = "/resources" + methodAndPath[1];
    return methodAndPath[0].equals("PUT")
        ? cf.put(path, body, ApiClient.ADMIN)
        : cf.post(path, body, ApiClient.ADMIN);
  }

  @Test
  void serviceRootReturnsServiceInfoAsJson() {
    ApiClient.Response response = cf.get("");

    assertEquals(200, response.status());
    JsonNode info = response.json();
    assertEquals("ChannelFinder Service", info.get("name").asString());
    assertEquals("Connected", info.get("elastic").get("status").asString());
  }

  @Test
  void emptyDirectoryListsReturn200AndEmptyArrays() {
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels"));
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/properties"));
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/tags"));
  }

  @Test
  void searchWithoutMatchesReturns200AndAnEmptyArrayNever404() {
    // Many HTTP APIs answer 404 when nothing matches. This service answers 200 with [].
    createProperties("admin", "iocid");
    createTags("admin", "archived");
    putChannels(
        withTags(
            withProperties(channel("ch1", "admin"), property("iocid", "admin", "ioc1")),
            tag("archived", "admin")));

    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels?~name=nothing*"));
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels?iocid=ioc2"));
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels?undefined=x"));
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels?~tag=nothing"));
    assertEquals(
        new ApiClient.Response(200, "[]"),
        cf.get("/resources/channels?~name=ch1&iocid=ioc1&~tag=nothing"));
  }

  @Test
  void countReturnsABareInteger() {
    assertEquals(new ApiClient.Response(200, "0"), cf.get("/resources/channels/count"));
  }

  @Test
  void combinedReturnsChannelsAndCount() throws Exception {
    ApiClient.Response response = cf.get("/resources/channels/combined");

    assertEquals(200, response.status());
    JSONAssert.assertEquals(
        "{\"channels\":[],\"count\":0}", response.body(), JSONCompareMode.STRICT);
  }

  @Test
  void scrollReturnsIdAndChannels() throws Exception {
    ApiClient.Response response = cf.get("/resources/scroll");

    assertEquals(200, response.status());
    JSONAssert.assertEquals(
        "{\"id\":null,\"channels\":[]}", response.body(), JSONCompareMode.STRICT);
  }

  @Test
  void processorsReportCountAndInfo() {
    // The archiver processor is off, but the test classpath contributes processors of its own
    // (ElasticConfig scans the whole package), so only the shape is checked here. api.rst documents
    // the list at /processors/info. The code serves it at /processors/processors, and this test
    // records the code.
    ApiClient.Response count = cf.get("/resources/processors/count");
    ApiClient.Response info = cf.get("/resources/processors/processors");

    assertEquals(200, count.status());
    assertEquals(200, info.status());
    assertEquals(Long.parseLong(count.body()), info.json().size());
    for (JsonNode processor : info.json()) {
      assertEquals(
          List.of("name", "enabled", "properties"), processor.propertyNames().stream().toList());
    }
  }

  @Test
  void responsesAreJson() {
    for (String path :
        List.of(
            "",
            "/resources/channels",
            "/resources/channels/count",
            "/resources/channels/combined",
            "/resources/properties",
            "/resources/tags",
            "/resources/scroll")) {
      assertEquals("application/json", cf.contentTypeOfGet(path), path);
    }
  }

  @Test
  void missingSingleResourcesReturn404() {
    assertEquals(404, cf.get("/resources/channels/missing").status());
    assertEquals(404, cf.get("/resources/properties/missing").status());
    assertEquals(404, cf.get("/resources/tags/missing").status());
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {ApiClient.ADMIN, ApiClient.USER})
  void deletingMissingResourcesReturns404(String credentials) {
    assertEquals(404, cf.delete("/resources/channels/missing", credentials).status());
    assertEquals(404, cf.delete("/resources/properties/missing", credentials).status());
    assertEquals(404, cf.delete("/resources/tags/missing", credentials).status());
  }

  @Test
  void successfulWritesReturnExactly200() {
    assertEquals(
        200,
        cf.put("/resources/properties/area", property("area", "admin").toString(), ApiClient.ADMIN)
            .status());
    assertEquals(
        200,
        cf.put("/resources/tags/archived", tag("archived", "admin").toString(), ApiClient.ADMIN)
            .status());
    String ch1 =
        withTags(
                withProperties(channel("ch1", "admin"), property("area", "admin", "north")),
                tag("archived", "admin"))
            .toString();
    assertEquals(200, cf.put("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
    assertEquals(200, cf.post("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
    assertEquals(200, cf.put("/resources/tags/archived/ch1", "", ApiClient.ADMIN).status());
    assertEquals(
        200,
        cf.put(
                "/resources/properties/area/ch1",
                property("area", "admin", "south").toString(),
                ApiClient.ADMIN)
            .status());
    assertEquals(200, cf.delete("/resources/tags/archived/ch1", ApiClient.ADMIN).status());
    assertEquals(200, cf.delete("/resources/properties/area/ch1", ApiClient.ADMIN).status());
    assertEquals(200, cf.delete("/resources/channels/ch1", ApiClient.ADMIN).status());
    assertEquals(200, cf.delete("/resources/tags/archived", ApiClient.ADMIN).status());
    assertEquals(200, cf.delete("/resources/properties/area", ApiClient.ADMIN).status());
  }

  @Test
  void channelResponseShape() throws Exception {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putChannels(
        withTags(
            withProperties(channel("ch1", "admin"), property("area", "admin", "north")),
            tag("archived", "admin")));

    ApiClient.Response response = cf.get("/resources/channels/ch1");

    assertEquals(200, response.status());
    JSONAssert.assertEquals(
        """
        {"name":"ch1","owner":"admin",
         "properties":[{"name":"area","owner":"admin","value":"north","channels":[]}],
         "tags":[{"name":"archived","owner":"admin","channels":[]}]}
        """,
        response.body(),
        JSONCompareMode.STRICT);
  }

  @Test
  void definitionResponseShape() throws Exception {
    createProperties("admin", "area");
    createTags("admin", "archived");

    JSONAssert.assertEquals(
        "[{\"name\":\"area\",\"owner\":\"admin\",\"value\":null,\"channels\":[]}]",
        cf.get("/resources/properties").body(),
        JSONCompareMode.STRICT);
    JSONAssert.assertEquals(
        "[{\"name\":\"archived\",\"owner\":\"admin\",\"channels\":[]}]",
        cf.get("/resources/tags").body(),
        JSONCompareMode.STRICT);
  }

  @Test
  void writingAChannelWithAnUndefinedPropertyReturns404() {
    String ch1 =
        withProperties(channel("ch1", "admin"), property("undefined", "admin", "x")).toString();

    assertEquals(404, cf.put("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value =
          "a channel with an undefined tag gives 404, although the channel in the path is not what"
              + " is missing",
      withExceptions = AssertionFailedError.class)
  void writingAChannelWithAnUndefinedTagIsRejectedWith400() {
    String ch1 = withTags(channel("ch1", "admin"), tag("undefined", "admin")).toString();

    // today: returns 404
    assertEquals(400, cf.put("/resources/channels/ch1", ch1, ApiClient.ADMIN).status());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("writes")
  void malformedBodyReturns400(String request) {
    assertEquals(400, send(request, "{not json").status());
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("singleWritesWithoutANameOrAnOwner")
  void writeWithoutANameOrAnOwnerReturns400(String request, String body) {
    assertEquals(400, send(request, body).status());
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("batchWritesOfAChannelWithoutAName")
  void batchWithAChannelWithoutANameReturns400AndWritesNothing(String request, String item) {
    String body = "[" + channel("ch1", "admin") + "," + item + "]";

    assertEquals(400, send(request, body).status());
    assertEquals(new ApiClient.Response(200, "[]"), cf.get("/resources/channels"));
  }
}
