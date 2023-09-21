package org.phoebus.channelfinder;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static org.junit.jupiter.api.Assertions.fail;


@WebMvcTest(PropertyManager.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
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
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        testProperty1.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),"value")),new ArrayList<Tag>())));
        List<Property> testProperties = Arrays.asList(testProperty0,testProperty1);
        cleanupTestProperties = testProperties;

        Iterable<Property> createdProperties = propertyManager.create(testProperties);
        Iterable<Property> propertyList = propertyManager.list();
        for(Property property: createdProperties) {
            property.setChannels(new ArrayList<Channel>());
        }
        // verify the properties were listed as expected
        Assertions.assertEquals(createdProperties, propertyList, "Failed to list all properties");
    }

    /**
     * read a single property
     * test the "withChannels" flag
     */
    @Test
    public void readXmlProperty() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner");
        testProperty1.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),
                                testChannel0.getOwner(),
                                Arrays.asList(new Property(testProperty1.getName(),testProperty1.getOwner(),"value")),
                                new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);

        Property createdProperty0 = propertyManager.create(testProperty0.getName(),testProperty0);
        Property createdProperty1 = propertyManager.create(testProperty1.getName(),testProperty1);

        // verify the created properties are read as expected
        // retrieve the testProperty0 without channels
        Property retrievedProperty = propertyManager.read(createdProperty0.getName(), false);
        Assertions.assertEquals(createdProperty0, retrievedProperty, "Failed to read the property");
        // retrieve the testProperty0 with channels
        retrievedProperty = propertyManager.read(createdProperty0.getName(), true);
        Assertions.assertEquals(createdProperty0, retrievedProperty, "Failed to read the property w/ channels");

        retrievedProperty = propertyManager.read(createdProperty1.getName(), false);
        // verify the property was read as expected
        testProperty1.setChannels(new ArrayList<Channel>());
        Assertions.assertEquals(testProperty1, retrievedProperty, "Failed to read the property");

        retrievedProperty = propertyManager.read(createdProperty1.getName(), true);
        // verify the property was read as expected
        Assertions.assertEquals(createdProperty1, retrievedProperty, "Failed to read the property w/ channels");
    }

    /**
     * attempt to read a single non existent property
     */
    @Test
    public void readNonExistingXmlProperty() {
        // verify the property failed to be read, as expected
        Assertions.assertThrows(ResponseStatusException.class, () -> propertyManager.read("fakeProperty", false));
    }

    /**
     * attempt to read a single non existent property with channels
     */
    @Test
    public void readNonExistingXmlProperty2() {
        // verify the property failed to be read, as expected
        Assertions.assertThrows(ResponseStatusException.class, () -> propertyManager.read("fakeProperty", true));
    }

    /**
     * create a simple property
     */
    @Test
    public void createXmlProperty() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0);

        // Create a simple property
        Property createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        Assertions.assertEquals(testProperty0, createdProperty, "Failed to create the property");

        //        Tag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
        //        // verify the property was created as expected
        //        assertEquals("Failed to create the property",testTag1,createdTag1);

        // Update the test property with a new owner
        Property updatedTestProperty0 = new Property("testProperty0", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0.getName(), updatedTestProperty0);
        Assertions.assertEquals(updatedTestProperty0, createdProperty, "Failed to create the property");
    }

    /**
     * Rename a simple property using create
     */
    @Test
    public void renameByCreateXmlProperty() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1);

        Property createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        createdProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        Assertions.assertEquals(testProperty1, createdProperty, "Failed to create the property");
        // verify that the old property is no longer present
        Assertions.assertFalse(propertyRepository.existsById(testProperty0.getName()), "Failed to replace the old property");
    }

    /**
     * create a single property with channels
     */
    @Test
    public void createXmlProperty2() {
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);

        Property createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        try {
            Property foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
            Assertions.assertEquals(testProperty0WithChannels, foundProperty, "Failed to create the property w/ channels. Expected " + testProperty0WithChannels.toLog() + " found "
                    + foundProperty.toLog());
        } catch (Exception e) {
            fail("Failed to create/find the property w/ channels");
        }

        //        Tag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
        //        // verify the property was created as expected
        //        assertEquals("Failed to create the property",testTag1,createdTag1);

        // Update the test property with a new owner
        Property updatedTestProperty0WithChannels = new Property("testProperty0WithChannels", "updateTestOwner");
        createdProperty = propertyManager.create(testProperty0WithChannels.getName(), updatedTestProperty0WithChannels);
        try {
            Property foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
            Assertions.assertEquals(updatedTestProperty0WithChannels, foundProperty, "Failed to create the property w/ channels. Expected " + updatedTestProperty0WithChannels.toLog() + " found "
                    + foundProperty.toLog());
        } catch (Exception e) {
            fail("Failed to create/find the property w/ channels");
        }
    }

    /**
     * Rename a single property with channels using create
     */
    @Test
    public void renameByCreateXmlProperty2() {
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property testProperty1WithChannels = new Property("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);

        // Create the testProperty0WithChannels
        Property createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // update the testProperty0WithChannels with testProperty1WithChannels
        createdProperty = propertyManager.create(testProperty0WithChannels.getName(), testProperty1WithChannels);
        try {
            Property foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(), true).get();
            Assertions.assertEquals(testProperty1WithChannels, foundProperty, "Failed to create the property w/ channels");
        } catch (Exception e) {
            fail("Failed to create/find the property w/ channels");
        }
        Assertions.assertFalse(propertyRepository.existsById(testProperty0WithChannels.getName()), "Failed to replace the old property");
    }    

    /**
     * create multiple properties
     */
    @Test
    public void createXmlProperties() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner");
        Property testProperty2 = new Property("testProperty2","testOwner");

        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property testProperty1WithChannels = new Property("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property testProperty2WithChannels = new Property("testProperty2WithChannels","testOwner");
        testProperty2WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty2WithChannels.getName(),testProperty2WithChannels.getOwner(),"value")),new ArrayList<Tag>())));

        List<Property> testProperties = Arrays.asList(testProperty0,testProperty1,testProperty2,testProperty0WithChannels,testProperty1WithChannels,testProperty2WithChannels);
        cleanupTestProperties = testProperties;

        Iterable<Property> createdProperties = propertyManager.create(testProperties);
        List<Property> foundProperties = new ArrayList<Property>();
        testProperties.forEach(prop -> foundProperties.add(propertyRepository.findById(prop.getName(),true).get()));
        Assertions.assertTrue(foundProperties.containsAll(Arrays.asList(testProperty0, testProperty1, testProperty2)), "Failed to create the properties");
        Channel testChannel0With3Props = new Channel(
                testChannel0.getName(),
                testChannel0.getOwner(),
                Arrays.asList(
                        new Property(testProperty0WithChannels.getName(), testProperty0WithChannels.getOwner(), "value"),
                        new Property(testProperty1WithChannels.getName(), testProperty0WithChannels.getOwner(), "value"),
                        new Property(testProperty2WithChannels.getName(), testProperty0WithChannels.getOwner(), "value")),
                EMPTY_LIST);
        Property expectedTestProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        expectedTestProperty0WithChannels.setChannels(Arrays.asList(testChannel0With3Props));
        Assertions.assertTrue(foundProperties.contains(expectedTestProperty0WithChannels));
    }

    /**
     * create by overriding multiple properties
     */
    @Test
    public void createXmlPropertiesWithOverride() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        List<Property> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);
        cleanupTestProperties = testProperties;

        //Create a set of original properties to be overriden
        propertyManager.create(testProperties);
        // Now update the test properties
        testProperty0.setOwner("testOwner-updated");
        testProperty0WithChannels.setOwner("testOwner-updated");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        
        List<Property> updatedTestProperties = Arrays.asList(testProperty0,testProperty0WithChannels);
        propertyManager.create(updatedTestProperties);
       
        // set owner back to original since it shouldn't change
        testProperty0.setOwner("testOwner");
        testProperty0WithChannels.setOwner("testOwner"); 
        
        List<Property> foundProperties = new ArrayList<Property>();
        testProperties.forEach(prop -> foundProperties.add(propertyRepository.findById(prop.getName(),true).get()));
        // verify the properties were created as expected
        Assertions.assertTrue(Iterables.elementsEqual(updatedTestProperties, foundProperties), "Failed to create the properties");

        testChannels.get(1).setProperties(Arrays.asList(new Property("testProperty0WithChannels","testOwner","value")));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty0WithChannels", "*");
        // verify the property was removed from the old channels
        Assertions.assertEquals(Arrays.asList(testChannels.get(1)), channelRepository.search(params).getChannels(), "Failed to delete the property from channels");
    }

    /**
     * add a single property to a single channel
     * @Todo fix this test after addsingle method is fixed
     */
    @Test
    public void addSingleXmlProperty() {
        Property testProperty0 = new Property("testProperty0", "testOwner");
        propertyRepository.index(testProperty0);
        testProperty0.setValue("value");
        cleanupTestProperties = Arrays.asList(testProperty0);

        propertyManager.addSingle(testProperty0.getName(), "testChannel0", testProperty0);
        Assertions.assertTrue(channelRepository.findById("testChannel0").get().getProperties().stream().anyMatch(p -> {
            return p.getName().equals(testProperty0.getName());
        }), "Failed to add property");
    }

    /**
     * update a property
     */
    @Test
    public void updateXmlProperty() {
        // A test property with only name and owner
        Property testProperty0 = new Property("testProperty0", "testOwner");
        // A test property with name, owner, and a single test channel with a copy of the property with a value and no channels
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        List<Property> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);
        cleanupTestProperties = testProperties;

        // Update on a non-existing property should result in the creation of that property
        // 1. Test a simple property 
        Property returnedProperty = propertyManager.update(testProperty0.getName(), testProperty0);
        Assertions.assertEquals(testProperty0, returnedProperty, "Failed to update property " + testProperty0);
        Assertions.assertEquals(testProperty0, propertyRepository.findById(testProperty0.getName()).get(), "Failed to update property " + testProperty0);
        // 2. Test a property with channels
        returnedProperty = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedProperty, "Failed to update property " + testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);

        // Update the property owner
        testProperty0.setOwner("newTestOwner");
        returnedProperty = propertyManager.update(testProperty0.getName(), testProperty0);
        Assertions.assertEquals(testProperty0, returnedProperty, "Failed to update property " + testProperty0);
        Assertions.assertEquals(testProperty0, propertyRepository.findById(testProperty0.getName()).get(), "Failed to update property " + testProperty0);
        testProperty0WithChannels.setOwner("newTestOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        returnedProperty = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedProperty, "Failed to update property " + testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * update a property's name and owner and value on its channels
     */
    @Test
    public void updateXmlPropertyOnChan() {
        // extra channel for this test
        Channel testChannelX = new Channel("testChannelX","testOwner");
        channelRepository.index(testChannelX);

        // A test property with name, owner, and 2 test channels
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value0")), EMPTY_LIST),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"valueX")), EMPTY_LIST)));
        // test property with different name, owner, and 1 different channel & 1 existing channel
        Property testProperty1WithChannels = new Property("testProperty1WithChannels","updateTestOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),EMPTY_LIST),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),EMPTY_LIST)));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);

        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // change name and owner on existing channel, add to new channel
        propertyManager.update(testProperty0WithChannels.getName(), testProperty1WithChannels);

        Property expectedProperty = new Property("testProperty1WithChannels", "updateTestOwner");
        expectedProperty.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value0")),EMPTY_LIST),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),EMPTY_LIST),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),EMPTY_LIST)));

        // verify that the old property "testProperty0WithChannels" was replaced with the new "testProperty1WithChannels" and lists of channels were combined
//        Optional<Property> foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(), true);
//        assertTrue("Failed to update the property",
//                foundProperty.isPresent() &&
//                expectedProperty.equals(foundProperty));

        // verify that the old property is no longer present
        Assertions.assertFalse(propertyRepository.existsById(testProperty0WithChannels.getName()), "Failed to replace the old property");

        expectedProperty = new Property("testProperty1WithChannels", "updateTestOwner", "value0");
        // test property of old channel not in update
        Assertions.assertTrue(channelRepository.findById(testChannel0.getName()).get().getProperties().contains(expectedProperty), "The property attached to the channel " + testChannels.get(0).toString() + " doesn't match the new property");

        expectedProperty = new Property("testProperty1WithChannels", "updateTestOwner", "value1");
        // test property of old channel and in update
        Assertions.assertTrue(channelRepository.findById(testChannel1.getName()).get().getProperties().contains(expectedProperty), "The property attached to the channel " + testChannels.get(1).toString() + " doesn't match the new property");

        expectedProperty = new Property("testProperty1WithChannels", "updateTestOwner", "newValueX");
        // test property of new channel
        Assertions.assertTrue(channelRepository.findById(testChannelX.getName()).get().getProperties().contains(expectedProperty), "The property attached to the channel " + testChannelX.toString() + " doesn't match the new property");

        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }

    /**
     * Rename a property using update
     */
    @Test
    public void renameByUpdateXmlProperty() {
        Property testProperty0 = new Property("testProperty0","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner");
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property testProperty1WithChannels = new Property("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty1,testProperty0WithChannels,testProperty1WithChannels);

        // Create the original properties
        Property createdProperty = propertyManager.create(testProperty0.getName(), testProperty0);
        Property createdPropertyWithChannels = propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // update the properties with new names, 0 -> 1
        Property updatedProperty = propertyManager.create(testProperty0.getName(), testProperty1);
        Property updatedPropertyWithChannels = propertyManager.create(testProperty0WithChannels.getName(), testProperty1WithChannels);

        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        try {
            Property foundProperty = propertyRepository.findById(testProperty1.getName()).get();
            Assertions.assertEquals(testProperty1, foundProperty, "Failed to update the property");
        } catch (Exception e) {
            fail("Failed to update/find the property");
        }        
        // verify that the old property is no longer present
        Assertions.assertFalse(propertyRepository.existsById(testProperty0.getName()), "Failed to replace the old property");

        // verify that the old property "testProperty0" was replaced with the new "testProperty1"
        try {
            Property foundProperty = propertyRepository.findById(testProperty1WithChannels.getName(), true).get();
            Assertions.assertEquals(testProperty1WithChannels, foundProperty, "Failed to update the property w/ channels");
        } catch (Exception e) {
            fail("Failed to update/find the property w/ channels");
        }
        // verify that the old property is no longer present
        Assertions.assertFalse(propertyRepository.existsById(testProperty0WithChannels.getName()), "Failed to replace the old property");

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
        Property testProperty0 = new Property("testProperty0", "testOwner");
        cleanupTestProperties = Arrays.asList(testProperty0);
        propertyManager.create(testProperty0.getName(), testProperty0);
        // Updating a property with no channels, the new channels should be added to the property
        // Add testChannel0 to testProperty0 which has no channels 
        testProperty0.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0.getName(),testProperty0.getOwner(),"value")),new ArrayList<Tag>())));
        Property returnedTag = propertyManager.update(testProperty0.getName(), testProperty0);
        Assertions.assertEquals(testProperty0, returnedTag, "Failed to update property " + testProperty0);
        Assertions.assertEquals(testProperty0, propertyRepository.findById(testProperty0.getName(), true).get(), "Failed to update property " + testProperty0);
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0 | update property channels: testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest2() {
        // A test property with testChannel0
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0, the update operation should append the testChannel1 while leaving the existing channel unaffected.         
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedTag, "Failed to update property " + testProperty0WithChannels);
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0 | update property channels: testChannel0,testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest3() {
        // A test property with testChannel0
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // testProperty0WithChannels already has testChannel0, the update request (which repeats the testChannel0) 
        // should append the testChannel1 while leaving the existing channel unaffected.    
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedTag, "Failed to update property " + testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0,testChannel1 | update property channels: testChannel0,testChannel1 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest4() {
        // A test property with testChannel0,testChannel1
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0 & testChannel1, the update request should be a NOP. 
        Property returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedTag, "Failed to update property " + testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * Update the channels/values associated with a property
     * Existing property channels: testChannel0,testChannel1 | update property channels: testChannel0 
     * Resultant property channels: testChannel0,testChannel1     
     */
    @Test
    public void updatePropertyTest5() {
        // A test property with testChannel0,testChannel1
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);
        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        // Updating a property with existing channels, the new channels should be added without affecting existing channels
        // testProperty0WithChannels already has testChannel0 & testChannel1, the update request should be a NOP. 
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Property returnedTag = propertyManager.update(testProperty0WithChannels.getName(), testProperty0WithChannels);
        Assertions.assertEquals(testProperty0WithChannels, returnedTag, "Failed to update property " + testProperty0WithChannels);
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        Assertions.assertEquals(testProperty0WithChannels, propertyRepository.findById(testProperty0WithChannels.getName(), true).get(), "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * Update the value of a property when channels have multiple properties
     */
    @Test
    public void updatePropertyWithChannelsTest() {
        // A test property with testChannel0,testChannel1
        Property testProperty0WithChannels = new Property("testProperty0WithChannels", "testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(), testChannel0.getOwner(), List.of(new Property(testProperty0WithChannels.getName(), testProperty0WithChannels.getOwner(), "property0channel0")), new ArrayList<Tag>()),
                new Channel(testChannel1.getName(), testChannel1.getOwner(), List.of(new Property(testProperty0WithChannels.getName(), testProperty0WithChannels.getOwner(), "property0channel1")), new ArrayList<Tag>())));

        Property testProperty1WithChannels = new Property("testProperty1WithChannels", "testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(), testChannel0.getOwner(), List.of(new Property(testProperty1WithChannels.getName(), testProperty1WithChannels.getOwner(), "property1channel0")), new ArrayList<Tag>()),
                new Channel(testChannel1.getName(), testChannel1.getOwner(), List.of(new Property(testProperty1WithChannels.getName(), testProperty1WithChannels.getOwner(), "property1channel1")), new ArrayList<Tag>())));

        propertyManager.create(testProperty0WithChannels.getName(), testProperty0WithChannels);
        propertyManager.create(testProperty1WithChannels.getName(), testProperty1WithChannels);

        Property newValueProperty = new Property(testProperty1WithChannels.getName(), testProperty1WithChannels.getOwner(), "newValueProperty");
        newValueProperty.setChannels(List.of(
                new Channel(testChannel1.getName(), testChannel1.getOwner(), List.of(new Property(newValueProperty.getName(), newValueProperty.getOwner(), "newValueProperty")), new ArrayList<Tag>())));
        propertyManager.update(newValueProperty.getName(), newValueProperty);

        List<Property> expected0Properties = List.of(new Property(testProperty0WithChannels.getName(), testProperty0WithChannels.getOwner(), "property0channel0"), new Property(testProperty1WithChannels.getName(), testProperty1WithChannels.getOwner(), "property1channel0"));
        Assertions.assertEquals(expected0Properties, channelRepository.findById(testChannel0.getName()).get().getProperties());
        List<Property> expected1Properties = List.of(new Property(testProperty0WithChannels.getName(), testProperty0WithChannels.getOwner(), "property0channel1"), new Property(newValueProperty.getName(), newValueProperty.getOwner(), "newValueProperty"));
        Assertions.assertEquals(expected1Properties, channelRepository.findById(testChannel1.getName()).get().getProperties());
    }

    /**
     * Update multiple properties
     * Update on non-existing properties should result in the creation of the properties
     */
    @Test
    public void updateMultipleProperties() {
        // A test property with only name and owner
        Property testProperty0 = new Property("testProperty0", "testOwner");
        // A test property with name, owner, and test channels
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>()),
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0,testProperty0WithChannels);

        propertyManager.update(Arrays.asList(testProperty0,testProperty0WithChannels));
        // Query ChannelFinder and verify updated channels and properties
        Property foundProperty = propertyRepository.findById(testProperty0.getName(), true).get();
        Assertions.assertEquals(testProperty0, foundProperty, "Failed to update property " + testProperty0);
        foundProperty = propertyRepository.findById(testProperty0WithChannels.getName(), true).get();
        Assertions.assertEquals(testProperty0WithChannels, foundProperty, "Failed to update property " + testProperty0WithChannels);
    }

    /**
     * update properties' names and values and attempt to change owners on their channels
     */
    @Test
    public void updateMultipleXmlPropertiesOnChan() {
        // extra channel for this test
        Channel testChannelX = new Channel("testChannelX","testOwner");
        channelRepository.index(testChannelX);
        // 2 test properties with name, owner, and test channels
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value0")),new ArrayList<Tag>()),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"valueX")),new ArrayList<Tag>())));
        Property testProperty1WithChannels = new Property("testProperty1WithChannels","testOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"value1")),new ArrayList<Tag>()),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"valueX")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels,testProperty1WithChannels);

        propertyManager.create(Arrays.asList(testProperty0WithChannels,testProperty1WithChannels));
        // change owners and add channels and change values
        testProperty0WithChannels.setOwner("updateTestOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel1.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"newValue1")),EMPTY_LIST),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"newValueX")),EMPTY_LIST)));
        testProperty1WithChannels.setOwner("updateTestOwner");
        testProperty1WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel1.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValue0")),EMPTY_LIST),
                new Channel(testChannelX.getName(),testChannelX.getOwner(),Arrays.asList(new Property(testProperty1WithChannels.getName(),testProperty1WithChannels.getOwner(),"newValueX")),EMPTY_LIST)));

        // update both properties
        propertyManager.update(Arrays.asList(testProperty0WithChannels,testProperty1WithChannels));
        // create expected properties

        // verify that the properties were updated
//        Optional<Property> foundProperty0 = propertyRepository.findById(testProperty0WithChannels.getName(), true);
//        assertTrue("Failed to update the property" + expectedProperty0.toString(),
//                foundProperty0.isPresent() &&
//                foundProperty0.get().getName().equalsIgnoreCase("testProperty0WithChannels") &&
//                foundProperty0.get().getChannels().);
//        assertEquals("Failed to update the property" + expectedProperty0.toString(),
//                expectedProperty0, foundProperty0);
//
//        Optional<Property> foundProperty1 = propertyRepository.findById(testProperty1WithChannels.getName(), true);
//        assertTrue("Failed to update the property" + expectedProperty1.toString(),
//                expectedProperty1.equals(foundProperty1));

        Property expectedProperty0 = new Property("testProperty0WithChannels", "testOwner", "value0");
        Property expectedProperty1 = new Property("testProperty1WithChannels", "testOwner", "newValue0");
        List<Property> expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channel0
        Assertions.assertEquals(expectedProperties, channelRepository.findById(testChannel0.getName()).get().getProperties(), "The property attached to the channel " + testChannels.get(0).toString() + " doesn't match the new property");

        expectedProperty0 = new Property("testProperty0WithChannels", "testOwner", "newValue1");
        expectedProperty1 = new Property("testProperty1WithChannels", "testOwner", "value1");
        expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channel1
        Assertions.assertTrue(channelRepository.findById(testChannel1.getName()).get().getProperties().containsAll(expectedProperties), "The property attached to the channel " + testChannels.get(1).toString() + " doesn't match the new property");

        expectedProperty0 = new Property("testProperty0WithChannels", "testOwner", "newValueX");
        expectedProperty1 = new Property("testProperty1WithChannels", "testOwner", "newValueX");
        expectedProperties = Arrays.asList(expectedProperty0,expectedProperty1);
        // test property of channelX
        Assertions.assertTrue(channelRepository.findById(testChannelX.getName()).get().getProperties().containsAll(expectedProperties), "The property attached to the channel " + testChannelX.toString() + " doesn't match the new property");

        // clean extra channel
        channelRepository.deleteById(testChannelX.getName());
    }

    /**
     * delete a single property 
     */
    @Test
    public void deleteXmlProperty() {
        Property testProperty0 = new Property("testProperty0", "testOwner");
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),EMPTY_LIST)));
        List<Property> testProperties = Arrays.asList(testProperty0,testProperty0WithChannels);
        cleanupTestProperties = testProperties;

        Iterable<Property> createdProperties = propertyManager.create(testProperties);

        propertyManager.remove(testProperty0.getName());
        // verify the property was deleted as expected
        Assertions.assertFalse( propertyRepository.existsById(testProperty0.getName()), "Failed to delete the property");

        propertyManager.remove(testProperty0WithChannels.getName());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty0WithChannels", "*");
        // verify the property was deleted and removed from all associated channels
        Assertions.assertFalse(propertyRepository.existsById(testProperty0WithChannels.getName()), "Failed to delete the property");
        Assertions.assertEquals(new ArrayList<Channel>(), channelRepository.search(params).getChannels(), "Failed to delete the property from channels");
    }

    /**
     * delete a single property from a single channel 
     */
    @Test
    public void deleteXmlPropertyFromChannel() {
        Property testProperty0WithChannels = new Property("testProperty0WithChannels","testOwner");
        testProperty0WithChannels.setChannels(Arrays.asList(
                new Channel(testChannel0.getName(),testChannel0.getOwner(),Arrays.asList(new Property(testProperty0WithChannels.getName(),testProperty0WithChannels.getOwner(),"value")),new ArrayList<Tag>())));
        cleanupTestProperties = Arrays.asList(testProperty0WithChannels);

        Property createdProperty = propertyManager.create(testProperty0WithChannels.getName(),testProperty0WithChannels);

        propertyManager.removeSingle(testProperty0WithChannels.getName(),testChannel0.getName());
        // verify the property was only removed from the single test channel
        Assertions.assertTrue(propertyRepository.existsById(testProperty0WithChannels.getName()), "Failed to not delete the property");

        // Verify the property is removed from the testChannel0
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.add("testProperty0WithChannels", "*");
        Assertions.assertFalse(channelRepository.search(searchParameters).getChannels().stream().anyMatch(ch -> {
            return ch.getName().equals(testChannel0.getName());
        }), "Failed to delete the property from channel");
    }



    // Helper operations to create and clean up the resources needed for successful
    // testing of the PropertyManager operations

    private Channel testChannel0 = new Channel("testChannel0", "testOwner");
    private Channel testChannel1 = new Channel("testChannel1", "testOwner");
    private Channel testChannelX = new Channel("testChannelX", "testOwner");

    private final List<Channel> testChannels = Arrays.asList(testChannel0,
            testChannel1,
            testChannelX);

    private List<Property> cleanupTestProperties = Collections.emptyList();

    @BeforeEach
    public void setup() {
        channelRepository.indexAll(testChannels);
    }

    @AfterEach
    public void cleanup() {
        // clean up
        testChannels.forEach(channel -> {
            try {
                if (channelRepository.existsById(channel.getName()))
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