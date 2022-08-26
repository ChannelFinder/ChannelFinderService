package org.phoebus.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class ChannelManagerIT {

    @Autowired
    ChannelManager channelManager;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * read a single channel
     */
    @Test
    public void readXmlChannel() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel0);
        
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        
        XmlChannel readChannel = channelManager.read(createdChannel.getName());
        // verify the channel was read as expected
        assertEquals("Failed to read the channel", createdChannel, readChannel);
    }

    /**
     * attempt to read a single non existent channel
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlChannel() {
        // verify the channel failed to be read, as expected
        channelManager.read("fakeChannel");
    }
    
    /**
     * create a simple channel
     */
    @Test
    public void createXmlChannel() {
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        cleanupTestChannels = Arrays.asList(testChannel0);
        
        // Create a simple channel
        XmlChannel createdChannel0 = channelManager.create(testChannel0.getName(), testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the channel", testChannel0, createdChannel0);

//      XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//      // verify the tag was created as expected
//      assertEquals("Failed to create the tag",testTag1,createdTag1);
        
        // Update the test channel with a new owner
        testChannel0.setOwner("updateTestOwner");
        XmlChannel updatedChannel0 = channelManager.create(testChannel0.getName(), testChannel0);
        // verify the channel was created as expected
        assertEquals("Failed to create the channel", testChannel0, updatedChannel0);        
    }

    /**
     * Rename a simple channel using create
     */
    @Test
    public void renameByCreateXmlChannel() {
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner");
        cleanupTestChannels = Arrays.asList(testChannel0,testChannel1);
        
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        createdChannel = channelManager.create(testChannel0.getName(), testChannel1);
        // verify that the old channel "testChannel0" was replaced with the new "testChannel1"
        assertEquals("Failed to create the channel", testChannel1, createdChannel);
        // verify that the old channel is no longer present
        assertFalse("Failed to replace the old channel", channelRepository.existsById(testChannel0.getName()));
    }
    
    /**
     * create a single channel with tags and properties
     */
    @Test
    public void createXmlChannel2() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel0);
        
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        try {
            XmlChannel foundChannel = channelRepository.findById(testChannel0.getName()).get();
            assertEquals("Failed to create the channel. Expected " + testChannel0.toLog() + " found " 
                    + foundChannel.toLog(), testChannel0, foundChannel);
        } catch (Exception e) {
            assertTrue("Failed to create/find the channel", false);
        }
        
