package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    // set up

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
        XmlTag retrivedTag = tagManager.read(createdTag0.getName(), false);
        assertEquals("Failed to read the tag", createdTag0, retrivedTag);
        // Retrieve the testTag0 with channels
        retrivedTag = tagManager.read(createdTag0.getName(), true);
        assertEquals("Failed to read the tag w/ channels", createdTag0, retrivedTag);

        // Retrieve the testTag1 without channels
        retrivedTag = tagManager.read(createdTag1.getName(), false);
        testTag1.setChannels(new ArrayList<XmlChannel>());
        assertEquals("Failed to read the tag", testTag1, retrivedTag);
        // Retrieve the testTag1 with channels
        retrivedTag = tagManager.read(createdTag1.getName(), true);
        assertEquals("Failed to read the tag w/ channels", createdTag1, retrivedTag);
    }

    /**
     * attempt to read a single non existent tag
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlTag() {
        // verify the tag failed to be read, as expected
        tagManager.read("fakeTag", false);
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
     * Rename a simple tag
     */
    @Test
    public void renameTag() {
        XmlTag testTag0 = new XmlTag("testTag0", "testOwner");
        XmlTag testTag1 = new XmlTag("testTag1", "testOwner");

        cleanupTestTags = Arrays.asList(testTag0, testTag1);

        XmlTag createdTag = tagManager.create(testTag0.getName(), testTag1);
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

        XmlTag createdTag = tagManager.create(testTag0WithChannels.getName(), copy(testTag0WithChannels));

        try {
            XmlTag foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
            assertTrue("Failed to create the tag w/ channels", tagCompare(testTag0WithChannels, foundTag));
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels", false);
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
     * Rename a single tag with channels
     */
    @Test
    public void renameXmlTag2() {

        XmlTag testTag0WithChannels = new XmlTag("testTag0WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);

        XmlTag testTag1WithChannels = new XmlTag("testTag1WithChannels", "testOwner");
        testTag0WithChannels.setChannels(testChannels);

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

        Iterable<XmlTag> createdTags = tagManager.create((testTags));
        // verify the tags were created as expected
        assertTrue("Failed to create the tags", Iterables.elementsEqual(testTags, createdTags));
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
        // verify the tag was added as expected
        assertTrue("Failed to add tag",
                channelRepository.findById("testChannel0").get().getTags().stream().anyMatch(t -> {
                    return t.getName().equals(testTag0.getName());
                }));
    }
//
//    /**
//     * update a tag 
//     */
//    @Test
//    public void updateXmlTag() {
//        testTagC.setChannels(testChannels);
//        testTagC1.setChannels(Arrays.asList(testChannels.get(0)));;
//        testTagC2.setChannels(Arrays.asList(testChannels.get(1)));
//        updateTestTagC.setChannels(testChannels);
//        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);
//
//        XmlTag returnedTag = tagManager.update(testTag.getName(), copy(testTag));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,testTag,testTag));
//
//        XmlTag returnedTag1 = tagManager.update("fakeTag", copy(testTag1));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag1,testTag1,testTag1));
//
//        returnedTag = tagManager.update(testTag.getName(), copy(updateTestTag));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTag,updateTestTag));
//
//        XmlTag updatedTag2 = tagManager.update(testTag.getName(), copy(testTag2));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag2,testTag2,testTag2));
//        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag.getName()));
//
//        returnedTag = tagManager.update(testTagC.getName(), copy(testTagC));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,testTagC,testTagC));
//
//        returnedTag1 = tagManager.update("fakeTag", copy(testTagC1));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag1,testTagC1,testTagC1));
//
//        // 0 -> -
//        testTagC.setChannels(Arrays.asList(testChannels.get(0)));
//        channelRepository.indexAll(testChannels);
//        tagManager.create(testTagC.getName(),copy(testTagC));
//        updateTestTagC.setChannels(new ArrayList<XmlChannel>());
//        returnedTag = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        XmlTag result = new XmlTag(updateTestTagC.getName(),updateTestTagC.getOwner());
//        result.setChannels(Arrays.asList(testChannels.get(0)));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTagC,result));
//
//        // 0 -> 1
//        updateTestTagC.setChannels(Arrays.asList(testChannels.get(1)));
//        returnedTag = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        result = new XmlTag(updateTestTagC.getName(),updateTestTagC.getOwner());
//        result.setChannels(testChannels);
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTagC,result));
//
//        // _ -> 1
//        testTagC.setChannels(new ArrayList<XmlChannel>());
//        tagRepository.index(copy(testTagC));
//        channelRepository.indexAll(testChannels);
//        updateTestTagC.setChannels(Arrays.asList(testChannels.get(1)));
//        returnedTag = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTagC,updateTestTagC));
//
//        // 1 -> 0,1
//        updateTestTagC.setChannels(testChannels);
//        returnedTag = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTagC,updateTestTagC));
//
//        // 0,1 -> 0,1
//        returnedTag = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag,updateTestTagC,updateTestTagC));
//
//        // 0,1 -> 0
//        updateTestTagC.setChannels(Arrays.asList(testChannels.get(0)));
//        returnedTag1 = tagManager.update(testTagC.getName(), copy(updateTestTagC));
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(returnedTag1,updateTestTagC,returnedTag));
//
//        updatedTag2 = tagManager.update(testTagC.getName(), copy(testTagC2));
//        result = new XmlTag(testTagC2.getName(),testTagC2.getOwner());
//        result.setChannels(testChannels);
//        // verify the tag was updated as expected
//        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag2,testTagC2,result));
//        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTagC.getName()));
//    }
//
//    /**
//     * update multiple tags 
//     */
//    @Test
//    public void updateXmlTags() {
//        testTagC.setChannels(testChannels);
//        testTagC1.setChannels(Arrays.asList(testChannels.get(0)));
//        testTagC2.setChannels(Arrays.asList(testChannels.get(0)));
//        updateTestTagC.setChannels(testChannels);
//        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);
//
//        List<XmlTag> tags = Arrays.asList(testTag,testTagC);
//        tagManager.create(copy(tags));
//
//        List<XmlTag> tagsRequest = Arrays.asList(updateTestTag,testTag1,updateTestTagC,testTagC1);
//        List<XmlTag> expectedTags = copy(tagsRequest);
//        Iterable<XmlTag> returnedTags = tagManager.update((tagsRequest));
//        // verify the tags were updated as expected
//        assertTrue("Failed to update the tag",updatedTagsCorrectly(returnedTags,tagsRequest,expectedTags));
//
//        updateTestTag.setChannels(Arrays.asList(testChannels.get(0)));
//        tagManager.update(updateTestTag.getName(),copy(updateTestTag));
//        updateTestTag.setChannels(new ArrayList<XmlChannel>());
//        
//        testTag1.setChannels(Arrays.asList(testChannels.get(0)));
//        
//        testTagC2.setChannels(Arrays.asList(testChannels.get(0)));
//        tagManager.create(testTagC2.getName(),testTagC2);
//        testTagC2.setChannels(Arrays.asList(testChannels.get(1)));
//        
//        testTag2.setChannels(testChannels);
//        tagManager.update(testTag2.getName(),copy(testTag2));
//        
//        testTagC1.setChannels(testChannels);
//        
//
//        tags = Arrays.asList(updateTestTagC,updateTestTag,testTag1,testTagC2,testTag2,testTagC1);
//        tagsRequest = copy(tags);  
//        returnedTags = tagManager.update((tagsRequest));
//
//        updateTestTag.setChannels(Arrays.asList(testChannels.get(0)));
//        testTagC2.setChannels(testChannels);
//        testTag2.setChannels(testChannels);
//        expectedTags = copy(tags);  
//        // verify the tags were updated as expected
//        assertTrue("Failed to update the tag",updatedTagsCorrectly(returnedTags,tagsRequest,expectedTags));
//    }

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
//
//    /**
//     * validate a tag request
//     */
//    @Test
//    public void validateXmlTagRequest() {
//        XmlChannel createdChannel = channelRepository.index(testChannels.get(0));
//        testTag1.setChannels(Arrays.asList(testChannels.get(0)));
//
//        try {
//            // verify the tag request is valid
//            tagManager.validateTagRequest(createdChannel.getName());
//            assertTrue("",true);
//        } catch (Exception e) {
//            assertTrue(e.getMessage(),false);
//        }
//
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest("fakeChannel");
//            assertTrue("Validated an invalid channel",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        try {
//            // verify the tag request is valid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("",true);
//        } catch (Exception e) {
//            assertTrue(e.getMessage(),false);
//        }
//
//        try {
//            // verify the tag request is valid
//            tagManager.validateTagRequest(testTag1);
//            assertTrue("",true);
//        } catch (Exception e) {
//            assertTrue(e.getMessage(),false);
//        }
//
//        testTag.setName(null);
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid tag name",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setName("");
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid tag name",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setName("testTag");
//        testTag.setOwner(null);
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid tag owner",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setOwner("");
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid tag owner",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setOwner("tagOwner");
//        XmlChannel chan = new XmlChannel("fakeChan","fakeOwner");
//        testTag.setChannels(Arrays.asList(chan));
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid channel",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setChannels(Arrays.asList(testChannels.get(0),chan));
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(testTag);
//            assertTrue("Validated an invalid channel",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }
//
//        testTag.setChannels(Arrays.asList(testChannels.get(0)));
//        try {
//            // verify the tag request is valid
//            tagManager.validateTagRequest(Arrays.asList(testTag,testTag1,testTagC));
//            assertTrue("",true);
//        } catch (Exception e) {
//            assertTrue(e.getMessage(),false);
//        }
//
//        testTag.setName("");
//        try {
//            // verify the tag request is invalid
//            tagManager.validateTagRequest(Arrays.asList(testTag,testTag1,testTagC));
//            assertTrue("Validated an invalid tag",false);
//        } catch (Exception e) {
//            assertTrue("",true);
//        }        
//    }

    // Helper operations to create and clean up the resources needed for successful
    // testing of the TagManager operations

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
                
            }
        });
        cleanupTestTags.forEach(tag -> {
            try {
                if (tagRepository.existsById(tag.getName())) {
                    tagRepository.deleteById(tag.getName());
                }
            } catch (Exception e) {
                
            }
        });
    }

    public static boolean tagCompare(XmlTag tag1, XmlTag tag2) {
        if(!(tag1.getName().equals(tag2.getName())) || !(tag1.getOwner().equals(tag2.getOwner()))) 
            return false;
        if(!(tag1.getChannels().containsAll(tag2.getChannels())) || !(tag2.getChannels().containsAll(tag1.getChannels())))
            return false;
        return true;
    }

    public boolean updatedTagCorrectly(XmlTag returnedTag,XmlTag tagRequest,XmlTag expectedTag) {
        if(!tagCompare(returnedTag,tagRequest))
            return false;
        if(!tagCompare(expectedTag,tagRepository.findById(tagRequest.getName(),true).get()))
            return false;
        return true;
    }

    public boolean updatedTagsCorrectly(Iterable<XmlTag> returnedTags, List<XmlTag> tagsRequest, List<XmlTag> expectedTags) {
        boolean correct = false;
        for(XmlTag tag: tagsRequest) {
            correct = false;
            for(XmlTag returnedTag: returnedTags) {
                if(tagCompare(tag,returnedTag)) {
                    correct = true;
                    break;
                }
            }
            if(!correct)
                return false;
        }

        List<XmlTag> foundTags = new ArrayList<XmlTag>();
        for(String tagName: tagsRequest.stream().map(tag -> tag.getName()).collect(Collectors.toList())) {
            foundTags.add(tagRepository.findById(tagName,true).get());
        }
        for(XmlTag tag: expectedTags) {
            correct = false;
            for(XmlTag foundTag: foundTags) {
                if(tagCompare(tag,foundTag)) {
                    correct =true;
                    break;
                }
            }
            if(!correct)
                return false;
        }
        return true;
    }
    
    public XmlTag copy(XmlTag tag) {
        XmlTag copy = new XmlTag(tag.getName(),tag.getOwner());
        List<XmlChannel> channels = new ArrayList<XmlChannel>();
        tag.getChannels().forEach(chan -> channels.add(new XmlChannel(chan.getName(),chan.getOwner())));
        copy.setChannels(channels);
        return copy;
    }
    
    public List<XmlTag> copy(List<XmlTag> tags) {
        List<XmlTag> copy = new ArrayList<XmlTag>();
        tags.forEach(tag -> copy.add(copy(tag)));
        return copy;
    }
}