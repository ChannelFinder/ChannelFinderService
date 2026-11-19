package org.phoebus.channelfinder.configuration;

import java.util.List;
import org.phoebus.channelfinder.entity.Channel;
import tools.jackson.core.JacksonException;

/**
 * Defines the contract for a post-processor that acts on a list of {@link Channel} objects.
 *
 * <p>Implementations are responsible for performing domain-specific side-effects on channels (e.g.
 * submitting PVs to an archiver appliance) after they have been stored in the Channel Finder
 * service. A processor can be individually enabled or disabled at runtime.
 */
public interface ChannelProcessor {

  /**
   * Returns whether this processor is currently enabled.
   *
   * @return {@code true} if the processor will execute when {@link #process(List)} is called,
   *     {@code false} if it is disabled and will be skipped.
   */
  boolean enabled();

  /**
   * Enables or disables this processor at runtime.
   *
   * @param enabled {@code true} to enable the processor, {@code false} to disable it.
   */
  void setEnabled(boolean enabled);

  /**
   * Returns a snapshot of this processor's current configuration and status.
   *
   * @return a {@link ChannelProcessorInfo} describing the processor name, enabled state, and any
   *     additional configuration key/value pairs.
   */
  ChannelProcessorInfo processorInfo();

  /**
   * Processes the given list of channels, performing whatever side-effects the implementation
   * requires (e.g. archiving, name-server registration).
   *
   * @param channels the channels to process; must not be {@code null}.
   * @return the number of channels that were successfully processed.
   * @throws JacksonException if deserialising a response from an external service fails.
   */
  long process(List<Channel> channels) throws JacksonException;
}
