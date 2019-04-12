package gov.bnl.channelfinder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelManager.class)
public class ChannelManagerIT {

    @Autowired
    ChannelManager channelManager;

    @Autowired
    TagRepository tagRepository;
    @Autowired
    PropertyRepository propertyRepository;

    /**
     * Test the basic operations of create, read, updated, and delete
     */
    @Test
    public void basicChannelCRUDOperations() {
        XmlChannel testChannel = new XmlChannel("test-channel0", "test-channel-owner");
        testChannel.addTag(testTags.get(0));
        XmlProperty prop0 = testProperties.get(0);
        prop0.setValue("test-prop0-value");
        testChannel.addProperty(prop0);
        try {
            // Create a channel
            XmlChannel createdChannel = channelManager.create(testChannel.getName(), testChannel);
            assertTrue("failed to create test channel", createdChannel != null && testChannel.equals(createdChannel));
            // Read a channel
            XmlChannel foundChannel = channelManager.read(testChannel.getName());
            assertTrue("failed to find by id the test channel", foundChannel != null && testChannel.equals(foundChannel));
            // update the channel
            // 1. with a new tag
            testChannel.addTag(testTags.get(1));
            XmlChannel updatedChannel = channelManager.update(testChannel.getName(), testChannel);
            assertTrue("failed to update test channel with additional tag", updatedChannel != null && testChannel.equals(updatedChannel));
            // 2. with a new property
            XmlProperty prop1 = testProperties.get(1);
            prop1.setValue("test-prop1-value");
            testChannel.addProperty(prop1);
            XmlChannel updatedChannel2 = channelManager.update(testChannel.getName(), testChannel);
            assertTrue("failed to update test channel with additional tag", updatedChannel2 != null && testChannel.equals(updatedChannel2));
            // 3. with a new property value
            XmlProperty updatedProp1 = testProperties.get(1);
            updatedProp1.setValue("test-prop1-updated-value");
            testChannel.addProperty(updatedProp1);
            XmlChannel updatedChannel3 = channelManager.update(testChannel.getName(), testChannel);
            assertTrue("failed to update test channel with additional tag", updatedChannel3 != null && testChannel.equals(updatedChannel3));

            // delete channel
            channelManager.remove(testChannel.getName());
            try {
                channelManager.read(testChannel.getName());
                fail("expected exception : ResponseStatusException:404 NOT_FOUND  was not thrown");
              } catch (ResponseStatusException e) {
                
              }

        } finally {
            // ensure that the test channel is removed
            try {
                channelManager.remove(testChannel.getName());
            }
            catch (Exception e) {
                
            }
        }
    }

    /**
     * Test the basic operations of create, read, updated, and delete on a list of channels
     */
    @Test
    public void basicChannelsCRUDOperations() {
        List<XmlChannel> testChannels = new ArrayList<XmlChannel>();
        for (int i = 0; i < 3; i++) {
            XmlChannel testChannel = new XmlChannel("test-channel" + i, "test-channel-owner");
            testChannel.addTag(testTags.get(i));
            XmlProperty prop = testProperties.get(i);
            prop.setValue("test-prop" + i + "-value");
            testChannel.addProperty(prop);
            testChannels.add(testChannel);
        }
        // Create a list of channels
        List<XmlChannel> createdChannels = Lists.newArrayList(channelManager.create(testChannels));
        assertTrue("failed to create test channel", createdChannels != null && testChannels.equals(createdChannels));
        // Find a list of channels
        LinkedMultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add("~name", "test-channel?");
        List<XmlChannel> foundChannels = channelManager.query(searchParameters);
        assertTrue("failed to create test channel expected 3 channels found " + foundChannels.size(),
                foundChannels != null && foundChannels.containsAll(testChannels));
        // Update a list of channels 
        // 1. with new tags and properties
        // Add test-tag0 and test-property0 to all channels
        testChannels.forEach(channel -> {
            channel.addTag(testTags.get(0));
            XmlProperty prop = testProperties.get(0);
            prop.setValue("test-prop0-value");
            channel.addProperty(prop);
        });
        List<XmlChannel> updatedChannels = Lists.newArrayList(channelManager.update(testChannels));
        assertTrue("failed to update test channels, it was expected that all channels with have test-tag0",
                updatedChannels.stream().allMatch(channel -> {
                    return channel.getTags().contains(testTags.get(0));
                }));
        XmlProperty expectedProperty = testProperties.get(0);
        expectedProperty.setValue("test-prop0-value");
        assertTrue("failed to update test channels, it was expected that all channels with have test-property0",
                updatedChannels.stream().allMatch(channel -> {
                    return channel.getProperties().contains(expectedProperty);
                }));

        // 2. with new property values
        // Delete a list of channels
        testChannels.forEach(channel -> {
            channelManager.remove(channel.getName());
        });
        testChannels.forEach(channel -> {
            try {
                channelManager.read(channel.getName());
                fail("failed to delete channel " + channel.getName());
            } catch (ResponseStatusException e) {
                
            }
        });

    }
    // Helper operations to create and clean up the resources needed for successful testing of the ChannelManager operations
    private final List<XmlTag> testTags = Arrays.asList(new XmlTag("test-tag0", "test-tag-owner"),
                                                        new XmlTag("test-tag1", "test-tag-owner"),
                                                        new XmlTag("test-tag2", "test-tag-owner"));

    private final List<XmlProperty> testProperties = Arrays.asList(
                                                        new XmlProperty("test-property0", "test-property-owner"),
                                                        new XmlProperty("test-property1", "test-property-owner"),
                                                        new XmlProperty("test-property2", "test-property-owner"));

    @Before
    public void setup() {
        tagRepository.indexAll(testTags);
        propertyRepository.indexAll(testProperties);
    }

    @After
    public void cleanup() {
        // clean up
        testTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        });
        testProperties.forEach(createdProperty -> {
            propertyRepository.deleteById(createdProperty.getName());
        });
    }
}
