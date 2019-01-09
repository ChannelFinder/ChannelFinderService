package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@WebMvcTest(TagManager.class)
public class TagIT {

    @Autowired
    TagManager tagManager;
    
    static XmlTag testTag;
    static XmlTag createdTag;
    static List<XmlTag> Tags; 
    
    @Test
    public void simpleTest() {
    	assertEquals("4 isn't 4", 4, 4);
    }

    /**
     * test the creation of a single tag
     * (create method)
     */
    @Test
    public void createTagTest() {
        testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to create the tag", testTag.getName(), createdTag.getName());
        assertEquals("Failed to create the tag", testTag.getOwner(), createdTag.getOwner());
        tagManager.remove("test-tag");
    }
    
    /**
     * test the retrieval of a tag
     * (read method)
     */
    @Test
    public void readTagTest() {
    	testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
    	XmlTag readTag = tagManager.read("test-tag",false);
        // check if the read tag has the correct name and owner
        assertEquals("Failed to read the tag", createdTag.getName(), readTag.getName());
        assertEquals("Failed to read the tag", createdTag.getOwner(), readTag.getOwner());
        tagManager.remove("test-tag");
    }
    
 // TODO - fix
    /**
     * test the updating of a tag
     * (update method)
     */
    @Test
    public void updateTagTest() {
    	testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
    	tagManager.update("updated-test-tag",createdTag);
        // check if the read tag has the correct name and owner
        assertEquals("Failed to update the tag", "updated-test-tag", createdTag.getName());
        tagManager.remove("updated-test-tag");
    }
    
 // TODO - fix
    /**
     * test the renaming of a tag
     * (renameTag method)
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
    	tagManager.renameTag(testTag,newTag);
        // check if the read tag has the correct name and owner
        assertEquals("Failed to rename the tag", newTag.getName(), testTag.getName());
        tagManager.remove("test-tag");
    }
    
    /**
     * test the removal of a tag
     * (remove method)
     */
    @Test
    public void removeTagTest() {
    	testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        createdTag = tagManager.create("test-tag", testTag);
        String removed = tagManager.remove("test-tag");
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to remove the tag", null, tagManager.read("test-tag",false));
    }

    /**
     * test the creation of multiple tags
     * (create method)
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
        List<XmlTag> createdTags = tagManager.createTags(Tags);
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to create the first tag", testTag.getName(), createdTags.get(0).getName());
        assertEquals("Failed to create the first tag", testTag.getOwner(), createdTags.get(0).getOwner());
        assertEquals("Failed to create the second tag", testTag1.getName(), createdTags.get(1).getName());
        assertEquals("Failed to create the second tag", testTag1.getOwner(), createdTags.get(1).getOwner());
        tagManager.remove("test-tag");
    }
    
 // TODO
    /**
     * test the retrieval of tags
     * (read method)
     * NOT DONE
     */
    @Test
    public void listTagsTest() {
    	//XmlTag readTag = tagManager.read("test-tag",false);
        // check if the read tag has the correct name and owner
//        assertEquals("Failed to read the tag", createdTag.getName(), readTag.getName());
//        assertEquals("Failed to read the tag", createdTag.getOwner(), readTag.getOwner());
//        tagManager.remove("test-tag");
    }

 // TODO
    /**
     * test the updating of multiple tags
     * (update method)
     */
    @Test
    public void updateTagsTest() {
//    	testTag = new XmlTag();
//        testTag.setName("test-tag");
//        testTag.setOwner("test-owner");
//        createdTag = tagManager.create("test-tag", testTag);
//    	tagManager.update("updated-test-tag",createdTag);
//        // check if the read tag has the correct name and owner
//        assertEquals("Failed to update the tag", "updated-test-tag", createdTag.getName());
//        tagManager.remove("updated-test-tag");
    }   

    /**
     * test the validation of a tag
     * (renameTag method)
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
        assertEquals("Failed to validate", true, tagManager.validateTag(testTag,newTag));
        // check if validated
        newTag.setName("new-test-tag");
        assertEquals("Failed to fail to validate", false, tagManager.validateTag(testTag,newTag));
        
        tagManager.remove("test-tag");
    }
    
    
}
