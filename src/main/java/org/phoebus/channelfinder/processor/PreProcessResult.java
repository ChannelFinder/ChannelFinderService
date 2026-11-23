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

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}
