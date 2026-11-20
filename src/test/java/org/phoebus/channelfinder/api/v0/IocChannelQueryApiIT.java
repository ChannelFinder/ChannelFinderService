package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.names;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.tagNames;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;
import static org.phoebus.channelfinder.api.v0.ApiJson.withTags;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests read queries on channels that carry the properties recCeiver writes: {@code hostName},
 * {@code iocName}, {@code iocIP}, {@code pvaPort} and {@code pvStatus}.
 */
class IocChannelQueryApiIT extends ApiTestBase {

  @BeforeEach
  void createIocChannels() {
    createProperties("admin", "hostName", "iocName", "iocIP", "pvaPort", "pvStatus");
    createTags("admin", "archived");
    putChannels(
        withTags(ioc("ABC:DEF:001", "host1", "IOC1", "Active"), tag("archived", "admin")),
        ioc("ABC:DEF:002", "host1", "IOC1", "Inactive"),
        ioc("ABC:GHI:001", "host2", "IOC2", "Active"));
  }

  private static ObjectNode ioc(String name, String host, String iocName, String pvStatus) {
    return withProperties(
        channel(name, "admin"),
        property("hostName", "admin", host),
        property("iocName", "admin", iocName),
        property("iocIP", "admin", "10.0.0." + host.substring(4)),
        property("pvaPort", "admin", "5075"),
        property("pvStatus", "admin", pvStatus));
  }

  @Test
  void tagListReturnsEveryTag() {
    ApiClient.Response response = cf.get("/resources/tags");

    assertEquals(200, response.status());
    assertEquals(List.of("archived"), names(response.json()));
  }

  @Test
  void nameTagAndPropertyFiltersCombine() {
    ApiClient.Response response =
        cf.get("/resources/channels?~name=ABC:DEF*&~tag=archived&hostName=host1");

    assertEquals(200, response.status());
    JsonNode channel = response.json().get(0);
    assertEquals(1, response.json().size());
    assertEquals("ABC:DEF:001", channel.get("name").asString());
    assertEquals("admin", channel.get("owner").asString());
    assertEquals("IOC1", propertyValue(channel, "iocName"));
    assertEquals(List.of("archived"), tagNames(channel));
  }

  @Test
  void channelTreeQueryReturnsEachChannelWithItsHostAndIoc() {
    // The service groups nothing. It returns the matching channels sorted by name. Each channel
    // carries hostName and iocName, which the Phoebus Channel Tree groups by.
    JsonNode channels = cf.get("/resources/channels?~name=*&hostName=*&iocName=*").json();

    assertEquals(List.of("ABC:DEF:001", "ABC:DEF:002", "ABC:GHI:001"), names(channels));
    assertEquals(
        List.of("host1/IOC1", "host1/IOC1", "host2/IOC2"),
        channels
            .valueStream()
            .map(c -> propertyValue(c, "hostName") + "/" + propertyValue(c, "iocName"))
            .toList());
  }

  @Test
  void activeChannelQueryReturnsTheIocAddress() {
    JsonNode active = cf.get("/resources/channels?pvStatus=Active&~name=ABC:GHI:001").json();
    JsonNode inactive = cf.get("/resources/channels?pvStatus=Active&~name=ABC:DEF:002").json();

    assertEquals("10.0.0.2", propertyValue(active.get(0), "iocIP"));
    assertEquals("5075", propertyValue(active.get(0), "pvaPort"));
    assertEquals(0, inactive.size());
  }
}
