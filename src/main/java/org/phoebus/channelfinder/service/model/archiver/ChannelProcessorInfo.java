package org.phoebus.channelfinder.service.model.archiver;

import java.util.Map;

public record ChannelProcessorInfo(String name, boolean enabled, Map<String, String> properties) {}
