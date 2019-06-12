package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(TagRepository.class)
@WithMockUser(roles = "CF-ADMINS")
public class TagRepositoryIT {

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ChannelRepository channelRepository;

    // set up
    XmlTag testTag = new XmlTag("testTag","testOwner");
    XmlTag updateTestTag = new XmlTag("testTag","updateTestOwner");
    XmlTag testTag1 = new XmlTag("testTag1","testOwner1");    
    XmlTag updateTestTag1 = new XmlTag("testTag1","updateTestOwner1");
    XmlChannel testChannel1 = new XmlChannel();
    XmlChannel testChannel2 = new XmlChannel();

    /**
     * index a single tag
     */
    @Test
    public void indexXmlTag() {
        XmlTag createdTag = tagRepository.index(testTag);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag", testTag, createdTag);

        // clean up
        tagRepository.deleteById(createdTag.getName());
    }

    /**
     * index multiple tags
     */
    @Test
    public void indexXmlTags() {
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);

        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        // verify the tags were created as expected
        assertTrue("Failed to create the list of tags", Iterables.elementsEqual(testTags, createdTags));

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        });       
    }

    /**
     * save a single tag
     */
    @Test
    public void saveXmlTag() {
        XmlTag createdTag = tagRepository.index(testTag);

        XmlTag updatedTestTag = tagRepository.save(updateTestTag);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag", updateTestTag, updatedTestTag);

        // clean up
        tagRepository.deleteById(updatedTestTag.getName());
    }

    /**
     * save multiple tags
     */
    @Test
    public void saveXmlTags() {
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);        
        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
        List<XmlTag> updateTestTags = Arrays.asList(testTag, testTag1);        

        Iterable<XmlTag> updatedTestTags = tagRepository.saveAll(updateTestTags);
        // verify the tags were updated as expected
        assertTrue("Failed to update the tags", Iterables.elementsEqual(updateTestTags, updatedTestTags));

        // clean up
        updatedTestTags.forEach(updatedTestTag -> {
            tagRepository.deleteById(updatedTestTag.getName());
        });
    }

    /**
     * find a single tag
     */
    @Test
    public void findXmlTag() {
        Optional<XmlTag> notFoundTag = tagRepository.findById(testTag.getName());
        // verify the tag was not found as expected
        assertNotEquals("Found the tag",testTag,notFoundTag);

        XmlTag createdTag = tagRepository.index(testTag);

        Optional<XmlTag> foundTag = tagRepository.findById(createdTag.getName());
        // verify the tag was found as expected
        assertEquals("Failed to find the tag",createdTag,foundTag.get());

        // clean up
        tagRepository.deleteById(createdTag.getName());
    }

    /**
     * check if a tag exists
     */
    @Test
    public void testTagsExist() {
        XmlTag createdTag = tagRepository.index(testTag);

        // verify the tag exists as expected
        assertTrue("Failed to check the existance of " + testTag.getName(), tagRepository.existsById(testTag.getName()));
        // verify the tag does not exist as expected
        assertTrue("Failed to check the existance of 'non-existant-tag'", !tagRepository.existsById("non-existant-tag"));

        // clean up
        tagRepository.deleteById(createdTag.getName());      
    }

    /**
     * find all tags
     */
    @Test
    public void findAllXmlTags() {
        List<XmlTag> testTags = Arrays.asList(testTag, testTag1);
        try {
            Set<XmlTag> createdTags = Sets.newHashSet(tagRepository.indexAll(testTags));
            Thread.sleep(2000);
            Set<XmlTag> listedTags = Sets.newHashSet(tagRepository.findAll());
            // verify the tag was created as expected
            assertEquals("Failed to list all created tags", createdTags, listedTags);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // clean up
            testTags.forEach(createdTag -> {
                tagRepository.deleteById(createdTag.getName());
            });
        }
    }

    /**
     * find multiple tags
     */
    @Test
    public void findXmlTags() {
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        List<String> tagNames = Arrays.asList(testTag.getName(),testTag1.getName());
        Iterable<XmlTag> notFoundTags = tagRepository.findAllById(tagNames);
        // verify the tag was not found as expected
        assertNotEquals("Found the tags",testTags,notFoundTags);

        Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);

        Iterable<XmlTag> foundTags = tagRepository.findAllById(tagNames);
        // verify the tag was found as expected
        assertEquals("Failed to find the tags",createdTags,foundTags);

        // clean up
        createdTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        });
    }

    /**
     * delete a single tag
     */
    @Test
    public void deleteXmlTag() {
        XmlTag createdTag = tagRepository.index(testTag);

        tagRepository.deleteById(createdTag.getName());
        // verify the tag was deleted as expected
        assertNotEquals("Failed to delete tag",testTag,tagRepository.findById(testTag.getName()));
    }

}
