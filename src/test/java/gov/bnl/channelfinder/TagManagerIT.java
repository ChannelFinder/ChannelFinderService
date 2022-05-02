package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.TagManager;
import org.phoebus.channelfinder.TagRepository;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;

@RunWith(SpringRunner.class)
@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class TagManagerIT {

    @Autowired
    TagManager tagManager;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ChannelRepository channelRepository;

    private static final Logger log = Logger.getLogger(TagManagerIT.class.getName());

    /**
     * list all tags
     */
    @Test
    public void listXmlTags() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        List<XmlTag> testTags = Arrays.asList(testTag0, testTag1);
        Iterable<XmlTag> createdTags = tagManager.create(testTags);

        Iterable<XmlTag> tagList = tagManager.list();
        for (XmlTag tag : createdTags) {
            tag.setChannels(new ArrayList<XmlChannel>());
        }
        // verify the tags were listed as expected
        assertEquals("Failed to list all tags", createdTags, tagList);
    }

    /**
     * read a single tag
     * test the "withChannels" flag
     */
    @Test
    public void readXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        XmlTag createdTag0 = tagManager.create(testTag0.getName(), testTag0);
        XmlTag createdTag1 = tagManager.create(testTag1.getName(), testTag1);

        // verify the created tags are read as expected
        // Retrieve the testTag0 without channels
        XmlTag retrievedTag = tagManager.read(createdTag0.getName(), false);
        assertEquals("Failed to read the tag", createdTag0, retrievedTag);
        // Retrieve the testTag0 with channels
        retrievedTag = tagManager.read(createdTag0.getName(), true);
        assertEquals("Failed to read the tag w/ channels", createdTag0, retrievedTag);

        // Retrieve the testTag1 without channels
        retrievedTag = tagManager.read(createdTag1.getName(), false);
        testTag1.setChannels(new ArrayList<XmlChannel>());
        assertEquals("Failed to read the tag", testTag1, retrievedTag);
        // Retrieve the testTag1 with channels
        retrievedTag = tagManager.read(createdTag1.getName(), true);
        assertEquals("Failed to read the tag w/ channels", createdTag1, retrievedTag);
    }

    /**
     * attempt to read a single non existent tag
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlTag() {
        // verify the tag failed to be read, as expected
        tagManager.read("fakeTag", false);
    }

    /**
     * attempt to read a single non existent tag with channels
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlTag2() {
        // verify the tag failed to be read, as expected
        tagManager.read("fakeTag", true);
    }

    /**
     * create a simple tag
     */
    @Test
    public void createXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0);

        // Create a simple tag
        XmlTag createdTag = tagManager.create(testTag0.getName(), testTag0);
        assertEquals("Failed to create the tag", testTag0, createdTag);

//        XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//        // verify the tag was created as expected
//        assertEquals("Failed to create the tag",testTag1,createdTag1);

        // Update the test tag with a new owner
        XmlTag updatedTestTag0 = new XmlTag("testTag0", "updateTestOwner");
        createdTag = tagManager.create(testTag0.getName(), copy(updatedTestTag0));
        assertEquals("Failed to create the tag", updatedTestTag0, createdTag);
    }

    /**
     * Rename a simple tag using create
     */
    @Test
    public void renameByCreateXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        XmlTag createdTag = tagManager.create(testTag0.getName(), testTag0);
        createdTag = tagManager.create(testTag0.getName(), testTag1);
        // verify that the old tag "testTag0" was replaced with the new "testTag1"
        assertEquals("Failed to create the tag", testTag1, createdTag);
        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0.getName()));
    }

    /**
     * Create a single tag with channels
     */
    @Test
    public void createXmlTag2() {
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0WithChannels);

        XmlTag createdTag = tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        try {
            XmlTag foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
            XmlTag expectedTag = new XmlTag("testTag0WithChannels", "testOwner");
            expectedTag.setChannels(Arrays.asList(
                    new XmlChannel("testChannel0", "testOwner"),
                    new XmlChannel("testChannel1", "testOwner")));
            assertTrue("Failed to create the tag w/ channels. Expected " + expectedTag.toLog() + " found "
                    + foundTag.toLog(), foundTag.equals(expectedTag));

        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels due to exception " + e.getMessage(), false);
        }

