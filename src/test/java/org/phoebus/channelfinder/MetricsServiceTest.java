package org.phoebus.channelfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.service.MetricsService;

class MetricsServiceTest {

  @Test
  void testGenerateAllMultiValueMaps_example1() {
    Map<String, List<String>> properties =
        Map.of(
            "a", List.of("b", "c"),
            "d", List.of("e", "!*"));
    List<Map<String, List<String>>> allMaps = MetricsService.generateAllMultiValueMaps(properties);

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
    List<Map<String, List<String>>> allMaps = MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(2 * 2, allMaps.size());

    boolean foundExpectedCombination = false;
    for (Map<String, List<String>> map : allMaps) {
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
    List<Map<String, List<String>>> allMaps = MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(0, allMaps.size());
  }

  @Test
  void testGenerateAllMultiValueMaps_emptyMap() {
    Map<String, List<String>> properties = Map.of();
    List<Map<String, List<String>>> allMaps = MetricsService.generateAllMultiValueMaps(properties);

    assertEquals(0, allMaps.size());
  }

  private Map<String, List<String>> createMap(
      String key1, String value1, String key2, String value2) {
    return Map.of(key1, List.of(value1), key2, List.of(value2));
  }
}
