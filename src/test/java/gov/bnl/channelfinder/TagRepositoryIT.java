package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TagRepository.class, ElasticSearchClient.class })
public class TagRepositoryIT {

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    TagRepository tagRepository;

    /**
     * A simple test to index a single tag
     */
    @Test
    public void indexXmlTag() {

        XmlTag testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");

        XmlTag createdTag = tagRepository.index(testTag);
        // verify the tag was created as expected
        assertEquals("Failed to create the test tag", testTag, createdTag);

        // clean up
        tagRepository.deleteById(createdTag.getName());

    }

    /**
     * A test to index a multiple tags
     */
    @Test
    public void indexXmlTags() {

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag testTag2 = new XmlTag();
        testTag2.setName("test-tag2");
        testTag2.setOwner("test-owner");

        List<XmlTag> testTags = Arrays.asList(testTag1, testTag2);

        try {
            Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);
            // verify the tag was created as expected
            assertTrue("Failed to create a list of tags", Iterables.elementsEqual(testTags, createdTags));

        } finally {
            // clean up
            testTags.forEach(createdTag -> {
                tagRepository.deleteById(createdTag.getName());
            });
        }
    }


    /**
     * Test is the requested tag exists
     */
    @Test
    public void testTagsExist() {

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag testTag2 = new XmlTag();
        testTag2.setName("test-tag2");
        testTag2.setOwner("test-owner");

        List<XmlTag> testTags = Arrays.asList(testTag1, testTag2);

        try {
            Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);

            // Test if created tags exist
            assertTrue("Failed to check the existance of " + testTag1.getName(), tagRepository.existsById(testTag1.getName()));
            assertTrue("Failed to check the existance of " + testTag2.getName(), tagRepository.existsById(testTag2.getName()));
            // Test the check for existance of a non existant tag returns false
            assertTrue("Failed to check the existance of 'non-existant-tag'", !tagRepository.existsById("non-existant-tag"));
        } finally {
            // clean up
            testTags.forEach(createdTag -> {
                tagRepository.deleteById(createdTag.getName());
            });
        }
    }

    /**
     * A test to index a multiple tags
     */
    @Test
    public void listAllXmlTags() {

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag testTag2 = new XmlTag();
        testTag2.setName("test-tag2");
        testTag2.setOwner("test-owner");

        List<XmlTag> testTags = Arrays.asList(testTag1, testTag2);
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
     * TODO A simple test to index a single tag with a few channels
     */
    @Test
    public void indexXmlTagWithChannels() {

    }
}
