package org.phoebus.channelfinder;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(PropertyManager.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
class PropertyValidationIT {

  @Autowired PropertyManager propertyManager;

  @Autowired ChannelRepository channelRepository;

  @Autowired ElasticConfig esService;

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  /** Attempt to Property request with null name */
  @Test
  void validateXmlPropertyRequestNullName() {
    Property testProperty1 = new Property(null, "testOwner");
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with empty name */
  @Test
  void validateXmlPropertyRequestEmptyName() {
    Property testProperty1 = new Property("", "testOwner");
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with null owner */
  @Test
  void validateXmlPropertyRequestNullOwner() {
    Property testProperty1 = new Property("testProperty1", null);
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with empty owner */
  @Test
  void validateXmlPropertyRequestEmptyOwner() {
    Property testProperty1 = new Property("testProperty1", "");
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with a non existent channel */
  @Test
  void validateXmlPropertyRequestFakeChannel() {
    Property testProperty1 = new Property("testProperty1", "testOwner");
    testProperty1.setChannels(Arrays.asList(new Channel("Non-existent-channel")));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with multiple non existent channels */
  @Test
  void validateXmlPropertyRequestFakeChannels() {
    Property testProperty1 = new Property("testProperty1", "testOwner");
    testProperty1.setChannels(
        Arrays.asList(new Channel("Non-existent-channel"), new Channel("Non-existent-channel")));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
  }

  /** Attempt to Property request with some existent(and valid) and some non existent channels */
  @Test
  void validateXmlPropertyRequestSomeFakeChannels() {
    Channel chan = new Channel("testChannel0", "testOwner");
    channelRepository.index(chan);
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    testProperty1.setChannels(
        Arrays.asList(
            new Channel(
                chan.getName(),
                chan.getOwner(),
                Arrays.asList(
                    new Property(testProperty1.getName(), testProperty1.getOwner(), "value")),
                new ArrayList<Tag>()),
            new Channel("Non-existent-channel")));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
    channelRepository.deleteById("testChannel0");
  }

  /** Attempt to Property request with a channel that has no prop */
  @Test
  void validateXmlPropertyRequestNoProp() {
    Channel chan = new Channel("testChannel0", "testOwner");
    channelRepository.index(chan);
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    testProperty1.setChannels(
        Arrays.asList(
            new Channel(
                chan.getName(), chan.getOwner(), new ArrayList<Property>(), new ArrayList<Tag>())));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
    channelRepository.deleteById("testChannel0");
  }

  /** Attempt to Property request with a null value */
  @Test
  void validateXmlPropertyRequestNullValue() {
    Channel chan = new Channel("testChannel0", "testOwner");
    channelRepository.index(chan);
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    testProperty1.setChannels(
        Arrays.asList(
            new Channel(
                chan.getName(),
                chan.getOwner(),
                Arrays.asList(
                    new Property(testProperty1.getName(), testProperty1.getOwner(), null)),
                new ArrayList<Tag>())));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
    channelRepository.deleteById("testChannel0");
  }

  /** Attempt to Property request with an empty value */
  @Test
  void validateXmlPropertyRequestEmptyValue() {
    Channel chan = new Channel("testChannel0", "testOwner");
    channelRepository.index(chan);
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    testProperty1.setChannels(
        Arrays.asList(
            new Channel(
                chan.getName(),
                chan.getOwner(),
                Arrays.asList(new Property(testProperty1.getName(), testProperty1.getOwner(), "")),
                new ArrayList<Tag>())));
    Assertions.assertThrows(
        ResponseStatusException.class,
        () -> propertyManager.validatePropertyRequest(testProperty1));
    channelRepository.deleteById("testChannel0");
  }

  /** Attempt to Property request with valid parameters */
  @Test
  void validateXmlPropertyRequest() {
    Channel chan = new Channel("testChannel0", "testOwner");
    channelRepository.index(chan);
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    testProperty1.setChannels(
        Arrays.asList(
            new Channel(
                chan.getName(),
                chan.getOwner(),
                Arrays.asList(
                    new Property(testProperty1.getName(), testProperty1.getOwner(), "value")),
                new ArrayList<Tag>())));
    try {
      propertyManager.validatePropertyRequest(testProperty1);
      Assertions.assertTrue(true);
    } catch (Exception e) {
      fail("Failed to validate with valid parameters");
    }
    channelRepository.deleteById("testChannel0");
  }

  /** Attempt to Property request with other valid parameters */
  @Test
  void validateXmlPropertyRequest2() {
    Property testProperty1 = new Property("testProperty1", "testOwner1");
    try {
      propertyManager.validatePropertyRequest(testProperty1);
      Assertions.assertTrue(true);
    } catch (Exception e) {
      fail("Failed to validate with valid parameters");
    }
  }
}
