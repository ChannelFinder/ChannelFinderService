package org.phoebus.channelfinder.api.v0;

import java.util.Arrays;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Builders for request bodies, and readers for the response bodies. */
final class ApiJson {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private ApiJson() {}

  static ObjectNode property(String name, String owner) {
    return MAPPER.createObjectNode().put("name", name).put("owner", owner);
  }

  static ObjectNode property(String name, String owner, String value) {
    return property(name, owner).put("value", value);
  }

  static ObjectNode tag(String name, String owner) {
    return MAPPER.createObjectNode().put("name", name).put("owner", owner);
  }

  static ObjectNode channel(String name, String owner) {
    return MAPPER.createObjectNode().put("name", name).put("owner", owner);
  }

  static ObjectNode withProperties(ObjectNode node, ObjectNode... properties) {
    node.set("properties", array(properties));
    return node;
  }

  static ObjectNode withTags(ObjectNode node, ObjectNode... tags) {
    node.set("tags", array(tags));
    return node;
  }

  static ObjectNode withChannels(ObjectNode node, ObjectNode... channels) {
    node.set("channels", array(channels));
    return node;
  }

  static ArrayNode array(JsonNode... nodes) {
    ArrayNode array = MAPPER.createArrayNode();
    Arrays.stream(nodes).forEach(array::add);
    return array;
  }

  static List<String> names(JsonNode array) {
    return array.valueStream().map(node -> node.get("name").asString()).toList();
  }

  /** Return the value of the named property on a channel node, or null when it is absent. */
  static String propertyValue(JsonNode channel, String propertyName) {
    return channel
        .get("properties")
        .valueStream()
        .filter(p -> p.get("name").asString().equals(propertyName))
        .map(p -> p.get("value").asString())
        .findFirst()
        .orElse(null);
  }

  static List<String> propertyNames(JsonNode channel) {
    return names(channel.get("properties"));
  }

  static List<String> tagNames(JsonNode channel) {
    return names(channel.get("tags"));
  }
}
