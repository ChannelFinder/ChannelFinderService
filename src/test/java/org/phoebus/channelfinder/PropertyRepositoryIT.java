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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);

        XmlProperty createdProperty = propertyRepository.index(testProperty);
        // verify the property was created as expected
        assertEquals("Failed to create the property", testProperty, createdProperty);
    }

    /**
     * index multiple properties
     */
    @Test
    public void indexXmlProperties() {
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");    
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        // verify the properties were created as expected
        assertTrue("Failed to create the list of properties", Iterables.elementsEqual(testProperties, createdProperties));
    }

    /**
     * save a single property
     */
    @Test
    public void saveXmlProperty() {
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        XmlProperty updateTestProperty = new XmlProperty("testProperty","updateTestOwner");
        XmlProperty updateTestProperty1 = new XmlProperty("testProperty1","updateTestOwner1");
        cleanupTestProperties = Arrays.asList(testProperty,updateTestProperty,updateTestProperty1);

        XmlProperty createdProperty = propertyRepository.index(testProperty);
        XmlProperty updatedTestProperty = propertyRepository.save(updateTestProperty);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with the same name", updateTestProperty, updatedTestProperty);

        XmlProperty updatedTestProperty1 = propertyRepository.save("testProperty",updateTestProperty1);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with a different name", updateTestProperty1, updatedTestProperty1);
    }

    /**
     * save multiple properties
     */
    @Test
    public void saveXmlProperties() {
        XmlProperty testProperty = new XmlProperty("testProperty", "testOwner");
        XmlProperty updateTestProperty = new XmlProperty("testProperty", "updateTestOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1", "testOwner1");
        XmlProperty updateTestProperty1 = new XmlProperty("testProperty1", "updateTestOwner1");
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);
        List<XmlProperty> updateTestProperties = Arrays.asList(updateTestProperty, updateTestProperty1);
        cleanupTestProperties = updateTestProperties;

        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        Iterable<XmlProperty> updatedTestProperties = propertyRepository.saveAll(updateTestProperties);
        // verify the properties were updated as expected
        assertTrue("Failed to update the properties", Iterables.elementsEqual(updateTestProperties, updatedTestProperties));
    }

    /**
     * find a single property
     */
    @Test
    public void findXmlProperty() {
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);
        
        Optional<XmlProperty> notFoundProperty = propertyRepository.findById(testProperty.getName());
        // verify the property was not found as expected
        assertTrue("Found the property " + testProperty.getName() + " which should not exist.",
                notFoundProperty.isEmpty());

        XmlProperty createdProperty = propertyRepository.index(testProperty);

        Optional<XmlProperty> foundProperty = propertyRepository.findById(createdProperty.getName());
        // verify the property was found as expected
        assertEquals("Failed to find the property",createdProperty,foundProperty.get());

        testProperty.setValue("test");
        XmlChannel channel = new XmlChannel("testChannel","testOwner",Arrays.asList(testProperty),new ArrayList<XmlTag>());
        XmlChannel createdChannel = channelRepository.index(channel);

        foundProperty = propertyRepository.findById(createdProperty.getName(),true);
        createdProperty.setChannels(Arrays.asList(channel));
        // verify the property was found as expected
        XmlProperty expectedProperty = new XmlProperty(createdProperty.getName(), createdProperty.getOwner());
        expectedProperty.setChannels(Arrays.asList(createdChannel));
        assertEquals("Failed to find the property", expectedProperty, foundProperty.get());

        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * check if a property exists
     */
    @Test
    public void testPropertyExists() {

        // check that non existing property returns false
        assertFalse("Failed to check the non existing property :" + "no-property", propertyRepository.existsById("no-property"));

        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        assertFalse("Test property " + testProperty.getName() + " already exists", propertyRepository.existsById(testProperty.getName()));
        XmlProperty createdProperty = propertyRepository.index(testProperty);
        cleanupTestProperties = Arrays.asList(createdProperty);

        // verify the property exists as expected
        assertTrue("Failed to check the existance of " + testProperty.getName(), propertyRepository.existsById(testProperty.getName()));
    }

    /**
     * find all properties
     */
    @Test
    public void findAllXmlProperties() {
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");    
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        try {
            Set<XmlProperty> createdProperties = Sets.newHashSet(propertyRepository.indexAll(testProperties));
            Set<XmlProperty> listedProperties = Sets.newHashSet(propertyRepository.findAll());
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
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");    
        List<XmlProperty> testProperties = Arrays.asList(testProperty,testProperty1);
        List<String> propertyNames = Arrays.asList(testProperty.getName(),testProperty1.getName());
        Iterable<XmlProperty> notFoundProperties = null;
        Iterable<XmlProperty> foundProperties = null;
        cleanupTestProperties = testProperties;

        try {
            notFoundProperties = propertyRepository.findAllById(propertyNames);
        } catch (ResponseStatusException e) {            
        } finally {
            // verify the properties were not found as expected
            assertNotEquals("Found the properties",testProperties,notFoundProperties);
        }

        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);

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
        XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
        Optional<XmlProperty> notFoundProperty = propertyRepository.findById(testProperty.getName());
        XmlProperty createdProperty = propertyRepository.index(testProperty);
        createdProperty.setValue("testValue");
        XmlChannel channel = new XmlChannel("testChannel","testOwner",Arrays.asList(createdProperty),null);        
        cleanupTestProperties = Arrays.asList(testProperty);

        XmlChannel createdChannel = channelRepository.index(channel);
        propertyRepository.deleteById(createdProperty.getName());
        // verify the property was deleted as expected
        assertNotEquals("Failed to delete property",testProperty,propertyRepository.findById(testProperty.getName()));

        XmlChannel foundChannel = channelRepository.findById("testChannel").get();
        // verify the property was deleted from channels as expected
        assertTrue("Failed to remove property from channel",foundChannel.getProperties().isEmpty());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty","*");
        List<XmlChannel> chans = channelRepository.search(params);
        // verify the property was deleted from channels as expected
        assertTrue("Failed to remove property from channel", chans.isEmpty());

        // channel clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    // helper operations to clean up proprepoIT

    private List<XmlProperty> cleanupTestProperties = Collections.emptyList();

    @After
    public void cleanup() {
        // clean up
        cleanupTestProperties.forEach(property -> {
            if (propertyRepository.existsById(property.getName())) {
                propertyRepository.deleteById(property.getName());
            }            
        });
    }
}