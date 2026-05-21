package org.phoebus.channelfinder.web.v0.controller;

import java.util.List;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.PropertyService;
import org.phoebus.channelfinder.web.v0.api.IProperty;
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
  public Iterable<Property> list() {
    return propertyService.list();
  }

  @Override
  public Property read(String propertyName, boolean withChannels) {
    return propertyService.read(propertyName, withChannels);
  }

  @Override
  public Property create(String propertyName, Property property) {
    return propertyService.create(propertyName, property);
  }

  @Override
  public Iterable<Property> create(Iterable<Property> properties) {
    return propertyService.create(properties);
  }

  @Override
  public Property addSingle(String propertyName, String channelName, Property property) {
    return propertyService.addSingle(propertyName, channelName, property);
  }

  @Override
  public Property update(String propertyName, Property property) {
    return propertyService.update(propertyName, property);
  }

  @Override
  public Iterable<Property> update(Iterable<Property> properties) {
    return propertyService.update(properties);
  }

  @Override
  public void removeBatch(String propertyName) {
    propertyService.removeBatch(propertyName);
  }

  @Override
  public void removeSingle(String propertyName, String channelName) {
    propertyService.removeSingle(propertyName, channelName);
  }

  @Override
  public long removeBatch(String propertyName, List<String> channelNames) {
    return propertyService.removeBatch(propertyName, channelNames);
  }
}
