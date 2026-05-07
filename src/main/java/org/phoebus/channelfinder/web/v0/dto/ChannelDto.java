package org.phoebus.channelfinder.web.v0.dto;

import java.util.ArrayList;
import java.util.List;

public record ChannelDto(
    String name, String owner, List<PropertyDto> properties, List<TagDto> tags) {

  public ChannelDto {
    properties = properties != null ? properties : new ArrayList<>();
    tags = tags != null ? tags : new ArrayList<>();
  }

  public ChannelDto(String name, String owner) {
    this(name, owner, new ArrayList<>(), new ArrayList<>());
  }
}
