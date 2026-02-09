package org.phoebus.channelfinder.entity;

import java.util.List;

public record SearchResult(List<Channel> channels, long count) {}
