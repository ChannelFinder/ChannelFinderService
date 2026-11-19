package org.phoebus.channelfinder.configuration;

import java.util.Map;

/**
 * An immutable snapshot of a {@link org.phoebus.channelfinder.configuration.ChannelProcessor}'s
 * identity and configuration.
 *
 * @param name the human-readable name that identifies this processor.
 * @param enabled {@code true} if the processor is currently active.
 * @param properties additional, implementation-specific configuration key/value pairs (e.g.
 *     archiver URLs, property names, auto-pause options).
 */
public record ChannelProcessorInfo(String name, boolean enabled, Map<String, String> properties) {}
