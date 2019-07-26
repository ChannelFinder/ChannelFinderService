package gov.bnl.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;


@RunWith(SpringRunner.class)
@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class TagValidationIT {

    @Autowired
    TagManager tagManager;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to XmlTag request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestNullName() {
        XmlTag testTag1 = new XmlTag(null, "testOwner");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestEmptyName() {
        XmlTag testTag1 = new XmlTag("", "testOwner");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestNullOwner() {
        XmlTag testTag1 = new XmlTag("testTag1", null);
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestEmptyOwner() {
        XmlTag testTag1 = new XmlTag("testTag1", "");
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with a non existent channel
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestFakeChannel() {
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(Arrays.asList(new XmlChannel("Non-existent-channel")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with multiple non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestFakeChannels() {
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new XmlChannel("Non-existent-channel"),
                        new XmlChannel("Non-existent-channel")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with some existent and some non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlTagRequestSomeFakeChannels() {
        channelRepository.indexAll(Arrays.asList(new XmlChannel("testChannel0", "testOwner")));
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(
                Arrays.asList(new XmlChannel("Non-existent-channel"),
                        new XmlChannel("testChannel0")));
        tagManager.validateTagRequest(testTag1);
    }

    /**
     * Attempt to XmlTag request with valid parameters
     */
    @Test
    public void validateXmlTagRequest() {
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        try {
            tagManager.validateTagRequest(testTag1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
    }
    
    /**
     * Attempt to XmlTag request with other valid parameters
     */
    @Test
    public void validateXmlTagRequest2() {
        channelRepository.indexAll(Arrays.asList(new XmlChannel("testChannel0", "testOwner")));

        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(Arrays.asList(new XmlChannel("testChannel0")));
        try {
            tagManager.validateTagRequest(testTag1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }

        channelRepository.deleteById("testChannel0");
    }
}