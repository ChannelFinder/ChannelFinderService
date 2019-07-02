package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.google.common.collect.Lists;

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
    XmlChannel testChannel = new XmlChannel("testChannel","testOwner");
    XmlChannel testChannel1 = new XmlChannel("testChannel1","testOwner");
    XmlTag testTag = new XmlTag("testTag","testOwner");
    XmlTag testTag1 = new XmlTag("testTag1","testOwner1");
    XmlTag testTag2 = new XmlTag("testTag2","testOwner2");   
    XmlTag testTagC = new XmlTag("testTagC","testOwnerC");    
    XmlTag testTagC1 = new XmlTag("testTagC1","testOwnerC1");    
    XmlTag testTagC2 = new XmlTag("testTagC2","testOwnerC2");    
    XmlTag updateTestTag = new XmlTag("testTag","updateTestOwner");  
    XmlTag updateTestTagC = new XmlTag("testTagC","updateTestOwner");
    XmlTag updateTestTag1 = new XmlTag("testTag1","updateTestOwner1");

    /**
     * list all tags
     */
    @Test // might actually work but messed up by full database
    public void listXmlTags() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTag1.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        Iterable<XmlTag> createdTags = tagManager.create(testTags);

        Iterable<XmlTag> tagList = tagManager.list();
        for(XmlTag tag: createdTags) {
            tag.setChannels(new ArrayList<XmlChannel>());
        }
        // verify the tags were listed as expected
        assertEquals("Failed to list all tags",createdTags,tagList);        

        // clean up
        createdTags.forEach(createdTag -> {
            tagManager.remove(createdTag.getName());
        }); 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * read a single tag
     */
    @Test
    public void readXmlTag() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTag1.setChannels(testChannels);
        XmlTag createdTag = tagManager.create(testTag.getName(),testTag);
        XmlTag createdTag1 = tagManager.create(testTag1.getName(),testTag1);


        XmlTag readTag = tagManager.read(createdTag.getName(), false);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag",createdTag,readTag);        

        readTag = tagManager.read(createdTag.getName(), true);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag w/ channels",createdTag,readTag);

        readTag = tagManager.read(createdTag1.getName(), false);
        testTag1.setChannels(new ArrayList<XmlChannel>());
        // verify the tag was read as expected
        assertEquals("Failed to read the tag",testTag1,readTag);

        readTag = tagManager.read(createdTag1.getName(), true);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag w/ channels",createdTag1,readTag);

        try {
            // verify the tag failed to be read, as expected
            readTag = tagManager.read("fakeTag", false);
            assertTrue("Failed to throw an error",false);
        } catch(ResponseStatusException e) {
            assertTrue(true);
        }

        try {
            // verify the tag failed to be read, as expected
            readTag = tagManager.read("fakeTag", true);
            assertTrue("Failed to throw an error",false);
        } catch(ResponseStatusException e) {
            assertTrue(true);
        }

        // clean up
        tagManager.remove(createdTag.getName());
        tagManager.remove(createdTag1.getName());
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * create a single tag
     */
    @Test
    public void createXmlTag() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTagC.setChannels(testChannels);
        testTagC1.setChannels(testChannels);
        updateTestTagC.setChannels(testChannels);
        testTagC2.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);

        XmlTag createdTag = tagManager.create(testTag.getName(), testTag);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag",testTag,createdTag);        

        XmlTag createdTag1 = tagManager.create("fakeTag", testTag1);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag",testTag1,createdTag1);        

        createdTag = tagManager.create(testTag.getName(), updateTestTag);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag",updateTestTag,createdTag);

        XmlTag createdTag2 = tagManager.create(testTag.getName(), testTag2);
        // verify the tag was created as expected
        assertEquals("Failed to create the tag",testTag2,createdTag2);
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag.getName()));

        createdTag = tagManager.create(testTagC.getName(), testTagC);
        try {
            XmlTag foundTag = tagRepository.findById(testTagC.getName(), true).get();
            // verify the tag was created as expected
            assertTrue("Failed to create the tag w/ channels",tagCompare(testTagC,foundTag)); 
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        createdTag1 = tagManager.create("fakeTag", testTagC1);
        try {
            XmlTag foundTag = tagRepository.findById(testTagC1.getName(), true).get();
            // verify the tag was created as expected
            assertTrue("Failed to create the tag w/ channels",tagCompare(testTagC1,foundTag)); 
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        createdTag = tagManager.create(testTagC.getName(), updateTestTagC);
        try {
            XmlTag foundTag = tagRepository.findById(updateTestTagC.getName(), true).get();
            // verify the tag was created as expected
            assertTrue("Failed to create the tag w/ channels",tagCompare(updateTestTagC,foundTag)); 
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        createdTag2 = tagManager.create(testTagC.getName(), testTagC2);  
        try {
            XmlTag foundTag = tagRepository.findById(testTagC2.getName(), true).get();           
            // verify the tag was created as expected
            assertTrue("Failed to create the tag w/ channels",tagCompare(testTagC2,foundTag)); 
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTagC.getName()));

        for(XmlTag tag: testTags) {
            try {
                tagManager.remove(tag.getName());
            } catch(Exception e) {}
        }
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * create multiple tags
     */
    @Test
    public void createXmlTags() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTagC.setChannels(testChannels);
        testTagC1.setChannels(testChannels);
        testTagC2.setChannels(testChannels);
        updateTestTagC.setChannels(testChannels);
        Iterable<XmlTag> testTags = Arrays.asList(testTag1,testTag2,testTagC,testTagC1,updateTestTag,testTagC2);

        XmlTag createdTag = tagManager.create(testTag.getName(),testTag);

        Iterable<XmlTag> createdTags = tagManager.create(testTags);
        // verify the tags were created as expected
        assertTrue("Failed to create the tags",Iterables.elementsEqual(testTags, createdTags));  
        assertFalse("Failed to replace the tag", testTag.equals(tagRepository.findById(testTag.getName()).get()));

        // clean up
        testTags.forEach(tag -> {
            tagManager.remove(tag.getName());
        }); 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * add a single tag to a single channel
     */
    @Test
    public void addSingleXmlTag() {
        channelRepository.index(testChannel);
        tagRepository.index(testTag);
        List<XmlTag> tag = Arrays.asList(testTag);

        tagManager.addSingle(testTag.getName(), testChannel.getName());
        //verify the tag was added as expected
        assertEquals("Failed to add tag",tag,channelRepository.findById(testChannel.getName()).get().getTags());

        // clean up
        tagManager.remove(testTag.getName());
        channelRepository.deleteById(testChannel.getName());
    }

    /**
     * update a tag 
     */
    @Test
    public void updateXmlTag() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTagC.setChannels(testChannels);
        testTagC1.setChannels(Arrays.asList(testChannel));;
        testTagC2.setChannels(Arrays.asList(testChannel1));
        updateTestTagC.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);

        XmlTag updatedTag = tagManager.update(testTag.getName(), testTag);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,testTag,testTag,testTag.getName()));

        XmlTag updatedTag1 = tagManager.update("fakeTag", testTag1);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag1,testTag1,testTag1,testTag1.getName()));

        updatedTag = tagManager.update(testTag.getName(), updateTestTag);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTag,updateTestTag,updateTestTag.getName()));

        XmlTag updatedTag2 = tagManager.update(testTag.getName(), testTag2);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag2,testTag2,testTag2,testTag2.getName()));
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag.getName()));

        updatedTag = tagManager.update(testTagC.getName(), testTagC);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,testTagC,testTagC,testTagC.getName()));

        updatedTag1 = tagManager.update("fakeTag", testTagC1);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag1,testTagC1,testTagC1,testTagC1.getName()));

        // 0 -> -
        testTagC.setChannels(Arrays.asList(testChannel));
        channelRepository.indexAll(testChannels);
        tagManager.create(testTagC.getName(),testTagC);
        updateTestTagC.setChannels(new ArrayList<XmlChannel>());
        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        XmlTag result = new XmlTag(updateTestTagC.getName(),updateTestTagC.getOwner());
        result.setChannels(Arrays.asList(testChannel));
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTagC,result,updateTestTagC.getName()));

        // 0 -> 1
        updateTestTagC.setChannels(Arrays.asList(testChannel1));
        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        result = new XmlTag(updateTestTagC.getName(),updateTestTagC.getOwner());
        result.setChannels(testChannels);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTagC,result,updateTestTagC.getName()));

        // _ -> 1
        testTagC.setChannels(new ArrayList<XmlChannel>());
        tagRepository.index(testTagC);
        channelRepository.indexAll(testChannels);
        updateTestTagC.setChannels(Arrays.asList(testChannel1));
        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTagC,updateTestTagC,updateTestTagC.getName()));

        // 1 -> 0,1
        updateTestTagC.setChannels(testChannels);
        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTagC,updateTestTagC,updateTestTagC.getName()));

        // 0,1 -> 0,1
        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag,updateTestTagC,updateTestTagC,updateTestTagC.getName()));

        // 0,1 -> 0
        updateTestTagC.setChannels(Arrays.asList(testChannel));
        updatedTag1 = tagManager.update(testTagC.getName(), updateTestTagC);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag1,updateTestTagC,updatedTag,updatedTag1.getName()));

        updatedTag2 = tagManager.update(testTagC.getName(), testTagC2);
        result = new XmlTag(testTagC2.getName(),testTagC2.getOwner());
        result.setChannels(testChannels);
        // verify the tag was updated as expected
        assertTrue("Failed to update the tag",updatedTagCorrectly(updatedTag2,testTagC2,result,updatedTag2.getName()));
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTagC.getName()));

        // clean up 
        for(XmlTag tag: testTags) {
            try {
                tagManager.remove(tag.getName());
            } catch(Exception e) {}
        }
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * update multiple tags 
     */
    @Test
    public void updateXmlTags() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTagC.setChannels(testChannels);
        testTagC1.setChannels(Arrays.asList(testChannel));;
        testTagC2.setChannels(Arrays.asList(testChannel));
        updateTestTagC.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);

        List<XmlTag> tags = Arrays.asList(testTag,testTagC);
        tagManager.create(tags);

        List<XmlTag> updateTags = Arrays.asList(updateTestTag,testTag1,updateTestTagC,testTagC1);
        Iterable<XmlTag> updatedTags = tagManager.update(updateTags);
        List<String> updateTagNames = new ArrayList<String>();        
        updateTags.forEach(tag -> updateTagNames.add(tag.getName()));
        // verify the tags were updated as expected
        assertTrue("Failed to update the tag",updatedTagsCorrectly(updatedTags,updateTags,updateTags,updateTagNames));


        updateTestTag.setChannels(Arrays.asList(testChannel));
        tagManager.update(updateTestTag.getName(),updateTestTag);
        updateTestTag.setChannels(new ArrayList<XmlChannel>());
        testTag1.setChannels(Arrays.asList(testChannel));
        testTagC2.setChannels(Arrays.asList(testChannel1));
        testTag2.setChannels(testChannels);
        tagManager.update(testTag2.getName(),testTag2);
        testTagC1.setChannels(testChannels);

        updateTags = Arrays.asList(updateTestTagC,updateTestTag,testTag1,testTagC2,testTag2,testTagC1);
        updatedTags = tagManager.update(updateTags);
        List<String> updateTagNames1 = new ArrayList<String>();        
        updateTags.forEach(tag -> updateTagNames1.add(tag.getName()));

        updateTestTag.setChannels(Arrays.asList(testChannel));
        testTagC2.setChannels(testChannels);
        testTag2.setChannels(testChannels);
        List<XmlTag> goalTags = Arrays.asList(updateTestTagC,updateTestTag,testTag1,testTagC2,testTag2,testTagC1);  
        // verify the tags were updated as expected
        assertTrue("Failed to update the tag",updatedTagsCorrectly(updatedTags,updateTags,goalTags,updateTagNames1));

        // clean up 
        for(XmlTag tag: testTags) {
            try {
                tagManager.remove(tag.getName());
            } catch(Exception e) {}
        }
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * delete a single tag 
     */
    @Test
    public void deleteXmlTag() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        testTag1.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        Iterable<XmlTag> createdTags = tagManager.create(testTags);

        tagManager.remove(testTag.getName());
        // verify the tag was deleted as expected
        assertTrue("Failed to delete the tag",!tagRepository.existsById(testTag.getName()));

        tagManager.remove(testTag1.getName());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag",testTag1.getName());
        // verify the tag was deleted as expected
        assertTrue("Failed to delete the tag",!tagRepository.existsById(testTag1.getName()));
        assertEquals("Failed to delete the tag from channels",new ArrayList<XmlChannel>(),channelRepository.search(params));

        // clean up 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });
    }

    /**
     * delete a single tag from a single channel 
     */
    @Test
    public void deleteXmlTagFromChannel() {
        XmlChannel createdChannel = channelRepository.index(testChannel);
        testTag1.setChannels(Arrays.asList(testChannel));
        XmlTag createdTag = tagManager.create(testTag.getName(),testTag);

        tagManager.removeSingle(testTag.getName(),testChannel.getName());
        // verify the tag was not deleted, as expected
        assertTrue("Failed to not delete the tag",tagRepository.existsById(testTag.getName()));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("~tag",testTag.getName());
        // verify the tag was deleted from the channel as expected
        assertEquals("Failed to delete the tag from channel",new ArrayList<XmlChannel>(),channelRepository.search(params));

        // clean up 
        channelRepository.deleteById(createdChannel.getName());
    }
    
    /**
     * validate a tag request
     */
    @Test
    public void validateXmlTagRequest() {
        XmlChannel createdChannel = channelRepository.index(testChannel);
        testTag1.setChannels(Arrays.asList(testChannel));
        
        try {
            // verify the tag request is valid
            tagManager.validateTagRequest(createdChannel.getName());
            assertTrue("",true);
        } catch (Exception e) {
            assertTrue(e.getMessage(),false);
        }
        
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest("fakeChannel");
            assertTrue("Validated an invalid channel",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        try {
            // verify the tag request is valid
            tagManager.validateTagRequest(testTag);
            assertTrue("",true);
        } catch (Exception e) {
            assertTrue(e.getMessage(),false);
        }
        
        try {
            // verify the tag request is valid
            tagManager.validateTagRequest(testTag1);
            assertTrue("",true);
        } catch (Exception e) {
            assertTrue(e.getMessage(),false);
        }
        
        testTag.setName(null);
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid tag name",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setName("");
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid tag name",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setName("testTag");
        testTag.setOwner(null);
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid tag owner",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setOwner("");
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid tag owner",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setOwner("tagOwner");
        testTag.setChannels(Arrays.asList(testChannel1));
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid channel",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setChannels(Arrays.asList(testChannel,testChannel1));
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(testTag);
            assertTrue("Validated an invalid channel",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        testTag.setChannels(Arrays.asList(testChannel));
        try {
            // verify the tag request is valid
            tagManager.validateTagRequest(Arrays.asList(testTag,testTag1,testTagC));
            assertTrue("",true);
        } catch (Exception e) {
            assertTrue(e.getMessage(),false);
        }
        
        testTag.setName("");
        try {
            // verify the tag request is invalid
            tagManager.validateTagRequest(Arrays.asList(testTag,testTag1,testTagC));
            assertTrue("Validated an invalid tag",false);
        } catch (Exception e) {
            assertTrue("",true);
        }
        
        
    }

    public static boolean tagCompare(XmlTag tag1, XmlTag tag2) {
        if(!(tag1.getName().equals(tag2.getName())) || !(tag1.getOwner().equals(tag2.getOwner()))) 
            return false;
        if(!(tag1.getChannels().containsAll(tag2.getChannels())) || !(tag2.getChannels().containsAll(tag1.getChannels())))
            return false;
        return true;
    }

    public boolean updatedTagCorrectly(XmlTag returnedTag,XmlTag passedInTag,XmlTag goalTag,String tagToFind) {
        if(!tagCompare(returnedTag,passedInTag))
            return false;
        if(!tagCompare(goalTag,tagRepository.findById(tagToFind,true).get()))
            return false;
        return true;
    }

    private boolean updatedTagsCorrectly(Iterable<XmlTag> returnedTags, List<XmlTag> passedInTags, List<XmlTag> goalTags,List<String> tagsToFind) {
        for(XmlTag tag: returnedTags) {
            boolean correct = false;
            for(XmlTag passedTag: passedInTags) {
                if(tagCompare(tag,passedTag)) {
                    correct = true;
                    break;
                }
            }
            if(!correct)
                return false;
        }

        List<XmlTag> foundTags = new ArrayList<XmlTag>();
        for(String tagName: tagsToFind) {
            foundTags.add(tagRepository.findById(tagName,true).get());           
        }
        for(XmlTag tag: goalTags) {
            boolean correct = false;
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
}
