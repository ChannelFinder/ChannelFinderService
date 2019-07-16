package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Lists;

@RunWith(SpringRunner.class)
@WebMvcTest(PropertyManager.class)
@WithMockUser(roles = "CF-ADMINS")
public class PropertyManagerIT {

    @Autowired
    PropertyManager propertyManager;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    // set up
    XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
    XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
    XmlProperty testProperty2 = new XmlProperty("testProperty2","testOwner2");   
    XmlProperty testPropertyC = new XmlProperty("testPropertyC","testOwnerC");    
    XmlProperty testPropertyC1 = new XmlProperty("testPropertyC1","testOwnerC1");    
    XmlProperty testPropertyC2 = new XmlProperty("testPropertyC2","testOwnerC2");    
    XmlProperty updateTestProperty = new XmlProperty("updateTestProperty","updateTestOwner");  
    XmlProperty updateTestPropertyC = new XmlProperty("updateTestPropertyC","updateTestOwner");
    XmlProperty updateTestPropertyC1 = new XmlProperty("updateTestPropertyC1","updateTestOwner1");

    /**
     * list all properties
     */
    @Test 
    public void listXmlProperties() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<XmlTag>())));
        List<XmlProperty> testProperties = Arrays.asList(testProperty0,testProperty1);
        cleanupTestProperties = testProperties;
        
        Iterable<XmlProperty> createdProperties = propertyManager.create(testProperties);
        Iterable<XmlProperty> propertyList = propertyManager.list();
        for(XmlProperty property: createdProperties) {
            property.setChannels(new ArrayList<XmlChannel>());
        }
        // verify the properties were listed as expected
        assertEquals("Failed to list all properties",createdProperties,propertyList);                
    }

    /**
     * read a single property
     * test the "withChannels" flag
     */
    @Test
    public void readXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);
        
        XmlProperty createdProperty0 = propertyManager.create(testProperty0.getName(),testProperty0);
        XmlProperty createdProperty1 = propertyManager.create(testProperty1.getName(),testProperty1);

        // verify the created properties are read as expected
        // retrieve the testProperty0 without channels
        XmlProperty retrievedProperty = propertyManager.read(createdProperty0.getName(), false);
        assertEquals("Failed to read the property",createdProperty0,retrievedProperty);        
        // retrieve the testProperty0 with channels
        retrievedProperty = propertyManager.read(createdProperty0.getName(), true);
        assertEquals("Failed to read the property w/ channels",createdProperty0,retrievedProperty);

        retrievedProperty = propertyManager.read(createdProperty1.getName(), false);
        testProperty1.setChannels(new ArrayList<XmlChannel>());
        // verify the property was read as expected
        assertEquals("Failed to read the property",testProperty1,retrievedProperty);

        retrievedProperty = propertyManager.read(createdProperty1.getName(), true);
        // verify the property was read as expected
        assertEquals("Failed to read the property w/ channels",createdProperty1,retrievedProperty);
    }
    
    /**
     * attempt to read a single non existent property
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlProperty() {
        // verify the tag failed to be read, as expected
        propertyManager.read("fakeProperty", false);
    }

    /**
     * attempt to read a single non existent property with channels
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlProperty2() {
        // verify the tag failed to be read, as expected
        propertyManager.read("fakeProperty", true);
    }
    
    /**
     * create a simple property
     */
    @Test
    public void createXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0);

        // Create a simple property
        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        assertEquals("Failed to create the property", testProperty0, createdProperty);

//        XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//        // verify the tag was created as expected
//        assertEquals("Failed to create the tag",testTag1,createdTag1);

        // Update the test property with a new owner
        XmlProperty updatedTestProperty0 = new XmlProperty("testProperty0", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0.getName(), updatedTestProperty0);
        assertEquals("Failed to create the property", updatedTestProperty0, createdProperty);
    }
    
    /**
     * Rename a simple property
     */
    @Test
    public void renameXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);
        
        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        createdProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        assertEquals("Failed to create the property", testProperty1, createdProperty);
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", tagRepository.existsById(testProperty0.getName()));
    }
    
    /**
     * create a simple property with channels
     */
    @Test
    public void createXmlProperty2() {
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty1.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);

        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
            assertEquals("Failed to create the property w/ channels", testProperty0WithChannels, foundProperty)
        } catch (Exception e) {
            assertTrue("Failed to create/find the property w/ channels", false);
        }
        
//        XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//        // verify the tag was created as expected
//        assertEquals("Failed to create the tag",testTag1,createdTag1);

        // Update the test property with a new owner
        XmlProperty updatedTestProperty0 = new XmlProperty("testProperty0", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0.getName(), updatedTestProperty0);
        assertEquals("Failed to create the tag", updatedTestProperty0, createdProperty);
    }
    
    /**
     * Rename a simple property with channels
     */
    @Test
    public void renameXmlProperty2() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);
        
        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        createdProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        assertEquals("Failed to create the property", testProperty1, createdProperty);
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", tagRepository.existsById(testProperty0.getName()));
    }
    
    
    
    
    
    
    // Helper operations to create and clean up the resources needed for successful
    // testing of the PropertyManager operations

    private final List<XmlChannel> testChannels = Arrays.asList(
            new XmlChannel("testChannel0", "testOwner"),
            new XmlChannel("testChannel1", "testOwner"));

    private List<XmlProperty> cleanupTestProperties = Collections.emptyList();
    
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
                System.out.println("Failed to clean up channel: " + channel.getName());
            }
        });
        cleanupTestProperties.forEach(property -> {
            if (propertyRepository.existsById(property.getName())) {
                propertyRepository.deleteById(property.getName());
            } 
        });
    }

    public XmlProperty copy(XmlProperty property, String value) {
        XmlProperty copy = new XmlProperty(property.getName(),property.getOwner());
        XmlProperty propwval = new XmlProperty(property.getName(),property.getOwner(),value);
        List<XmlChannel> channels = new ArrayList<XmlChannel>();
        property.getChannels().forEach(chan -> channels.add(new XmlChannel(chan.getName(),chan.getOwner())));
        channels.forEach(chan -> chan.addProperty(propwval));
        copy.setChannels(channels);
        return copy;
    }
}