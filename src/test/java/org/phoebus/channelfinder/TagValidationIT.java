package org.phoebus.channelfinder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;


@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
public class TagValidationIT {

    @Autowired
    TagManager tagManager;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to Tag request with null name
     */
    @Test
    public void validateXmlTagRequestNullName() {
        Tag testTag1 = new Tag(null, "testOwner");
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with empty name
     */
    @Test
    public void validateXmlTagRequestEmptyName() {
        Tag testTag1 = new Tag("", "testOwner");
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with null owner
     */
    @Test
    public void validateXmlTagRequestNullOwner() {
        Tag testTag1 = new Tag("testTag1", null);
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with empty owner
     */
    @Test
    public void validateXmlTagRequestEmptyOwner() {
        Tag testTag1 = new Tag("testTag1", "");
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with a non existent channel
     */
    @Test
    public void validateXmlTagRequestFakeChannel() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(Arrays.asList(new Channel("Non-existent-channel")));
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with multiple non existent channels
     */
    @Test
    public void validateXmlTagRequestFakeChannels() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new Channel("Non-existent-channel"),
                        new Channel("Non-existent-channel")));
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with some existent and some non existent channels
     */
    @Test
    public void validateXmlTagRequestSomeFakeChannels() {
        channelRepository.indexAll(Arrays.asList(new Channel("testChannel0", "testOwner")));
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new Channel("Non-existent-channel"),
                        new Channel("testChannel0")));
        Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.validateTagRequest(testTag1));
    }

    /**
     * Attempt to Tag request with valid parameters
     */
    @Test
    public void validateXmlTagRequest() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        try {
            tagManager.validateTagRequest(testTag1);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            fail("Failed to validate with valid parameters");
        }
    }
    
    /**
     * Attempt to Tag request with other valid parameters
     */
    @Test
    public void validateXmlTagRequest2() {
        channelRepository.indexAll(Arrays.asList(new Channel("testChannel0", "testOwner")));

        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(Arrays.asList(new Channel("testChannel0")));
        try {
            tagManager.validateTagRequest(testTag1);
            Assertions.assertTrue(true);
        } catch (Exception e) {
            fail("Failed to validate with valid parameters");
        }

        channelRepository.deleteById("testChannel0");
    }
}