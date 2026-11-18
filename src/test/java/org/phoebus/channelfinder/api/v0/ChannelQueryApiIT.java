package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests the channel query language on {@code GET /channels}. Results are compared as ordered name
 * lists, so every test also checks the sort.
 */
class ChannelQueryApiIT extends ApiTestBase {

  // Two channels share the prefix MNO. DEF_UPPER and DEF_LOWER differ only in case. GHI has no
  // properties and no tags.
  private static final String DEF_UPPER = "DEF:001";
  private static final String GHI = "GHI:JKL";
  private static final String MNO_1 = "MNO:001";
  private static final String MNO_2 = "MNO:002";
  private static final String DEF_LOWER = "def:003";

  @BeforeEach
  void createChannels() {
    createProperties("admin", "area", "number", "pvStatus");
    createTags("admin", "archived", "beam");
    putChannels(
        withTags(
            withProperties(
                channel(MNO_1, "admin"),
                property("area", "admin", "north"),
                property("number", "admin", "001"),
                property("pvStatus", "admin", "Active")),
            tag("archived", "admin")),
        withTags(
            withProperties(
                channel(MNO_2, "admin"),
                property("area", "admin", "north"),
                property("number", "admin", "002"),
                property("pvStatus", "admin", "Inactive")),
            tag("archived", "admin"),
            tag("beam", "admin")),
        withProperties(
            channel(DEF_UPPER, "admin"),
            property("area", "admin", "South"),
            property("number", "admin", "001"),
            property("pvStatus", "admin", "Active")),
        withTags(
            withProperties(channel(DEF_LOWER, "admin"), property("area", "admin", "south")),
            tag("beam", "admin")),
        channel(GHI, "admin"));
  }

  private List<String> query(String query) {
    ApiClient.Response response = cf.get("/resources/channels?" + query);
    assertEquals(200, response.status(), response.body());
    return names(response.json());
  }

  @Test
  void noFilterReturnsEverythingSortedByNameInByteOrder() {
    assertEquals(
        List.of(DEF_UPPER, GHI, MNO_1, MNO_2, DEF_LOWER),
        names(cf.get("/resources/channels").json()));
  }

  @Test
  void nameIsAGlob() {
    assertEquals(List.of(MNO_1, MNO_2), query("~name=MNO:*"));
    assertEquals(List.of(MNO_1, MNO_2), query("~name=MNO:00?"));
    assertEquals(List.of(GHI), query("~name=GHI:JKL"));
  }

  @Test
  void nameMatchingIsCaseInsensitive() {
    assertEquals(List.of(MNO_1), query("~name=mno:001*"));
    assertEquals(List.of(DEF_UPPER, DEF_LOWER), query("~name=DEF:*"));
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value =
          "a single channel is found only by its exact name, but api.rst says names ignore case",
      withExceptions = AssertionFailedError.class)
  void singleChannelLookupIgnoresCase() {
    ApiClient.Response response = cf.get("/resources/channels/mno:001");

    // today: returns 404
    assertEquals(200, response.status());
    assertEquals(MNO_1, response.json().get("name").asString());
  }

  @Test
  void pipeCommaAndSemicolonAlternateNames() {
    assertEquals(List.of(DEF_UPPER, GHI), query("~name=GHI:JKL|DEF:001"));
    assertEquals(List.of(DEF_UPPER, GHI), query("~name=GHI:JKL,DEF:001"));
    assertEquals(List.of(DEF_UPPER, GHI), query("~name=GHI:JKL;DEF:001"));
  }

  @Test
  void repeatedNameParametersAreAnded() {
    assertEquals(List.of(MNO_2), query("~name=MNO:*&~name=*002*"));
  }

  @Test
  void negatedNameIsNotNegated() {
    // `~name!=` is easy to read as a negation, as `~tag!=` and `prop!=` are. The code strips the
    // `!` but does not apply it to names, so only GHI:JKL is returned.
    assertEquals(List.of(GHI), query("~name!=GHI:JKL"));
  }

  @Test
  void tagMatchesByNameCaseInsensitively() {
    assertEquals(List.of(MNO_1, MNO_2), query("~tag=archived"));
    assertEquals(List.of(MNO_1, MNO_2), query("~tag=ARCH*"));
  }

  @Test
  void tagAlternationIsOrAndRepetitionIsAnd() {
    assertEquals(List.of(MNO_1, MNO_2, DEF_LOWER), query("~tag=archived|beam"));
    assertEquals(List.of(MNO_2), query("~tag=archived&~tag=beam"));
  }

  @Test
  void tagDoesNotMatchPropertyNames() {
    // api.rst disagrees with itself. Its description of `~tag` says it matches tag or property
    // names. Its table of search parameters says tag names. The code matches tag names only, and
    // this test records the code.
    assertEquals(List.of(), query("~tag=area"));
  }

  @Test
  void negatedTagExcludesChannelsWithTheTag() {
    assertEquals(List.of(DEF_UPPER, GHI, DEF_LOWER), query("~tag!=archived"));
  }

  @Test
  void propertyValueIsACaseInsensitiveGlob() {
    assertEquals(List.of(MNO_1, MNO_2), query("area=north"));
    assertEquals(List.of(DEF_UPPER, DEF_LOWER), query("area=south"));
    assertEquals(List.of(DEF_UPPER, MNO_1), query("number=*1"));
  }

  @Test
  void propertyNameMatchingIsCaseInsensitive() {
    assertEquals(List.of(MNO_1, MNO_2), query("AREA=north"));
  }

