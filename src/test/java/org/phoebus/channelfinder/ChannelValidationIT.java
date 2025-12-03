package org.phoebus.channelfinder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.PropertyRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.rest.controller.ChannelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelManager.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelValidationIT {

  @Autowired ChannelManager channelManager;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ChannelRepository channelRepository;

  @Autowired ElasticConfig esService;

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  /** Attempt to Channel request with null name */
  @Test
  void validateXmlChannelRequestNullName() {
    Channel testChannel1 = new Channel(null, "testOwner");
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));
  }

  /** Attempt to Channel request with empty name */
  @Test
  void validateXmlChannelRequestEmptyName() {
    Channel testChannel1 = new Channel("", "testOwner");
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));
  }

  /** Attempt to Channel request with null owner */
  @Test
  void validateXmlChannelRequestNullOwner() {
    Channel testChannel1 = new Channel("testChannel1", null);
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));
  }

  /** Attempt to Channel request with empty owner */
  @Test
  void validateXmlChannelRequestEmptyOwner() {
    Channel testChannel1 = new Channel("testChannel1", "");
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));
  }

  /** Attempt to Channel request with a non existent tag */
  @Test
  void validateXmlChannelRequestFakeTag() {
    // set up
    Property prop = new Property("testProperty1", "testOwner");
    propertyRepository.index(prop);
    prop.setValue("value");

    Channel testChannel1 =
        new Channel(
            "testChannel1",
            "testOwner",
            Arrays.asList(prop),
            Arrays.asList(new Tag("Non-existent-tag")));
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));

    // clean up
    propertyRepository.deleteById(prop.getName());
  }

  /** Attempt to Channel request with a non existent prop */
  @Test
  void validateXmlChannelRequestFakeProp() {
    // set up
    Tag tag = new Tag("testTag1", "testOwner");
    tagRepository.index(tag);

    Channel testChannel1 =
        new Channel(
            "testChannel1",
            "testOwner",
            Arrays.asList(new Property("Non-existent-property", "Non-existent-property")),
            Arrays.asList(tag));
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));

    // clean up
    tagRepository.deleteById(tag.getName());
  }

  /** Attempt to Channel request with a null value prop */
  @Test
  void validateXmlChannelRequestNullProp() {
    // set up
    Tag tag = new Tag("testTag1", "testOwner");
    tagRepository.index(tag);
    Property prop = new Property("testProperty1", "testOwner");
    propertyRepository.index(prop);
    prop.setValue(null);

    Channel testChannel1 =
        new Channel("testChannel1", "testOwner", Arrays.asList(prop), Arrays.asList(tag));
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));

    // clean up
    tagRepository.deleteById(tag.getName());
    propertyRepository.deleteById(prop.getName());
  }

  /** Attempt to Channel request with an empty value prop */
  @Test
  void validateXmlChannelRequestEmptyProp() {
    // set up
    Tag tag = new Tag("testTag1", "testOwner");
    tagRepository.index(tag);
    Property prop = new Property("testProperty1", "testOwner");
    propertyRepository.index(prop);
    prop.setValue("");

    Channel testChannel1 =
        new Channel("testChannel1", "testOwner", Arrays.asList(prop), Arrays.asList(tag));
    assertThrows(
        ResponseStatusException.class, () -> channelManager.validateChannelRequest(testChannel1));

    // clean up
    tagRepository.deleteById(tag.getName());
    propertyRepository.deleteById(prop.getName());
  }

  /** Attempt to Channel request with valid parameters */
  @Test
  void validateXmlChannelRequest() {
    Channel testChannel1 = new Channel("testChannel1", "testOwner");
    try {
      channelManager.validateChannelRequest(testChannel1);
      Assertions.assertTrue(true);
    } catch (Exception e) {
      fail("Failed to validate with valid parameters");
    }
  }

  /** Attempt to Channel request with valid parameters */
  @Test
  void validateXmlChannelRequest2() {
    // set up
    Tag tag = new Tag("testTag1", "testOwner");
    tagRepository.index(tag);
    Property prop = new Property("testProperty1", "testOwner");
    propertyRepository.index(prop);
    prop.setValue("value");

    Channel testChannel1 =
        new Channel("testChannel1", "testOwner", Arrays.asList(prop), Arrays.asList(tag));
    channelManager.validateChannelRequest(testChannel1);

    // clean up
    tagRepository.deleteById(tag.getName());
    propertyRepository.deleteById(prop.getName());
  }
}
