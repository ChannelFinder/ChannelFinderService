package org.phoebus.channelfinder;

import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.web.v0.api.ITag;
import org.phoebus.channelfinder.web.v0.mapper.TagMapper;

class TagControllerHelper {

  private final ITag manager;

  TagControllerHelper(ITag manager) {
    this.manager = manager;
  }

  Tag apiCreate(String name, Tag tag) {
    return TagMapper.toDomain(manager.create(name, TagMapper.toDto(tag)));
  }

  Iterable<Tag> apiCreate(Iterable<Tag> tags) {
    return TagMapper.toDomains(manager.create(TagMapper.toDtos(tags)));
  }

  Tag apiRead(String name, boolean withChannels) {
    return TagMapper.toDomain(manager.read(name, withChannels));
  }

  Iterable<Tag> apiList() {
    return TagMapper.toDomains(manager.list());
  }

  Tag apiUpdate(String name, Tag tag) {
    return TagMapper.toDomain(manager.update(name, TagMapper.toDto(tag)));
  }

  Iterable<Tag> apiUpdate(Iterable<Tag> tags) {
    return TagMapper.toDomains(manager.update(TagMapper.toDtos(tags)));
  }

  Tag apiAddSingle(String tagName, String channelName) {
    return TagMapper.toDomain(manager.addSingle(tagName, channelName));
  }
}
