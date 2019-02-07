package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(PropertyRepository.class)
public class PropertyRepositoryIT {

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ElasticSearchClient esService;

    /**
     * A simple test to index a single property
     */
    @Test
    public void indexXmlProperty() {

        XmlProperty testProperty = new XmlProperty();
        testProperty.setName("test-property");
        testProperty.setOwner("test-owner");

        XmlProperty createdProperty = propertyRepository.index(testProperty);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", testProperty, createdProperty);

        // clean up
        propertyRepository.deleteById(createdProperty.getName());

    }

    /**
     * A test to index a multiple properties
     */
    @Test
    public void indexXmlProperties() {

        XmlProperty testProperty1 = new XmlProperty();
        testProperty1.setName("test-property1");
        testProperty1.setOwner("test-owner");

        XmlProperty testProperty2 = new XmlProperty();
        testProperty2.setName("test-property2");
        testProperty2.setOwner("test-owner");

        List<XmlProperty> testPropertys = Arrays.asList(testProperty1, testProperty2);

        Iterable<XmlProperty> createdPropertys = propertyRepository.indexAll(testPropertys);
        // verify the tag was created as expected
        assertTrue("Failed to create a list of properties", Iterables.elementsEqual(testPropertys, createdPropertys));

        // clean up
        createdPropertys.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        });

    }

    /**
     * Test is the requested property exists
     */
    @Test
    public void testPropertyExist() {
        XmlProperty testProperty1 = new XmlProperty();
        testProperty1.setName("test-property1");
        testProperty1.setOwner("test-owner");

        XmlProperty testProperty2 = new XmlProperty();
        testProperty2.setName("test-property2");
        testProperty2.setOwner("test-owner");

        List<XmlProperty> testPropertys = Arrays.asList(testProperty1, testProperty2);

        Iterable<XmlProperty> createdPropertys = propertyRepository.indexAll(testPropertys);

        try {
            // Test if created tags exist
            assertTrue("Failed to check the existance of " + testProperty1.getName(), propertyRepository.existsById(testProperty1.getName()));
            assertTrue("Failed to check the existance of " + testProperty2.getName(), propertyRepository.existsById(testProperty2.getName()));
            // Test the check for existance of a non existant tag returns false
            assertTrue("Failed to check the existance of 'non-existant-tag'", !propertyRepository.existsById("non-existant-tag"));
        } finally {
            // clean up
            createdPropertys.forEach(createdProperty -> {
                propertyRepository.deleteById(createdProperty.getName());
            });
        }
    }

    /**
     * A test to index a multiple properties
     */
    @Test
    public void listAllXmlProperties() {

        XmlProperty testProperty1 = new XmlProperty();
        testProperty1.setName("test-property1");
        testProperty1.setOwner("test-owner");

        XmlProperty testProperty2 = new XmlProperty();
        testProperty2.setName("test-property2");
        testProperty2.setOwner("test-owner");

        List<XmlProperty> testProperties = Arrays.asList(testProperty1, testProperty2);
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
            testProperties.forEach(createdTag -> {
                propertyRepository.deleteById(createdTag.getName());
            });
        }
    }

    /**
     * TODO A simple test to index a single tag with a few channels
     */
    @Test
    public void indexXmlPropertyWithChannels() {

    }
}
