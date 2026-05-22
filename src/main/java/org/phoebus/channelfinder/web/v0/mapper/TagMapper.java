package org.phoebus.channelfinder.web.v0.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.web.v0.dto.ChannelDto;
import org.phoebus.channelfinder.web.v0.dto.TagDto;

public final class TagMapper {

  private TagMapper() {}

  public static TagDto toDto(Tag tag) {
    List<ChannelDto> channels =
        tag.getChannels().isEmpty()
            ? new ArrayList<>()
            : tag.getChannels().stream().map(ChannelMapper::toDto).toList();
    return new TagDto(tag.getName(), tag.getOwner(), channels);
  }

  public static Tag toDomain(TagDto dto) {
    Tag tag = new Tag(dto.name(), dto.owner());
    if (!dto.channels().isEmpty()) {
      tag.setChannels(dto.channels().stream().map(ChannelMapper::toDomain).toList());
    }
    return tag;
  }

  public static List<TagDto> toDtos(Iterable<Tag> tags) {
    return StreamSupport.stream(tags.spliterator(), false).map(TagMapper::toDto).toList();
  }

  public static List<Tag> toDomains(Iterable<TagDto> dtos) {
    return StreamSupport.stream(dtos.spliterator(), false).map(TagMapper::toDomain).toList();
  }
}
