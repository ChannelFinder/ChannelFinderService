package org.phoebus.channelfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@WebMvcTest(PropertyRepository.class)
@PropertySource(value = "classpath:application_test.properties")
public class PropertyRepositoryIT {

    @Autowired
    ElasticConfig esService;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * index a single property
     */
    @Test
    public void indexXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);

        Property createdProperty = propertyRepository.index(testProperty);
        // verify the property was created as expected
        assertEquals("Failed to create the property", testProperty, createdProperty);
    }

    /**
     * index multiple properties
     */
    @Test
    public void indexXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);
        // verify the properties were created as expected
        assertTrue("Failed to create the list of properties", Iterables.elementsEqual(testProperties, createdProperties));
    }

    /**
     * save a single property
     */
    @Test
    public void saveXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        Property updateTestProperty = new Property("testProperty","updateTestOwner");
        Property updateTestProperty1 = new Property("testProperty1","updateTestOwner1");
        cleanupTestProperties = Arrays.asList(testProperty,updateTestProperty,updateTestProperty1);

        Property createdProperty = propertyRepository.index(testProperty);
        Property updatedTestProperty = propertyRepository.save(updateTestProperty);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with the same name", updateTestProperty, updatedTestProperty);

        Property updatedTestProperty1 = propertyRepository.save("testProperty",updateTestProperty1);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with a different name", updateTestProperty1, updatedTestProperty1);
    }

    /**
     * save multiple properties
     */
    @Test
    public void saveXmlProperties() {
        Property testProperty = new Property("testProperty", "testOwner");
        Property updateTestProperty = new Property("testProperty", "updateTestOwner");
        Property testProperty1 = new Property("testProperty1", "testOwner1");
        Property updateTestProperty1 = new Property("testProperty1", "updateTestOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        List<Property> updateTestProperties = Arrays.asList(updateTestProperty, updateTestProperty1);
        cleanupTestProperties = updateTestProperties;

        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);
        Iterable<Property> updatedTestProperties = propertyRepository.saveAll(updateTestProperties);
        // verify the properties were updated as expected
        assertTrue("Failed to update the properties", Iterables.elementsEqual(updateTestProperties, updatedTestProperties));
    }

    /**
     * find a single property
     */
    @Test
    public void findXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);
        
        Optional<Property> notFoundProperty = propertyRepository.findById(testProperty.getName());
        // verify the property was not found as expected
        assertTrue("Found the property " + testProperty.getName() + " which should not exist.",
                notFoundProperty.isEmpty());

        Property createdProperty = propertyRepository.index(testProperty);

        Optional<Property> foundProperty = propertyRepository.findById(createdProperty.getName());
        // verify the property was found as expected
        assertEquals("Failed to find the property",createdProperty,foundProperty.get());

        testProperty.setValue("test");
        Channel channel = new Channel("testChannel","testOwner",Arrays.asList(testProperty),new ArrayList<Tag>());
        Channel createdChannel = channelRepository.index(channel);
        cleanupTestChannels = Arrays.asList(createdChannel);

        foundProperty = propertyRepository.findById(createdProperty.getName(),true);
        createdProperty.setChannels(Arrays.asList(channel));
        // verify the property was found as expected
        Property expectedProperty = new Property(createdProperty.getName(), createdProperty.getOwner());
        expectedProperty.setChannels(Arrays.asList(createdChannel));
        assertEquals("Failed to find the property", expectedProperty, foundProperty.get());

    }

    /**
     * check if a property exists
     */
    @Test
    public void testPropertyExists() {

        // check that non existing property returns false
        assertFalse("Failed to check the non existing property :" + "no-property", propertyRepository.existsById("no-property"));

        Property testProperty = new Property("testProperty","testOwner");
        assertFalse("Test property " + testProperty.getName() + " already exists", propertyRepository.existsById(testProperty.getName()));
        Property createdProperty = propertyRepository.index(testProperty);
        cleanupTestProperties = Arrays.asList(createdProperty);

        // verify the property exists as expected
        assertTrue("Failed to check the existance of " + testProperty.getName(), propertyRepository.existsById(testProperty.getName()));
    }

    /**
     * find all properties
     */
    @Test
    public void findAllXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        try {
            Set<Property> createdProperties = Sets.newHashSet(propertyRepository.indexAll(testProperties));
            Set<Property> listedProperties = Sets.newHashSet(propertyRepository.findAll());
            // verify the properties were listed as expected
            assertEquals("Failed to list all created properties", createdProperties, listedProperties);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * find multiple properties
     */
    @Test
    public void findXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty,testProperty1);
        List<String> propertyNames = Arrays.asList(testProperty.getName(),testProperty1.getName());
        Iterable<Property> notFoundProperties = null;
        Iterable<Property> foundProperties = null;
        cleanupTestProperties = testProperties;

        try {
            notFoundProperties = propertyRepository.findAllById(propertyNames);
        } catch (ResponseStatusException e) {            
        } finally {
            // verify the properties were not found as expected
            assertNotEquals("Found the properties",testProperties,notFoundProperties);
        }

        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);

        try {
            foundProperties = propertyRepository.findAllById(propertyNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the properties were found as expected
            assertEquals("Failed to find the properties",createdProperties,foundProperties);
        }
    }

    /**
     * delete a single property
     */
    @Test
    public void deleteXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        Optional<Property> notFoundProperty = propertyRepository.findById(testProperty.getName());
        Property createdProperty = propertyRepository.index(testProperty);
        createdProperty.setValue("testValue");
        Channel channel = new Channel("testChannel","testOwner",Arrays.asList(createdProperty),null);
        cleanupTestProperties = Arrays.asList(testProperty);

        Channel createdChannel = channelRepository.index(channel);
        cleanupTestChannels = Arrays.asList(createdChannel);
        propertyRepository.deleteById(createdProperty.getName());
        // verify the property was deleted as expected
        assertNotEquals("Failed to delete property",testProperty,propertyRepository.findById(testProperty.getName()));

        Channel foundChannel = channelRepository.findById("testChannel").get();
        // verify the property was deleted from channels as expected
        assertTrue("Failed to remove property from channel",foundChannel.getProperties().isEmpty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty","*");
        List<Channel> chans = channelRepository.search(params).getChannels();
        // verify the property was deleted from channels as expected
        assertTrue("Failed to remove property from channel", chans.isEmpty());
    }

    // helper operations to clean up proprepoIT
    private List<Property> cleanupTestProperties = Collections.emptyList();

    private List<Channel> cleanupTestChannels = Collections.emptyList();

    @After
    public void cleanup() {
        // clean up
        cleanupTestProperties.forEach(property -> {
            if (propertyRepository.existsById(property.getName())) {
                propertyRepository.deleteById(property.getName());
            }            
        });

        cleanupTestChannels.forEach(channel -> {
            if (channelRepository.existsById(channel.getName())) {
                channelRepository.deleteById(channel.getName());
            }
        });
    }
}