//        createdTag1 = tagManager.create("fakeTag", copy(testTagC1));
//        try {
//            XmlTag foundTag = tagRepository.findById(testTagC1.getName(), true).get();
//            // verify the tag was created as expected
//            assertTrue("Failed to create the tag w/ channels",tagCompare(testTagC1,foundTag)); 
//        } catch (Exception e) {
//            assertTrue("Failed to create/find the tag w/ channels",false);
//        }

        XmlTag updatedTestTag0WithChannels = new XmlTag("testTag0WithChannels", "updateTestOwner");

        createdTag = tagManager.create(testTag0WithChannels.getName(), copy(updatedTestTag0WithChannels));
        try {
            XmlTag foundTag = tagRepository.findById(updatedTestTag0WithChannels.getName(), true).get();
            // verify the tag was created as expected
            assertTrue("Failed to create the tag w/ channels", tagCompare(updatedTestTag0WithChannels, foundTag));
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels", false);
        }
    }
    
    /**
     * Rename a single tag with channels using create
     */
    @Test
    public void renameByCreateXmlTag2() {
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0WithChannels, testTag1WithChannels);
        
        // Create the testTag0WithChannels
        XmlTag createdTag = tagManager.create(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        // update the testTag0WithChannels with testTag1WithChannels
        createdTag = tagManager.create(testTag0WithChannels.getName(), copy(testTag1WithChannels));
        try {
            XmlTag foundTag = tagRepository.findById(testTag1WithChannels.getName(), true).get();
            assertTrue("Failed to create the tag w/ channels", tagCompare(testTag1WithChannels, foundTag));
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels", false);
        }
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0WithChannels.getName()));
    }

    /**
     * create multiple tags
     */
    @Test
    public void createXmlTags() {

        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        XmlTag testTag2 = new XmlTag("testTag2", "testOwner");

        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        XmlTag testTag2WithChannels = new XmlTag("testTag2WithChannels", "testOwner");
        testTag2WithChannels.setChannels(testChannels);

        List<XmlTag> testTags = Arrays.asList(testTag0, testTag1, testTag2, testTag0WithChannels, testTag1WithChannels, testTag2WithChannels);
        cleanupTestTags = testTags;

        Iterable<XmlTag> createdTags = tagManager.create(copy(testTags));
        List<XmlTag> foundTags = new ArrayList<XmlTag>();
        testTags.forEach(tag -> foundTags.add(tagRepository.findById(tag.getName(),true).get()));
        assertEquals("Failed to create the tags", testTags, foundTags);
    }

    /**
     * create by overriding multiple tags
     * 
     * attempting to change owners will have no effect
     * changing channels will have an effect
     */
    @Test
    public void createXmlTagsWithOverride() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0)));
       
        List<XmlTag> testTags = Arrays.asList(testTag0, testTag0WithChannels);
        cleanupTestTags = testTags;

        // Create a set of original tags to be overriden
        tagManager.create("testTag0", copy(testTag0));
        tagManager.create("testTag0WithChannels", copy(testTag0WithChannels));
        // Now update the test tags
        testTag0.setOwner("testOwner-updated");
        testTag0WithChannels.setOwner("testOwner-updated");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(1)));

        List<XmlTag> updatedTestTags = Arrays.asList(testTag0, testTag0WithChannels);
        Iterable<XmlTag> createdTags = tagManager.create(copy(updatedTestTags));
        
        // set owner back to original since it shouldn't change
        testTag0.setOwner("testOwner");
        testTag0WithChannels.setOwner("testOwner");        
        // verify the tags were updated as expected    
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0, foundTag.equals(testTag0));
        foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0WithChannels, foundTag.equals(testTag0WithChannels));

        testTag0WithChannels.setChannels(new ArrayList<XmlChannel>());
        testChannels.get(1).setTags(Arrays.asList(testTag0WithChannels));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag", testTag0WithChannels.getName());
        // verify the tag was removed from the old channels
        assertEquals("Failed to change the channels the tag is attached to correctly",
                Arrays.asList(testChannels.get(1)), channelRepository.search(params));    
    }
    
    /**
     * add a single tag to a single channel
     */
    @Test
    public void addSingleXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        tagRepository.index(testTag0);
        cleanupTestTags = Arrays.asList(testTag0);

        tagManager.addSingle(testTag0.getName(), "testChannel0");
        assertTrue("Failed to add tag",
                channelRepository.findById("testChannel0").get().getTags().stream().anyMatch(t -> {
                    return t.getName().equals(testTag0.getName());
                }));
    }

    /**
     * update a tag 
     */
    @Test
    public void updateXmlTag() {
        // A test tag with only name and owner
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        // A test tag with name, owner, and a single test channel
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0, testTag0WithChannels);

        // Update on a non-existing tag should result in the creation of that tag
        // 1. Test a simple tag 
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(testTag0));
        assertTrue("Failed to update tag " + testTag0, testTag0.equals(tagRepository.findById(testTag0.getName()).get()));
        // 2. Test a tag with channels
        returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels, returnedTag.equals(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels, testTag0WithChannels.equals(tagRepository.findById(testTag0WithChannels.getName(), true).get()));

        // Update the tag owner
        testTag0.setOwner("newTestOwner");
        returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(testTag0));
        assertTrue("Failed to update tag " + testTag0, testTag0.equals(tagRepository.findById(testTag0.getName()).get()));
        testTag0WithChannels.setOwner("newTestOwner");
        returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels, returnedTag.equals(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels, testTag0WithChannels.equals(tagRepository.findById(testTag0WithChannels.getName(), true).get()));
    }
    
    /**
     * update a tag's name and owner on its channels
     */
    @Test
    public void updateXmlTagOnChan() {
        // extra channel for this test
        XmlChannel testChannelX = new XmlChannel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // A test tag with name, owner, and 2 test channels
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0),testChannelX));
        // test tag with different name, owner, and 1 different channel & 1 existing channel
        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "updateTestOwner");
        testTag1WithChannels.setChannels(Arrays.asList(testChannels.get(1),new XmlChannel("testChannelX","testOwner")));
        cleanupTestTags = Arrays.asList(testTag0WithChannels,testTag1WithChannels);
        
        tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        // change name and owner on existing channel, add to new channel
        tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);
        
        XmlTag expectedTag = new XmlTag("testTag1WithChannels", "updateTestOwner");
        expectedTag.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner"),
                new XmlChannel("testChannelX","testOwner")));
        // verify that the old tag "testTag0WithChannels" was replaced with the new "testTag1WithChannels" and lists of channels were combined
        try {
            XmlTag foundTag = tagRepository.findById(testTag1WithChannels.getName(),true).get();
            assertTrue("Failed to update the tag", expectedTag.equals(foundTag));
        } catch (Exception e) {
            assertTrue("Failed to update/find the tag", false);
        }        
        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0WithChannels.getName()));       
        
        expectedTag = new XmlTag("testTag1WithChannels", "updateTestOwner");
        // test tag of old channel not in update
        assertTrue("The tag attached to the channel " + testChannels.get(0).toString() + " doesn't match the new tag",channelRepository.findById(testChannels.get(0).getName()).get().getTags().get(0).equals(expectedTag));
        // test tag of old channel and in update
        assertTrue("The tag attached to the channel " + testChannels.get(1).toString() + " doesn't match the new tag",channelRepository.findById(testChannels.get(1).getName()).get().getTags().get(0).equals(expectedTag));
        // test tag of new channel
        assertTrue("The tag attached to the channel " + testChannelX.toString() + " doesn't match the new tag",channelRepository.findById(testChannelX.getName()).get().getTags().get(0).equals(expectedTag));
        
        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }
    
    /**
     * Rename a tag using update
     */
    @Test
    public void renameByUpdateXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1, testTag0WithChannels, testTag1WithChannels);

        // Create the original tags
        XmlTag createdTag = tagManager.create(testTag0.getName(), testTag0);
        XmlTag createdTagWithChannels = tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        // update the tags with new names, 0 -> 1
        XmlTag updatedTag = tagManager.update(testTag0.getName(), testTag1);
        XmlTag updatedTagWithChannels = tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);

        // verify that the old tag "testTag0" was replaced with the new "testTag1"
        try {
            XmlTag foundTag = tagRepository.findById(testTag1.getName()).get();
            assertTrue("Failed to update the tag", tagCompare(updatedTag, foundTag));
        } catch (Exception e) {
            assertTrue("Failed to update/find the tag", false);
        }        
        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0.getName()));       
        
        // verify that the old tag "testTag0WithChannels" was replaced with the new "testTag1WithChannels"
        try {
            XmlTag foundTag = tagRepository.findById(testTag1WithChannels.getName(), true).get();
            assertTrue("Failed to update the tag w/ channels", tagCompare(updatedTagWithChannels, foundTag));
        } catch (Exception e) {
            assertTrue("Failed to update/find the tag w/ channels", false);
        }
        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0WithChannels.getName()));
        
        // TODO add test for failure case
    }

    /**
     * Update the channels associated with a tag
     * Existing tag channels: none | update tag channels: testChannel0 
     * Resultant tag channels: testChannel0
     */
    @Test
    public void updateTagTest1() {
        // A test tag with only name and owner
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0);
        tagManager.create(testTag0.getName(), testTag0);
        // Updating a tag with no channels, the new channels should be added to the tag
        // Add testChannel0 to testTag0 which has no channels 
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(testTag0));
        assertTrue("Failed to update tag " + testTag0, testTag0.equals(tagRepository.findById(testTag0.getName(), true).get()));
    }
    
    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0 | update tag channels: testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest2() {
        // A test tag with testChannel0
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0);
        tagManager.create(testTag0.getName(), testTag0);
        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0, the update operation should append the testChannel1 while leaving the existing channel unaffected.
        testTag0.setChannels(Arrays.asList(testChannels.get(1)));
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(testTag0));

        // Query ChannelFinder and verify updated channels and tags
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        XmlTag expectedTag = new XmlTag("testTag0", "testOwner");
        expectedTag.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner")));
        assertTrue("Failed to update tag " + testTag0, foundTag.equals(expectedTag));
    }

    /**
     * Update the channels associated with a tag.
     * Existing tag channels: testChannel0 | update tag channels: testChannel0,testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest3() {
        // A test tag with testChannel0
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0);

        XmlTag expectedTag = tagManager.create(testTag0.getName(), testTag0);
        expectedTag.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner")));

        // testTag0 already has testChannel0, the update request (which repeats the testChannel0) should append the testChannel1 while leaving the existing channel unaffected.
        testTag0.setChannels(testChannels);
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0.toLog(), returnedTag.equals(expectedTag));

        // Query ChannelFinder and verify updated channels and tags
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0.toLog(), foundTag.equals(expectedTag));
    }

    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0,testChannel1 | update tag channels: testChannel0,testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest4() {
        // A test tag with testChannel0,testChannel1
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        testTag0.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0);

        XmlTag expectedTag = tagManager.create(testTag0.getName(), testTag0);
        expectedTag.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner")));

        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0 & testChannel1, the update request should be a NOP.
        testTag0.setChannels(testChannels);
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(expectedTag));

        // Query ChannelFinder and verify updated channels and tags
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0, foundTag.equals(expectedTag));
    }

    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0,testChannel1 | update tag channels: testChannel0 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest5() {
        // A test tag with testChannel0,testChannel1
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        testTag0.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0);
        XmlTag expectedTag = tagManager.create(testTag0.getName(), testTag0);
        expectedTag.setChannels(Arrays.asList(new XmlChannel("testChannel0", "testOwner")));

        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0 & testChannel1, the update operation should be a NOP.
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        XmlTag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertTrue("Failed to update tag " + testTag0, returnedTag.equals(expectedTag));
        // Query ChannelFinder and verify updated channels and tags
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        expectedTag.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner")));
        assertTrue("Failed to update tag " + testTag0, foundTag.equals(expectedTag));
    }
    
    /**
     * Update multiple tags
     * Update on non-existing tags should result in the creation of the tags
     */
    @Test
    public void updateMultipleTags() {
        // A test tag with only name and owner
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        // A test tag with name, owner, and test channels
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag0WithChannels);

        Iterable<XmlTag> returnedTag = tagManager.update(Arrays.asList(copy(testTag0), copy(testTag0WithChannels)));
        // Query ChannelFinder and verify updated channels and tags
        XmlTag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0, foundTag.equals(testTag0));
        foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
        assertTrue("Failed to update tag " + testTag0WithChannels, foundTag.equals(testTag0WithChannels));
    }

    /**
     * update tags' names and attempt to change owners on their channels
     */
    @Test
    public void updateMultipleXmlTagsOnChan() {
        // extra channel for this test
        XmlChannel testChannelX = new XmlChannel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // 2 test tags with name, owner, and channels
        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0),testChannelX));
        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(Arrays.asList(testChannels.get(1),testChannelX));
        cleanupTestTags = Arrays.asList(testTag0WithChannels,testTag1WithChannels);
        
        tagManager.create(Arrays.asList(testTag0WithChannels,testTag1WithChannels));
        // change owners and add channels
        testTag0WithChannels.setOwner("updateTestOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(1),testChannelX));
        testTag1WithChannels.setOwner("updateTestOwner");
        testTag1WithChannels.setChannels(Arrays.asList(testChannels.get(0),testChannelX));
        
        // update both tags
        tagManager.update(Arrays.asList(testTag0WithChannels,testTag1WithChannels));
        // create expected tags
        XmlTag expectedTag0 = new XmlTag("testTag0WithChannels", "testOwner");
        expectedTag0.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner"),
                new XmlChannel("testChannelX","testOwner")));
        XmlTag expectedTag1 = new XmlTag("testTag1WithChannels", "testOwner");
        expectedTag1.setChannels(Arrays.asList(
                new XmlChannel("testChannel0", "testOwner"),
                new XmlChannel("testChannel1", "testOwner"),
                new XmlChannel("testChannelX","testOwner")));
        
        // verify that the tags were updated
        try {
            XmlTag foundTag0 = tagRepository.findById(testTag0WithChannels.getName(),true).get();
            assertTrue("Failed to update the tag " + expectedTag0.toString(), expectedTag0.equals(foundTag0));
        } catch (Exception e) {
            assertTrue("Failed to update/find the tag " + expectedTag0.toString(), false);
        }  
        try {
            XmlTag foundTag1 = tagRepository.findById(testTag1WithChannels.getName(),true).get();
            assertTrue("Failed to update the tag " + expectedTag1.toString(), expectedTag1.equals(foundTag1));
        } catch (Exception e) {
            assertTrue("Failed to update/find the tag " + expectedTag1.toString(), false);
        }  
        
        expectedTag0 = new XmlTag("testTag0WithChannels", "testOwner");
        expectedTag1 = new XmlTag("testTag1WithChannels", "testOwner");
        List<XmlTag> expectedTags = Arrays.asList(expectedTag0,expectedTag1);
        // check if tags attached to channels are correct
        // test tag of channel0
        channelRepository.findById(testChannels.get(0).getName()).get().getTags();
        assertTrue("The tags attached to the channel " + testChannels.get(0).toString() + " doesn't match the expected tags",channelRepository.findById(testChannels.get(0).getName()).get().getTags().equals(expectedTags));
        // test tag of channel1 (tags need to be sorted because they are not sorted when gotten from channel)
        List<XmlTag> sortedTags = channelRepository.findById(testChannels.get(1).getName()).get().getTags();
        sortedTags.sort(Comparator.comparing(XmlTag:: getName));
        assertTrue("The tags attached to the channel " + testChannels.get(1).toString() + " doesn't match the expected tags",sortedTags.equals(expectedTags));
        // test tag of channelX
        assertTrue("The tags attached to the channel " + testChannelX.toString() + " doesn't match the expected tags",channelRepository.findById(testChannelX.getName()).get().getTags().equals(expectedTags));
        
        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }
    
    /**
     * delete a single tag 
     */
    @Test
    public void deleteXmlTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag0,testTag1);
        cleanupTestTags = testTags;
        
        Iterable<XmlTag> createdTags = tagManager.create(testTags);

        tagManager.remove(testTag0.getName());
        // verify the tag was deleted as expected
        assertTrue("Failed to delete the tag", !tagRepository.existsById(testTag0.getName()));

        tagManager.remove(testTag1.getName());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag", testTag1.getName());
        // verify the tag was deleted and removed from all associated channels
        assertTrue("Failed to delete the tag", !tagRepository.existsById(testTag1.getName()));
        assertEquals("Failed to delete the tag from channels",
                new ArrayList<XmlChannel>(), channelRepository.search(params));
    }

    /**
     * delete a single tag from a single channel 
     */
    @Test
    public void deleteXmlTagFromChannel() {
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag1);

        XmlTag createdTag = tagManager.create(testTag1.getName(),testTag1);

        tagManager.removeSingle(testTag1.getName(),testChannels.get(0).getName());
        // verify the tag was only removed from the single test channel
        assertTrue("Failed to not delete the tag", tagRepository.existsById(testTag1.getName()));

        // Verify the tag is removed from the testChannel0
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.add("~tag", testTag1.getName());
        assertFalse("Failed to delete the tag from channel", channelRepository.search(searchParameters).stream().anyMatch(ch -> {
            return ch.getName().equals(testChannels.get(0).getName());
        }));
    }

    // A set of test channels populated into channelfinder for test purposes. These
    // channels are added and removed before each test

    private final List<XmlChannel> testChannels = Arrays.asList(
            new XmlChannel("testChannel0", "testOwner"),
            new XmlChannel("testChannel1", "testOwner"));

    private List<XmlTag> cleanupTestTags = Collections.emptyList();

    @Before
    public void setup() {
        channelRepository.indexAll(testChannels);
    }

    @After
    public void cleanup() {
        // clean up
        testChannels.forEach(channel -> {
            try {
                channelRepository.deleteById(channel.getName());
            } catch (Exception e) {
                log.warning("Failed to clean up channel: " + channel.getName());
            }
        });
        cleanupTestTags.forEach(tag -> {
            if (tagRepository.existsById(tag.getName())) {
                tagRepository.deleteById(tag.getName());
            }
        });
    }

    /**
     * Compare the two tags ignoring the order of the associated channels
     * @param tag1
     * @param tag2
     * @return true is the tags match
     */
    private static boolean tagCompare(XmlTag tag1, XmlTag tag2) {
        if(!(tag1.getName().equals(tag2.getName())) || !(tag1.getOwner().equals(tag2.getOwner()))) 
            return false;
        if(!(tag1.getChannels().containsAll(tag2.getChannels())) || !(tag2.getChannels().containsAll(tag1.getChannels())))
            return false;
        return true;
    }

    private static XmlTag copy(XmlTag tag) {
        XmlTag copy = new XmlTag(tag.getName(),tag.getOwner());
        List<XmlChannel> channels = new ArrayList<XmlChannel>();
        tag.getChannels().forEach(chan -> channels.add(new XmlChannel(chan.getName(),chan.getOwner())));
        copy.setChannels(channels);
        return copy;
    }
    
    private static List<XmlTag> copy(List<XmlTag> tags) {
        List<XmlTag> copy = new ArrayList<XmlTag>();
        tags.forEach(tag -> copy.add(copy(tag)));
        return copy;
    }
}