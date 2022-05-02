package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.ElasticSearchClient;
import org.phoebus.channelfinder.TagRepository;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(TagRepository.class)
public class TagRepositoryIT {

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    TagRepository tagRepository;
    
    @Autowired
    ChannelRepository channelRepository;

    /**
     * index a single tag
     */
    @Test
    public void indexXmlTag() {      
        XmlTag testTag = new XmlTag("testTag","testOwner");
        cleanupTestTags = Arrays.asList(testTag);
        
        XmlTag createdTag = tagRepository.index(testTag);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag", testTag, createdTag);
    }

    /**
     * index multiple tags
     */
    @Test
    public void indexXmlTags() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag testTag1 = new XmlTag("testTag1","testOwner1");    
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);
        cleanupTestTags = testTags;

        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        // verify the tags were created as expected
        assertTrue("Failed to create the list of tags", Iterables.elementsEqual(testTags, createdTags));
    }

    /**
     * save a single tag
     */
    @Test
    public void saveXmlTag() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag updateTestTag = new XmlTag("testTag","updateTestOwner");
        XmlTag updateTestTag1 = new XmlTag("testTag1","updateTestOwner1");
        cleanupTestTags = Arrays.asList(testTag,updateTestTag,updateTestTag1);
        
        XmlTag createdTag = tagRepository.index(testTag);
        XmlTag updatedTestTag = tagRepository.save(updateTestTag);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag with the same name", updateTestTag, updatedTestTag);

        XmlTag updatedTestTag1 = tagRepository.save("testTag",updateTestTag1);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag with a different name", updateTestTag1, updatedTestTag1);
    }

    /**
     * save multiple tags
     */
    @Test
    public void saveXmlTags() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag testTag1 = new XmlTag("testTag1","testOwner1");
        XmlTag updateTestTag = new XmlTag("testTag","updateTestOwner");
        XmlTag updateTestTag1 = new XmlTag("testTag1","updateTestOwner1");    
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);        
        List<XmlTag> updateTestTags = Arrays.asList(updateTestTag, updateTestTag1);     
        cleanupTestTags = updateTestTags;
        
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        Iterable<XmlTag> updatedTestTags = tagRepository.saveAll(updateTestTags);
        // verify the tags were updated as expected
        assertTrue("Failed to update the tags", Iterables.elementsEqual(updateTestTags, updatedTestTags));
    }

    /**
     * find a single tag
     */
    @Test
    public void findXmlTag() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        cleanupTestTags = Arrays.asList(testTag);
        
        Optional<XmlTag> notFoundTag = tagRepository.findById(testTag.getName());
        // verify the tag was not found as expected
        assertNotEquals("Found the tag",testTag,notFoundTag);
        
        XmlTag createdTag = tagRepository.index(testTag);
        Optional<XmlTag> foundTag = tagRepository.findById(createdTag.getName());
        // verify the tag was found as expected
        assertEquals("Failed to find the tag",createdTag,foundTag.get());
        
        XmlChannel channel = new XmlChannel("testChannel","testOwner",null,Arrays.asList(createdTag));
        XmlChannel createdChannel = channelRepository.index(channel);
        
        foundTag = tagRepository.findById(createdTag.getName(),true);
        createdTag.setChannels(Arrays.asList(new XmlChannel(channel.getName(),channel.getOwner())));
        // verify the tag was found as expected
        assertEquals("Failed to find the tag",createdTag,foundTag.get());

        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * check if a tag exists
     */
    @Test
    public void testTagExists() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        cleanupTestTags = Arrays.asList(testTag);

        XmlTag createdTag = tagRepository.index(testTag);
        // verify the tag exists as expected
        assertTrue("Failed to check the existance of " + testTag.getName(), tagRepository.existsById(testTag.getName()));
        // verify the tag does not exist, as expected
        assertTrue("Failed to check the non-existance of 'non-existant-tag'", !tagRepository.existsById("non-existant-tag"));
    }

    /**
     * find all tags
     */
    @Test
    public void findAllXmlTags() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag testTag1 = new XmlTag("testTag1","testOwner1");    
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);
        cleanupTestTags = testTags;
        
        try {
            Set<XmlTag> createdTags = Sets.newHashSet(tagRepository.indexAll(testTags));
            Set<XmlTag> listedTags = Sets.newHashSet(tagRepository.findAll());
            // verify the tag was created as expected
            assertEquals("Failed to list all created tags", createdTags, listedTags);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * find multiple tags
     */
    @Test
    public void findXmlTags() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag testTag1 = new XmlTag("testTag1","testOwner1");    
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        List<String> tagNames = Arrays.asList(testTag.getName(),testTag1.getName());
        Iterable<XmlTag> notFoundTags = null;
        Iterable<XmlTag> foundTags = null;
        cleanupTestTags = testTags;
        
        try {
            notFoundTags = tagRepository.findAllById(tagNames);
        } catch (ResponseStatusException e) {            
        } finally {
            // verify the tags were not found as expected
            assertNotEquals("Found the tags",testTags,notFoundTags);
        }

        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);

        try {
            foundTags = tagRepository.findAllById(tagNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the tags were found as expected
            assertEquals("Failed to find the tags",createdTags,foundTags);
        }
    }

    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        XmlTag testTag = new XmlTag("testTag","testOwner");
        XmlTag createdTag = tagRepository.index(testTag);
        XmlChannel channel = new XmlChannel("testChannel","testOwner",null,Arrays.asList(createdTag));        
        cleanupTestTags = Arrays.asList(testTag);
        
        XmlChannel createdChannel = channelRepository.index(channel);
        tagRepository.deleteById(createdTag.getName());
        // verify the tag was deleted as expected
        assertNotEquals("Failed to delete tag",testTag,tagRepository.findById(testTag.getName()));
        
        XmlChannel foundChannel = channelRepository.findById("testChannel").get();
        // verify the tag was deleted from channels as expected
        assertTrue("Failed to remove tag from channel",foundChannel.getTags().isEmpty());
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag","testChannel");
        List<XmlChannel> chans = channelRepository.search(params);
        // verify the tag was deleted from channels as expected
        assertTrue("Failed to remove tag from channel",chans.isEmpty());
        
        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    // helper operations to clean up tagrepoIT
    
    private List<XmlTag> cleanupTestTags = Collections.emptyList();

    @After
    public void cleanup() {
        // clean up
        cleanupTestTags.forEach(tag -> {
            if (tagRepository.existsById(tag.getName())) {
                tagRepository.deleteById(tag.getName());
            }
        });
    }
}