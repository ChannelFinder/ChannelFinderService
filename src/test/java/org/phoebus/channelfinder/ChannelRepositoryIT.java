package org.phoebus.channelfinder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
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
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel);
        
        Channel createdChannel = channelRepository.index(testChannel);
        // verify the channel was created as expected
        assertEquals("Failed to create the channel", testChannel, createdChannel);
    }

    /**
     * index multiple channels
     */
    @Test
    public void indexXmlChannels() {
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
        cleanupTestChannels = testChannels;

        Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
        // verify the channels were created as expected
        assertTrue("Failed to create the channels", Iterables.elementsEqual(testChannels, createdChannels));
    }

    /**
     * save a single channel
     */
    @Test
    public void saveXmlChannel() {
        Channel testChannel = new Channel("testChannel","testOwner");
        Channel updateTestChannel =
                new Channel("testChannel","updateTestOwner", testProperties.subList(0,1), testTags.subList(0,1));
        Channel updateTestChannel1 =
                new Channel("testChannel","updateTestOwner", testProperties.subList(1,2), testTags.subList(1,2));
        Channel updateTestChannel2 =
                new Channel("updateTestChannel1","updateTestOwner1",testUpdatedProperties, testUpdatedTags);
        Channel createdChannel = channelRepository.index(testChannel);
        cleanupTestChannels = Arrays.asList(testChannel, updateTestChannel, updateTestChannel1, updateTestChannel2);

        // Update Channel with new owner a new property and a new tag
        Channel updatedTestChannel = channelRepository.save(updateTestChannel);
        // verify that the channel was updated as expected
        assertEquals("Failed to update the channel with the same name", updateTestChannel, updatedTestChannel);

        // Update Channel with a second property and tag
        Channel updatedTestChannel1 = channelRepository.save(updateTestChannel1);
        // verify that the channel was updated with the new tags and properties while preserving the old ones
        Channel expectedChannel = new Channel("testChannel","updateTestOwner");
        expectedChannel.addProperties(testProperties);
        expectedChannel.addTags(testTags);
        assertEquals("Failed to update the channel with the same name", updateTestChannel, updatedTestChannel);
    }


    /**
     * save multiple channels 
     */
    @Test
    public void saveXmlChannels() {
        Channel testChannel = new Channel("testChannel","testOwner", testProperties, testTags);
        Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
        Channel updateTestChannel = new Channel("testChannel", "updateTestOwner", testUpdatedProperties, testUpdatedTags);
        Channel updateTestChannel1 = new Channel("testChannel1", "updateTestOwner1", testUpdatedProperties, testUpdatedTags);
        List<Channel> testChannels = Arrays.asList(testChannel,testChannel1);
        List<Channel> updateTestChannels = Arrays.asList(updateTestChannel,updateTestChannel1);
        Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1,updateTestChannel,updateTestChannel1);

        Iterable<Channel> updatedTestChannels = channelRepository.saveAll(updateTestChannels);
        // verify the channels were updated as expected
        List<Channel> expectedChannels = Arrays.asList(
                new Channel("testChannel","updateTestOwner", testUpdatedProperties, testUpdatedTags),
                new Channel("testChannel1","updateTestOwner1", testUpdatedProperties, testUpdatedTags)
        );
        assertEquals("Failed to update the channels: ",expectedChannels, updatedTestChannels);
    }

    /**
     * find a single channel
     */
    @Test
    public void findXmlChannel() {
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        cleanupTestChannels = Arrays.asList(testChannel);

        Optional<Channel> notFoundChannel = channelRepository.findById(testChannel.getName());
        // verify the channel was not found as expected
        assertNotEquals("Found the channel",testChannel,notFoundChannel);

        Channel createdChannel = channelRepository.index(testChannel);

        Optional<Channel> foundChannel = channelRepository.findById(createdChannel.getName());
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
        Iterable<Tag> createdTags = tagRepository.indexAll(testTags);
        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);
        Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
        Channel createdChannel = channelRepository.index(testChannel);
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
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
        Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        // verify the channels exist as expected
        assertTrue("Failed to check the existance of channels", channelRepository.existsByIds(Arrays.asList("testChannel", "testChannel1")));
        // verify the channel does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-channel'", !channelRepository.existsByIds(Arrays.asList("test-channel1", "non-existant-channel")));    
    }

