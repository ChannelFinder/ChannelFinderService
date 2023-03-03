package org.phoebus.channelfinder.docker;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.phoebus.channelfinder.XmlTag;
import org.phoebus.channelfinder.example.PopulateServiceUtil;

/**
 * Purpose to provide test fixture that can be used in multiple test classes and tests.
 *
 * <p>
 * Class is tightly coupled to ChannelFinder and Elasticsearch, and requires those to be up and running.
 * Intended usage is by docker integration tests for ChannelFinder and Elasticsearch.
 *
 * @author Lars Johansson
 *
 * @see ITTestFixture
 * @see PopulateServiceUtil
 */
public class ITTestFixturePopulateService {

    static int CHUNK_SIZE_TAGS       = 20;
    static int CHUNK_SIZE_PROPERTIES = 20;
    static int CHUNK_SIZE_CHANNELS   = 8;

    // convenience
    static XmlChannel[] channels_all_properties_tags;

    /**
     * This class is not to be instantiated.
     */
    private ITTestFixturePopulateService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Set up test fixture.
     */
    static void setup() {
        // note
        //     order of items is important
        //         to add properties to channels
        //         to add tags to channels
        //         for assert statements

        createChannelsPropertiesTags();

        // list channels, number of channels of use for integration tests
        channels_all_properties_tags = ITUtilChannels.assertListChannels(-1);
    }

    /**
     * Tear down test fixture.
     */
    static void tearDown() {
        // note
        //     not necessary to remove items from channels in order to tear down (properties, tags)
        //         properties, tags, channels can be deleted regardless

        tearDownChannelsPropertiesTags();
    }

    /**
     * Create test fixture, channels, properties, tags.
     *
     * @see PopulateService
     * @see PopulateServiceUtil
     */
    private static void createChannelsPropertiesTags() {
        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertListChannels(0);

            // 1500 channels per cell will be created
            int numberOfCells = 1;
            Collection<XmlChannel> channels = null;

            // insertSRCellChannels
            //     find out channels
            //     create in chunks
            //         tags, properties, channels
            //         not exceed curl content length limit
            //         performance
            for (int i = 1; i <= numberOfCells; i++) {
                String cell = String.format("%03d", i);
                try {
                    channels = PopulateServiceUtil.insertSRCellChannels(cell);

                    createTags(channels);
                    createProperties(channels);
                    createChannels(channels);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // insertBOCellChannels
            //     find out channels
            //     create in chunks
            //         tags, properties, channels
            //         not exceed curl content length limit
            //         performance
            for (int i = 1; i <= numberOfCells; i++) {
                String cell = String.format("%03d", i);
                try {
                    channels = PopulateServiceUtil.insertBOCellChannels(cell);

                    createTags(channels);
                    createProperties(channels);
                    createChannels(channels);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Create test fixture, tags given channels.
     */
    private static void createTags(Collection<XmlChannel> channels) {
        // avoid create tag twice
        // create in chunks
        //         not exceed curl content length limit
        //         performance
        ArrayList<XmlTag> tags  = new ArrayList<>();
        HashSet<XmlTag>   tags2 = new HashSet<>();
        ArrayList<XmlTag> tags3 = new ArrayList<>();
        for (XmlChannel channel : channels) {
            tags.addAll(channel.getTags());
        }
        for (XmlTag tag : tags) {
            tags2.add(tag);
        }
        for (XmlTag tag : tags2) {
            tags3.add(tag);
            if (tags3.size() >= CHUNK_SIZE_TAGS) {
                ITUtilTags.assertCreateReplaceTags("", tags3.toArray(XmlTag[]::new));
                tags3.clear();
            }
        }
        if (!tags3.isEmpty()) {
            ITUtilTags.assertCreateReplaceTags("", tags3.toArray(XmlTag[]::new));
            tags3.clear();
        }
    }

    /**
     * Create test fixture, properties given channels.
     */
    private static void createProperties(Collection<XmlChannel> channels) {
        // avoid create property twice
        // create in chunks
        //         not exceed curl content length limit
        //         performance
        HashMap<String, XmlProperty> properties = new HashMap<>();
        ArrayList<XmlProperty> properties3 = new ArrayList<>();
        for (XmlChannel channel : channels) {
            for (XmlProperty property : channel.getProperties()) {
                if (properties.get(property.getName()) == null) {
                    properties.put(property.getName(), new XmlProperty(property.getName(), property.getOwner()));
                }
            }
        }
        for (Entry<String, XmlProperty> entry : properties.entrySet()) {
            properties3.add(entry.getValue());
            if (properties3.size() >= CHUNK_SIZE_PROPERTIES) {
                ITUtilProperties.assertCreateReplaceProperties("", properties3.toArray(XmlProperty[]::new));
                properties3.clear();
            }
        }
        if (!properties3.isEmpty()) {
            ITUtilProperties.assertCreateReplaceProperties("", properties3.toArray(XmlProperty[]::new));
            properties3.clear();
        }
    }

    /**
     * Create test fixture, tags channels.
     */
    private static void createChannels(Collection<XmlChannel> channels) {
        // create in chunks
        //         not exceed curl content length limit
        //         performance
        ArrayList<XmlChannel> list = new ArrayList<>();
        for (XmlChannel channel : channels) {
            list.add(channel);
            if (list.size() == CHUNK_SIZE_CHANNELS) {
                ITUtilChannels.assertCreateReplaceMultipleChannels("", list.toArray(XmlChannel[]::new));
                list.clear();
            }
        }
        if (!list.isEmpty()) {
            ITUtilChannels.assertCreateReplaceMultipleChannels("", list.toArray(XmlChannel[]::new));
            list.clear();
        }
    }

    /**
     * Tear down test fixture, channels, properties, tags.
     */
    private static void tearDownChannelsPropertiesTags() {
        try {
            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            // not remove content since container is shut down

            channels_all_properties_tags = null;
        } catch (Exception e) {
            fail();
        }
    }

}
