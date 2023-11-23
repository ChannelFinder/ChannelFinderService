package org.phoebus.channelfinder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(PropertyRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
class PropertyRepositoryIT {

    @Autowired
    ElasticConfig esService;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    @AfterAll
    void tearDown() throws IOException {
        ElasticConfigIT.teardown(esService);
    }
    /**
     * index a single property
     */
    @Test
    void indexXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);

        Property createdProperty = propertyRepository.index(testProperty);
        // verify the property was created as expected
        Assertions.assertEquals(testProperty, createdProperty, "Failed to create the property");
    }

    /**
     * index multiple properties
     */
    @Test
    void indexXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);
        // verify the properties were created as expected
        Assertions.assertTrue(Iterables.elementsEqual(testProperties, createdProperties), "Failed to create the list of properties");
    }

    /**
     * save a single property
     */
    @Test
    void saveXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        Property updateTestProperty = new Property("testProperty","updateTestOwner");
        Property updateTestProperty1 = new Property("testProperty1","updateTestOwner1");
        cleanupTestProperties = Arrays.asList(testProperty,updateTestProperty,updateTestProperty1);

        Property createdProperty = propertyRepository.index(testProperty);
        Property updatedTestProperty = propertyRepository.save(updateTestProperty);
        // verify the property was updated as expected
        Assertions.assertEquals(updateTestProperty, updatedTestProperty, "Failed to update the property with the same name");

        Property updatedTestProperty1 = propertyRepository.save("testProperty",updateTestProperty1);
        // verify the property was updated as expected
        Assertions.assertEquals(updateTestProperty1, updatedTestProperty1, "Failed to update the property with a different name");
    }

    /**
     * save multiple properties
     */
    @Test
    void saveXmlProperties() {
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
        Assertions.assertTrue(Iterables.elementsEqual(updateTestProperties, updatedTestProperties), "Failed to update the properties");
    }

    /**
     * find a single property
     */
    @Test
    void findXmlProperty() {
        Property testProperty = new Property("testProperty","testOwner");
        cleanupTestProperties = Arrays.asList(testProperty);
        
        Optional<Property> notFoundProperty = propertyRepository.findById(testProperty.getName());
        // verify the property was not found as expected
        Assertions.assertTrue(notFoundProperty.isEmpty(), "Found the property " + testProperty.getName() + " which should not exist.");

        Property createdProperty = propertyRepository.index(testProperty);

        Optional<Property> foundProperty = propertyRepository.findById(createdProperty.getName());
        // verify the property was found as expected
        Assertions.assertEquals(createdProperty, foundProperty.get(), "Failed to find the property");

        testProperty.setValue("test");
        Channel channel = new Channel("testChannel","testOwner",Arrays.asList(testProperty),new ArrayList<Tag>());
        Channel createdChannel = channelRepository.index(channel);
        cleanupTestChannels = Arrays.asList(createdChannel);

        foundProperty = propertyRepository.findById(createdProperty.getName(),true);
        createdProperty.setChannels(Arrays.asList(channel));
        // verify the property was found as expected
        Property expectedProperty = new Property(createdProperty.getName(), createdProperty.getOwner());
        expectedProperty.setChannels(Arrays.asList(createdChannel));
        Assertions.assertEquals(expectedProperty, foundProperty.get(), "Failed to find the property");

    }

    /**
     * check if a property exists
     */
    @Test
    void testPropertyExists() {

        // check that non existing property returns false
        Assertions.assertFalse(propertyRepository.existsById("no-property"), "Failed to check the non existing property :" + "no-property");

        Property testProperty = new Property("testProperty","testOwner");
        Assertions.assertFalse(propertyRepository.existsById(testProperty.getName()), "Test property " + testProperty.getName() + " already exists");
        Property createdProperty = propertyRepository.index(testProperty);
        cleanupTestProperties = Arrays.asList(createdProperty);

        // verify the property exists as expected
        Assertions.assertTrue(propertyRepository.existsById(testProperty.getName()), "Failed to check the existance of " + testProperty.getName());
    }

    /**
     * find all properties
     */
    @Test
    void findAllXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        try {
            Set<Property> createdProperties = Sets.newHashSet(propertyRepository.indexAll(testProperties));
            Set<Property> listedProperties = Sets.newHashSet(propertyRepository.findAll());
            // verify the properties were listed as expected
            Assertions.assertEquals(createdProperties, listedProperties, "Failed to list all created properties");
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    /**
     * find all properties
     */
    @Test
    void countXmlProperties() {
        Property testProperty = new Property("testProperty","testOwner");
        Property testProperty1 = new Property("testProperty1","testOwner1");
        List<Property> testProperties = Arrays.asList(testProperty, testProperty1);
        cleanupTestProperties = testProperties;

        try {
            Set<Property> createdProperties = Sets.newHashSet(propertyRepository.indexAll(testProperties));
            long listedPropertiesCount = propertyRepository.count();
            // verify the properties were listed as expected
            Assertions.assertEquals(createdProperties.size(), listedPropertiesCount, "Failed to count all created properties");
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    /**
     * find multiple properties
     */
    @Test
    void findXmlProperties() {
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
            Assertions.assertNotEquals(testProperties, notFoundProperties, "Found the properties");
        }

        Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);

        try {
            foundProperties = propertyRepository.findAllById(propertyNames);
        } catch (ResponseStatusException e) {
        } finally {
            // verify the properties were found as expected
            Assertions.assertEquals(createdProperties, foundProperties, "Failed to find the properties");
        }
    }

    /**
     * delete a single property
     */
    @Test
    void deleteXmlProperty() {
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
        Assertions.assertNotEquals(testProperty, propertyRepository.findById(testProperty.getName()), "Failed to delete property");

        Channel foundChannel = channelRepository.findById("testChannel").get();
        // verify the property was deleted from channels as expected
        Assertions.assertTrue(foundChannel.getProperties().isEmpty(), "Failed to remove property from channel");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("testProperty","*");
        List<Channel> chans = channelRepository.search(params).getChannels();
        // verify the property was deleted from channels as expected
        Assertions.assertTrue(chans.isEmpty(), "Failed to remove property from channel");
    }

    // helper operations to clean up proprepoIT
    private List<Property> cleanupTestProperties = Collections.emptyList();

    private List<Channel> cleanupTestChannels = Collections.emptyList();

    @AfterEach
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