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
    @Test
    public void listXmlTags() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        List<XmlChannel> createdChannels = (List<XmlChannel>) channelRepository.indexAll(testChannels);
        testTag1.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        List<XmlTag> createdTags = tagManager.create(testTags);

        Iterable<XmlTag> tagList = tagManager.list(null);
        // verify the tags were listed as expected
        assertEquals("Failed to list all tags",(Iterable<XmlTag>)createdTags,tagList);        

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
        List<XmlChannel> createdChannels = (List<XmlChannel>) channelRepository.indexAll(testChannels);
        testTag1.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1);
        List<XmlTag> createdTags = tagManager.create(testTags);

        XmlTag readTag = tagManager.read(createdTags.get(0).getName(), false);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag",createdTags.get(0),readTag);        

        readTag = tagManager.read(createdTags.get(0).getName(), true);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag w/ channels",createdTags.get(0),readTag);

        readTag = tagManager.read(createdTags.get(1).getName(), false);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag",createdTags.get(1),readTag);

        readTag = tagManager.read(createdTags.get(1).getName(), true);
        // verify the tag was read as expected
        assertEquals("Failed to read the tag w/ channels",createdTags.get(1),readTag);

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
        createdTags.forEach(createdTag -> {
            tagManager.remove(createdTag.getName());
        }); 
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
        List<XmlChannel> createdChannels = (List<XmlChannel>) channelRepository.indexAll(testChannels);
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

        for(XmlChannel chan: testChannels) {
            chan.setTags(Arrays.asList(testTagC));
        }
        testTagC.setChannels(testChannels);
        createdTag = tagManager.create(testTagC.getName(), testTagC);
        try {
            XmlTag foundTag = tagRepository.findById(testTagC.getName(), true).get();
            // verify the tag was created as expected
            assertEquals("Failed to create the tag w/ channels",testTagC,foundTag); 
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        for(XmlChannel chan: testChannels) {
            chan.setTags(Arrays.asList(testTagC1));
        }
        testTagC1.setChannels(testChannels);
        createdTag1 = tagManager.create("fakeTag", testTagC1);
        try {
            XmlTag foundTag = tagRepository.findById(testTagC1.getName(), true).get();
            // verify the tag was created as expected
            assertEquals("Failed to create the tag w/ channels",testTagC1,foundTag);
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        for(XmlChannel chan: testChannels) {
            chan.setTags(Arrays.asList(updateTestTagC));
        }
        updateTestTagC.setChannels(testChannels);
        createdTag = tagManager.create(testTagC.getName(), updateTestTagC);
        try {
            XmlTag foundTag = tagRepository.findById(updateTestTagC.getName(), true).get();
            // verify the tag was created as expected
            assertEquals("Failed to create the tag w/ channels", updateTestTagC,foundTag);
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }

        for(XmlChannel chan: testChannels) {
            chan.setTags(Arrays.asList(testTagC2));
        }
        testTagC2.setChannels(testChannels);
        createdTag2 = tagManager.create(testTagC.getName(), testTagC2);  
        try {
            XmlTag foundTag = tagRepository.findById(testTagC2.getName(), true).get();           
            // verify the tag was created as expected
            assertEquals("Failed to create the tag w/ channels",testTagC2,foundTag);
        } catch (Exception e) {
            assertTrue("Failed to create/find the tag w/ channels",false);
        }
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTagC.getName()));

        // clean up
        testTags.forEach(tag -> {
            tagManager.remove(tag.getName());
        }); 
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
        List<XmlChannel> createdChannels = (List<XmlChannel>) channelRepository.indexAll(testChannels);
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
    public void updateTagTest() {
        List<XmlChannel> testChannels = Arrays.asList(testChannel,testChannel1);
        List<XmlChannel> createdChannels = (List<XmlChannel>) channelRepository.indexAll(testChannels);
        testTagC.setChannels(testChannels);
        testTagC1.setChannels(Arrays.asList(testChannel));;
        testTagC2.setChannels(Arrays.asList(testChannel1));
        updateTestTagC.setChannels(testChannels);
        List<XmlTag> testTags = Arrays.asList(testTag,testTag1,testTag2,testTagC,testTagC1,updateTestTag,updateTestTagC,testTagC2);

        XmlTag updatedTag = tagManager.update(testTag.getName(), testTag);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag",testTag,updatedTag);        

        XmlTag updatedTag1 = tagManager.update("fakeTag", testTag1);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag",testTag1,updatedTag1);        

        updatedTag = tagManager.update(testTag.getName(), updateTestTag);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag",updateTestTag,updatedTag);

        XmlTag updatedTag2 = tagManager.update(testTag.getName(), testTag2);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag",testTag2,updatedTag2);
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTag.getName()));

        updatedTag = tagManager.update(testTagC.getName(), testTagC);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag w/ channels",testTagC,updatedTag);        

        updatedTag1 = tagManager.update("fakeTag", testTagC1);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag w/ channels",testTagC1,updatedTag1);

        updatedTag = tagManager.update(testTagC.getName(), updateTestTagC);
        // verify the tag was updated as expected
        XmlTag check = new XmlTag(updateTestTag.getName(),updateTestTag.getOwner());
        assertEquals("Failed to update the tag w/ channels", updateTestTagC,updatedTag);

        assertTrue("",updatedTag.getChannels().containsAll(testTagC.getChannels()) && updatedTag.getChannels().containsAll(updateTestTagC.getChannels()));


        updatedTag2 = tagManager.update(testTagC.getName(), testTagC2);
        // verify the tag was updated as expected
        assertEquals("Failed to update the tag w/ channels",testTagC2,updatedTag2);
        assertFalse("Failed to replace the old tag", tagRepository.existsById(testTagC.getName()));

        // clean up
        testTags.forEach(tag -> {
            tagManager.remove(tag.getName());
        }); 
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });

    }

    /**
     * test the renaming of a tag (renameTag method)
     */
    @Test
    public void renameTagTest() {
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        tagManager.create("test-tag", testTag);
        XmlTag newTag = new XmlTag();
        newTag.setOwner("test-owner");
        newTag.setName("new-test-tag");
        // XmlTag renamedTag = tagManager.renameTag(testTag,newTag);
        // check if the read tag has the correct name and owner
        // assertEquals("Failed to rename the tag", newTag.getName(),
        // renamedTag.getName());
        tagManager.remove("test-tag");
        tagManager.remove("new-test-tag");
    }

    /**
     * test the removal of a tag (remove method)
     */
    @Test
    public void removeTagTest() {
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
        tagManager.remove("test-tag");
        // now check if the tag was removed
        // assertEquals("Failed to remove the tag", "Deleted successfully", removed);
        assertEquals("Failed to remove the tag", null, tagManager.read("test-tag", false));
    }

    /**
     * test the creation of multiple tags (create method)
     */
    @Test
    public void createTagsTest() {
        Tags = new ArrayList<XmlTag>();
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner1");
        Tags.add(testTag);
        Tags.add(testTag1);
        List<XmlTag> createdTags = tagManager.create(Tags);
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to create the first tag", testTag.getName(), createdTags.get(0).getName());
        assertEquals("Failed to create the first tag", testTag.getOwner(), createdTags.get(0).getOwner());
        assertEquals("Failed to create the second tag", testTag1.getName(), createdTags.get(1).getName());
        assertEquals("Failed to create the second tag", testTag1.getOwner(), createdTags.get(1).getOwner());
        tagManager.remove("test-tag");
        tagManager.remove("test-tag1");
    }

    // TODO
    /**
     * test the retrieval of tags (read method) NOT DONE
     */
    @Test
    public void listTagsTest() {
        Tags = new ArrayList<XmlTag>();
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner1");
        XmlTag testTag2 = new XmlTag();
        testTag1.setName("test-tag2");
        testTag1.setOwner("test-owner2");
        Tags.add(testTag);
        Tags.add(testTag1);
        Tags.add(testTag2);
        tagManager.create("test-tag", testTag);
        tagManager.create("test-tag1", testTag1);
        tagManager.create("test-tag2", testTag2);
        Map<String, String> map = new HashMap<String, String>();
        List<XmlTag> tagList = Lists.newArrayList(tagManager.list(map));
        for (int i = 0; i < tagList.size(); i++) {
            assertEquals("Failed to list correct tags(name incorrect)", Tags.get(i).getName(),
                    tagList.get(i).getName());
            assertEquals("Failed to list correct tags(owner incorrect)", Tags.get(i).getOwner(),
                    tagList.get(i).getOwner());
        }
        tagManager.remove("test-tag");
        tagManager.remove("test-tag1");
        tagManager.remove("test-tag2");
    }

    // TODO *requires channel code which is not yet implemented*
    /**
     * test the updating of multiple tags (update method)
     */
    @Test
    public void updateTagsTest() {
        // testTag = new XmlTag();
        // testTag.setName("test-tag");
        // testTag.setOwner("test-owner");
        // createdTag = tagManager.create("test-tag", testTag);
        // tagManager.update("updated-test-tag",createdTag);
        // // check if the read tag has the correct name and owner
        // assertEquals("Failed to update the tag", "updated-test-tag",
        // createdTag.getName());
        // tagManager.remove("updated-test-tag");
    }

    /**
     * test the validation of a tag (renameTag method)
     */
    @Test
    public void validateTagTest() {
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        tagManager.create("test-tag", testTag);
        XmlTag newTag = new XmlTag();
        newTag.setOwner("test-owner");
        newTag.setName("test-tag");
        // check if validated
        assertEquals("Failed to validate", true, tagManager.validateTag(testTag, newTag));
        // check if validated
        newTag.setName("new-test-tag");
        assertEquals("Failed to fail to validate", false, tagManager.validateTag(testTag, newTag));

        tagManager.remove("test-tag");
        tagManager.remove("new-test-tag");

    }

    // TODO *requires channel code which is not yet implemented*
    /**
     * test the creation of a single tag (create method)
     */
    @Test
    public void createSingleTagTest() {
        // testTag = new XmlTag();
        // testTag.setName("test-tag");
        // testTag.setOwner("test-owner");
        // createdTag = tagManager.create("test-tag", testTag);
        // // now check if the created tag has the correct name and owner
        // assertEquals("Failed to create the tag", testTag.getName(),
        // createdTag.getName());
        // assertEquals("Failed to create the tag", testTag.getOwner(),
        // createdTag.getOwner());
        // tagManager.remove("test-tag");
    }

    // TODO *requires channel code which is not yet implemented*
    /**
     * test the removal of a single tag (remove method)
     */
    @Test
    public void removeSingleTagTest() {
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
        tagManager.remove("test-tag");
        // now check if the created tag has the correct name and owner
        // assertEquals("Failed to remove the tag", null,
        // tagManager.read("test-tag",false));
        tagManager.remove("test-tag");
    }

}
