package org.phoebus.channelfinder.web.v0.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;
import org.phoebus.channelfinder.web.v0.dto.PropertyDto;

public final class PropertyMapper {

  private PropertyMapper() {}

  public static PropertyDto toDto(Property property) {
    List<ChannelDto> channels =
        property.getChannels().isEmpty()
            ? new ArrayList<>()
            : property.getChannels().stream().map(ChannelMapper::toDto).toList();
    return new PropertyDto(property.getName(), property.getOwner(), property.getValue(), channels);
  }

  public static Property toDomain(PropertyDto dto) {
    Property property = new Property(dto.name(), dto.owner(), dto.value());
    if (!dto.channels().isEmpty()) {
      property.setChannels(dto.channels().stream().map(ChannelMapper::toDomain).toList());
    }
    return property;
  }

  public static List<PropertyDto> toDtos(Iterable<Property> properties) {
    return StreamSupport.stream(properties.spliterator(), false)
        .map(PropertyMapper::toDto)
        .toList();
  }

  public static List<Property> toDomains(Iterable<PropertyDto> dtos) {
    return StreamSupport.stream(dtos.spliterator(), false).map(PropertyMapper::toDomain).toList();
  }
}
