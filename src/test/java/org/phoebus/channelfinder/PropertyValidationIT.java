package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.PropertyManager;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.phoebus.channelfinder.XmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;


@RunWith(SpringRunner.class)
@WebMvcTest(PropertyManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class PropertyValidationIT {

    @Autowired
    PropertyManager propertyManager;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * Attempt to XmlProperty request with null name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullName() {
        XmlProperty testProperty1 = new XmlProperty(null, "testOwner");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with empty name
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyName() {
        XmlProperty testProperty1 = new XmlProperty("", "testOwner");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with null owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullOwner() {
        XmlProperty testProperty1 = new XmlProperty("testProperty1", null);
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with empty owner
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyOwner() {
        XmlProperty testProperty1 = new XmlProperty("testProperty1", "");
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with a non existent channel
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestFakeChannel() {
        XmlProperty testProperty1 = new XmlProperty("testProperty1", "testOwner");
        testProperty1.setChannels(Arrays.asList(new XmlChannel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with multiple non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestFakeChannels() {
        XmlProperty testProperty1 = new XmlProperty("testProperty1", "testOwner");
        testProperty1.setChannels(
                Arrays.asList(new XmlChannel("Non-existent-channel"),
                        new XmlChannel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
    }

    /**
     * Attempt to XmlProperty request with some existent(and valid) and some non existent channels
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestSomeFakeChannels() {
        XmlChannel chan = new XmlChannel("testChannel0", "testOwner");
        channelRepository.index(chan);
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(chan.getName(),chan.getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<XmlTag>()),
                new XmlChannel("Non-existent-channel")));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to XmlProperty request with a channel that has no prop
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNoProp() {
        XmlChannel chan = new XmlChannel("testChannel0", "testOwner");
        channelRepository.index(chan);
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(chan.getName(),chan.getOwner(),new ArrayList<XmlProperty>(),new ArrayList<XmlTag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");    
    }

    /**
     * Attempt to XmlProperty request with a null value
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestNullValue() {
        XmlChannel chan = new XmlChannel("testChannel0", "testOwner");
        channelRepository.index(chan);
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(chan.getName(),chan.getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),null)),new ArrayList<XmlTag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to XmlProperty request with an empty value
     */
    @Test(expected = ResponseStatusException.class)
    public void validateXmlPropertyRequestEmptyValue() {
        XmlChannel chan = new XmlChannel("testChannel0", "testOwner");
        channelRepository.index(chan);
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(chan.getName(),chan.getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"")),new ArrayList<XmlTag>())));
        propertyManager.validatePropertyRequest(testProperty1);
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to XmlProperty request with valid parameters
     */
    @Test
    public void validateXmlPropertyRequest() {
        XmlChannel chan = new XmlChannel("testChannel0", "testOwner");
        channelRepository.index(chan);
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(chan.getName(),chan.getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<XmlTag>())));
        try {
            propertyManager.validatePropertyRequest(testProperty1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
        channelRepository.deleteById("testChannel0");
    }

    /**
     * Attempt to XmlProperty request with other valid parameters
     */
    @Test
    public void validateXmlPropertyRequest2() {
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        try {
            propertyManager.validatePropertyRequest(testProperty1);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue("Failed to validate with valid parameters",false);
        }
    }
}