package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(PropertyRepository.class)
public class PropertyRepositoryIT {

    @Autowired
    ElasticSearchClient esService;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;
    
    // set up
    XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
    XmlProperty updateTestProperty = new XmlProperty("testProperty","updateTestOwner");
    XmlProperty testProperty1 = new XmlProperty("testProperty1","testOwner1");    
    XmlProperty updateTestProperty1 = new XmlProperty("testProperty1","updateTestOwner1");

    /**
     * index a single property
     */
    @Test
    public void indexXmlProperty() {
        XmlProperty createdProperty = propertyRepository.index(testProperty);
        // verify the property was created as expected
        assertEquals("Failed to create the property", testProperty, createdProperty);

        // clean up
        propertyRepository.deleteById(createdProperty.getName());

    }

    /**
     * index multiple properties
     */
    @Test
    public void indexXmlProperties() {
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);

        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        // verify the properties were created as expected
        assertTrue("Failed to create the list of properties", Iterables.elementsEqual(testProperties, createdProperties));

        // clean up
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        });

    }

    /**
     * save a single property
     */
    @Test
    public void saveXmlProperty() {
        XmlProperty createdProperty = propertyRepository.index(testProperty);

        XmlProperty updatedTestProperty = propertyRepository.save(updateTestProperty);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with the same name", updateTestProperty, updatedTestProperty);

        XmlProperty updatedTestProperty1 = propertyRepository.save("testProperty",updateTestProperty1);
        // verify the property was updated as expected
        assertEquals("Failed to update the property with a different name", updateTestProperty1, updatedTestProperty1);

        // clean up
        propertyRepository.deleteById(updatedTestProperty1.getName());
    }

    /**
     * save multiple properties
     */
    @Test
    public void saveXmlProperties() {
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);        
        Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(testProperties);
        List<XmlProperty> updateTestProperties = Arrays.asList(updateTestProperty, updateTestProperty1);        

        Iterable<XmlProperty> updatedTestProperties = propertyRepository.saveAll(updateTestProperties);
        // verify the properties were updated as expected
        assertTrue("Failed to update the properties", Iterables.elementsEqual(updateTestProperties, updatedTestProperties));

        // clean up
        updatedTestProperties.forEach(updatedTestProperty -> {
            propertyRepository.deleteById(updatedTestProperty.getName());
        });
    }
    
    /**
     * find a single property
     */
    @Test
    public void findXmlProperty() {
        Optional<XmlProperty> notFoundProperty = propertyRepository.findById(testProperty.getName());
        // verify the property was not found as expected
        assertNotEquals("Found the property",testProperty,notFoundProperty);

        XmlProperty createdProperty = propertyRepository.index(testProperty);

        Optional<XmlProperty> foundProperty = propertyRepository.findById(createdProperty.getName());
        // verify the property was found as expected
        assertEquals("Failed to find the property",createdProperty,foundProperty.get());

        testProperty.setValue("test");
        XmlChannel channel = new XmlChannel("testChannel","testOwner",Arrays.asList(testProperty),null);
        XmlChannel createdChannel = channelRepository.index(channel);
        
        foundProperty = propertyRepository.findById(createdProperty.getName(),true);
        createdProperty.setChannels(Arrays.asList(createdChannel));
        // verify the property was found as expected
        assertEquals("Failed to find the property",createdProperty,foundProperty.get());
        
        // clean up
        propertyRepository.deleteById(createdProperty.getName());
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * check if a property exists
     */
    @Test
    public void testPropertyExists() {
        XmlProperty createdProperty = propertyRepository.index(testProperty);

        // verify the property exists as expected
        assertTrue("Failed to check the existance of " + testProperty.getName(), propertyRepository.existsById(testProperty.getName()));
        // verify the property does not exist as expected
        assertTrue("Failed to check the existance of 'non-existant-property'", !propertyRepository.existsById("non-existant-property"));

        // clean up
        propertyRepository.deleteById(createdProperty.getName());   
    }

    /**
     * find all properties
     */
    @Test
    public void findAllXmlProperties() {
        List<XmlProperty> testProperties = Arrays.asList(testProperty, testProperty1);
        try {
            Set<XmlProperty> createdProperties = Sets.newHashSet(propertyRepository.indexAll(testProperties));
            Thread.sleep(2000);
            Set<XmlProperty> listedProperties = Sets.newHashSet(propertyRepository.findAll());
            // verify the tag was created as expected
            assertEquals("Failed to list all created tags", createdProperties, listedProperties);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // clean up
            testProperties.forEach(createdProperty -> {
                propertyRepository.deleteById(createdProperty.getName());
            });
        }
    }

    /**
     * find multiple properties
     */
    @Test
    public void findXmlProperties() {
        List<XmlProperty> testProperties = Arrays.asList(testProperty,testProperty1);
        List<String> propertyNames = Arrays.asList(testProperty.getName(),testProperty1.getName());
        Iterable<XmlProperty> notFoundProperties = null;
        Iterable<XmlProperty> foundProperties = null;

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

        // clean up
        createdProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        });
    }

    /**
     * delete a single property
     */
    @Test
    public void deleteXmlTag() {
        XmlProperty createdProperty = propertyRepository.index(testProperty);

        propertyRepository.deleteById(createdProperty.getName());
        // verify the property was deleted as expected
        assertNotEquals("Failed to delete property",testProperty,propertyRepository.findById(testProperty.getName()));
    }
}
