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
        // verify the property failed to be read, as expected
        propertyManager.read("fakeProperty", false);
    }

    /**
     * attempt to read a single non existent property with channels
     */
    @Test(expected = ResponseStatusException.class)
    public void readNonExistingXmlProperty2() {
        // verify the property failed to be read, as expected
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
        //        // verify the property was created as expected
        //        assertEquals("Failed to create the property",testTag1,createdTag1);

        // Update the test property with a new owner
        XmlProperty updatedTestProperty0 = new XmlProperty("testProperty0", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0.getName(), updatedTestProperty0);
        assertEquals("Failed to create the property", updatedTestProperty0, createdProperty);
    }

    /**
     * Rename a simple property using create
     */
    @Test
    public void renameByCreateXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);

        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        createdProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        assertEquals("Failed to create the property", testProperty1, createdProperty);
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", propertyRepository.existsById(testProperty0.getName()));
    }

    /**
     * create a single property with channels
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
        //        // verify the property was created as expected
        //        assertEquals("Failed to create the property",testTag1,createdTag1);

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
     * Rename a single property with channels using create
     */
    @Test
    public void renameByCreateXmlProperty2() {
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
        testProperty0WithChannels.setOwner("testOwner-updated");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        
        List<XmlProperty> updatedTestProperties = Arrays.asList(testProperty0,testProperty0WithChannels);        
        propertyManager.create(updatedTestProperties);
       
        // set owner back to original since it shouldn't change
        testProperty0.setOwner("testOwner");
        testProperty0WithChannels.setOwner("testOwner"); 
        
        List<XmlProperty> foundProperties = new ArrayList<XmlProperty>();
        testProperties.forEach(prop -> foundProperties.add(propertyRepository.findById(prop.getName(),true).get()));
        // verify the properties were created as expected
        assertTrue("Failed to create the properties", Iterables.elementsEqual(updatedTestProperties, foundProperties));

        testChannels.get(1).setProperties(Arrays.asList(new XmlProperty("testProperty0WithChannels","testOwner","value")));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty0WithChannels", "*");
        // verify the property was removed from the old channels
        assertEquals("Failed to delete the property from channels",
                Arrays.asList(testChannels.get(1)), channelRepository.search(params));
    }

    /**
     * add a single property to a single channel
     * @Todo fix this test after addsingle method is fixed
     */
    @Test
    public void addSingleXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0", "testOwner");
        propertyRepository.index(testProperty0);
        testProperty0.setValue("value");
        cleanupTestProperties = Arrays.asList(testProperty0);

        propertyManager.addSingle(testProperty0.getName(), "testChannel0", testProperty0);
        assertTrue("Failed to add property",
                channelRepository.findById("testChannel0").get().getProperties().stream().anyMatch(p -> {
                    return p.getName().equals(testProperty0.getName());
                }));
    }

    /**
     * update a property
     */
    @Test
    public void updateXmlProperty() {
        // A test property with only name and owner
        XmlProperty testProperty0 = new XmlProperty("testProperty0", "testOwner");
        // A test property with name, owner, and a single test channel with a copy of the property with a value and no channels
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        List<XmlProperty> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);        
        cleanupTestProperties = testProperties;

        // Update on a non-existing property should result in the creation of that property
        // 1. Test a simple property 
        XmlProperty returnedProperty = propertyManager.update(testProperty0.getName(), testProperty0);
        assertTrue("Failed to update property " + testProperty0, returnedProperty.equals(testProperty0));
        assertTrue("Failed to update property " + testProperty0, testProperty0.equals(propertyRepository.findById(testProperty0.getName()).get()));
        // 2. Test a property with channels
        returnedProperty = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedProperty.equals(testProperty0WithChannels));
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));

        // Update the property owner
        testProperty0.setOwner("newTestOwner");
        returnedProperty = propertyManager.update(testProperty0.getName(), testProperty0);
        assertTrue("Failed to update property " + testProperty0, returnedProperty.equals(testProperty0));
        assertTrue("Failed to update property " + testProperty0, testProperty0.equals(propertyRepository.findById(testProperty0.getName()).get()));
        testProperty0WithChannels.setOwner("newTestOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        returnedProperty = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedProperty.equals(testProperty0WithChannels));
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));
    }

    /**
     * update a property's name and owner and value on its channels
     */
    @Test
    public void updateXmlPropertyOnChan() {
        // extra channel for this test
        XmlChannel testChannelX = new XmlChannel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // A test property with name, owner, and 2 test channels
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"valueX")),new ArrayList<XmlTag>())));      
        // test property with different name, owner, and 1 different channel & 1 existing channel
        XmlProperty testProperty1WithChannels = new XmlProperty("testProperty1WithChannels","updateTestOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);

        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // change name and owner on existing channel, add to new channel
        propertyManager.update(testProperty0WithChannels.getName(), testProperty1WithChannels);

        XmlProperty expectedProperty = new XmlProperty("testProperty1WithChannels", "updateTestOwner");
        expectedProperty.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      

        // verify that the old property "testProperty0WithChannels" was replaced with the new "testProperty1WithChannels" and lists of channels were combined
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(),true).get();
            assertTrue("Failed to update the property", expectedProperty.equals(foundProperty));
        } catch (Exception e) {
            assertTrue("Failed to update/find the property", false);
        }        
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", propertyRepository.existsById(testProperty0WithChannels.getName()));       

        expectedProperty = new XmlProperty("testProperty1WithChannels", "updateTestOwner", "value0");
        // test property of old channel not in update
        assertTrue("The property attached to the channel " + testChannels.get(0).toString() + " doesn't match the new property",channelRepository.findById(testChannels.get(0).getName()).get().getProperties().get(0).equals(expectedProperty));

        expectedProperty = new XmlProperty("testProperty1WithChannels", "updateTestOwner", "value1");
        // test property of old channel and in update
        assertTrue("The property attached to the channel " + testChannels.get(1).toString() + " doesn't match the new property",channelRepository.findById(testChannels.get(1).getName()).get().getProperties().get(0).equals(expectedProperty));

        expectedProperty = new XmlProperty("testProperty1WithChannels", "updateTestOwner", "newValueX");
        // test property of new channel
        assertTrue("The property attached to the channel " + testChannelX.toString() + " doesn't match the new property",channelRepository.findById(testChannelX.getName()).get().getProperties().get(0).equals(expectedProperty));

        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }

    /**
     * Rename a property using update
     */
    @Test
    public void renameByUpdateXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner");
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        XmlProperty testProperty1WithChannels = new XmlProperty("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1,testProperty0WithChannels,testProperty1WithChannels);

        // Create the original properties
        XmlProperty createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        XmlProperty createdPropertyWithChannels = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // update the properties with new names, 0 -> 1
        XmlProperty updatedProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        XmlProperty updatedPropertyWithChannels = propertyManager.create(testProperty0WithChannels.getName(), testProperty1WithChannels);

        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty1.getName()).get();
            assertEquals("Failed to update the property", testProperty1, foundProperty);
        } catch (Exception e) {
            assertTrue("Failed to update/find the property", false);
        }        
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", propertyRepository.existsById(testProperty0.getName()));

        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        try {
            XmlProperty foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(), true).get();
            assertEquals("Failed to update the property w/ channels", testProperty1WithChannels, foundProperty);
        } catch (Exception e) {
            assertTrue("Failed to update/find the property w/ channels", false);
        }
        // verify that the old property is no longer present
        assertFalse("Failed to replace the old property", propertyRepository.existsById(testProperty0WithChannels.getName()));

        // TODO add test for failure case
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: none | update property channels: testChannel0 
     * Resultant property channels: testChannel0     
     */
    @Test
    public void updatePropertyTest1() {
        // A test property with only name and owner
        XmlProperty testProperty0 = new XmlProperty("testProperty0", "testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0);
        propertyManager.create(testProperty0.getName(), testProperty0);
        // Updating a property with no channels, the new channels should be added to the property
        // Add testChannel0 to testProperty0 which has no channels 
        testProperty0.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0.getName(),testProperty0.getOwner(),"value")),new ArrayList<XmlTag>())));      
        XmlProperty returnedTag = propertyManager.update(testProperty0.getName(), testProperty0);
        assertTrue("Failed to update property " + testProperty0, returnedTag.equals(testProperty0));
        assertTrue("Failed to update property " + testProperty0, testProperty0.equals(propertyRepository.findById(testProperty0.getName(), true).get()));
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0 | update property channels: testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest2() {
        // A test property with testChannel0
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0, the update operation should append the testChannel1 while leaving the existing channel unaffected.         
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        XmlProperty returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedTag.equals(testProperty0WithChannels));
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0 | update property channels: testChannel0,testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest3() {
        // A test property with testChannel0
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // testProperty0WithChannels already has testChannel0, the update request (which repeats the testChannel0) 
        // should append the testChannel1 while leaving the existing channel unaffected.    
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),      
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        XmlProperty returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedTag.equals(testProperty0WithChannels));
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0,testChannel1 | update property channels: testChannel0,testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest4() {
        // A test property with testChannel0,testChannel1
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),      
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0 & testChannel1, the update request should be a NOP. 
        XmlProperty returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedTag.equals(testProperty0WithChannels));
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0,testChannel1 | update property channels: testChannel0 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest5() {
        // A test property with testChannel0,testChannel1
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),      
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0 & testChannel1, the update request should be a NOP. 
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        XmlProperty returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        assertTrue("Failed to update property " + testProperty0WithChannels, returnedTag.equals(testProperty0WithChannels));
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),      
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        assertTrue("Failed to update property " + testProperty0WithChannels, testProperty0WithChannels.equals(propertyRepository.findById(testProperty0WithChannels.getName(), true).get()));
    }

    /**
     * Update multiple properties
     * Update on non-existing properties should result in the creation of the properties
     */
    @Test
    public void updateMultipleProperties() {
        // A test property with only name and owner
        XmlProperty testProperty0 = new XmlProperty("testProperty0", "testOwner");
        // A test property with name, owner, and test channels
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>()),      
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty0WithChannels);

        propertyManager.update(Arrays.asList(testProperty0,testProperty0WithChannels));
        // Query ChannelFinder and verify updated channels and properties
        XmlProperty foundProperty = propertyRepository.findById(testProperty0.getName(), true).get();
        assertTrue("Failed to update property " + testProperty0, foundProperty.equals(testProperty0));
        foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
        assertTrue("Failed to update property " + testProperty0WithChannels, foundProperty.equals(testProperty0WithChannels));  
    }

    /**
     * update properties' names and values and attempt to change owners on their channels
     */
    @Test
    public void updateMultipleXmlPropertiesOnChan() {
        // extra channel for this test
        XmlChannel testChannelX = new XmlChannel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // 2 test properties with name, owner, and test channels
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"valueX")),new ArrayList<XmlTag>())));      
        XmlProperty testProperty1WithChannels = new XmlProperty("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"valueX")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);

        propertyManager.create(Arrays.asList(testProperty0WithChannels,testProperty1WithChannels));
        // change owners and add channels and change values
        testProperty0WithChannels.setOwner("updateTestOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"newValue1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      
        testProperty1WithChannels.setOwner("updateTestOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValue0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      

        // update both properties
        propertyManager.update(Arrays.asList(testProperty0WithChannels,testProperty1WithChannels));
        // create expected properties
        XmlProperty expectedProperty0 = new XmlProperty("testProperty0WithChannels", "testOwner");
        expectedProperty0.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(expectedProperty0.getName(),expectedProperty0.getOwner(),"value0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(expectedProperty0.getName(),expectedProperty0.getOwner(),"newValue1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(expectedProperty0.getName(),expectedProperty0.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      
        XmlProperty expectedProperty1 = new XmlProperty("testProperty1WithChannels", "testOwner");
        expectedProperty1.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(expectedProperty1.getName(),expectedProperty1.getOwner(),"newValue0")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannels.get(1).getName(),testChannels.get(1).getOwner(),Arrays.asList(new XmlProperty(expectedProperty1.getName(),expectedProperty1.getOwner(),"value1")),new ArrayList<XmlTag>()),
                new XmlChannel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new XmlProperty(expectedProperty1.getName(),expectedProperty1.getOwner(),"newValueX")),new ArrayList<XmlTag>())));      

        // verify that the properties were updated
        try {
            XmlProperty foundProperty0 = propertyRepository.findById(testProperty0WithChannels.getName(),true).get();
            assertTrue("Failed to update the property" + expectedProperty0.toString(), expectedProperty0.equals(foundProperty0));
        } catch (Exception e) {
            assertTrue("Failed to update/find the property" + expectedProperty0.toString(), false);
        }        
        try {
            XmlProperty foundProperty1 = propertyRepository.findById(testProperty1WithChannels.getName(),true).get();
            assertTrue("Failed to update the property" + expectedProperty1.toString(), expectedProperty1.equals(foundProperty1));
        } catch (Exception e) {
            assertTrue("Failed to update/find the property" + expectedProperty1.toString(), false);
        }
        
        expectedProperty0 = new XmlProperty("testProperty0WithChannels", "testOwner", "value0");
        expectedProperty1 = new XmlProperty("testProperty1WithChannels", "testOwner", "newValue0");
        List<XmlProperty> expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channel0
        assertTrue("The property attached to the channel " + testChannels.get(0).toString() + " doesn't match the new property",channelRepository.findById(testChannels.get(0).getName()).get().getProperties().equals(expectedProperties));

        expectedProperty0 = new XmlProperty("testProperty0WithChannels", "testOwner", "newValue1");
        expectedProperty1 = new XmlProperty("testProperty1WithChannels", "testOwner", "value1");        
        expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channel1
        assertTrue("The property attached to the channel " + testChannels.get(1).toString() + " doesn't match the new property",channelRepository.findById(testChannels.get(1).getName()).get().getProperties().equals(expectedProperties));

        expectedProperty0 = new XmlProperty("testProperty0WithChannels", "testOwner", "newValueX");
        expectedProperty1 = new XmlProperty("testProperty1WithChannels", "testOwner", "newValueX");        
        expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channelX
        assertTrue("The property attached to the channel " + testChannelX.toString() + " doesn't match the new property",channelRepository.findById(testChannelX.getName()).get().getProperties().equals(expectedProperties));

        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }

    /**
     * delete a single property 
     */
    @Test
    public void deleteXmlProperty() {
        XmlProperty testProperty0 = new XmlProperty("testProperty0", "testOwner");
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        List<XmlProperty> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);
        cleanupTestProperties = testProperties;

        Iterable<XmlProperty> createdProperties = propertyManager.create(testProperties);

        propertyManager.remove(testProperty0.getName());
        // verify the property was deleted as expected
        assertTrue("Failed to delete the property", !propertyRepository.existsById(testProperty0.getName()));

        propertyManager.remove(testProperty0WithChannels.getName());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty0WithChannels", "*");
        // verify the property was deleted and removed from all associated channels
        assertTrue("Failed to delete the property", !propertyRepository.existsById(testProperty0WithChannels.getName()));
        assertEquals("Failed to delete the property from channels",
                new ArrayList<XmlChannel>(), channelRepository.search(params));
    }

    /**
     * delete a single property from a single channel 
     */
    @Test
    public void deleteXmlPropertyFromChannel() {
        XmlProperty testProperty0WithChannels = new XmlProperty("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new XmlChannel(testChannels.get(0).getName(),testChannels.get(0).getOwner(),Arrays.asList(new XmlProperty(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<XmlTag>())));      
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);

        XmlProperty createdProperty = propertyManager.create(testProperty0WithChannels.getName(),testProperty0WithChannels);

        propertyManager.removeSingle(testProperty0WithChannels.getName(),testChannels.get(0).getName());
        // verify the property was only removed from the single test channel
        assertTrue("Failed to not delete the property", propertyRepository.existsById(testProperty0WithChannels.getName()));

        // Verify the property is removed from the testChannel0
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.add("testProperty0WithChannels", "*");
        assertFalse("Failed to delete the property from channel", channelRepository.search(searchParameters).stream().anyMatch(ch -> {
            return ch.getName().equals(testChannels.get(0).getName());
        }));
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
}