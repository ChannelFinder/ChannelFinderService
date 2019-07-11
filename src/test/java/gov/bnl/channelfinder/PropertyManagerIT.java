package gov.bnl.channelfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
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
    XmlProperty testProperty = new XmlProperty("testProperty","testOwner");
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
    @Test // might actually work but messed up by full database
    public void listXmlProperties() {
        testChannels.forEach(chan -> chan.addProperty(testPropertyC));
        testPropertyC.setChannels(testChannels);
        List<XmlProperty> testProperties = Arrays.asList(testProperty,testPropertyC);
        Iterable<XmlProperty> createdProperty = propertyManager.create(testProperties);

        Iterable<XmlProperty> propertyList = propertyManager.list();
        for(XmlProperty property: createdProperty) {
            property.setChannels(new ArrayList<XmlChannel>());
        }
        // verify the properties were listed as expected
        assertEquals("Failed to list all properties",createdProperty,propertyList);                
    }

    /**
     * read a single property
     */
    @Test
    public void readXmlProperty() {
        testPropertyC.setChannels(testChannels);
        XmlProperty createdProperty = propertyManager.create(testProperty.getName(),testProperty);
        XmlProperty createdProperty1 = propertyManager.create(testPropertyC.getName(),testPropertyC);

        XmlProperty readProperty = propertyManager.read(createdProperty.getName(), false);
        // verify the property was read as expected
        assertEquals("Failed to read the property",createdProperty,readProperty);        

        readProperty = propertyManager.read(createdProperty.getName(), true);
        // verify the property was read as expected
        assertEquals("Failed to read the property w/ channels",createdProperty,readProperty);

        readProperty = propertyManager.read(createdProperty1.getName(), false);
        testPropertyC.setChannels(new ArrayList<XmlChannel>());
        // verify the property was read as expected
        assertEquals("Failed to read the property",testPropertyC,readProperty);

        readProperty = propertyManager.read(createdProperty1.getName(), true);
        // verify the property was read as expected
        assertEquals("Failed to read the property w/ channels",createdProperty1,readProperty);

        try {
            // verify the property failed to be read, as expected
            readProperty = propertyManager.read("fakeProperty", false);
            assertTrue("Failed to throw an error",false);
        } catch(ResponseStatusException e) {
            assertTrue(true);
        }

        try {
            // verify the property failed to be read, as expected
            readProperty = propertyManager.read("fakeProperty", true);
            assertTrue("Failed to throw an error",false);
        } catch(ResponseStatusException e) {
            assertTrue(true);
        }
    }

    // Helper operations to create and clean up the resources needed for successful
    // testing of the PropertyManager operations

    private final List<XmlChannel> testChannels = Arrays.asList(
            new XmlChannel("testChannel", "testOwner"),
            new XmlChannel("testChannel1", "testOwner"));

    private final List<XmlProperty> allTestProperties = Arrays.asList(
            testProperty,testProperty1,testProperty2,testPropertyC,testPropertyC1,testPropertyC2,updateTestProperty,updateTestPropertyC,updateTestPropertyC1);

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
            } catch (Exception e) {}
        });
        allTestProperties.forEach(property -> {
            try {
                propertyRepository.deleteById(property.getName());
            } catch (Exception e) {}
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