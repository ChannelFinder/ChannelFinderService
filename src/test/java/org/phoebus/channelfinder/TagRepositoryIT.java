package org.phoebus.channelfinder;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@WebMvcTest(TagRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
public class TagRepositoryIT {

    @Autowired
    ElasticConfig esService;

    @Autowired
    TagRepository tagRepository;
    
    @Autowired
    ChannelRepository channelRepository;

    /**
     * index a single tag
     */
    @Test
    public void indexXmlTag() {      
        Tag testTag = new Tag("testTag","testOwner");
        cleanupTestTags = List.of(testTag);
        
        Tag createdTag = tagRepository.index(testTag);
        // verify the tag was created as expected
        Assertions.assertEquals(testTag, createdTag, "Failed to create the tag");
    }

    /**
     * index multiple tags
     */
    @Test
    public void indexXmlTags() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag testTag1 = new Tag("testTag1","testOwner1");
        List<Tag> testTags = Arrays.asList(testTag, testTag1);
        cleanupTestTags = testTags;

        Iterable<Tag> createdTags = tagRepository.indexAll(testTags);
        // verify the tags were created as expected
        Assertions.assertEquals(testTags, createdTags, "Failed to create the list of tags");
    }

    /**
     * save a single tag
     */
    @Test
    public void saveXmlTag() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag updateTestTag = new Tag("testTag","updateTestOwner");
        Tag updateTestTag1 = new Tag("testTag1","updateTestOwner1");
        cleanupTestTags = Arrays.asList(testTag,updateTestTag,updateTestTag1);
        
        Tag createdTag = tagRepository.index(testTag);
        Tag updatedTestTag = tagRepository.save(updateTestTag);
        // verify the tag was updated as expected
        Assertions.assertEquals(updateTestTag, updatedTestTag, "Failed to update the tag with the same name");

        Tag updatedTestTag1 = tagRepository.save("testTag",updateTestTag1);
        // verify the tag was updated as expected
        Assertions.assertEquals(updateTestTag1, updatedTestTag1, "Failed to update the tag with a different name");
    }

    /**
     * save multiple tags
     */
    @Test
    public void saveXmlTags() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag testTag1 = new Tag("testTag1","testOwner1");
        Tag updateTestTag = new Tag("testTag","updateTestOwner");
        Tag updateTestTag1 = new Tag("testTag1","updateTestOwner1");
        List<Tag> testTags = Arrays.asList(testTag, testTag1);
        List<Tag> updateTestTags = Arrays.asList(updateTestTag, updateTestTag1);
        cleanupTestTags = updateTestTags;
        
        Iterable<Tag> createdTags = tagRepository.indexAll(testTags);
        Iterable<Tag> updatedTestTags = tagRepository.saveAll(updateTestTags);
        // verify the tags were updated as expected
        Assertions.assertEquals(updateTestTags, updatedTestTags, "Failed to update the tags");
    }

    /**
     * find a single tag
     */
    @Test
    public void findXmlTag() {
        Tag testTag = new Tag("testTag","testOwner");
        cleanupTestTags = Arrays.asList(testTag);
        
        Optional<Tag> notFoundTag = tagRepository.findById(testTag.getName());
        // verify the tag was not found as expected
        Assertions.assertTrue(notFoundTag.isEmpty(), "Found the test tag which has not yet been created");
        
        Tag createdTag = tagRepository.index(testTag);
        Optional<Tag> foundTag = tagRepository.findById(createdTag.getName());
        // verify the tag was found as expected
        Assertions.assertEquals(createdTag, foundTag.get(), "Failed to create/find the test tag");

        // Create a channel with the test tag and find a tag with its associated channels
        Channel channel = new Channel("testChannel","testOwner",null,Arrays.asList(createdTag));
        Channel createdChannel = channelRepository.index(channel);
        
        foundTag = tagRepository.findById(createdTag.getName(),true);
        createdTag.setChannels(Arrays.asList(new Channel(channel.getName(),channel.getOwner())));
        // verify the tag was found as expected

        Tag expectedTag = new Tag(createdTag.getName(), createdTag.getOwner());
        expectedTag.setChannels(Arrays.asList(createdChannel));
        Assertions.assertEquals(expectedTag, foundTag.get(), "Failed to find the tag");

        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * check if a tag exists
     */
    @Test
    public void testTagExists() {
        Tag testTag = new Tag("testTag","testOwner");
        cleanupTestTags = Arrays.asList(testTag);

        Tag createdTag = tagRepository.index(testTag);
        // verify the tag exists as expected
        Assertions.assertTrue(tagRepository.existsById(testTag.getName()), "Failed to check the existance of " + testTag.getName());
        // verify the tag does not exist, as expected
        Assertions.assertFalse(tagRepository.existsById("non-existant-tag"), "Failed to check the non-existance of 'non-existant-tag'");
    }

    /**
     * find all tags
     */
    @Test
    public void findAllXmlTags() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag testTag1 = new Tag("testTag1","testOwner1");
        List<Tag> testTags = Arrays.asList(testTag, testTag1);
        cleanupTestTags = testTags;
        
        try {
            Set<Tag> createdTags = Sets.newHashSet(tagRepository.indexAll(testTags));
            Set<Tag> listedTags = Sets.newHashSet(tagRepository.findAll());
            // verify the tag was created as expected
            Assertions.assertEquals(listedTags, createdTags, "Failed to list all created tags");
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * find multiple tags
     */
    @Test
    public void findXmlTags() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag testTag1 = new Tag("testTag1","testOwner1");
        List<Tag> testTags = Arrays.asList(testTag,testTag1);
        List<String> tagNames = Arrays.asList(testTag.getName(),testTag1.getName());
        Iterable<Tag> notFoundTags = null;
        Iterable<Tag> foundTags = null;
        cleanupTestTags = testTags;
        
        try {
            notFoundTags = tagRepository.findAllById(tagNames);
        } catch (ResponseStatusException e) {            
        } finally {
            // verify the tags were not found as expected
            Assertions.assertNotEquals(testTags, notFoundTags, "Found the tags");
        }

        Iterable<Tag> createdTags = tagRepository.indexAll(testTags);

        try {
            foundTags = tagRepository.findAllById(tagNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the tags were found as expected
            Assertions.assertEquals(createdTags, foundTags, "Failed to find the tags");
        }
    }

    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        Tag testTag = new Tag("testTag","testOwner");
        Tag createdTag = tagRepository.index(testTag);
        Channel channel = new Channel("testChannel","testOwner",null,Arrays.asList(createdTag));
        cleanupTestTags = Arrays.asList(testTag);
        
        Channel createdChannel = channelRepository.index(channel);
        tagRepository.deleteById(createdTag.getName());
        // verify the tag was deleted as expected
        Assertions.assertNotEquals(testTag, tagRepository.findById(testTag.getName()), "Failed to delete tag");

        // verify the tag was deleted from channels as expected
        Channel foundChannel = channelRepository.findById("testChannel").get();
        Assertions.assertTrue(foundChannel.getTags().isEmpty(), "Failed to remove tag from channel");

        // verify the tag was deleted from all channels as expected
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag","testChannel");
        List<Channel> chans = channelRepository.search(params).getChannels();
        Assertions.assertTrue(chans.isEmpty(), "Failed to remove tag from channel");
        
        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    // helper operations to clean up tagrepoIT
    
    private List<Tag> cleanupTestTags = Collections.emptyList();

    @AfterEach
    public void cleanup() {
        // clean up
        cleanupTestTags.forEach(tag -> {
            if (tagRepository.existsById(tag.getName())) {
                tagRepository.deleteById(tag.getName());
            }
        });
    }
}