  @Test
  void propertyAlternationIsOr() {
    assertEquals(List.of(DEF_UPPER, MNO_1, MNO_2), query("number=001|002"));
    assertEquals(List.of(DEF_UPPER, MNO_1, MNO_2), query("number=001,002"));
    assertEquals(List.of(DEF_UPPER, MNO_1, MNO_2), query("number=001;002"));
  }

  @Test
  void repeatedPropertyParametersAreOred() {
    // api.rst documents that expressions for the same property are ORed and that the rest are
    // ANDed. It is still worth a test, because repeated `~name` and `~tag` parameters are ANDed,
    // and the two are easy to confuse.
    assertEquals(List.of(DEF_UPPER, MNO_1, MNO_2, DEF_LOWER), query("area=north&area=South"));
  }

  @Test
  void differentPropertiesAreAnded() {
    assertEquals(List.of(MNO_1), query("area=north&pvStatus=Active"));
  }

  @Test
  void negatedPropertyValueMatchesOnlyChannelsThatHaveTheProperty() {
    // Channels without the property do not match, which is easy to miss. `area!=*` is the query
    // for those.
    assertEquals(List.of(DEF_UPPER, DEF_LOWER), query("area!=north"));
  }

  @Test
  void negatedWildcardMatchesChannelsWithoutTheProperty() {
    assertEquals(List.of(GHI, DEF_LOWER), query("number!=*"));
  }

  @Test
  void wildcardValueIsAnExistenceQuery() {
    assertEquals(List.of(DEF_UPPER, MNO_1, MNO_2), query("number=*"));
  }

  @Test
  void bareKeyMatchesNothing() {
    // A bare `key` reads like a test for the property. The service uses the empty value as a
    // pattern, and no stored value is empty, so nothing matches.
    assertEquals(List.of(), query("number"));
  }

  @Test
  void propertiesTagsAndNamesCombineWithAnd() {
    assertEquals(List.of(MNO_2), query("~name=MNO:*&~tag=beam&area=north"));
  }

  @Test
  void fromIsAnOffsetNotAPageNumber() {
    // api.rst says `~from` starts the search "from given page". The code uses it as an offset
    // into the sorted result, and this test records the code. `~from=1` skips one channel. A page
    // number of 1 would skip three channels, or none for pages counted from 1.
    assertEquals(List.of(DEF_UPPER, GHI, MNO_1), query("~size=3"));
    assertEquals(List.of(GHI, MNO_1, MNO_2), query("~size=3&~from=1"));
  }

  @Test
  void sizeZeroReturnsNoChannels() {
    assertEquals(List.of(), query("~size=0"));
  }

  @Test
  void repeatedSizeTakesTheLargest() {
    // Most HTTP APIs take the first or the last of repeated values. The code takes the largest on
    // purpose.
    assertEquals(List.of(DEF_UPPER, GHI, MNO_1), query("~size=1&~size=3"));
  }

  @Test
  void searchAfterContinuesAfterTheNamedChannel() {
    assertEquals(List.of(MNO_1, MNO_2, DEF_LOWER), query("~search_after=GHI:JKL"));
    assertEquals(List.of(MNO_1), query("~search_after=GHI:JKL&~size=1"));
  }

  @Test
  void sizePlusFromAboveTheMaxResultWindowIsRejectedWith400() {
    assertEquals(200, cf.get("/resources/channels?~size=10000").status());
    assertEquals(400, cf.get("/resources/channels?~size=10001").status());
    assertEquals(400, cf.get("/resources/channels?~size=2&~from=9999").status());
  }

  @Test
  void defaultSizeIsTenThousand() {
    putChannels(
        IntStream.range(0, 10_000)
            .mapToObj(i -> channel(String.format("ZZ:%05d", i), "admin"))
            .toArray(ObjectNode[]::new));

    assertEquals(10_000, cf.get("/resources/channels").json().size());
    assertEquals("10005", cf.get("/resources/channels/count").body());
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"~size=ten", "~from=ten", "~size=-1", "~from=-1"})
  void nonNumericOrNegativeSizeAndFromAreRejectedWith400(String query) {
    assertEquals(400, cf.get("/resources/channels?" + query).status());
  }

  @Test
  void countAppliesFiltersAndIgnoresPaging() {
    assertEquals("2", cf.get("/resources/channels/count?~name=MNO:*&~size=1").body());
    assertEquals("5", cf.get("/resources/channels/count").body());
    assertEquals("0", cf.get("/resources/channels/count?~name=nothing").body());
  }

  @Test
  void combinedCountIsThePageSizeUnlessTotalHitsAreTracked() {
    // `count` reads like the total number of matches. Without `~track_total_hits` it is the number
    // of channels on the page. api.rst says the parameter makes the count the total without
    // pagination, which implies this. It is still worth a test, because the default is easy to
    // misread.
    ApiClient.Response untracked = cf.get("/resources/channels/combined?~size=2");
    ApiClient.Response tracked =
        cf.get("/resources/channels/combined?~size=2&~track_total_hits=true");

    assertEquals(200, untracked.status());
    assertEquals(List.of(DEF_UPPER, GHI), names(untracked.json().get("channels")));
    assertEquals(2, untracked.json().get("count").asLong());
    assertEquals(200, tracked.status());
    assertEquals(List.of(DEF_UPPER, GHI), names(tracked.json().get("channels")));
    assertEquals(5, tracked.json().get("count").asLong());
  }
}
