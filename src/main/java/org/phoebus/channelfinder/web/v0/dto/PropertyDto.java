package org.phoebus.channelfinder.web.v0.dto;

import java.util.ArrayList;
import java.util.List;

public record PropertyDto(String name, String owner, String value, List<ChannelDto> channels) {

  public PropertyDto {
    channels = channels != null ? channels : new ArrayList<>();
  }

  public PropertyDto(String name, String owner, String value) {
    this(name, owner, value, new ArrayList<>());
  }
}