//    /**
//     * find all channels
//     */
//    @Test
//    public void findAllXmlChannels() {
//        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
//        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
//        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
//        Set<Channel> createdChannels = Sets.newHashSet(channelRepository.indexAll(testChannels));
//        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);
//
//        try {
//            //Thread.sleep(2000);
//            Set<Channel> listedChannels = Sets.newHashSet(channelRepository.findAll());
//            // verify the channel was created as expected
//            //assertEquals("Failed to list all created channel", createdChannels, listedChannels);
//            assertTrue("Failed to list all created channel", listedChannels.containsAll(createdChannels));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * find multiple channels
     */
    @Test
    public void findXmlChannels() {
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
        List<String> channelNames = Arrays.asList(testChannel.getName(),testChannel1.getName());
        Iterable<Channel> notFoundChannels= null;
        Iterable<Channel> foundChannels = null;

        try { 
            notFoundChannels = channelRepository.findAllById(channelNames);
        } catch (ResponseStatusException e) { 
        } finally {
            // verify the channels were not found as expected
            assertNotEquals("Found the channels",testChannels,notFoundChannels);           
        }

        Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
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
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
        ChannelRepository.ResponseSearch foundChannelsResponse = null;

        List<Channel> createdChannels = channelRepository.indexAll(testChannels);
        ChannelRepository.ResponseSearch createdResponseSearch = new ChannelRepository.ResponseSearch(createdChannels, 2);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            MultiValueMap searchParameters = new LinkedMultiValueMap();
            searchParameters.set(testProperties.get(0).getName().toLowerCase(), "*");
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on property name search (all lower case)", createdResponseSearch, foundChannelsResponse);

            searchParameters.set(testProperties.get(0).getName().toUpperCase(), "*");
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on property name search (all upper case)", createdResponseSearch, foundChannelsResponse);

            searchParameters.clear();
            searchParameters.set("~tag", testTags.get(0).getName().toLowerCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on tags name search (all lower case)", createdResponseSearch, foundChannelsResponse);

            searchParameters.set("~tag", testTags.get(0).getName().toUpperCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on tags name search (all upper case)", createdResponseSearch, foundChannelsResponse);

        } catch (ResponseStatusException e) {
        }
    }

    /**
     * find channels using case insensitive names searches
     */
    @Test
    public void findChannelByCaseInsensitiveSearch() {
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel testChannel1 = new Channel("testChannel1","testOwner1",testProperties,testTags);
        List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
        ChannelRepository.ResponseSearch foundChannelsResponse = null;

        List<Channel> createdChannels = channelRepository.indexAll(testChannels);
        ChannelRepository.ResponseSearch createdResponseSearch = new ChannelRepository.ResponseSearch(createdChannels, 2);
        cleanupTestChannels = Arrays.asList(testChannel,testChannel1);

        try {
            MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

            // Search for a single channel
            searchParameters.set("~name", "testChannel");
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (exact)", new ChannelRepository.ResponseSearch(List.of(testChannel), 1), foundChannelsResponse);

            searchParameters.set("~name", "testChannel".toLowerCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all lower case)", new ChannelRepository.ResponseSearch(List.of(testChannel), 1), foundChannelsResponse);

            searchParameters.set("~name", "testChannel".toUpperCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all upper case)", new ChannelRepository.ResponseSearch(List.of(testChannel), 1), foundChannelsResponse);

            // Search for multiple channels using case insensitive name searches
            searchParameters.clear();
            searchParameters.set("~name", "testChannel*");
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (exact)", createdResponseSearch, foundChannelsResponse);

            searchParameters.set("~name", "testChannel*".toLowerCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all lower case)", createdResponseSearch, foundChannelsResponse);

            searchParameters.set("~name", "testChannel*".toUpperCase());
            foundChannelsResponse = channelRepository.search(searchParameters);
            assertEquals("Failed to find the based on channel name search (all upper case)", createdResponseSearch, foundChannelsResponse);


        } catch (ResponseStatusException e) {
        }
    }
    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        Channel testChannel = new Channel("testChannel","testOwner",testProperties,testTags);
        Channel createdChannel = channelRepository.index(testChannel);
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
        Channel testChannel = new Channel();
        testChannel.setName("test-channel1");
        testChannel.setOwner("test-owner");
        cleanupTestChannels = Arrays.asList(testChannel);

        List<Property> props = createTestProperties(2);
        try {
            testChannel.addProperty(testProperties.get(0));
            testChannel.addTag(testTags.get(0));
            Channel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // update the channel with new tags and properties
            testChannel.setTags(testTags);
            testChannel.setProperties(testProperties);
            Channel updatedChannel = channelRepository.save(testChannel);
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, updatedChannel);
            assertTrue("Failed updated the channel with new tags", testChannel.getTags().containsAll(testTags));
            assertTrue("Failed updated the channel with new properties", testChannel.getProperties().containsAll(testProperties));
            // update the channel with new property values
            testProperties.get(0).setValue("new-value0");
            testProperties.get(1).setValue("new-value1");
            testChannel.setProperties(testProperties);
            Channel updatedValueChannel = channelRepository.save(testChannel);
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
        Channel testChannel = new Channel();
        testChannel.setName("testChannel");
        testChannel.setOwner("testOwner");
        cleanupTestChannels = Arrays.asList(testChannel);
        
        List<Property> props = createTestProperties(2);
        try {
            testChannel.addTag(testTags.get(0));
            testChannel.addProperty(testProperties.get(4));
            Channel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // update the channel with new tags and properties provided via partial object

            Channel updateTestChannel = new Channel();
            updateTestChannel.setName("test-update-channel1");
            updateTestChannel.setOwner("test-owner");
            updateTestChannel.addTag(testTags.get(1));
            updateTestChannel.addProperty(testProperties.get(1));
            cleanupTestChannels.add(updateTestChannel);

            Channel updatedChannel = channelRepository.save(updateTestChannel);

            Channel expectedTestChannel = new Channel();
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
    private List<Property> createTestProperties(int count){
        List<Property> testProperties = new ArrayList<Property>();
        for (int i = 0; i < count; i++) {
            Property testProperty = new Property();
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
    private List<Tag> createTestTags(int count){
        List<Tag> testTags = new ArrayList<Tag>();
        for (int i = 0; i < count; i++) {
            Tag testTag = new Tag();
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

    private final List<Tag> testTags = Arrays.asList(
            new Tag("testTag","testOwner"),
            new Tag("testTag1","testOwner1"));

    private final List<Tag> testUpdatedTags = Arrays.asList(
            new Tag("testTag","updateTestOwner"),
            new Tag("testTag1","updateTestOwner1"));
    
    private final List<Property> testProperties = Arrays.asList(
            new Property("testProperty","testOwner","value"),
            new Property("testProperty1","testOwner1","value"));

    private final List<Property> testUpdatedProperties = Arrays.asList(
            new Property("testProperty","updateTestOwner","updatedValue"),
            new Property("testProperty1","updateTestOwner1","updatedValue"));

    private List<Channel> cleanupTestChannels = Collections.emptyList();

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