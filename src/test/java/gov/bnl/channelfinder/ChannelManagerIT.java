package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class ChannelManagerIT {

    @Autowired
    TagRepository tagRepository;
    
    @Autowired
    PropertyRepository propertyRepository;
    
    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    ChannelManager channelManager;
    
    // set up
    XmlChannel testChannel0 = new XmlChannel("testChannel0","testChannelOwner0");
    XmlChannel testChannel1 = new XmlChannel("testChannel1","testChannelOwner1");
    XmlChannel testChannel2 = new XmlChannel("testChannel2","testChannelOwner2");

    /**
     * read a single channel
     */
    @Test
    public void readXmlChannel() {
        testChannel0.setTags(testTags);
        testChannel0.setProperties(testProperties);
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(),testChannel0);

        XmlChannel readChannel = channelManager.read(createdChannel.getName());
        // verify the channel was read as expected
        assertEquals("Failed to read the tag",createdChannel,readChannel);        

        try {
            // verify the channel failed to be read, as expected
            readChannel = channelManager.read("fakeChannel");
            assertTrue("Failed to throw an error",false);
        } catch(ResponseStatusException e) {
            assertTrue(true);
        }
    }

    /**
     * create a single channel
     */
    @Test
    public void createXmlChannel() {
        testChannel0.setTags(testTags);
        testChannel0.setProperties(testProperties);
        testChannel1.setTags(testTags);
        testChannel1.setProperties(testProperties);
        
        XmlChannel createdChannel0 = channelManager.create(testChannel2.getName(),testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel0,createdChannel0); 
        
        XmlChannel createdChannel1 = channelManager.create(testChannel1.getName(),testChannel1);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel1,createdChannel1);
        
        testChannel0.setOwner("newOwner");
        createdChannel0 = channelManager.create(testChannel0.getName(),testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel0,createdChannel0);
        
        createdChannel1 = channelManager.create(testChannel0.getName(),testChannel1);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel1,createdChannel1);
        // verify the old channel was deleted as expected
        if(channelRepository.existsById(testChannel0.getName()))
            assertTrue(true);
        else {
            fail("Failed to remove the old channel");
        }
    }
    
    /**
     * create multiple channels
     */
    @Test
    public void createXmlChannels() {
        testChannel0.setTags(testTags);
        testChannel0.setProperties(testProperties);
        testChannel1.setTags(testTags);
        testChannel1.setProperties(testProperties);
        
        XmlChannel createdChannel0 = channelManager.create(testChannel0.getName(),testChannel0);

        Iterable<XmlChannel> createdChannels = channelManager.create(Arrays.asList(testChannel0,testChannel1,testChannel2));
        // verify the channels were created as expected
        assertTrue("Failed to create the channels",Iterables.elementsEqual(testChannels, createdChannels));  
    }

    /**
     * update a single channel
     */
    @Test
    public void updateXmlChannel() {
        testChannel0.setTags(testTags);
        testChannel0.setProperties(testProperties);
        testChannel1.setTags(testTags);
        testChannel1.setProperties(testProperties);
        
        XmlChannel createdChannel0 = channelManager.create(testChannel2.getName(),testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel0,createdChannel0); 
        
        XmlChannel createdChannel1 = channelManager.create(testChannel1.getName(),testChannel1);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel1,createdChannel1);
        
        testChannel0.setOwner("newOwner");
        createdChannel0 = channelManager.create(testChannel0.getName(),testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel0,createdChannel0);
        
        createdChannel1 = channelManager.create(testChannel0.getName(),testChannel1);
        // verify the channel was created as expected
        assertEquals("Failed to create the tag",testChannel1,createdChannel1);
        // verify the old channel was deleted as expected
        if(channelRepository.existsById(testChannel0.getName()))
            assertTrue(true);
        else {
            fail("Failed to remove the old channel");
        }
    }
    
    /**
     * Test the basic operations of create, read, updated, and delete on a list of channels
     */
    @Test
    public void basicChannelsCRUDOperations() {
        List<XmlChannel> testChannels = new ArrayList<XmlChannel>();
        for (int i = 0; i < 3; i++) {
            XmlChannel testChannel = new XmlChannel("test-channel" + i, "test-channel-owner");
            testChannel.addTag(testTags.get(i));
            XmlProperty prop = testProperties.get(i);
            prop.setValue("test-prop" + i + "-value");
            testChannel.addProperty(prop);
            testChannels.add(testChannel);
        }
        // Create a list of channels
        List<XmlChannel> createdChannels = Lists.newArrayList(channelManager.create(testChannels));
        assertTrue("failed to create test channel", createdChannels != null && testChannels.equals(createdChannels));
        // Find a list of channels
        LinkedMultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add("~name", "test-channel?");
        List<XmlChannel> foundChannels = channelManager.query(searchParameters);
        assertTrue("failed to create test channel expected 3 channels found " + foundChannels.size(),
                foundChannels != null && foundChannels.containsAll(testChannels));
        // Update a list of channels 
        // 1. with new tags and properties
        // Add test-tag0 and test-property0 to all channels
        testChannels.forEach(channel -> {
            channel.addTag(testTags.get(0));
            XmlProperty prop = testProperties.get(0);
            prop.setValue("test-prop0-value");
            channel.addProperty(prop);
        });
        List<XmlChannel> updatedChannels = Lists.newArrayList(channelManager.update(testChannels));
        assertTrue("failed to update test channels, it was expected that all channels with have test-tag0",
                updatedChannels.stream().allMatch(channel -> {
                    return channel.getTags().contains(testTags.get(0));
                }));
        XmlProperty expectedProperty = testProperties.get(0);
        expectedProperty.setValue("test-prop0-value");
        assertTrue("failed to update test channels, it was expected that all channels with have test-property0",
                updatedChannels.stream().allMatch(channel -> {
                    return channel.getProperties().contains(expectedProperty);
                }));

        // 2. with new property values
        // Delete a list of channels
        testChannels.forEach(channel -> {
            channelManager.remove(channel.getName());
        });
        testChannels.forEach(channel -> {
            try {
                channelManager.read(channel.getName());
                fail("failed to delete channel " + channel.getName());
            } catch (ResponseStatusException e) {
                
            }
        });

    }
    // Helper operations to create and clean up the resources needed for successful
    // testing of the ChannelManager operations
    
    private final List<XmlTag> testTags = Arrays.asList(new XmlTag("testTag0", "testTagOwner0"),
                                                        new XmlTag("testTag1", "testTagOwner1"),
                                                        new XmlTag("testTag2", "testTagOwner2"));

    private final List<XmlProperty> testProperties = Arrays.asList(
                                                        new XmlProperty("testProperty0", "testPropertyOwner0"),
                                                        new XmlProperty("testProperty1", "testPropertyOwner1"),
                                                        new XmlProperty("testProperty2", "testPropertyOwner2"));

    private final List<XmlChannel> testChannels = Arrays.asList(
            testChannel0,testChannel1,testChannel2);
    @Before
    public void setup() {
        tagRepository.indexAll(testTags);
        propertyRepository.indexAll(testProperties);
    }

    @After
    public void cleanup() {
        // clean up
        testTags.forEach(createdTag -> {
            try {
                tagRepository.deleteById(createdTag.getName());
            } catch (Exception e) {}
        });
        testProperties.forEach(createdProperty -> {
            try {
                propertyRepository.deleteById(createdProperty.getName());
            } catch (Exception e) {}
        });
        testChannels.forEach(createdChannel -> {
            try {
                channelRepository.deleteById(createdChannel.getName());
            } catch (Exception e) {}            
        });
    }
}