package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;


@RunWith(SpringRunner.class)
@WebMvcTest(ChannelManager.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
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
     * Attempt to Channel request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullName() {
        Channel testChannel1 = new Channel(null, "testOwner");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to Channel request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyName() {
        Channel testChannel1 = new Channel("", "testOwner");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to Channel request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullOwner() {
        Channel testChannel1 = new Channel("testChannel1", null);
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to Channel request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyOwner() {
        Channel testChannel1 = new Channel("testChannel1", "");
        channelManager.validateChannelRequest(testChannel1);
    }

    /**
     * Attempt to Channel request with a non existent tag
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestFakeTag() {
        // set up
        Property prop = new Property("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("value");
        
        Channel testChannel1 = new Channel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(new Tag("Non-existent-tag")));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        propertyRepository.deleteById(prop.getName());
    }

    /**
     * Attempt to Channel request with a non existent prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestFakeProp() {
        // set up
        Tag tag = new Tag("testTag1","testOwner");
        tagRepository.index(tag);
        
        Channel testChannel1 = new Channel("testChannel1", "testOwner",Arrays.asList(new Property("Non-existent-property","Non-existent-property")),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
    }

    /**
     * Attempt to Channel request with a null value prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestNullProp() {
        // set up
        Tag tag = new Tag("testTag1","testOwner");
        tagRepository.index(tag);
        Property prop = new Property("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue(null);
        
        Channel testChannel1 = new Channel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }
    
    /**
     * Attempt to Channel request with an empty value prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlChannelRequestEmptyProp() {
        // set up
        Tag tag = new Tag("testTag1","testOwner");
        tagRepository.index(tag);
        Property prop = new Property("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("");
        
        Channel testChannel1 = new Channel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }

    /**
     * Attempt to Channel request with valid parameters
     */
    @Test
    public void validateXmlChannelRequest() {
        Channel testChannel1 = new Channel("testChannel1", "testOwner");
        try {
            channelManager.validateChannelRequest(testChannel1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
    }
    
    /**
     * Attempt to Channel request with valid parameters
     */
    @Test
    public void validateXmlChannelRequest2() {
     // set up
        Tag tag = new Tag("testTag1","testOwner");
        tagRepository.index(tag);
        Property prop = new Property("testProperty1","testOwner");
        propertyRepository.index(prop);
        prop.setValue("value");
        
        Channel testChannel1 = new Channel("testChannel1", "testOwner",Arrays.asList(prop),Arrays.asList(tag));
        channelManager.validateChannelRequest(testChannel1);
        
        // clean up
        tagRepository.deleteById(tag.getName());
        propertyRepository.deleteById(prop.getName());
    }
}