package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;


@RunWith(SpringRunner.class)
@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
@PropertySource(value = "classpath:application_test.properties")
public class TagValidationIT {

    @Autowired
    TagManager tagManager;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to Tag request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestNullName() {
        Tag testTag1 = new Tag(null, "testOwner");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestEmptyName() {
        Tag testTag1 = new Tag("", "testOwner");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestNullOwner() {
        Tag testTag1 = new Tag("testTag1", null);
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestEmptyOwner() {
        Tag testTag1 = new Tag("testTag1", "");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with a non existent channel
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestFakeChannel() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(Arrays.asList(new Channel("Non-existent-channel")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with multiple non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestFakeChannels() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new Channel("Non-existent-channel"),
                        new Channel("Non-existent-channel")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with some existent and some non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestSomeFakeChannels() {
        channelRepository.indexAll(Arrays.asList(new Channel("testChannel0", "testOwner")));
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new Channel("Non-existent-channel"),
                        new Channel("testChannel0")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to Tag request with valid parameters
     */
    @Test
    public void validateXmlTagRequest() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        try {
            tagManager.validateTagRequest(testTag1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
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
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }

        channelRepository.deleteById("testChannel0");
    }
}