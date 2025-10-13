package org.phoebus.channelfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class MetricsServiceTest {

  @Test
  void testGenerateAllMultiValueMaps_example1() {
    Map<String, List<String>> properties =
        Map.of(
            "a", List.of("b", "c"),
            "d", List.of("e", "!*"));
    List<MultiValueMap<String, String>> allMaps =
        MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(4, allMaps.size());

    assertTrue(allMaps.contains(createMap("a", "b", "d", "e")));
    assertTrue(allMaps.contains(createMap("a", "c", "d", "e")));
    assertTrue(allMaps.contains(createMap("a", "b", "d!", "*")));
    assertTrue(allMaps.contains(createMap("a", "c", "d!", "*")));
  }

  @Test
  void testGenerateAllMultiValueMaps_example2() {
    Map<String, List<String>> properties =
        Map.of(
            "x", List.of("y", "z"),
            "p", List.of("q"),
            "r", List.of("s", "t"));
    List<MultiValueMap<String, String>> allMaps =
        MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(2 * 2, allMaps.size()); // 2 options (value or null) for each of 2 keys

    // Just a basic check, exhaustive check would be large
    boolean foundExpectedCombination = false;
    for (MultiValueMap<String, String> map : allMaps) {
      if (map.get("x").contains("y") && map.get("p").contains("q") && map.get("r").contains("s")) {
        foundExpectedCombination = true;
        break;
      }
    }
    assertTrue(foundExpectedCombination);
  }

  @Test
  void testGenerateAllMultiValueMaps_emptyList() {
    Map<String, List<String>> properties = Map.of("m", List.of());
    List<MultiValueMap<String, String>> allMaps =
        MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(0, allMaps.size()); // One with m=null, one with m implicitly null (not present)
  }

  @Test
  void testGenerateAllMultiValueMaps_emptyMap() {
    Map<String, List<String>> properties = Map.of();
    List<MultiValueMap<String, String>> allMaps =
        MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(1, allMaps.size());
  }

  // Helper method to create a MultiValueMap for easier assertion
  private MultiValueMap<String, String> createMap(
      String key1, String value1, String key2, String value2) {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    if (key1 != null) {
      map.add(key1, value1);
    }
    if (key2 != null) {
      map.add(key2, value2);
    }
    return map;
  }

  private MultiValueMap<String, String> createMap(String key, String value) {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    if (key != null) {
      map.add(key, value);
    }
    return map;
  }
}
