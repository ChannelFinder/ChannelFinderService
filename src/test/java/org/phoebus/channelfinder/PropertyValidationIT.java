package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
@WebMvcTest(PropertyManager.class)
@WithMockUser(roles = "CF-ADMINS")
@PropertySource(value = "classpath:application_test.properties")
public class PropertyValidationIT {

    @Autowired
    PropertyManager propertyManager;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to Property request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullName() {
        Property testProperty1 = new Property(null, "testOwner");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyName() {
        Property testProperty1 = new Property("", "testOwner");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullOwner() {
        Property testProperty1 = new Property("testProperty1", null);
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyOwner() {
        Property testProperty1 = new Property("testProperty1", "");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with a non existent channel
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestFakeChannel() {
        Property testProperty1 = new Property("testProperty1", "testOwner");
        testProperty1.setChannels(Arrays.asList(new Channel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with multiple non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestFakeChannels() {
        Property testProperty1 = new Property("testProperty1", "testOwner");
        testProperty1.setChannels(
                Arrays.asList(new Channel("Non-existent-channel"),
                        new Channel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to Property request with some existent(and valid) and some non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestSomeFakeChannels() {
        Channel chan = new Channel("testChannel0", "testOwner");
        channelRepository.index(chan);
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(chan.getName(),chan.getOwner(),Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to Property request with a channel that has no prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNoProp() {
        Channel chan = new Channel("testChannel0", "testOwner");
        channelRepository.index(chan);
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(chan.getName(),chan.getOwner(),new ArrayList<Property>(),new ArrayList<Tag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");    
    }

    /**
     * Attempt to Property request with a null value
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullValue() {
        Channel chan = new Channel("testChannel0", "testOwner");
        channelRepository.index(chan);
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(chan.getName(),chan.getOwner(),Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),null)),new ArrayList<Tag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to Property request with an empty value
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyValue() {
        Channel chan = new Channel("testChannel0", "testOwner");
        channelRepository.index(chan);
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(chan.getName(),chan.getOwner(),Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),"")),new ArrayList<Tag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to Property request with valid parameters
     */
    @Test
    public void validateXmlPropertyRequest() {
        Channel chan = new Channel("testChannel0", "testOwner");
        channelRepository.index(chan);
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(chan.getName(),chan.getOwner(),Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<Tag>())));
        try {
            propertyManager.validatePropertyRequest(testProperty1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to Property request with other valid parameters
     */
    @Test
    public void validateXmlPropertyRequest2() {
        Property testProperty1 = new Property("testProperty1","testOwner1");
        try {
            propertyManager.validatePropertyRequest(testProperty1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
    }
}