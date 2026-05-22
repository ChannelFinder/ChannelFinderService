package org.phoebus.channelfinder;

import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.web.v0.api.IProperty;
import org.phoebus.channelfinder.web.v0.mapper.PropertyMapper;

class PropertyControllerHelper {

  private final IProperty manager;

  PropertyControllerHelper(IProperty manager) {
    this.manager = manager;
  }

  Property apiCreate(String name, Property property) {
    return PropertyMapper.toDomain(manager.create(name, PropertyMapper.toDto(property)));
  }

  Iterable<Property> apiCreate(Iterable<Property> properties) {
    return PropertyMapper.toDomains(manager.create(PropertyMapper.toDtos(properties)));
  }

  Property apiRead(String name, boolean withChannels) {
    return PropertyMapper.toDomain(manager.read(name, withChannels));
  }

  Iterable<Property> apiList() {
    return PropertyMapper.toDomains(manager.list());
  }

  Property apiUpdate(String name, Property property) {
    return PropertyMapper.toDomain(manager.update(name, PropertyMapper.toDto(property)));
  }

  void apiUpdate(Iterable<Property> properties) {
    manager.update(PropertyMapper.toDtos(properties));
  }

  Property apiAddSingle(String propertyName, String channelName, Property property) {
    return PropertyMapper.toDomain(
        manager.addSingle(propertyName, channelName, PropertyMapper.toDto(property)));
  }
}