//      XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//      // verify the tag was created as expected
//      assertEquals("Failed to create the tag",testTag1,createdTag1);
        
        // Update the test channel with a new owner
        testChannel0.setOwner("updateTestOwner");
        createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        try {
            XmlChannel foundChannel = channelRepository.findById(testChannel0.getName()).get();
            assertEquals("Failed to create the channel. Expected " + testChannel0.toLog() + " found " 
                    + foundChannel.toLog(), testChannel0, foundChannel);
        } catch (Exception e) {
            assertTrue("Failed to create/find the channel", false);
        }       
    }
    
    /**
     * Rename a single channel with tags and properties using create
     */
    @Test
    public void renameByCreateXmlChannel2() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel0,testChannel1);
        
        // Create the testChannel0
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        // update the testChannel0 with testChannel1
        createdChannel = channelManager.create(testChannel0.getName(), testChannel1);
        // verify that the old channel "testChannel0" was replaced with the new "testChannel1"
        try {
            XmlChannel foundChannel = channelRepository.findById(testChannel1.getName()).get();
            assertEquals("Failed to create the channel", testChannel1, foundChannel);
        } catch (Exception e) {
            assertTrue("Failed to create/find the channel", false);
        }        
        // verify that the old channel is no longer present
        assertFalse("Failed to replace the old channel", channelRepository.existsById(testChannel0.getName()));
    }
    
    /**
     * create multiple channels
     */
    @Test
    public void createXmlChannels() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",testProperties,testTags);
        XmlChannel testChannel2 = new XmlChannel("testChannel2", "testOwner");
        List<XmlChannel> testChannels = Arrays.asList(testChannel0,testChannel1,testChannel2);
        cleanupTestChannels = testChannels;

        Iterable<XmlChannel> createdChannels = channelManager.create(testChannels);
        List<XmlChannel> foundChannels = new ArrayList<XmlChannel>();
        testChannels.forEach(chan -> foundChannels.add(channelRepository.findById(chan.getName()).get()));
        // verify the channels were created as expected
        assertTrue("Failed to create the channels", Iterables.elementsEqual(testChannels, foundChannels));
    }

    /**
     * create by overriding multiple channels
     */
    @Test
    public void createXmlChannelsWithOverride() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel0,testChannel1);
        cleanupTestChannels = testChannels;

        //Create a set of original channels to be overriden
        channelManager.create(testChannels);
        // Now update the test channels
        testChannel0.setOwner("testOwner-updated");
        testChannel1.setTags(Collections.emptyList());
        testChannel1.setProperties(Collections.emptyList());

        List<XmlChannel> updatedTestChannels = Arrays.asList(testChannel0,testChannel1);        
        channelManager.create(updatedTestChannels);
        List<XmlChannel> foundChannels = new ArrayList<XmlChannel>();
        testChannels.forEach(chan -> foundChannels.add(channelRepository.findById(chan.getName()).get()));
        // verify the channels were created as expected
        assertTrue("Failed to create the channels", Iterables.elementsEqual(updatedTestChannels, foundChannels));
    }
    
    /**
     * update a channel
     */
    @Test
    public void updateXmlChannel() {
        testProperties.forEach(prop -> prop.setValue("value"));
        // A test channel with only name and owner
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        // A test channel with name, owner, tags and props
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel0,testChannel1);
        
        // Update on a non-existing channel should result in the creation of that channel
        // 1. Test a simple channel 
        XmlChannel returnedChannel = channelManager.update(testChannel0.getName(), testChannel0);
        assertEquals("Failed to update channel " + testChannel0, testChannel0, returnedChannel);
        assertEquals("Failed to update channel " + testChannel0, testChannel0, channelRepository.findById(testChannel0.getName()).get());
        // 2. Test a channel with tags and props
        returnedChannel = channelManager.update(testChannel1.getName(), testChannel1);
        assertEquals("Failed to update channel " + testChannel1, testChannel1, returnedChannel);
        assertEquals("Failed to update channel " + testChannel1, testChannel1, channelRepository.findById(testChannel1.getName()).get());

        // Update the channel owner
        testChannel0.setOwner("newTestOwner");
        returnedChannel = channelManager.update(testChannel0.getName(), testChannel0);
        assertEquals("Failed to update channel " + testChannel0, testChannel0, returnedChannel);
        assertEquals("Failed to update channel " + testChannel0, testChannel0, channelRepository.findById(testChannel0.getName()).get());
        testChannel1.setOwner("newTestOwner");
        returnedChannel = channelManager.update(testChannel1.getName(), testChannel1);
        assertEquals("Failed to update channel " + testChannel1, testChannel1, returnedChannel);
        assertEquals("Failed to update channel " + testChannel1, testChannel1, channelRepository.findById(testChannel1.getName()).get());
    }
    
    /**
     * Rename a channel using update
     */
    @Test
    public void renameByUpdateXmlChannel() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner");
        XmlChannel testChannel2 = new XmlChannel("testChannel2", "testOwner",testProperties,testTags);
        XmlChannel testChannel3 = new XmlChannel("testChannel3", "testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel0,testChannel1,testChannel2,testChannel3);
        
        // Create the testChannels
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        XmlChannel createdChannelWithItems = channelManager.create(testChannel2.getName(), testChannel2);
        // update the testChannels
        XmlChannel renamedChannel = channelManager.update(testChannel0.getName(), testChannel1);
        XmlChannel renamedChannelWithItems = channelManager.update(testChannel2.getName(), testChannel3);
        
        // verify that the old channels were replaced by the new ones
        try {
            XmlChannel foundChannel = channelRepository.findById(testChannel1.getName()).get();
            assertEquals("Failed to create the channel", testChannel1, foundChannel);
        } catch (Exception e) {
            assertTrue("Failed to create/find the channel", false);
        }        
        // verify that the old channel is no longer present
        assertFalse("Failed to replace the old channel", channelRepository.existsById(testChannel0.getName()));
        
        try {
            XmlChannel foundChannel = channelRepository.findById(testChannel3.getName()).get();
            assertEquals("Failed to create the channel", testChannel3, foundChannel);
        } catch (Exception e) {
            assertTrue("Failed to create/find the channel", false);
        }        
        // verify that the old channel is no longer present
        assertFalse("Failed to replace the old channel", channelRepository.existsById(testChannel2.getName()));
        
        // TODO add test for failure case
    }
    
    /**
     * update a channel by adding tags and adding properties and changing properties
     */
    @Test
    public void updateXmlChannelItems() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",
                Arrays.asList(testProperties.get(0),testProperties.get(1)),Arrays.asList(testTags.get(0),testTags.get(1)));
        cleanupTestChannels = Arrays.asList(testChannel0);
        
        // Create the testChannel
        XmlChannel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
        
        // set up the new testChannel
        testProperties.get(1).setValue("newValue");
        testChannel0 = new XmlChannel("testChannel0", "testOwner",
                Arrays.asList(testProperties.get(1),testProperties.get(2)),Arrays.asList(testTags.get(1),testTags.get(2)));
        
        // update the testChannel
        XmlChannel updatedChannel = channelManager.update(testChannel0.getName(), testChannel0);
        
        XmlChannel expectedChannel = new XmlChannel("testChannel0", "testOwner",testProperties,testTags);
        XmlChannel foundChannel = channelRepository.findById("testChannel0").get();
        foundChannel.getTags().sort((XmlTag o1, XmlTag o2) -> {
            return o1.getName().compareTo(o2.getName());
        });
        foundChannel.getProperties().sort((XmlProperty o1, XmlProperty o2) -> {
            return o1.getName().compareTo(o2.getName());
        });
        assertEquals(
                "Did not update channel correctly, expected " + expectedChannel.toLog() + " but actual was "
                        + foundChannel.toLog(),
                expectedChannel, foundChannel);
    }
    
    /**
     * update multiple channels
     * first update non-existing channels which should create them
     * second update the newly created channels which should change them
     */
    @Test
    public void updateMultipleXmlChannels() {
        testProperties.forEach(prop -> prop.setValue("value"));
        // A test channel with only name and owner
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
        // A test channel with name, owner, tags and props
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel0,testChannel1);
        cleanupTestChannels = testChannels;
        
        // Update on non-existing channels should result in the creation of those channels
        Iterable<XmlChannel> returnedChannels = channelManager.update(testChannels);
        // 1. Test a simple channel 
        assertEquals("Failed to update channel " + testChannel0, testChannel0, channelRepository.findById(testChannel0.getName()).get());
        // 2. Test a channel with tags and props
        assertEquals("Failed to update channel " + testChannel1, testChannel1, channelRepository.findById(testChannel1.getName()).get());

        // Update the channel owner
        testChannel0.setOwner("newTestOwner");
        testChannel1.setOwner("newTestOwner");
        returnedChannels = channelManager.update(testChannels);
        assertEquals("Failed to update channel " + testChannel0, testChannel0, channelRepository.findById(testChannel0.getName()).get());
        assertEquals("Failed to update channel " + testChannel1, testChannel1, channelRepository.findById(testChannel1.getName()).get());
    }
    
    /**
     * update multiple channels by adding tags and adding properties and changing properties
     */
    @Test
    public void updateMultipleXmlChannelsWithItems() {
        testProperties.forEach(prop -> prop.setValue("value"));
        XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner",
                Arrays.asList(testProperties.get(0),testProperties.get(1)),Arrays.asList(testTags.get(0),testTags.get(1)));
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner",
                Arrays.asList(testProperties.get(0),testProperties.get(2)),Arrays.asList(testTags.get(0),testTags.get(2)));
        List<XmlChannel> testChannels = Arrays.asList(testChannel0,testChannel1);
        cleanupTestChannels = testChannels;
        
        // Create the testChannel
        Iterable<XmlChannel> createdChannels = channelManager.create(testChannels);
        
        // set up the new testChannel
        testProperties.forEach(prop -> prop.setValue("newValue"));
        testChannel0 = new XmlChannel("testChannel0", "testOwner",
                Arrays.asList(testProperties.get(0),testProperties.get(2)),Arrays.asList(testTags.get(0),testTags.get(2)));
        testChannel1 = new XmlChannel("testChannel1", "testOwner",
                Arrays.asList(testProperties.get(0),testProperties.get(1)),Arrays.asList(testTags.get(0),testTags.get(1)));
        testChannels = Arrays.asList(testChannel0,testChannel1);
        
        // update the testChannel
        Iterable<XmlChannel> updatedChannels = channelManager.update(testChannels);

        // set up the expected testChannels
        testChannel0 = new XmlChannel("testChannel0", "testOwner", 
                Arrays.asList(testProperties.get(0),new XmlProperty("testProperty1", "testPropertyOwner1","value"),testProperties.get(2)), testTags);
        testChannel1 = new XmlChannel("testChannel1", "testOwner", 
                Arrays.asList(testProperties.get(0), testProperties.get(1), new XmlProperty("testProperty2", "testPropertyOwner2","value")), testTags);
        Iterable<XmlChannel> expectedChannels = Arrays.asList(testChannel0,testChannel1);
        
        Iterable<XmlChannel> foundChannels = channelRepository.findAllById(Arrays.asList("testChannel0","testChannel1"));
        foundChannels.forEach(chan -> chan.getTags().sort((XmlTag o1, XmlTag o2) -> {
            return o1.getName().compareTo(o2.getName());}));        
        foundChannels.forEach(chan -> chan.getProperties().sort((XmlProperty o1, XmlProperty o2) -> {
            return o1.getName().compareTo(o2.getName());}));
        
        assertEquals("Did not update channel correctly, expected " + testChannel0.toLog() + " and " + testChannel1.toLog() + " but actual was "
                + foundChannels.iterator().next().toLog() + " and " + foundChannels.iterator().next().toLog(), expectedChannels, foundChannels);
        }
    
    /**
     * delete a channel
     */
    @Test
    public void deleteXmlChannel() {
    XmlChannel testChannel0 = new XmlChannel("testChannel0", "testOwner");
    cleanupTestChannels = Arrays.asList(testChannel0);

    channelManager.create(testChannel0.getName(),testChannel0);    
    channelManager.remove(testChannel0.getName());
    // verify the channel was deleted as expected
    assertTrue("Failed to delete the channel", !channelRepository.existsById(testChannel0.getName()));
    }
    
    // Helper operations to create and clean up the resources needed for successful
    // testing of the ChannelManager operations

    private final List<XmlTag> testTags = Arrays.asList(
            new XmlTag("testTag0", "testTagOwner0"),
            new XmlTag("testTag1", "testTagOwner1"), 
            new XmlTag("testTag2", "testTagOwner2"));

    private final List<XmlProperty> testProperties = Arrays.asList(
            new XmlProperty("testProperty0", "testPropertyOwner0"),
            new XmlProperty("testProperty1", "testPropertyOwner1"),
            new XmlProperty("testProperty2", "testPropertyOwner2"));

    private List<XmlChannel> cleanupTestChannels = Collections.emptyList();

    @Before
    public void setup() {
        tagRepository.indexAll(testTags);
        propertyRepository.indexAll(testProperties);
    }

    @After
    public void cleanup() {
        // clean up
        testTags.forEach(tag -> {
            try {
                tagRepository.deleteById(tag.getName());
            } catch (Exception e) {
                System.out.println("Failed to clean up tag: " + tag.getName());
            }
        });
        testProperties.forEach(property -> {
            try {
                propertyRepository.deleteById(property.getName());
            } catch (Exception e) {
                System.out.println("Failed to clean up property: " + property.getName());
            }
        });
        cleanupTestChannels.forEach(channel -> {
            if (channelRepository.existsById(channel.getName())) {
                channelRepository.deleteById(channel.getName());
            }
        });
    }
}