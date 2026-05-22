package org.phoebus.channelfinder.web.v0.dto;

import java.util.List;

public record SearchResultDto(List<ChannelDto> channels, long count) {}
