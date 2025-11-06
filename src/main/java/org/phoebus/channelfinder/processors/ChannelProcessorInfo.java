package org.phoebus.channelfinder.processors;

import java.util.Map;

public record ChannelProcessorInfo(String name, boolean enabled, Map<String, String> properties) {}
