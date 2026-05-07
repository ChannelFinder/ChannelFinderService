package org.phoebus.channelfinder.web.v0.dto;

import java.util.ArrayList;
import java.util.List;

public record TagDto(String name, String owner, List<ChannelDto> channels) {

  public TagDto {
    channels = channels != null ? channels : new ArrayList<>();
  }

  public TagDto(String name, String owner) {
    this(name, owner, new ArrayList<>());
  }
}
