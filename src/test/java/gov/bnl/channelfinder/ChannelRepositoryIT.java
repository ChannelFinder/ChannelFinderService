package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
public class ChannelRepositoryIT {

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    // set up tags
    XmlTag testTag = new XmlTag("testTag","testOwner");
    XmlTag updateTestTag = new XmlTag("testTag","updateTestOwner");
    XmlTag testTag1 = new XmlTag("testTag1","testOwner1");    
    XmlTag updateTestTag1 = new XmlTag("testTag1","updateTestOwner1");
    List<XmlTag> testTags = Arrays.asList(testTag, testTag1);
    // set up props
    XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
    XmlProperty updateTestProperty = new XmlProperty("updateTestProperty","updateTestOwner");
    XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");    
    XmlProperty updateTestProperty1 = new XmlProperty("updateTestProperty1","updateTestOwner1");
    List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);
   
    /**
     * index a single channel
     */
    @Test
    public void indexXmlChannel() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);

        XmlChannel createdChannel = channelRepository.index(testChannel);
        // verify the channel was created as expected
        assertEquals("Failed to create the channel", testChannel, createdChannel);

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * index multiple channels
     */
    @Test
    public void indexXmlChannels() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        // verify the channels were created as expected
        assertTrue("Failed to create the channels", Iterables.elementsEqual(testChannels, createdChannels));

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * save a single channel
     */
    @Test
    public void saveXmlChannel() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel updateTestChannel = new XmlChannel("testChannel","updateTestOwner",testProperties,testTags);
        XmlChannel updateTestChannel1 = new XmlChannel("updateTestChannel1","updateTestOwner1",testProperties,testTags);
        XmlChannel createdChannel = channelRepository.index(testChannel);

        XmlChannel updatedTestChannel = channelRepository.save(updateTestChannel);
        // verify that the channel was updated as expected
        assertEquals("Failed to update the channel with the same name",updateTestChannel,updatedTestChannel);

        XmlChannel updatedTestChannel1 = channelRepository.save("testChannel",updateTestChannel1);
        // verify that the channel was updated as expected
        assertEquals("Failed to update the channel with a different name",updateTestChannel1,updatedTestChannel1);

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        channelRepository.deleteById(updatedTestChannel1.getName());
    }


    /**
     * save multiple channels 
     */
    @Test
    public void saveXmlChannels() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        XmlChannel updateTestChannel = new XmlChannel("testChannel","updateTestOwner",testProperties,testTags);
        XmlChannel updateTestChannel1 = new XmlChannel("testChannel1","updateTestOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        List<XmlChannel> updateTestChannels = Arrays.asList(updateTestChannel,updateTestChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);

        Iterable<XmlChannel> updatedTestChannels = channelRepository.saveAll(updateTestChannels);
        // verify the channels were updated as expected
        assertTrue("Failed to update the channels", Iterables.elementsEqual(updateTestChannels, updatedTestChannels));

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        updatedTestChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * find a single channel
     */
    @Test
    public void findXmlChannel() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);

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
        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        channelRepository.deleteById(createdChannel.getName());
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

        // verify the channel exists as expected
        assertTrue("Failed to check the existance of " + testChannel.getName(), channelRepository.existsById(testChannel.getName()));
        // verify the channel does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-channel'", !channelRepository.existsById("non-existant-channel"));

        // clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * check if channels exist
     */
    @Test
    public void testChannelsExist() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);

        // verify the channels exist as expected
        assertTrue("Failed to check the existance of channels", channelRepository.existsByIds(Arrays.asList("testChannel", "testChannel1")));
        // verify the channel does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-channel'", !channelRepository.existsByIds(Arrays.asList("test-channel1", "non-existant-channel")));
        // clean up
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });     
    }

    /**
     * find all channels
     */
    @Test
    public void findAllXmlChannels() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner1",testProperties,testTags);
        List<XmlChannel> testChannels = Arrays.asList(testChannel, testChannel1);
        Set<XmlChannel> createdChannels = Sets.newHashSet(channelRepository.indexAll(testChannels));

        try {
            //Thread.sleep(2000);
            Set<XmlChannel> listedChannels = Sets.newHashSet(channelRepository.findAll());
            // verify the channel was created as expected
            //assertEquals("Failed to list all created channel", createdChannels, listedChannels);
            assertTrue("Failed to list all created channel", listedChannels.containsAll(createdChannels));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // clean up
            testChannels.forEach(createdChannel -> {
                channelRepository.deleteById(createdChannel.getName());
            });
        }
    }

    /**
     * find multiple channels
     */
    @Test
    public void findXmlChannels() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
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

        try {
            foundChannels = channelRepository.findAllById(channelNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the channels were found as expected
            assertEquals("Failed to find the tags",createdChannels,foundChannels);           
        }

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        }); 
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        }); 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });   
    }
    
    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        XmlChannel testChannel = new XmlChannel("testChannel","testOwner",testProperties,testTags);
        XmlChannel createdChannel = channelRepository.index(testChannel);
        
        channelRepository.deleteById(createdChannel.getName());
        // verify the channel was deleted as expected
        assertNotEquals("Failed to delete the channel", testChannel, channelRepository.findById(testChannel.getName()));

    }

    /**
     * Create a channel with a tag
     */
    @Test
    public void indexChannelWithTag() {
        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag createdTag = tagRepository.index(testTag1);
        testChannel1.addTag(createdTag);
        XmlChannel createdChannel = channelRepository.index(testChannel1);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", createdChannel, testChannel1);
        // verify the tag was created as expected
        //            assertEquals("Failed to list all created tags", createdTags, listedTags);
        // clean up
        channelRepository.deleteById(createdChannel.getName());
        tagRepository.deleteById(createdTag.getName());
    }

    /**
     * Create a channel with a list of tags
     */
    @Test
    public void indexChannelWithTags() {
        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag testTag2 = new XmlTag();
        testTag2.setName("test-tag2");
        testTag2.setOwner("test-owner");

        List<XmlTag> testTags = Arrays.asList(testTag1, testTag2);

        List<XmlTag> createdTags = Lists.newArrayList(tagRepository.indexAll(testTags));

        testChannel1.addTags(createdTags);
        XmlChannel createdChannel = channelRepository.index(testChannel1);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", testChannel1, createdChannel);
        // verify the tag was created as expected

        // clean up
        channelRepository.deleteById(createdChannel.getName());
        testTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        });
    }

    /**
     * Create a channel with a property
     */
    @Test
    public void indexChannelWithProperty() {

        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlProperty testProperty1 = new XmlProperty();
        testProperty1.setName("test-property1");
        testProperty1.setOwner("test-owner");
        testProperty1.setValue("test-property1-value");

        try {
            XmlProperty createdProperty = propertyRepository.index(testProperty1);
            testChannel1.addProperty(createdProperty);
            XmlChannel createdChannel = channelRepository.index(testChannel1);
            // verify the channel was created as expected
            assertEquals("Failed to create the test channel with property", createdChannel, testChannel1);
        } finally {
            // clean up
            channelRepository.deleteById(testChannel1.getName());
            propertyRepository.deleteById(testProperty1.getName());
        }
    }

    /**
     * Create a channel with a list of properties
     */
    @Test
    public void indexChannelWithProperties() {

        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        List<XmlProperty> testProperties = createTestProperties(2);

        try {
            testChannel1.addProperties(testProperties);
            XmlChannel createdChannel = channelRepository.index(testChannel1);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of properties", testChannel1, createdChannel);
            // verify the tag was created as expected
        } finally {
            // clean up
            channelRepository.deleteById(testChannel1.getName());
            testProperties.forEach(testProperty -> {
                propertyRepository.deleteById(testProperty.getName());
            });
        }
    }

    /**
     * Create a channel with a list of tags and properties
     */
    @Test
    public void indexChannelWithTagsAndProperties() {

        XmlChannel testChannel = new XmlChannel();
        testChannel.setName("test-channel1");
        testChannel.setOwner("test-owner");

        List<XmlProperty> testProperties = createTestProperties(2);
        List<XmlTag> testTags = createTestTags(2);
        try {
            testChannel.addProperties(testProperties);
            testChannel.addTags(testTags);
            XmlChannel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // verify the tag was created as expected
        } finally {
            // clean up
            channelRepository.deleteById(testChannel.getName());
            testProperties.forEach(testProperty -> {
                propertyRepository.deleteById(testProperty.getName());
            });

            testTags.forEach(testTag -> {
                tagRepository.deleteById(testTag.getName());
            });
        }
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

        List<XmlProperty> testProperties = createTestProperties(2);
        List<XmlTag> testTags = createTestTags(2);
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

        } finally {
            // clean up
            channelRepository.deleteById(testChannel.getName());
            testProperties.forEach(testProperty -> {
                propertyRepository.deleteById(testProperty.getName());
            });

            testTags.forEach(testTag -> {
                tagRepository.deleteById(testTag.getName());
            });
        }
    }

    /**
     * Update a channel with patial objects, this is needed when you want to add a
     * single tag or property
     */
    @Test
    public void updateChannelWithPartialObjects() {

        XmlChannel testChannel = new XmlChannel();
        testChannel.setName("test-update-channel1");
        testChannel.setOwner("test-owner");

        List<XmlProperty> testProperties = createTestProperties(2);
        List<XmlTag> testTags = createTestTags(2);
        try {
            testChannel.addTag(testTags.get(0));
            testChannel.addProperty(testProperties.get(0));
            XmlChannel createdChannel = channelRepository.index(testChannel);
            // verify the tag was created as expected
            assertEquals("Failed to create the test channel with a list of tags & properties", testChannel, createdChannel);
            // update the channel with new tags and properties provided via partial object

            XmlChannel updateTestChannel = new XmlChannel();
            updateTestChannel.setName("test-update-channel1");
            updateTestChannel.setOwner("test-owner");
            updateTestChannel.addTag(testTags.get(1));
            updateTestChannel.addProperty(testProperties.get(1));

            XmlChannel updatedChannel = channelRepository.save(updateTestChannel);

            XmlChannel expectedTestChannel = new XmlChannel();
            expectedTestChannel.setName("test-update-channel1");
            expectedTestChannel.setOwner("test-owner");
            expectedTestChannel.addTag(testTags.get(0));
            expectedTestChannel.addTag(testTags.get(1));
            expectedTestChannel.addProperty(testProperties.get(0));
            expectedTestChannel.addProperty(testProperties.get(1));
            assertEquals("Failed to create the test channel with a list of tags & properties", expectedTestChannel, updatedChannel);

        } finally {
            // clean up
            channelRepository.deleteById(testChannel.getName());
            testProperties.forEach(testProperty -> {
                propertyRepository.deleteById(testProperty.getName());
            });

            testTags.forEach(testTag -> {
                tagRepository.deleteById(testTag.getName());
            });
        }
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

}
