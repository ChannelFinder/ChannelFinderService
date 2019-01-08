package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;

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
    

    @Test
    public void simpleTest() {
    	assertEquals("4 isn't 4", 4, 4);
    }

    /**
     * test the creation of a single tag
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

        // TODO Cleanup - remove the test tag that was previously created
    }
    
    /**
     * test the retrieval of a tag
     */
    @Test
    public void readTagTest() {
    	XmlTag readTag = tagManager.read("test-tag",false);
        // check if the read tag has the correct name and owner
        assertEquals("Failed to read the tag", createdTag.getName(), readTag.getName());
        assertEquals("Failed to read the tag", createdTag.getOwner(), readTag.getOwner());

    }
    
    /**
     * test the removal of a tag
     */
    @Test
    public void removeTagTest() {
        String removed = tagManager.remove("test-tag");
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to remove the tag", null, tagManager.read("test-tag",false));

        // TODO Cleanup - remove the test tag that was previously created
    }

}
