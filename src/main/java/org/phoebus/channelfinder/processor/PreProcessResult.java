package org.phoebus.channelfinder.processor;

import java.util.List;
import java.util.Map;

/**
 * The result of a pre-processing operation on a batch of channels.
 *
 * <p>Contains non-blocking warnings collected per channel. Hard failures are communicated via
 * exceptions thrown from {@link ChannelPreProcessor#preProcess(List, Map)}. Warnings are logged for
 * visibility but do not prevent the channel create/update from proceeding.
 *
 * @param warnings a map from channel name to a list of warning messages (e.g. "missing recommended
 *     property X", "channel last modified 6 months ago"). Empty map if no warnings were
 *     encountered.
 * @param preprocessorName the human-readable name of the preprocessor that produced this result.
 */
public record PreProcessResult(Map<String, List<String>> warnings, String preprocessorName) {

  /**
   * Creates an empty result (no warnings).
   *
   * @param preprocessorName the name of the preprocessor.
   * @return a result with an empty warnings map.
   */
  public static PreProcessResult empty(String preprocessorName) {
    return new PreProcessResult(Map.of(), preprocessorName);
  }

  /**
   * Checks if this result contains any warnings.
   *
   * @return {@code true} if the warnings map is not empty, {@code false} otherwise.
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Returns the number of channels with warnings.
   *
   * @return the number of distinct channel names in the warnings map.
   */
  public int channelsWithWarnings() {
    return warnings.size();
  }

  /**
   * Returns the total count of warning messages across all channels.
   *
   * @return the sum of all warning list sizes.
   */
  public int totalWarnings() {
    return warnings.values().stream().mapToInt(List::size).sum();
  }
}
