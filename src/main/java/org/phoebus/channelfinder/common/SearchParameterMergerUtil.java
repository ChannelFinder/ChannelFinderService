package org.phoebus.channelfinder.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility class for merging search parameters from URL query string and JSON request body.
 *
 * <p>Merging strategy:
 *
 * <ul>
 *   <li>For regular search parameters (e.g., ~name, property names, ~tag): values from both URL and
 *       body are added as separate entries in the MultiValueMap.
 *   <li>For control parameters (~size, ~from, ~search_after, ~track_total_hits): URL values take
 *       precedence over body values (body values ignored if URL value exists).
 * </ul>
 */
public final class SearchParameterMergerUtil {

  private static final String CONTROL_SIZE = "~size";
  private static final String CONTROL_FROM = "~from";
  private static final String CONTROL_SEARCH_AFTER = "~search_after";
  private static final String CONTROL_TRACK_TOTAL_HITS = "~track_total_hits";

  private SearchParameterMergerUtil() {
    // Utility class - private constructor
  }

  private static boolean isControlParameter(String key) {
    return CONTROL_SIZE.equals(key)
        || CONTROL_FROM.equals(key)
        || CONTROL_SEARCH_AFTER.equals(key)
        || CONTROL_TRACK_TOTAL_HITS.equals(key);
  }

  /**
   * Merges URL request parameters with body parameters.
   *
   * <p>URL parameters take precedence for control keys. For regular search parameters, body values
   * are added as additional entries to the MultiValueMap, allowing the search layer to handle
   * multiple values for the same parameter.
   *
   * @param urlParams URL query parameters (may be null or empty)
   * @param bodyParams JSON request body as a map (may be null or empty)
   * @return merged parameters as a MultiValueMap
   */
  public static MultiValueMap<String, String> mergeParameters(
      MultiValueMap<String, String> urlParams, Map<String, String> bodyParams) {
    MultiValueMap<String, String> merged = new LinkedMultiValueMap<>();

    // Add URL parameters first
    if (urlParams != null && !urlParams.isEmpty()) {
      for (Map.Entry<String, List<String>> entry : urlParams.entrySet()) {
        merged.put(entry.getKey(), new LinkedList<>(entry.getValue()));
      }
    }

    // Merge body parameters if present
    if (bodyParams != null && !bodyParams.isEmpty()) {
      mergeBodyParams(merged, bodyParams);
    }

    return merged;
  }

  private static void mergeBodyParams(
      MultiValueMap<String, String> merged, Map<String, String> bodyParams) {
    for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
      String key = entry.getKey();
      String bodyValue = entry.getValue();

      if (bodyValue == null || bodyValue.trim().isEmpty()) {
        continue; // Skip empty body values
      }

      if (isControlParameter(key)) {
        // For control parameters, URL takes precedence
        if (!merged.containsKey(key)) {
          merged.set(key, bodyValue);
        }
      } else {
        // For regular search parameters, add the body value to the list
        merged.add(key, bodyValue);
      }
    }
  }
}
