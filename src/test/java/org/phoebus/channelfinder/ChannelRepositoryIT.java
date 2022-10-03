package org.phoebus.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
public class ChannelRepositoryIT {

    @Autowired
    ElasticConfig esService;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

     /**
     * index a single channel
     */
    @Test
    public void indexXmlChannel() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel);
        
        XmlChannel createdChannel = channelRepository.index(testChannel);
        // verify the channel was created as expected
        assertEquals("Failed to create the channel", testChannel, createdChannel);
    }

    /**
     * index multiple channels
     */
    @Test
    public void indexXmlChannels() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        cleanupTestChannels = testChannels;

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        // verify the channels were created as expected
        assertTrue("Failed to create the channels", Iterables.elementsEqual(testChannels, createdChannels));
    }

    /**
     * save a single channel
     */
    @Test
    public void saveXmlChannel() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner");
        XmlChannel updateTestChannel =
                new XmlChannel("testChannel","updateTestOwner", testProperties.subList(0,1), testTags.subList(0,1));
        XmlChannel updateTestChannel1 =
                new XmlChannel("testChannel","updateTestOwner", testProperties.subList(1,2), testTags.subList(1,2));
        XmlChannel updateTestChannel2 =
                new XmlChannel("updateTestChannel1","updateTestOwner1",testUpdatedProperties, testUpdatedTags);
        XmlChannel createdChannel = channelRepository.index(testChannel);
        cleanupTestChannels = Arrays.asList(testChannel, updateTestChannel, updateTestChannel1, updateTestChannel2);

        // Update Channel with new owner a new property and a new tag
        XmlChannel updatedTestChannel = channelRepository.save(updateTestChannel);
        // verify that the channel was updated as expected
        assertEquals("Failed to update the channel with the same name", updateTestChannel, updatedTestChannel);

        // Update Channel with a second property and tag
        XmlChannel updatedTestChannel1 = channelRepository.save(updateTestChannel1);
        // verify that the channel was updated with the new tags and properties while preserving the old ones
        XmlChannel expectedChannel = new XmlChannel("testChannel","updateTestOwner");
        expectedChannel.addProperties(testProperties);
        expectedChannel.addTags(testTags);
        assertEquals("Failed to update the channel with the same name", updateTestChannel, updatedTestChannel);
    }


    /**
     * save multiple channels 
     */
    @Test
    public void saveXmlChannels() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner", testProperties, testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1", "testOwner1", testProperties, testTags);
        XmlChannel updateTestChannel = new XmlChannel("testChannel", "updateTestOwner", testUpdatedProperties, testUpdatedTags);
        XmlChannel updateTestChannel1 = new XmlChannel("testChannel1", "updateTestOwner1", testUpdatedProperties, testUpdatedTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        List<XmlChannel> updateTestChannels = Arrays.asList(updateTestChannel,updateTestChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1,updateTestChannel,updateTestChannel1);

        Iterable<XmlChannel> updatedTestChannels = channelRepository.saveAll(updateTestChannels);
        // verify the channels were updated as expected
        List<XmlChannel> expectedChannels = Arrays.asList(
                new XmlChannel("testChannel","updateTestOwner", testUpdatedProperties, testUpdatedTags),
                new XmlChannel("testChannel1","updateTestOwner1", testUpdatedProperties, testUpdatedTags)
        );
        assertEquals("Failed to update the channels: ",expectedChannels, updatedTestChannels);
    }

    /**
     * find a single channel
     */
    @Test
    public void findXmlChannel() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel);

        Optional<XmlChannel> notFoundChannel = channelRepository.findById(testChannel.getName());
        // verify the channel was not found as expected
        assertNotEquals("Found the channel",testChannel,notFoundChannel);

        XmlChannel createdChannel = channelRepository.index(testChannel);

        Optional<XmlChannel> foundChannel = channelRepository.findById(createdChannel.getName());
        // verify the channel was found as expected
        if(foundChannel.isPresent()) {
            assertEquals("Failed to find the channel", createdChannel, foundChannel.get());
        }
        else
            assertTrue("Failed to find the channel", false);
    }

    /**
     * check if a channel exists
     */
    @Test
    public void testChannelExists() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel", "testOwner", testProperties, testTags);
        XmlChannel createdChannel = channelRepository.index(testChannel);
        cleanupTestChannels = Arrays.asList(testChannel);

        // verify the channel exists as expected
        assertTrue("Failed to check the existance of " + testChannel.getName(), channelRepository.existsById(testChannel.getName()));
        // verify the channel does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-channel'", !channelRepository.existsById("non-existant-channel"));
    }

    /**
     * check if channels exist
     */
    @Test
    public void testChannelsExist() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        // verify the channels exist as expected
        assertTrue("Failed to check the existance of channels", channelRepository.existsByIds(Arrays.asList("testChannel", "testChannel1")));
        // verify the channel does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-channel'", !channelRepository.existsByIds(Arrays.asList("test-channel1", "non-existant-channel")));    
    }

    /**
     * find all channels
     */
    @Test
    public void findAllXmlChannels() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Set<XmlChannel> createdChannels = Sets.newHashSet(channelRepository.indexAll(testChannels));
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            //Thread.sleep(2000);
            Set<XmlChannel> listedChannels = Sets.newHashSet(channelRepository.findAll());
            // verify the channel was created as expected
            //assertEquals("Failed to list all created channel", createdChannels, listedChannels);
            assertTrue("Failed to list all created channel", listedChannels.containsAll(createdChannels));
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * find multiple channels
     */
    @Test
    public void findXmlChannels() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        List<String> channelNames = Arrays.asList(testChannel.getName(),testChannel1.getName());
        Iterable<XmlChannel> notFoundChannels= null;
        Iterable<XmlChannel> foundChannels = null;

        try { 
            notFoundChannels = channelRepository.findAllById(channelNames);
        } catch (ResponseStatusException e) { 
        } finally {
            // verify the channels were not found as expected
            assertNotEquals("Found the channels",testChannels,notFoundChannels);           
        }

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            foundChannels = channelRepository.findAllById(channelNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the channels were found as expected
            assertEquals("Failed to find the tags",createdChannels,foundChannels);           
        }
    }

    /**
     * find channels using case insensitive tag and property names searches
     */
    @Test
    public void findChannels() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Iterable<XmlChannel> foundChannels = null;

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            MultiValueMap searchParameters = new LinkedMultiValueMap();
            searchParameters.set(testProperties.get(0).getName().toLowerCase(), "*");
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on property name search (all lower case)", createdChannels, foundChannels);

            searchParameters.set(testProperties.get(0).getName().toUpperCase(), "*");
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on property name search (all upper case)", createdChannels, foundChannels);

            searchParameters.clear();
            searchParameters.set("~tag", testTags.get(0).getName().toLowerCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on tags name search (all lower case)", createdChannels, foundChannels);

            searchParameters.set("~tag", testTags.get(0).getName().toUpperCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on tags name search (all upper case)", createdChannels, foundChannels);

        } catch (ResponseStatusException e) {
        }
    }

    /**
     * find channels using case insensitive names searches
     */
    @Test
    public void findChannelByCaseInsensitiveSearch() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Iterable<XmlChannel> foundChannels = null;

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            MultiValueMap searchParameters = new LinkedMultiValueMap();

            // Search for a single channel
            searchParameters.set("~name", "testChannel");
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (exact)", List.of(testChannel), foundChannels);

            searchParameters.set("~name", "testChannel".toLowerCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all lower case)", List.of(testChannel), foundChannels);

            searchParameters.set("~name", "testChannel".toUpperCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all upper case)", List.of(testChannel), foundChannels);

            // Search for multiple channels using case insensitive name searches
            searchParameters.clear();
            searchParameters.set("~name", "testChannel*");
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (exact)", testChannels, foundChannels);

            searchParameters.set("~name", "testChannel*".toLowerCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all lower case)", testChannels, foundChannels);

            searchParameters.set("~name", "testChannel*".toUpperCase());
            foundChannels = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all upper case)", testChannels, foundChannels);


        } catch (ResponseStatusException e) {
        }
    }
    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel createdChannel = channelRepository.index(testChannel);
        cleanupTestChannels = Arrays.asList(testChannel);

        channelRepository.deleteById(createdChannel.getName());
        // verify the channel was deleted as expected
        assertNotEquals("Failed to delete the channel", testChannel, channelRepository.findById(testChannel.getName()));
    }

    /**
     * Update a channel with
     * 1. additional list of tags and properties
     * 2. update the values of existing properties
     */
    @Test
    public void updateChannelWithTagsAndProperties() {
        XmlChannel testChannel = new XmlChannel();
        testChannel.setName("test-channel1");
        testChannel.setOwner("test-owner");
        cleanupTestChannels = Arrays.asList(testChannel);

        List<XmlProperty> props = createTestProperties(2);
        try {
            testChannel.addProperty(testProperties.get(0));
            testChannel.addTag(testTags.get(0));
            XmlChannel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // update the channel with new tags and properties
            testChannel.setTags(testTags);
            testChannel.setProperties(testProperties);
            XmlChannel updatedChannel = channelRepository.save(testChannel);
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, updatedChannel);
            assertTrue("Failed updated the channel with new tags", testChannel.getTags().containsAll(testTags));
            assertTrue("Failed updated the channel with new properties", testChannel.getProperties().containsAll(testProperties));
            // update the channel with new property values
            testProperties.get(0).setValue("new-value0");
            testProperties.get(1).setValue("new-value1");
            testChannel.setProperties(testProperties);
            XmlChannel updatedValueChannel = channelRepository.save(testChannel);
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, updatedValueChannel);
            assertTrue("Failed updated the channel with new tags", testChannel.getTags().containsAll(testTags));
            assertTrue("Failed updated the channel with new properties", testChannel.getProperties().containsAll(testProperties));
        } catch (Exception e) {}
        props.forEach(testProperty -> {
            propertyRepository.deleteById(testProperty.getName());
});
    }

    /**
     * Update a channel with partial objects, this is needed when you want to add a
     * single tag or property
     */
    @Test
    public void updateChannelWithPartialObjects() {
        XmlChannel testChannel = new XmlChannel();
        testChannel.setName("testChannel");
        testChannel.setOwner("testOwner");
        cleanupTestChannels = Arrays.asList(testChannel);
        
        List<XmlProperty> props = createTestProperties(2);
        try {
            testChannel.addTag(testTags.get(0));
            testChannel.addProperty(testProperties.get(4));
            XmlChannel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // update the channel with new tags and properties provided via partial object

            XmlChannel updateTestChannel = new XmlChannel();
            updateTestChannel.setName("test-update-channel1");
            updateTestChannel.setOwner("test-owner");
            updateTestChannel.addTag(testTags.get(1));
            updateTestChannel.addProperty(testProperties.get(1));
            cleanupTestChannels.add(updateTestChannel);

            XmlChannel updatedChannel = channelRepository.save(updateTestChannel);

            XmlChannel expectedTestChannel = new XmlChannel();
            expectedTestChannel.setName("test-update-channel1");
            expectedTestChannel.setOwner("test-owner");
            expectedTestChannel.addTag(testTags.get(0));
            expectedTestChannel.addTag(testTags.get(1));
            expectedTestChannel.addProperty(testProperties.get(0));
            expectedTestChannel.addProperty(testProperties.get(1));
            assertEquals("Failed to create the test channel with a list of tags & properties", expectedTestChannel, updatedChannel);
        } catch (Exception e) {}
        props.forEach(testProperty -> {
            propertyRepository.deleteById(testProperty.getName());
});
    }



    /**
     * A utility class which will create the requested number of test properties named 'test-property#' 
     * @return list of created properties
     */
    private List<XmlProperty> createTestProperties(int count){
        List<XmlProperty> testProperties = new ArrayList<XmlProperty>();
        for (int i = 0; i < count; i++) {
            XmlProperty testProperty = new XmlProperty();
            testProperty.setName("test-property"+i);
            testProperty.setOwner("test-owner");
            testProperty.setValue("test-property"+i+"-value");
            testProperties.add(testProperty);
        }
        try {
            return Lists.newArrayList(propertyRepository.indexAll(testProperties));
        } catch (Exception e) {
            propertyRepository.deleteAll(testProperties);
            return Collections.emptyList();
        }
    }


    /**
     * A utility class which will create the requested number of test tags named 'test-tag#' 
     * @return list of created tags
     */
    private List<XmlTag> createTestTags(int count){
        List<XmlTag> testTags = new ArrayList<XmlTag>();
        for (int i = 0; i < count; i++) {
            XmlTag testTag = new XmlTag();
            testTag.setName("test-tag"+i);
            testTag.setOwner("test-owner");
            testTags.add(testTag);
        }
        try {
            return Lists.newArrayList(tagRepository.indexAll(testTags));
        } catch (Exception e) {
            tagRepository.deleteAll(testTags);
            return Collections.emptyList();
        }

    }

    // Helper operations to create and clean up the resources needed for successful
    // testing of the channelRepository operations

    private final List<XmlTag> testTags = Arrays.asList(
            new XmlTag("testTag","testOwner"),
            new XmlTag("testTag1","testOwner1"));

    private final List<XmlTag> testUpdatedTags = Arrays.asList(
            new XmlTag("testTag","updateTestOwner"),
            new XmlTag("testTag1","updateTestOwner1"));
    
    private final List<XmlProperty> testProperties = Arrays.asList(
            new XmlProperty("testProperty","testOwner","value"),
            new XmlProperty("testProperty1","testOwner1","value"));

    private final List<XmlProperty> testUpdatedProperties = Arrays.asList(
            new XmlProperty("testProperty","updateTestOwner","updatedValue"),
            new XmlProperty("testProperty1","updateTestOwner1","updatedValue"));

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
        testUpdatedTags.forEach(tag -> {
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
        testUpdatedProperties.forEach(property -> {
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