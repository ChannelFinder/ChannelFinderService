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

    @Test
    public void simpleTest() {
    	assertEquals("4 isn't 4", 4, 4);
    }

    /**
     * test the creation of a single tag
     */
    @Test
    public void createTagTest() {
        XmlTag testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        XmlTag createdTag = tagManager.create("test-tag", testTag);
        // now check if the created tag has the correct name and owner
        assertEquals("Failed to create the tag", testTag.getName(), createdTag.getName());
        assertEquals("Failed to create the tag", testTag.getOwner(), createdTag.getOwner());

        // TODO Cleanup - remove the test tag that was previously created
    }

}
