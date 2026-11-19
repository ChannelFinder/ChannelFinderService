package org.phoebus.channelfinder.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.processor.ChannelPreProcessor;
import org.phoebus.channelfinder.processor.PreProcessResult;
import org.springframework.stereotype.Service;

/**
 * Manages and orchestrates multiple {@link ChannelPreProcessor} implementations.
 *
 * <p>Runs enabled preprocessors in sequence, each receiving the same unmodified channel list and
 * existing state. Preprocessors are isolated: each receives a fresh view of the input and cannot
 * see warnings or results from other preprocessors. Exceptions from any preprocessor immediately
 * propagate and abort processing. Warnings are aggregated across all preprocessors for logging and
 * API visibility.
 */
@Service
public class ChannelPreProcessorService {

  private static final Logger logger = Logger.getLogger(ChannelPreProcessorService.class.getName());

  private final List<ChannelPreProcessor> preProcessors;

  public ChannelPreProcessorService(List<ChannelPreProcessor> preProcessors) {
    this.preProcessors = preProcessors != null ? preProcessors : new ArrayList<>();
  }

  /**
   * Runs all enabled pre-processors on the given channels in order, aggregating their warnings.
   *
   * <p>Each preprocessor receives the unmodified input channels and existing state. Preprocessors
   * run in isolation and cannot see results from previous preprocessors. If any preprocessor throws
   * an exception, it immediately propagates and stops all further processing.
   *
   * @param channels the channels to inspect; unchanged by this operation.
   * @param existingChannels a map of channels currently stored in the service, keyed by name.
   * @return an aggregated {@link PreProcessResult} combining warnings from all enabled
   *     preprocessors. If no preprocessors run, an empty result is returned.
   * @throws Exception if any preprocessor throws, aborting the operation.
   */
  public PreProcessResult preProcess(List<Channel> channels, Map<String, Channel> existingChannels)
      throws Exception {
    Map<String, List<String>> aggregatedWarnings = new HashMap<>();

    for (ChannelPreProcessor processor : preProcessors) {
      if (!processor.enabled()) {
        logger.log(
            Level.FINE,
            () ->
                String.format(
                    "Preprocessor '%s' is disabled; skipping.",
                    processor.preProcessorInfo().name()));
        continue;
      }

      logger.log(
          Level.FINE,
          () ->
              String.format(
                  "Running preprocessor '%s' on %d channels.",
                  processor.preProcessorInfo().name(), channels.size()));

      PreProcessResult result = processor.preProcess(channels, existingChannels);

      // Aggregate warnings from this preprocessor
      result
          .warnings()
          .forEach(
              (channelName, warnings) ->
                  aggregatedWarnings
                      .computeIfAbsent(channelName, k -> new ArrayList<>())
                      .addAll(warnings));

      if (result.hasWarnings()) {
        logger.log(
            Level.INFO,
            () ->
                String.format(
                    "Preprocessor '%s' returned %d warnings across %d channels.",
                    result.preprocessorName(),
                    result.totalWarnings(),
                    result.channelsWithWarnings()));
      }
    }

    return new PreProcessResult(aggregatedWarnings, "aggregated");
  }
}
