package org.phoebus.channelfinder.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Unit tests for SearchParameterMergerUtil. */
class SearchParameterMergerUtilTest {

  @Test
  void testMergeParameters_urlOnly() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~name", "name1");
    urlParams.add("prop", "value1");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, null);

    assertEquals(2, merged.size());
    assertEquals("name1", merged.getFirst("~name"));
    assertEquals("value1", merged.getFirst("prop"));
  }

  @Test
  void testMergeParameters_bodyOnly() {
    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~name", "name2");
    bodyParams.put("prop", "value2");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(new LinkedMultiValueMap<>(), bodyParams);

    assertEquals(2, merged.size());
    assertEquals("name2", merged.getFirst("~name"));
    assertEquals("value2", merged.getFirst("prop"));
  }

  @Test
  void testMergeParameters_bothUrlAndBody_mergeRegularParams() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~name", "name1");
    urlParams.add("prop", "value1");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~name", "name2");
    bodyParams.put("prop", "value2");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // Regular params should be added as separate values in MultiValueMap
    assertEquals(2, merged.get("~name").size());
    assertTrue(merged.get("~name").contains("name1"));
    assertTrue(merged.get("~name").contains("name2"));
    assertEquals(2, merged.get("prop").size());
    assertTrue(merged.get("prop").contains("value1"));
    assertTrue(merged.get("prop").contains("value2"));
  }

  @Test
  void testMergeParameters_controlParams_urlTakesPrecedence() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~size", "100");
    urlParams.add("~from", "0");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~size", "50");
    bodyParams.put("~from", "10");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // Control params: URL should take precedence
    assertEquals("100", merged.getFirst("~size"));
    assertEquals("0", merged.getFirst("~from"));
  }

  @Test
  void testMergeParameters_controlParams_bodyUsedWhenUrlMissing() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~name", "name1");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~size", "50");
    bodyParams.put("~from", "10");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // Control params from body should be used if not in URL
    assertEquals("50", merged.getFirst("~size"));
    assertEquals("10", merged.getFirst("~from"));
  }

  @Test
  void testMergeParameters_searchAfterControl() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~search_after", "abc123");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~search_after", "def456");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // URL should take precedence
    assertEquals("abc123", merged.getFirst("~search_after"));
  }

  @Test
  void testMergeParameters_trackTotalHitsControl() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~track_total_hits", "true");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~track_total_hits", "false");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // URL should take precedence
    assertEquals("true", merged.getFirst("~track_total_hits"));
  }

  @Test
  void testMergeParameters_emptyBodyValues() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~name", "name1");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~name", "name2");
    bodyParams.put("prop", "");
    bodyParams.put("tag", "   ");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // Empty/whitespace values should be skipped
    assertEquals(2, merged.get("~name").size());
    assertTrue(merged.get("~name").contains("name1"));
    assertTrue(merged.get("~name").contains("name2"));
    assertFalse(merged.containsKey("prop"));
    assertFalse(merged.containsKey("tag"));
  }

  @Test
  void testMergeParameters_nullInputs() {
    MultiValueMap<String, String> merged = SearchParameterMergerUtil.mergeParameters(null, null);

    assertTrue(merged.isEmpty());
  }

  @Test
  void testMergeParameters_complexMix() {
    MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
    urlParams.add("~name", "ch1");
    urlParams.add("~size", "100");
    urlParams.add("prop1", "val1");

    Map<String, String> bodyParams = new HashMap<>();
    bodyParams.put("~name", "ch2");
    bodyParams.put("~size", "50");
    bodyParams.put("prop1", "val2");
    bodyParams.put("prop2", "val3");

    MultiValueMap<String, String> merged =
        SearchParameterMergerUtil.mergeParameters(urlParams, bodyParams);

    // Regular params merged as multiple values, control params URL takes precedence
    assertEquals(2, merged.get("~name").size());
    assertTrue(merged.get("~name").contains("ch1"));
    assertTrue(merged.get("~name").contains("ch2"));
    assertEquals("100", merged.getFirst("~size"));
    assertEquals(2, merged.get("prop1").size());
    assertTrue(merged.get("prop1").contains("val1"));
    assertTrue(merged.get("prop1").contains("val2"));
    assertEquals("val3", merged.getFirst("prop2"));
  }
}
