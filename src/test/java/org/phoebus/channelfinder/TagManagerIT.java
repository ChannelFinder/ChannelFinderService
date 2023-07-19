package org.phoebus.channelfinder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static java.util.Collections.EMPTY_LIST;

@RunWith(SpringRunner.class)
@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
@ContextConfiguration(classes = {TagRepository.class, ElasticConfig.class})
@TestPropertySource(value = "classpath:application_test.properties")
public class TagManagerIT {

    @Autowired
    TagManager tagManager;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ChannelRepository channelRepository;

    private static final Logger logger = Logger.getLogger(TagManagerIT.class.getName());

    /**
     * list all tags
     */
    @Test
    public void listXmlTags() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        List<Tag> testTags = Arrays.asList(testTag0, testTag1);
        Iterable<Tag> createdTags = tagManager.create(testTags);

        Iterable<Tag> tagList = tagManager.list();
        for (Tag tag : createdTags) {
            tag.setChannels(new ArrayList<Channel>());
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
        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        Tag createdTag0 = tagManager.create(testTag0.getName(), testTag0);
        Tag createdTag1 = tagManager.create(testTag1.getName(), testTag1);

        // verify the created tags are read as expected
        // Retrieve the testTag0 without channels
        Tag retrievedTag = tagManager.read(createdTag0.getName(), false);
        assertEquals("Failed to read the tag", createdTag0, retrievedTag);
        // Retrieve the testTag0 with channels
        retrievedTag = tagManager.read(createdTag0.getName(), true);
        assertEquals("Failed to read the tag w/ channels", createdTag0, retrievedTag);

        // Retrieve the testTag1 without channels
        retrievedTag = tagManager.read(createdTag1.getName(), false);
        testTag1.setChannels(new ArrayList<Channel>());
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
        Tag testTag0 = new Tag("testTag0", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0);

        // Create a simple tag
        Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
        assertEquals("Failed to create the tag", testTag0, createdTag);

//        Tag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//        // verify the tag was created as expected
//        assertEquals("Failed to create the tag",testTag1,createdTag1);

        // Update the test tag with a new owner
        Tag updatedTestTag0 = new Tag("testTag0", "updateTestOwner");
        createdTag = tagManager.create(testTag0.getName(), copy(updatedTestTag0));
        assertEquals("Failed to create the tag", updatedTestTag0, createdTag);
    }

