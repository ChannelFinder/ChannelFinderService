package gov.bnl.channelfinder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Lists;

@RunWith(SpringRunner.class)
@WebMvcTest(PropertyManager.class)
public class PropertyManagerIT {

    @Autowired
    PropertyManager propertyManager;

    @Autowired
    TagRepository tagRepository;
    @Autowired
    ChannelRepository channelRepository;
    @Autowired
    PropertyRepository propertyRepository;


    private final List<XmlProperty> testProperties = Arrays.asList(
            new XmlProperty("test-property0", "test-property-owner"),
            new XmlProperty("test-property1", "test-property-owner"),
            new XmlProperty("test-property2", "test-property-owner"));

    /**
     * Test the basic operations of create, read, updated, and delete
     */
    @Test
    public void basicChannelCRUDOperations() {

        XmlProperty testProperty = testProperties.get(0);

        try {
            // Create a property
            XmlProperty createdProperty = propertyManager.create(testProperty.getName(), testProperty);
            assertTrue("failed to create test property", createdProperty != null && testProperty.equals(createdProperty));
            // Read a property
            XmlProperty foundProperty = propertyManager.read(testProperty.getName(), false);
            assertTrue("failed to find by id the test property",
                    foundProperty != null && testProperty.equals(foundProperty));
            // update the property

            // delete property
            propertyManager.remove(testProperty.getName());
            try {
                propertyManager.read(testProperty.getName(), false);
                fail("expected exception : ResponseStatusException:404 NOT_FOUND  was not thrown");
            } catch (ResponseStatusException e) {

            }

        } finally {
            // ensure that the test channel is removed
            try {
                propertyManager.remove(testProperty.getName());
            } catch (Exception e) {

            }
        }
    }

    /**
     * Test the basic operations of create, read, updated, and delete on a list of
     * channels
     */
    @Test
    public void basicChannelsCRUDOperations() {

        // Create a list of properties
        List<XmlProperty> createdProperties = propertyManager.create(testProperties);
        assertTrue("failed to create test properties", createdProperties != null && testProperties.equals(createdProperties));
        // list of properties
        List<XmlProperty> foundChannels = Lists.newArrayList(propertyManager.list());
        assertTrue("failed to create test properties expected 3 properties found " + foundChannels.size(),
                foundChannels != null && foundChannels.containsAll(testProperties));
        // Update a list of properties

        // Delete a list of properties
        testProperties.forEach(property -> {
            propertyManager.remove(property.getName());
        });
        testProperties.forEach(property -> {
            try {
                propertyManager.read(property.getName(), false);
                fail("failed to delete property " + property.getName());
            } catch (ResponseStatusException e) {

            }
        });

    }

    // Helper operations to create and clean up the resources needed for successful
    // testing of the PropertyManager operations
    
    private final List<XmlChannel> testChannels = Arrays.asList(
            new XmlChannel("test-channel0", "test-channel-owner"),
            new XmlChannel("test-channel1", "test-channel-owner"),
            new XmlChannel("test-channel2", "test-channel-owner"));

    @Before
    public void setup() {
        channelRepository.indexAll(testChannels);
    }

    @After
    public void cleanup() {
        // clean up
        testChannels.forEach(createdProperty -> {
            channelRepository.deleteById(createdProperty.getName());
        });
    }
}
