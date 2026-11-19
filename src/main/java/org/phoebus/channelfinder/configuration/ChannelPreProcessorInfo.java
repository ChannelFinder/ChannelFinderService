package org.phoebus.channelfinder.configuration;

import java.util.Map;

/**
 * An immutable snapshot of a {@link ChannelPreProcessor}'s identity and configuration.
 *
 * @param name the human-readable name that identifies this pre-processor.
 * @param enabled {@code true} if the pre-processor is currently active.
 * @param properties additional, implementation-specific configuration key/value pairs (e.g.
 *     property names that are inspected or injected).
 */
public record ChannelPreProcessorInfo(
    String name, boolean enabled, Map<String, String> properties) {}
