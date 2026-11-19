package org.phoebus.channelfinder.configuration;

import java.util.List;
import java.util.Map;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.exceptions.ChannelPreProcessException;

/**
 * Defines the contract for a read-only pre-processor that inspects a list of {@link Channel}
 * objects <em>before</em> they are created or updated in the Channel Finder service.
 *
 * <p>Implementations examine incoming channels and compare them against the currently stored state
 * to check properties, log observations, or notify external systems. Preprocessors <strong>must not
 * mutate</strong> the channel objects they receive. Each preprocessor runs independently and does
 * not see warnings or results from other preprocessors.
 *
 * <p><strong>Return vs. Exception semantics:</strong>
 *
 * <ul>
 *   <li><strong>Warnings (returned):</strong> advisory observations (e.g. "missing recommended
 *       property X"), logged for visibility but do not block create/update.
 *   <li><strong>Hard failures (thrown):</strong> {@link ChannelPreProcessException} for validation
 *       failures or fatal errors (e.g. mandatory property missing, notify service unreachable) that
 *       should abort the operation.
 * </ul>
 */
public interface ChannelPreProcessor {

  /**
   * Returns whether this pre-processor is currently enabled.
   *
   * @return {@code true} if the pre-processor will execute when {@link #preProcess(List, Map)} is
   *     called, {@code false} if it is disabled and will be skipped.
   */
  boolean enabled();

  /**
   * Enables or disables this pre-processor at runtime.
   *
   * @param enabled {@code true} to enable the pre-processor, {@code false} to disable it.
   */
  void setEnabled(boolean enabled);

  /**
   * Returns a snapshot of this pre-processor's current configuration and status.
   *
   * @return a {@link ChannelPreProcessorInfo} describing the pre-processor name, enabled state, and
   *     any additional configuration key/value pairs.
   */
  ChannelPreProcessorInfo preProcessorInfo();

  /**
   * Inspects a list of channels before they are persisted, without modifying them.
   *
   * <p>Implementations may:
   *
   * <ul>
   *   <li>Check channel properties against local or external rules.
   *   <li>Compare incoming channels with existing state to detect changes.
   *   <li>Log or notify external systems of channel creations/updates.
   *   <li>Return advisory warnings for non-blocking issues.
   *   <li>Throw {@link ChannelPreProcessException} to reject invalid channels.
   * </ul>
   *
   * @param channels the incoming channels to inspect; read-only, must not be modified.
   * @param existingChannels the channels currently stored in the service, keyed by channel name;
   *     may be empty but must not be {@code null}.
   * @return a {@link PreProcessResult} containing warnings per channel and this preprocessor's
   *     name.
   * @throws ChannelPreProcessException if inspection fails and the create/update operation should
   *     be aborted.
   */
  PreProcessResult preProcess(List<Channel> channels, Map<String, Channel> existingChannels)
      throws ChannelPreProcessException;
}
