package org.phoebus.channelfinder;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.rest.api.ITag;
import org.phoebus.channelfinder.rest.controller.TagController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(TagController.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
class TagValidationIT {

  @Autowired
  ITag tagManager;

  @Autowired ElasticConfig esService;
  @Autowired ChannelRepository channelRepository;

  /** Attempt to Tag request with null name */
  @Test
  void validateXmlTagRequestNullName() {
    Tag testTag1 = new Tag(null, "testOwner");
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with empty name */
  @Test
  void validateXmlTagRequestEmptyName() {
    Tag testTag1 = new Tag("", "testOwner");
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with null owner */
  @Test
  void validateXmlTagRequestNullOwner() {
    Tag testTag1 = new Tag("testTag1", null);
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with empty owner */
  @Test
  void validateXmlTagRequestEmptyOwner() {
    Tag testTag1 = new Tag("testTag1", "");
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with a non existent channel */
  @Test
  void validateXmlTagRequestFakeChannel() {
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(Arrays.asList(new Channel("Non-existent-channel")));
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with multiple non existent channels */
  @Test
  void validateXmlTagRequestFakeChannels() {
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(
        Arrays.asList(new Channel("Non-existent-channel"), new Channel("Non-existent-channel")));
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with some existent and some non existent channels */
  @Test
  void validateXmlTagRequestSomeFakeChannels() {
    channelRepository.indexAll(Arrays.asList(new Channel("testChannel0", "testOwner")));
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(
        Arrays.asList(new Channel("Non-existent-channel"), new Channel("testChannel0")));
    Assertions.assertThrows(
        ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
  }

  /** Attempt to Tag request with valid parameters */
  @Test
  void validateXmlTagRequest() {
    Tag testTag1 = new Tag("testTag1", "testOwner");
    try {
      tagManager.validateTagRequest(testTag1);
      Assertions.assertTrue(true);
    } catch (Exception e) {
      fail("Failed to validate with valid parameters");
    }
  }

  /** Attempt to Tag request with other valid parameters */
  @Test
  void validateXmlTagRequest2() {
    channelRepository.indexAll(List.of(new Channel("testChannel0", "testOwner")));

    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(List.of(new Channel("testChannel0")));
    try {
      tagManager.validateTagRequest(testTag1);
      Assertions.assertTrue(true);
    } catch (Exception e) {
      fail("Failed to validate with valid parameters");
    }

    channelRepository.deleteById("testChannel0");
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }
}
