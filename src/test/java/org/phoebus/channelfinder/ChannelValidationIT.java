package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;


@RunWith(SpringRunner.class)
@WebMvcTest(ChannelManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class ChannelValidationIT {

    @Autowired
    ChannelManager channelManager;

    @Autowired
    TagRepository tagRepository;
    
    @Autowired
    PropertyRepository propertyRepository;
    
    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to XmlChannel request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullName() {
        XmlChannel testChannel1 = new XmlChannel(null, "testOwner");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to XmlChannel request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyName() {
        XmlChannel testChannel1 = new XmlChannel("", "testOwner");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to XmlChannel request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullOwner() {
        XmlChannel testChannel1 = new XmlChannel("testChannel1", null);
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to XmlChannel request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyOwner() {
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to XmlChannel request with a non existent tag
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestFakeTag() {
        // set up
        XmlProperty prop = new XmlProperty("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("value");
        
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(new XmlTag("Non-existent-tag")));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        propertyRepository.deleteById(prop.getName());
    }

    /**
     * Attempt to XmlChannel request with a non existent prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestFakeProp() {
        // set up
        XmlTag tag = new XmlTag("testTag1","testOwner");
        tagRepository.index(tag);
        
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",Arrays.asList(new XmlProperty("Non-existent-property","Non-existent-property")),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
    }

    /**
     * Attempt to XmlChannel request with a null value prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullProp() {
        // set up
        XmlTag tag = new XmlTag("testTag1","testOwner");
        tagRepository.index(tag);
        XmlProperty prop = new XmlProperty("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue(null);
        
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }
    
    /**
     * Attempt to XmlChannel request with an empty value prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyProp() {
        // set up
        XmlTag tag = new XmlTag("testTag1","testOwner");
        tagRepository.index(tag);
        XmlProperty prop = new XmlProperty("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("");
        
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }

    /**
     * Attempt to XmlChannel request with valid parameters
     */
    @Test
    public void validateXmlChannelRequest() {
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner");
        try {
            channelManager.validateChannelRequest(testChannel1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
    }
    
    /**
     * Attempt to XmlChannel request with valid parameters
     */
    @Test
    public void validateXmlChannelRequest2() {
     // set up
        XmlTag tag = new XmlTag("testTag1","testOwner");
        tagRepository.index(tag);
        XmlProperty prop = new XmlProperty("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("value");
        
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }
}