    /**
     * Rename a simple tag using create
     */
    @Test
    public void renameByCreateXmlTag() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
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
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0WithChannels);

        Tag createdTag = tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        try {
            Tag foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
            Tag expectedTag = new Tag("testTag0WithChannels", "testOwner");
            expectedTag.setChannels(Arrays.asList(
                    new Channel("testChannel0", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0WithChannels", "testOwner"))),
                    new Channel("testChannel1", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0WithChannels", "testOwner")))));
            assertEquals("Failed to create the tag w/ channels. Expected " + expectedTag.toLog() + " found "
                    + foundTag.toLog(), expectedTag, foundTag);

        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels due to exception " + e.getMessage(), false);
        }

//        createdTag1 = tagManager.create("fakeTag", copy(testTagC1));
//        try {
//            Tag foundTag = tagRepository.findById(testTagC1.getName(), true).get();
//            // verify the tag was created as expected
//            assertTrue("Failed to create the tag w/ channels",tagCompare(testTagC1,foundTag)); 
//        } catch (Exception e) {
//            assertTrue("Failed to create/find the tag w/ channels",false);
//        }

        Tag updatedTestTag0WithChannels = new Tag("testTag0WithChannels", "updateTestOwner");

        createdTag = tagManager.create(testTag0WithChannels.getName(), copy(updatedTestTag0WithChannels));
        try {
            Tag foundTag = tagRepository.findById(updatedTestTag0WithChannels.getName(), true).get();
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
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0WithChannels, testTag1WithChannels);
        
        // Create the testTag0WithChannels
        Tag createdTag = tagManager.create(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        // update the testTag0WithChannels with testTag1WithChannels
        createdTag = tagManager.create(testTag0WithChannels.getName(), copy(testTag1WithChannels));
        try {
            Tag foundTag = tagRepository.findById(testTag1WithChannels.getName(), true).get();
            assertFalse("Failed to rename the Tag - the old tag still exists",
                    tagRepository.existsById("testTag0WithChannels"));
            assertTrue("Failed to rename the Tag - the new tag does not exists",
                    tagRepository.existsById("testTag1WithChannels"));
            assertSame("Failed to rename the Tag - the new tag does have the channels",
                    2, tagRepository.findById("testTag1WithChannels", true).get().getChannels().size());
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

        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        Tag testTag2 = new Tag("testTag2", "testOwner");

        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        Tag testTag2WithChannels = new Tag("testTag2WithChannels", "testOwner");
        testTag2WithChannels.setChannels(testChannels);

        List<Tag> testTags = Arrays.asList(testTag0, testTag1, testTag2, testTag0WithChannels, testTag1WithChannels, testTag2WithChannels);
        cleanupTestTags = testTags;

        tagManager.create(copy(testTags));
        List<Tag> foundTags = new ArrayList<Tag>();
        testTags.forEach(tag -> foundTags.add(tagRepository.findById(tag.getName(),true).get()));
        assertTrue("Failed to create the tags testTag0 ", foundTags.contains(testTag0));
        assertTrue("Failed to create the tags testTag1 ", foundTags.contains(testTag1));
        assertTrue("Failed to create the tags testTag2 ", foundTags.contains(testTag2));
        // Check for creation of tags with channels
        testTags.stream().filter(tag -> tag.getName().endsWith("WithChannels")).forEach(
                (t) -> {
                    Optional<Tag> testTagWithchannels = tagRepository.findById(t.getName(), true);
                    assertTrue("failed to create test tag : " + t.getName(), testTagWithchannels.isPresent());
                    assertSame("failed to create tag with channels : " + t.getName(), 2, t.getChannels().size());
                }
        );
    }

    /**
     * create by overriding multiple tags
     * 
     * attempting to change owners will have no effect
     * changing channels will have an effect
     */
    @Test
    public void createXmlTagsWithOverride() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
        
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0)));
       
        List<Tag> testTags = Arrays.asList(testTag0, testTag0WithChannels);
        cleanupTestTags = testTags;

        // Create a set of original tags to be overriden
        tagManager.create("testTag0", copy(testTag0));
        tagManager.create("testTag0WithChannels", copy(testTag0WithChannels));
        // Now update the test tags
        testTag0.setOwner("testOwner-updated");
        testTag0WithChannels.setOwner("testOwner-updated");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(1)));

        List<Tag> updatedTestTags = Arrays.asList(testTag0, testTag0WithChannels);
        Iterable<Tag> createdTags = tagManager.create(copy(updatedTestTags));
        
        // set owner back to original since it shouldn't change
        testTag0.setOwner("testOwner");
        testTag0WithChannels.setOwner("testOwner");
        // verify the tags were updated as expected
        Optional<Tag> foundTag = tagRepository.findById(testTag0.getName(), true);
        assertTrue("Failed to update tag " + testTag0, foundTag.isPresent() && foundTag.get().equals(testTag0));
        foundTag = tagRepository.findById(testTag0WithChannels.getName(), true);
        assertTrue("Failed to update tag " + testTag0WithChannels,
                foundTag.isPresent() &&
                        foundTag.get().getChannels().size() == 1);

        testTag0WithChannels.setChannels(new ArrayList<Channel>());
        testChannels.get(1).setTags(Arrays.asList(testTag0WithChannels));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag", testTag0WithChannels.getName());
        // verify the tag was removed from the old channels
        assertEquals("Failed to change the channels the tag is attached to correctly",
                Arrays.asList(testChannels.get(1)), channelRepository.search(params).getChannels());
    }
    
    /**
     * add a single tag to a single channel
     */
    @Test
    public void addSingleXmlTag() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
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
        Tag testTag0 = new Tag("testTag0", "testOwner");
        // A test tag with name, owner, and a single test channel
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0, testTag0WithChannels);

        // Update on a non-existing tag should result in the creation of that tag
        // 1. Test a simple tag 
        Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertEquals("Failed to update tag " + testTag0, testTag0, returnedTag);
        assertEquals("Failed to update tag " + testTag0, testTag0, tagRepository.findById(testTag0.getName()).get());
        // 2. Test a tag with channels
        returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels,
                returnedTag.getName().equalsIgnoreCase(testTag0WithChannels.getName()) && returnedTag.getChannels().size() == 1);
        assertSame("Failed to update tag " + testTag0WithChannels,
                1, tagRepository.findById(testTag0WithChannels.getName(), true).get().getChannels().size());

        // Update the tag owner
        testTag0.setOwner("newTestOwner");
        returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertEquals("Failed to update tag " + testTag0, testTag0, returnedTag);
        assertEquals("Failed to update tag " + testTag0, testTag0, tagRepository.findById(testTag0.getName()).get());
        testTag0WithChannels.setOwner("newTestOwner");
        returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
        assertTrue("Failed to update tag " + testTag0WithChannels,
                returnedTag.getName().equalsIgnoreCase(testTag0WithChannels.getName()) &&
                        returnedTag.getChannels().size() == 1);
        Optional<Tag> queriedTag = tagRepository.findById(testTag0WithChannels.getName(), true);
        assertTrue("Failed to update tag " + testTag0WithChannels,
                queriedTag.isPresent() &&
                        queriedTag.get().getName().equalsIgnoreCase(testTag0WithChannels.getName()) &&
                        queriedTag.get().getChannels().size() == 1);
    }
    
    /**
     * update a tag's name and owner on its channels
     */
    @Test
    public void updateXmlTagOnChan() {
        // extra channel for this test
        Channel testChannelX = new Channel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // A test tag with name, owner, and 2 test channels
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0),testChannelX));
        // test tag with different name, owner, and 1 different channel & 1 existing channel
        Tag testTag1WithChannels = new Tag("testTag1WithChannels", "updateTestOwner");
        testTag1WithChannels.setChannels(Arrays.asList(testChannels.get(1),new Channel("testChannelX","testOwner")));
        cleanupTestTags = Arrays.asList(testTag0WithChannels,testTag1WithChannels);
        
        tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        // change name and owner on existing channel, add to new channel
        tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);
        
        Tag expectedTag = new Tag("testTag1WithChannels", "updateTestOwner");
        expectedTag.setChannels(Arrays.asList(
                new Channel("testChannel0", "testOwner"),
                new Channel("testChannel1", "testOwner"),
                new Channel("testChannelX","testOwner")));
        // verify that the old tag "testTag0WithChannels" was replaced with the new "testTag1WithChannels" and lists of channels were combined

        Optional<Tag> foundTag = tagRepository.findById(testTag1WithChannels.getName(), true);
        assertTrue("Failed to update the tag", foundTag.isPresent() &&
                foundTag.get().getName().equalsIgnoreCase("testTag1WithChannels") &&
                foundTag.get().getChannels().size() == 3);

        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0WithChannels.getName()));       
        
        expectedTag = new Tag("testTag1WithChannels", "updateTestOwner");
        // test tag of old channel not in update
        assertTrue("The tag attached to the channel " + testChannels.get(0).toString() + " doesn't match the new tag",
                channelRepository.findById(testChannels.get(0).getName()).get().getTags().contains(expectedTag));
        // test tag of old channel and in update
        assertTrue("The tag attached to the channel " + testChannels.get(1).toString() + " doesn't match the new tag",
                channelRepository.findById(testChannels.get(1).getName()).get().getTags().contains(expectedTag));
        // test tag of new channel
        assertTrue("The tag attached to the channel " + testChannelX.toString() + " doesn't match the new tag",
                channelRepository.findById(testChannelX.getName()).get().getTags().contains(expectedTag));
        
        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }
    
    /**
     * Rename a tag using update
     */
    @Test
    public void renameByUpdateXmlTag() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
        testTag1WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag1, testTag0WithChannels, testTag1WithChannels);

        // Create the original tags
        Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
        Tag createdTagWithChannels = tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
        // update the tags with new names, 0 -> 1
        Tag updatedTag = tagManager.update(testTag0.getName(), testTag1);
        Tag updatedTagWithChannels = tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);

        // verify that the old tag "testTag0" was replaced with the new "testTag1"
        Optional<Tag> foundTag = tagRepository.findById(testTag1.getName());
        assertTrue("Failed to update the tag", foundTag.isPresent());
        // verify that the old tag is no longer present
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag0.getName()));

        // verify that the old tag "testTag0WithChannels" was replaced with the new "testTag1WithChannels"
        foundTag = tagRepository.findById(testTag1WithChannels.getName(), true);
        assertTrue("Failed to update the tag w/ channels", foundTag.isPresent() && foundTag.get().getChannels().size() == 2);
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
        Tag testTag0 = new Tag("testTag0", "testOwner");
        cleanupTestTags = Arrays.asList(testTag0);
        tagManager.create(testTag0.getName(), testTag0);
        // Updating a tag with no channels, the new channels should be added to the tag
        // Add testChannel0 to testTag0 which has no channels 
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertEquals("Failed to update tag " + returnedTag,
                returnedTag,
                tagRepository.findById(testTag0.getName(), true).get());
    }
    
    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0 | update tag channels: testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest2() {
        // A test tag with testChannel0
        Tag testTag0 = new Tag("testTag0", "testOwner");
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0);
        Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
        assertSame("Failed to update tag " + testTag0,
                1, createdTag.getChannels().size());
        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0, the update operation should append the testChannel1 while leaving the existing channel unaffected.
        testTag0.setChannels(Arrays.asList(testChannels.get(1)));
        Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));

        // Query ChannelFinder and verify updated channels and tags
        Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        assertSame("Failed to update tag " + testTag0, 2, foundTag.getChannels().size());
    }

    /**
     * Update the channels associated with a tag.
     * Existing tag channels: testChannel0 | update tag channels: testChannel0,testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest3() {
        // A test tag with testChannel0
        Tag testTag0 = new Tag("testTag0", "testOwner");
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        cleanupTestTags = Arrays.asList(testTag0);

        tagManager.create(testTag0.getName(), testTag0);
        // testTag0 already has testChannel0, the update request (which repeats the testChannel0) should append
        // the testChannel1 while leaving the existing channel unaffected.
        testTag0.setChannels(testChannels);
        tagManager.update(testTag0.getName(), copy(testTag0));

        // Query ChannelFinder and verify updated channels and tags
        Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();

        Tag expectedChannelTag = new Tag("testTag0", "testOwner");
        assertTrue("Failed to update tag " + testTag0.toLog(),
                foundTag.getChannels().containsAll(Arrays.asList(
                        new Channel("testChannel0", "testOwner", Collections.EMPTY_LIST, Arrays.asList(expectedChannelTag)),
                        new Channel("testChannel1", "testOwner", Collections.EMPTY_LIST, Arrays.asList(expectedChannelTag)))));
    }

    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0,testChannel1 | update tag channels: testChannel0,testChannel1 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest4() {
        // A test tag with testChannel0,testChannel1
        Tag testTag0 = new Tag("testTag0", "testOwner");
        testTag0.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0);

        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0 & testChannel1, the update request should be a NOP.
        testTag0.setChannels(testChannels);
        Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertSame("Failed to update tag " + testTag0, 2, returnedTag.getChannels().size());

        // Query ChannelFinder and verify updated channels and tags
        Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();
        Tag expectedTag = tagManager.create(testTag0.getName(), testTag0);
        expectedTag.setChannels(Arrays.asList(
                new Channel("testChannel0", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0", "testOwner"))),
                new Channel("testChannel1", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0", "testOwner")))));
        assertEquals("Failed to update tag " + testTag0, expectedTag, foundTag);
    }

    /**
     * Update the channels associated with a tag
     * Existing tag channels: testChannel0,testChannel1 | update tag channels: testChannel0 
     * Resultant tag channels: testChannel0,testChannel1
     */
    @Test
    public void updateTagTest5() {
        // A test tag with testChannel0,testChannel1
        Tag testTag0 = new Tag("testTag0", "testOwner");
        testTag0.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0);
        Tag expectedTag = tagManager.create(testTag0.getName(), testTag0);

        // Updating a tag with existing channels, the new channels should be added without affecting existing channels
        // testTag0 already has testChannel0 & testChannel1, the update operation should be a NOP.
        testTag0.setChannels(Arrays.asList(testChannels.get(0)));
        Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
        assertSame("Failed to update tag " + testTag0, 2, returnedTag.getChannels().size());
        // Query ChannelFinder and verify updated channels and tags
        Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();

        expectedTag.setChannels(Arrays.asList(new Channel("testChannel0", "testOwner")));
        expectedTag.setChannels(Arrays.asList(
                new Channel("testChannel0", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0", "testOwner"))),
                new Channel("testChannel1", "testOwner", EMPTY_LIST, Arrays.asList(new Tag("testTag0", "testOwner")))));
        assertEquals("Failed to update tag " + testTag0, expectedTag, foundTag);
    }
    
    /**
     * Update multiple tags
     * Update on non-existing tags should result in the creation of the tags
     */
    @Test
    public void updateMultipleTags() {
        // A test tag with only name and owner
        Tag testTag0 = new Tag("testTag0", "testOwner");
        // A test tag with name, owner, and test channels
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag0, testTag0WithChannels);

        Iterable<Tag> returnedTag = tagManager.update(Arrays.asList(copy(testTag0), copy(testTag0WithChannels)));
        // Query ChannelFinder and verify updated channels and tags
        Optional<Tag> foundTag = tagRepository.findById(testTag0.getName(), true);
        assertTrue("Failed to update tag " + testTag0.toLog(),
                foundTag.isPresent() && foundTag.get().equals(testTag0));
        foundTag = tagRepository.findById(testTag0WithChannels.getName(), true);
        assertTrue("Failed to update tag with channels " + testTag0WithChannels.toLog(),
                foundTag.isPresent() && foundTag.get().getChannels().size() == 2);
    }

    /**
     * update tags' names and attempt to change owners on their channels
     */
    @Test
    public void updateMultipleXmlTagsOnChan() {
        // extra channel for this test
        Channel testChannelX = new Channel("testChannelX","testOwner");
        channelRepository.index(testChannelX);

        // 2 test tags with name, owner, and channels
        Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(Arrays.asList(testChannels.get(0),testChannelX));
        Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
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

        // verify that the tags were updated
        Tag foundTag0 = tagRepository.findById(testTag0WithChannels.getName(), true).get();
        assertSame("Failed to update the tag " + testTag0WithChannels.getName(), 3, foundTag0.getChannels().size());
        Tag foundTag1 = tagRepository.findById(testTag1WithChannels.getName(), true).get();
        assertSame("Failed to update the tag " + testTag0WithChannels.getName(), 3, foundTag0.getChannels().size());

        Tag expectedTag0 = new Tag("testTag0WithChannels", "testOwner");
        Tag expectedTag1 = new Tag("testTag1WithChannels", "testOwner");
        List<Tag> expectedTags = Arrays.asList(expectedTag0,expectedTag1);
        // check if tags attached to channels are correct
        // test tag of channel0
        assertEquals("The tags attached to the channel " + testChannels.get(0).toString() + " doesn't match the expected tags",
                expectedTags, channelRepository.findById(testChannels.get(0).getName()).get().getTags());
        // test tag of channel1 (tags need to be sorted because they are not sorted when gotten from channel)
        List<Tag> sortedTags = channelRepository.findById(testChannels.get(1).getName()).get().getTags();
        sortedTags.sort(Comparator.comparing(Tag::getName));
        assertEquals("The tags attached to the channel " + testChannels.get(1).toString() + " doesn't match the expected tags", expectedTags, sortedTags);
        // test tag of channelX
        assertEquals("The tags attached to the channel " + testChannelX.toString() + " doesn't match the expected tags", expectedTags, channelRepository.findById(testChannelX.getName()).get().getTags());
        
    }
    
    /**
     * delete a single tag 
     */
    @Test
    public void deleteXmlTag() {
        Tag testTag0 = new Tag("testTag0", "testOwner");
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        List<Tag> testTags = Arrays.asList(testTag0,testTag1);
        cleanupTestTags = testTags;
        
        Iterable<Tag> createdTags = tagManager.create(testTags);

        tagManager.remove(testTag0.getName());
        // verify the tag was deleted as expected
        assertTrue("Failed to delete the tag", !tagRepository.existsById(testTag0.getName()));

        tagManager.remove(testTag1.getName());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag", testTag1.getName());
        // verify the tag was deleted and removed from all associated channels
        assertTrue("Failed to delete the tag", !tagRepository.existsById(testTag1.getName()));
        assertEquals("Failed to delete the tag from channels",
                new ArrayList<Channel>(), channelRepository.search(params).getChannels());
    }

    /**
     * delete a single tag from a single channel 
     */
    @Test
    public void deleteXmlTagFromChannel() {
        Tag testTag1 = new Tag("testTag1", "testOwner");
        testTag1.setChannels(testChannels);
        cleanupTestTags = Arrays.asList(testTag1);

        Tag createdTag = tagManager.create(testTag1.getName(),testTag1);

        tagManager.removeSingle(testTag1.getName(),testChannels.get(0).getName());
        // verify the tag was only removed from the single test channel
        assertTrue("Failed to not delete the tag", tagRepository.existsById(testTag1.getName()));

        // Verify the tag is removed from the testChannel0
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.add("~tag", testTag1.getName());
        assertFalse("Failed to delete the tag from channel", channelRepository.search(searchParameters).getChannels().stream().anyMatch(ch -> {
            return ch.getName().equals(testChannels.get(0).getName());
        }));
    }

    // A set of test channels populated into channelfinder for test purposes. These
    // channels are added and removed before each test

    private final List<Channel> testChannels = Arrays.asList(
            new Channel("testChannel0", "testOwner"),
            new Channel("testChannel1", "testOwner"));

    private List<Tag> cleanupTestTags = Collections.emptyList();

    @Before
    public void setup() {
        channelRepository.indexAll(testChannels);
    }

    @After
    public void cleanup() {
        // clean up
        testChannels.forEach(channel -> {
            try {
                if(channelRepository.existsById(channel.getName()))
                    channelRepository.deleteById(channel.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to clean up channel: " + channel.getName());
            }
        });
        // additional cleanup for "testChannelX"
        if(channelRepository.existsById("testChannelX"))
            channelRepository.deleteById("testChannelX");
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
    private static boolean tagCompare(Tag tag1, Tag tag2) {
        if(!(tag1.getName().equals(tag2.getName())) || !(tag1.getOwner().equals(tag2.getOwner()))) 
            return false;
        if(!(tag1.getChannels().containsAll(tag2.getChannels())) || !(tag2.getChannels().containsAll(tag1.getChannels())))
            return false;
        return true;
    }

    private static Tag copy(Tag tag) {
        Tag copy = new Tag(tag.getName(),tag.getOwner());
        List<Channel> channels = new ArrayList<Channel>();
        tag.getChannels().forEach(chan -> channels.add(new Channel(chan.getName(),chan.getOwner())));
        copy.setChannels(channels);
        return copy;
    }
    
    private static List<Tag> copy(List<Tag> tags) {
        List<Tag> copy = new ArrayList<Tag>();
        tags.forEach(tag -> copy.add(copy(tag)));
        return copy;
    }
}