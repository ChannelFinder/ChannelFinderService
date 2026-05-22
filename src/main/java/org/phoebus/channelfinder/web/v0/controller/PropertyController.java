package org.phoebus.channelfinder.web.v0.controller;

import org.phoebus.channelfinder.service.PropertyService;
import org.phoebus.channelfinder.web.v0.api.IProperty;
import org.phoebus.channelfinder.web.v0.dto.PropertyDto;
import org.phoebus.channelfinder.web.v0.mapper.PropertyMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/properties")
public class PropertyController implements IProperty {

  private final PropertyService propertyService;

  public PropertyController(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  @Override
  public Iterable<PropertyDto> list() {
    return PropertyMapper.toDtos(propertyService.list());
  }

  @Override
  public PropertyDto read(String propertyName, boolean withChannels) {
    return PropertyMapper.toDto(propertyService.read(propertyName, withChannels));
  }

  @Override
  public PropertyDto create(String propertyName, PropertyDto property) {
    return PropertyMapper.toDto(
        propertyService.create(propertyName, PropertyMapper.toDomain(property)));
  }

  @Override
  public Iterable<PropertyDto> create(Iterable<PropertyDto> properties) {
    return PropertyMapper.toDtos(propertyService.create(PropertyMapper.toDomains(properties)));
  }

  @Override
  public PropertyDto addSingle(String propertyName, String channelName, PropertyDto property) {
    return PropertyMapper.toDto(
        propertyService.addSingle(propertyName, channelName, PropertyMapper.toDomain(property)));
  }

  @Override
  public PropertyDto update(String propertyName, PropertyDto property) {
    return PropertyMapper.toDto(
        propertyService.update(propertyName, PropertyMapper.toDomain(property)));
  }

  @Override
  public Iterable<PropertyDto> update(Iterable<PropertyDto> properties) {
    return PropertyMapper.toDtos(propertyService.update(PropertyMapper.toDomains(properties)));
  }

  @Override
  public void remove(String propertyName) {
    propertyService.remove(propertyName);
  }

  @Override
  public void removeSingle(String propertyName, String channelName) {
    propertyService.removeSingle(propertyName, channelName);
  }
}
