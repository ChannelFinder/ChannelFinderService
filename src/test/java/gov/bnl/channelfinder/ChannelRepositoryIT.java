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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@WebMvcTest(ChannelRepository.class)
public class ChannelRepositoryIT {

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ElasticSearchClient esService;

    /**
     * A simple test to index a single property
     */
    @Test
    public void indexXmlChannel() {

        XmlChannel testChannel = new XmlChannel();
        testChannel.setName("test-channel");
        testChannel.setOwner("test-owner");

        XmlChannel createdChannel = channelRepository.index(testChannel);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", testChannel, createdChannel);

        // clean up
        channelRepository.deleteById(createdChannel.getName());
    }

    /**
     * A test to index a multiple properties
     */
    @Test
    public void indexXmlChannels() {

        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlChannel testChannel2 = new XmlChannel();
        testChannel2.setName("test-channel2");
        testChannel2.setOwner("test-owner");

        List<XmlChannel> testChannels = Arrays.asList(testChannel1, testChannel2);

        Iterable<XmlChannel> createdChannels = channelRepository.indexAll(testChannels);
        // verify the tag was created as expected
        assertTrue("Failed to create a list of properties", Iterables.elementsEqual(testChannels, createdChannels));

        // clean up
        createdChannels.forEach(createdChannel -> {
            channelRepository.deleteById(createdChannel.getName());
        });

    }

    /**
     * A test to index a multiple properties
     */
    @Test
    public void listAllXmlChannels() {

        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlChannel testChannel2 = new XmlChannel();
        testChannel2.setName("test-channel2");
        testChannel2.setOwner("test-owner");

        List<XmlChannel> testChannels = Arrays.asList(testChannel1, testChannel2);
        try {
            Set<XmlChannel> createdChannels = Sets.newHashSet(channelRepository.indexAll(testChannels));
            Thread.sleep(2000);
            Set<XmlChannel> listedChannels = Sets.newHashSet(channelRepository.findAll());
            // verify the tag was created as expected
            assertEquals("Failed to list all created tags", createdChannels, listedChannels);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // clean up
            testChannels.forEach(createdChannel -> {
                channelRepository.deleteById(createdChannel.getName());
            });
        }
    }

    /**
     * Create a channel with a tag
     */
    @Test
    public void indexChannelWithTag() {
        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag createdTag = tagRepository.index(testTag1);
        testChannel1.addTag(createdTag);
        XmlChannel createdChannel = channelRepository.index(testChannel1);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", createdChannel, testChannel1);
        // verify the tag was created as expected
//            assertEquals("Failed to list all created tags", createdTags, listedTags);
        // clean up
        channelRepository.deleteById(createdChannel.getName());
        tagRepository.deleteById(createdTag.getName());
    }

    /**
     * TODO Create a channel with a list of tags
     */
    @Test
    public void indexChannelWithTags() {
        XmlChannel testChannel1 = new XmlChannel();
        testChannel1.setName("test-channel1");
        testChannel1.setOwner("test-owner");

        XmlTag testTag1 = new XmlTag();
        testTag1.setName("test-tag1");
        testTag1.setOwner("test-owner");

        XmlTag testTag2 = new XmlTag();
        testTag2.setName("test-tag2");
        testTag2.setOwner("test-owner");

        List<XmlTag> testTags = Arrays.asList(testTag1, testTag2);

        List<XmlTag> createdTags = Lists.newArrayList(tagRepository.indexAll(testTags));

        testChannel1.addTags(createdTags);
        XmlChannel createdChannel = channelRepository.index(testChannel1);
        // verify the tag was created as expected
        assertEquals("Failed to create the test property", testChannel1, createdChannel);
        // verify the tag was created as expected

        // clean up
        channelRepository.deleteById(createdChannel.getName());
        testTags.forEach(createdTag -> {
            tagRepository.deleteById(createdTag.getName());
        });
    }

    /**
     * TODO Create a channel with a property
     */
    @Test
    public void indexChannelWithProperty() {

    }

    /**
     * TODO Create a channel with a list of properties
     */
    @Test
    public void indexChannelWithProperties() {

    }
}
