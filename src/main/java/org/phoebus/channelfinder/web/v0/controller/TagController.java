package org.phoebus.channelfinder.web.v0.controller;

import org.phoebus.channelfinder.service.TagService;
import org.phoebus.channelfinder.web.v0.api.ITag;
import org.phoebus.channelfinder.web.v0.dto.TagDto;
import org.phoebus.channelfinder.web.v0.mapper.TagMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/tags")
public class TagController implements ITag {

  private final TagService tagService;

  public TagController(TagService tagService) {
    this.tagService = tagService;
  }

  @Override
  public Iterable<TagDto> list() {
    return TagMapper.toDtos(tagService.list());
  }

  @Override
  public TagDto read(String tagName, boolean withChannels) {
    return TagMapper.toDto(tagService.read(tagName, withChannels));
  }

  @Override
  public TagDto create(String tagName, TagDto tag) {
    return TagMapper.toDto(tagService.create(tagName, TagMapper.toDomain(tag)));
  }

  @Override
  public Iterable<TagDto> create(Iterable<TagDto> tags) {
    return TagMapper.toDtos(tagService.create(TagMapper.toDomains(tags)));
  }

  @Override
  public TagDto addSingle(String tagName, String channelName) {
    return TagMapper.toDto(tagService.addSingle(tagName, channelName));
  }

  @Override
  public TagDto update(String tagName, TagDto tag) {
    return TagMapper.toDto(tagService.update(tagName, TagMapper.toDomain(tag)));
  }

  @Override
  public Iterable<TagDto> update(Iterable<TagDto> tags) {
    return TagMapper.toDtos(tagService.update(TagMapper.toDomains(tags)));
  }

  @Override
  public void remove(String tagName) {
    tagService.remove(tagName);
  }

  @Override
  public void removeSingle(String tagName, String channelName) {
    tagService.removeSingle(tagName, channelName);
  }
}
