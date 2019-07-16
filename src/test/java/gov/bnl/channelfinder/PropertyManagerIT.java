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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
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
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);

        XmlProperty createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
            assertEquals("Failed to create the property w/ channels. Expected " + testProperty0WithChannels.toLog() + " found " 
                    + foundProperty.toLog(), testProperty0WithChannels, foundProperty);
        } catch (Exception e) {
            assertTrue("Failed to create/find the property w/ channels", false);
        }
        
//        XmlTag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
//        // verify the tag was created as expected
//        assertEquals("Failed to create the tag",testTag1,createdTag1);

        // Update the test property with a new owner
        XmlProperty updatedTestProperty0WithChannels = new XmlProperty("testProperty0WithChannels", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0WithChannels.getName(), updatedTestProperty0WithChannels);
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
            assertEquals("Failed to create the property w/ channels. Expected " + updatedTestProperty0WithChannels.toLog() + " found " 
                    + foundProperty.toLog(), updatedTestProperty0WithChannels, foundProperty);
        } catch (Exception e) {
            assertTrue("Failed to create/find the property w/ channels", false);
        }
    }
    
    /**
     * Rename a simple property with channels
     */
    @Test
    public void renameXmlProperty2() {
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        XmlProperty testProperty1WithChannels = new XmlProperty("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);
        
        // Create the testProperty0WithChannels
        XmlProperty createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // update the testProperty0WithChannels with testProperty1WithChannels
        createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty1WithChannels);
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(), true).get();
            assertEquals("Failed to create the property w/ channels", testProperty1WithChannels, foundProperty);
        } catch (Exception e) {
            assertTrue("Failed to create/find the property w/ channels", false);
        }
        assertFalse("Failed to replace the old property", propertyRepository.existsById(testProperty0WithChannels.getName()));
    }    
    
    /**
     * create multiple properties
     */
    @Test
    public void createXmlProperties() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        XmlProperty testProperty2 = new XmlProperty("testProperty2","testOwner");

        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        XmlProperty testProperty1WithChannels = new XmlProperty("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        XmlProperty testProperty2WithChannels = new XmlProperty("testProperty2WithChannels","testOwner");
        testProperty2WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty2WithChannels.getName(),testProperty2WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        
        List<XmlProperty> testProperties = Arrays.asList(testProperty0,testProperty1,testProperty2,testProperty0WithChannels,testProperty1WithChannels,testProperty2WithChannels);        
        cleanupTestProperties = testProperties;
        
        Iterable<XmlProperty> createdProperties = propertyManager.create(testProperties);
        List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
        testProperties.forEach(prop -> foundProperties.add(propertyRepository.findById(prop.getName(),true).get()));
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        testProperty2WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty2WithChannels.getName(),testProperty2WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));    
        assertEquals("Failed to create the properties", testProperties, foundProperties);
    }
    
    /**
     * create by overriding multiple properties
     */
    @Test
    public void createXmlPropertiesWithOverride() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        List<XmlProperty> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);        
        cleanupTestProperties = testProperties;
    
        //Create a set of original properties to be overriden
        propertyManager.create(testProperties);
        // Now update the test properties
        testProperty0.setOwner("testOwner-updated");
        testProperty0WithChannels.setChannels(Collections.emptyList());
        
        List<XmlProperty> updatedTestProperties = Arrays.asList(testProperty0,testProperty0WithChannels);        
        propertyManager.create(updatedTestProperties);
        List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
        testProperties.forEach(prop -> foundProperties.add(propertyRepository.findById(prop.getName(),true).get()));
        // verify the properties were created as expected
        assertTrue("Failed to create the properties", Iterables.elementsEqual(updatedTestProperties, foundProperties));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty0WithChannels", "*");
        // verify the property was removed from the old channels
        assertEquals("Failed to delete the property from channels",
                new ArrayList<XmlChannel>(), channelRepository.search(params));
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