/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.channelfinder.docker;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;

import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.phoebus.channelfinder.XmlTag;

/**
 * Purpose to provide test fixture that can be used in multiple test classes and tests.
 *
 * <p>
 * Class is tightly coupled to ChannelFinder and Elasticsearch, and requires those to be up and running.
 * Intended usage is by docker integration tests for ChannelFinder and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITTestFixture {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         set up, tear down and tests (assert statements) to ensure test fixture as expected
    //     ------------------------------------------------------------------------------------------------
    //     Content
    //         channels (10)
    //             ABC:DEF-GHI:JKL:001
    //             ABC:DEF-GHI:JKL:002
    //             ABC:DEF-GHI:JKL:003
    //             ABC:DEF-GHI:JKL:010
    //             ABC:DEF-GHI:JKL:011
    //             ABC:DEF-XYZ:JKL:001
    //             ABC:DEF-XYZ:JKL:002
    //             ABC:DEF-XYZ:JKL:003
    //             ABC:DEF-XYZ:JKL:010
    //             ABC:DEF-XYZ:JKL:011
    //         properties (4)
    //             domain      (10 channels, values - cryo, power)
    //             element     (10 channels, values - source, initial, radio, magnet, supra)
    //             type        (10 channels, values - read, write, readwrite)
    //             cell        (10 channels, values - block1, block2, block3, block4, block5)
    //         tags (4)
    //             archived    ( 4 channels)
    //             handle_this ( 3 channels)
    //             noteworthy  (10 channels)
    //             not_used    ( 0 channels)
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------------------
    // test data
    //     channels, owner o1
    //         ABC:DEF-GHI:JKL:001
    //         ABC:DEF-GHI:JKL:002
    //         ABC:DEF-GHI:JKL:003
    //         ABC:DEF-GHI:JKL:010
    //         ABC:DEF-GHI:JKL:011
    //         ABC:DEF-XYZ:JKL:001
    //         ABC:DEF-XYZ:JKL:002
    //         ABC:DEF-XYZ:JKL:003
    //         ABC:DEF-XYZ:JKL:010
    //         ABC:DEF-XYZ:JKL:011

    static XmlChannel channel_ghi001;
    static XmlChannel channel_ghi002;
    static XmlChannel channel_ghi003;
    static XmlChannel channel_ghi010;
    static XmlChannel channel_ghi011;
    static XmlChannel channel_xyz001;
    static XmlChannel channel_xyz002;
    static XmlChannel channel_xyz003;
    static XmlChannel channel_xyz010;
    static XmlChannel channel_xyz011;

    static XmlChannel channel_ghi001_properties_tags;
    static XmlChannel channel_ghi002_properties_tags;
    static XmlChannel channel_ghi003_properties_tags;
    static XmlChannel channel_ghi010_properties_tags;
    static XmlChannel channel_ghi011_properties_tags;
    static XmlChannel channel_xyz001_properties_tags;
    static XmlChannel channel_xyz002_properties_tags;
    static XmlChannel channel_xyz003_properties_tags;
    static XmlChannel channel_xyz010_properties_tags;
    static XmlChannel channel_xyz011_properties_tags;

    // convenience
    static XmlChannel[] channels_all_properties_tags;

    // test data
    //     properties, owner o1
    //         domain
    //         element
    //         type
    //         cell

    static XmlProperty property_domain;
    static XmlProperty property_element;
    static XmlProperty property_type;
    static XmlProperty property_cell;

    static XmlProperty property_domain_channels;
    static XmlProperty property_element_channels;
    static XmlProperty property_type_channels;
    static XmlProperty property_cell_channels;

    // test data
    //     tags, owner o1
    //         archived
    //         handle_this
    //         noteworthy
    //         not_used

    static XmlTag tag_archived;
    static XmlTag tag_handle_this;
    static XmlTag tag_noteworthy;
    static XmlTag tag_not_used;

    static XmlTag tag_archived_channels;
    static XmlTag tag_handle_this_channels;
    static XmlTag tag_noteworthy_channels;
    static XmlTag tag_not_used_channels;

    /**
     * This class is not to be instantiated.
     */
    private ITTestFixture() {
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

        setupProperties();
        setupTags();
        setupChannels();

        createProperties();
        createTags();
        createChannels();

        setupAddPropertiesToChannels();
        setupAddTagsToChannels();

        // list channels, of use for integration tests
        channels_all_properties_tags = ITUtilChannels.assertListChannels(10);
    }

    /**
     * Tear down test fixture.
     */
    static void tearDown() {
        // note
        //     not necessary to remove items from channels in order to tear down (properties, tags)
        //         properties, tags, channels can be deleted regardless

        tearDownProperties();
        tearDownTags();
        tearDownChannels();
    }

    /**
     * Set up test fixture, properties.
     */
    private static void setupProperties() {
        property_domain  = new XmlProperty("domain",  "o1");
        property_element = new XmlProperty("element", "o1");
        property_type    = new XmlProperty("type",    "o1");
        property_cell    = new XmlProperty("cell",    "o1");

        property_domain_channels  = new XmlProperty("domain",  "o1");
        property_element_channels = new XmlProperty("element", "o1");
        property_type_channels    = new XmlProperty("type",    "o1");
        property_cell_channels    = new XmlProperty("cell",    "o1");
    }

    /**
     * Set up test fixture, tags.
     */
    private static void setupTags() {
        tag_archived    = new XmlTag("archived",    "o1");
        tag_handle_this = new XmlTag("handle_this", "o1");
        tag_noteworthy  = new XmlTag("noteworthy",  "o1");
        tag_not_used    = new XmlTag("not_used",    "o1");

        tag_archived_channels    = new XmlTag("archived",    "o1");
        tag_handle_this_channels = new XmlTag("handle_this", "o1");
        tag_noteworthy_channels  = new XmlTag("noteworthy",  "o1");
        tag_not_used_channels    = new XmlTag("not_used",    "o1");
    }

    /**
     * Set up test fixture, channels.
     */
    private static void setupChannels() {
        channel_ghi001 = new XmlChannel("ABC:DEF-GHI:JKL:001", "o1");
        channel_ghi002 = new XmlChannel("ABC:DEF-GHI:JKL:002", "o1");
        channel_ghi003 = new XmlChannel("ABC:DEF-GHI:JKL:003", "o1");
        channel_ghi010 = new XmlChannel("ABC:DEF-GHI:JKL:010", "o1");
        channel_ghi011 = new XmlChannel("ABC:DEF-GHI:JKL:011", "o1");
        channel_xyz001 = new XmlChannel("ABC:DEF-XYZ:JKL:001", "o1");
        channel_xyz002 = new XmlChannel("ABC:DEF-XYZ:JKL:002", "o1");
        channel_xyz003 = new XmlChannel("ABC:DEF-XYZ:JKL:003", "o1");
        channel_xyz010 = new XmlChannel("ABC:DEF-XYZ:JKL:010", "o1");
        channel_xyz011 = new XmlChannel("ABC:DEF-XYZ:JKL:011", "o1");

        channel_ghi001_properties_tags = new XmlChannel("ABC:DEF-GHI:JKL:001", "o1");
        channel_ghi002_properties_tags = new XmlChannel("ABC:DEF-GHI:JKL:002", "o1");
        channel_ghi003_properties_tags = new XmlChannel("ABC:DEF-GHI:JKL:003", "o1");
        channel_ghi010_properties_tags = new XmlChannel("ABC:DEF-GHI:JKL:010", "o1");
        channel_ghi011_properties_tags = new XmlChannel("ABC:DEF-GHI:JKL:011", "o1");
        channel_xyz001_properties_tags = new XmlChannel("ABC:DEF-XYZ:JKL:001", "o1");
        channel_xyz002_properties_tags = new XmlChannel("ABC:DEF-XYZ:JKL:002", "o1");
        channel_xyz003_properties_tags = new XmlChannel("ABC:DEF-XYZ:JKL:003", "o1");
        channel_xyz010_properties_tags = new XmlChannel("ABC:DEF-XYZ:JKL:010", "o1");
        channel_xyz011_properties_tags = new XmlChannel("ABC:DEF-XYZ:JKL:011", "o1");
    }

    /**
     * Create test fixture, properties.
     */
    private static void createProperties() {
        XmlProperty[] properties_4 = new XmlProperty[] {
                property_domain,
                property_element,
                property_type,
                property_cell
        };

        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertListProperties(0);

            // --------------------------------------------------------------------------------
            // put
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertCreateReplaceProperties("", properties_4);

            ITUtilProperties.assertRetrieveProperty("/domain",                    property_domain);
            ITUtilProperties.assertRetrieveProperty("/domain?withChannels=true",  property_domain);
            ITUtilProperties.assertRetrieveProperty("/domain?withChannels=false", property_domain);

            ITUtilProperties.assertRetrieveProperty("/element",                    property_element);
            ITUtilProperties.assertRetrieveProperty("/element?withChannels=true",  property_element);
            ITUtilProperties.assertRetrieveProperty("/element?withChannels=false", property_element);

            ITUtilProperties.assertRetrieveProperty("/type",                    property_type);
            ITUtilProperties.assertRetrieveProperty("/type?withChannels=true",  property_type);
            ITUtilProperties.assertRetrieveProperty("/type?withChannels=false", property_type);

            ITUtilProperties.assertRetrieveProperty("/cell",                    property_cell);
            ITUtilProperties.assertRetrieveProperty("/cell?withChannels=true",  property_cell);
            ITUtilProperties.assertRetrieveProperty("/cell?withChannels=false", property_cell);

            ITUtilProperties.assertListProperties(4,
                    property_cell,
                    property_domain,
                    property_element,
                    property_type);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Create test fixture, tags.
     */
    private static void createTags() {
        XmlTag[] tags_4 = new XmlTag[] {
                tag_archived,
                tag_handle_this,
                tag_noteworthy,
                tag_not_used
        };

        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilTags.assertListTags(0);

            // --------------------------------------------------------------------------------
            // put
            // --------------------------------------------------------------------------------

            ITUtilTags.assertCreateReplaceTags("", tags_4);

            ITUtilTags.assertRetrieveTag("/archived",                    tag_archived);
            ITUtilTags.assertRetrieveTag("/archived?withChannels=true",  tag_archived);
            ITUtilTags.assertRetrieveTag("/archived?withChannels=false", tag_archived);

            ITUtilTags.assertRetrieveTag("/handle_this",                    tag_handle_this);
            ITUtilTags.assertRetrieveTag("/handle_this?withChannels=true",  tag_handle_this);
            ITUtilTags.assertRetrieveTag("/handle_this?withChannels=false", tag_handle_this);

            ITUtilTags.assertRetrieveTag("/noteworthy",                    tag_noteworthy);
            ITUtilTags.assertRetrieveTag("/noteworthy?withChannels=true",  tag_noteworthy);
            ITUtilTags.assertRetrieveTag("/noteworthy?withChannels=false", tag_noteworthy);

            ITUtilTags.assertRetrieveTag("/not_used",                    tag_not_used);
            ITUtilTags.assertRetrieveTag("/not_used?withChannels=true",  tag_not_used);
            ITUtilTags.assertRetrieveTag("/not_used?withChannels=false", tag_not_used);

            ITUtilTags.assertListTags(4,
                    tag_archived,
                    tag_handle_this,
                    tag_not_used,
                    tag_noteworthy);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Create test fixture, channels.
     */
    private static void createChannels() {
        XmlChannel[] channels_10 = new XmlChannel[] {
                channel_ghi001,
                channel_ghi002,
                channel_ghi003,
                channel_ghi010,
                channel_ghi011,
                channel_xyz001,
                channel_xyz002,
                channel_xyz003,
                channel_xyz010,
                channel_xyz011
        };

        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertListChannels(0);

            // --------------------------------------------------------------------------------
            // put
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertCreateReplaceMultipleChannels("", channels_10);

            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:001", ITTestFixture.channel_ghi001_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:002", ITTestFixture.channel_ghi002_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:003", ITTestFixture.channel_ghi003_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:010", ITTestFixture.channel_ghi010_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:011", ITTestFixture.channel_ghi011_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:001", ITTestFixture.channel_xyz001_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:002", ITTestFixture.channel_xyz002_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:003", ITTestFixture.channel_xyz003_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:010", ITTestFixture.channel_xyz010_properties_tags);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:011", ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertListChannels(10,
                  channel_ghi001,
                  channel_ghi002,
                  channel_ghi003,
                  channel_ghi010,
                  channel_ghi011,
                  channel_xyz001,
                  channel_xyz002,
                  channel_xyz003,
                  channel_xyz010,
                  channel_xyz011);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Set up test fixture, add properties to channels.
     */
    private static void setupAddPropertiesToChannels() {
        // properties
        //     domain
        //     element
        //     type
        //     cell

        try {
            // --------------------------------------------------------------------------------
            // add property (domain) to multiple channels
            // --------------------------------------------------------------------------------

            XmlProperty property_domain_cryo     = new XmlProperty(property_domain.getName(), property_domain.getOwner(), "cryo");
            XmlProperty property_domain_power    = new XmlProperty(property_domain.getName(), property_domain.getOwner(), "power");

            channel_ghi001_properties_tags.addProperty(property_domain_cryo);
            channel_ghi002_properties_tags.addProperty(property_domain_cryo);
            channel_ghi003_properties_tags.addProperty(property_domain_cryo);
            channel_ghi010_properties_tags.addProperty(property_domain_cryo);
            channel_ghi011_properties_tags.addProperty(property_domain_cryo);
            channel_xyz001_properties_tags.addProperty(property_domain_power);
            channel_xyz002_properties_tags.addProperty(property_domain_power);
            channel_xyz003_properties_tags.addProperty(property_domain_power);
            channel_xyz010_properties_tags.addProperty(property_domain_power);
            channel_xyz011_properties_tags.addProperty(property_domain_power);
            property_domain_channels.getChannels().add(channel_ghi001_properties_tags);
            property_domain_channels.getChannels().add(channel_ghi002_properties_tags);
            property_domain_channels.getChannels().add(channel_ghi003_properties_tags);
            property_domain_channels.getChannels().add(channel_ghi010_properties_tags);
            property_domain_channels.getChannels().add(channel_ghi011_properties_tags);
            property_domain_channels.getChannels().add(channel_xyz001_properties_tags);
            property_domain_channels.getChannels().add(channel_xyz002_properties_tags);
            property_domain_channels.getChannels().add(channel_xyz003_properties_tags);
            property_domain_channels.getChannels().add(channel_xyz010_properties_tags);
            property_domain_channels.getChannels().add(channel_xyz011_properties_tags);

            ITUtilProperties.assertAddPropertyMultipleChannels("/domain", property_domain_channels);

            ITUtilProperties.assertListProperties(4,
                    property_cell,
                    property_domain,
                    property_element,
                    property_type);

            ITUtilProperties.assertRetrieveProperty("/domain",                    property_domain_channels);
            ITUtilProperties.assertRetrieveProperty("/domain?withChannels=true",  property_domain_channels);
            ITUtilProperties.assertRetrieveProperty("/domain?withChannels=false", property_domain);

            // --------------------------------------------------------------------------------
            // add property (element) to multiple channels
            // --------------------------------------------------------------------------------

            XmlProperty property_element_source   = new XmlProperty(property_element.getName(), property_element.getOwner(), "source");
            XmlProperty property_element_initial  = new XmlProperty(property_element.getName(), property_element.getOwner(), "initial");
            XmlProperty property_element_radio    = new XmlProperty(property_element.getName(), property_element.getOwner(), "radio");
            XmlProperty property_element_magnet   = new XmlProperty(property_element.getName(), property_element.getOwner(), "magnet");
            XmlProperty property_element_supra    = new XmlProperty(property_element.getName(), property_element.getOwner(), "supra");

            channel_ghi001_properties_tags.addProperty(property_element_source);
            channel_ghi002_properties_tags.addProperty(property_element_initial);
            channel_ghi003_properties_tags.addProperty(property_element_radio);
            channel_ghi010_properties_tags.addProperty(property_element_magnet);
            channel_ghi011_properties_tags.addProperty(property_element_supra);
            channel_xyz001_properties_tags.addProperty(property_element_source);
            channel_xyz002_properties_tags.addProperty(property_element_initial);
            channel_xyz003_properties_tags.addProperty(property_element_radio);
            channel_xyz010_properties_tags.addProperty(property_element_magnet);
            channel_xyz011_properties_tags.addProperty(property_element_supra);
            property_element_channels.getChannels().add(channel_ghi001_properties_tags);
            property_element_channels.getChannels().add(channel_ghi002_properties_tags);
            property_element_channels.getChannels().add(channel_ghi003_properties_tags);
            property_element_channels.getChannels().add(channel_ghi010_properties_tags);
            property_element_channels.getChannels().add(channel_ghi011_properties_tags);
            property_element_channels.getChannels().add(channel_xyz001_properties_tags);
            property_element_channels.getChannels().add(channel_xyz002_properties_tags);
            property_element_channels.getChannels().add(channel_xyz003_properties_tags);
            property_element_channels.getChannels().add(channel_xyz010_properties_tags);
            property_element_channels.getChannels().add(channel_xyz011_properties_tags);

            ITUtilProperties.assertAddPropertyMultipleChannels("/element", property_element_channels);

            ITUtilProperties.assertListProperties(4,
                    property_cell,
                    property_domain,
                    property_element,
                    property_type);

            ITUtilProperties.assertRetrieveProperty("/element",                    property_element_channels);
            ITUtilProperties.assertRetrieveProperty("/element?withChannels=true",  property_element_channels);
            ITUtilProperties.assertRetrieveProperty("/element?withChannels=false", property_element);

            // --------------------------------------------------------------------------------
            // add property (type) to multiple channels
            // --------------------------------------------------------------------------------

            XmlProperty property_type_read      = new XmlProperty(property_type.getName(), property_type.getOwner(), "read");
            XmlProperty property_type_write     = new XmlProperty(property_type.getName(), property_type.getOwner(), "write");
            XmlProperty property_type_readwrite = new XmlProperty(property_type.getName(), property_type.getOwner(), "readwrite");

            channel_ghi001_properties_tags.addProperty(property_type_read);
            channel_ghi002_properties_tags.addProperty(property_type_read);
            channel_ghi003_properties_tags.addProperty(property_type_write);
            channel_ghi010_properties_tags.addProperty(property_type_write);
            channel_ghi011_properties_tags.addProperty(property_type_readwrite);
            channel_xyz001_properties_tags.addProperty(property_type_readwrite);
            channel_xyz002_properties_tags.addProperty(property_type_write);
            channel_xyz003_properties_tags.addProperty(property_type_write);
            channel_xyz010_properties_tags.addProperty(property_type_read);
            channel_xyz011_properties_tags.addProperty(property_type_read);
            property_type_channels.getChannels().add(channel_ghi001_properties_tags);
            property_type_channels.getChannels().add(channel_ghi002_properties_tags);
            property_type_channels.getChannels().add(channel_ghi003_properties_tags);
            property_type_channels.getChannels().add(channel_ghi010_properties_tags);
            property_type_channels.getChannels().add(channel_ghi011_properties_tags);
            property_type_channels.getChannels().add(channel_xyz001_properties_tags);
            property_type_channels.getChannels().add(channel_xyz002_properties_tags);
            property_type_channels.getChannels().add(channel_xyz003_properties_tags);
            property_type_channels.getChannels().add(channel_xyz010_properties_tags);
            property_type_channels.getChannels().add(channel_xyz011_properties_tags);

            ITUtilProperties.assertAddPropertyMultipleChannels("/type", property_type_channels);

            ITUtilProperties.assertListProperties(4,
                    property_cell,
                    property_domain,
                    property_element,
                    property_type);

            ITUtilProperties.assertRetrieveProperty("/type",                    property_type_channels);
            ITUtilProperties.assertRetrieveProperty("/type?withChannels=true",  property_type_channels);
            ITUtilProperties.assertRetrieveProperty("/type?withChannels=false", property_type);

            // --------------------------------------------------------------------------------
            // add property (cell) to multiple channels
            // --------------------------------------------------------------------------------

            XmlProperty property_cell_block1   = new XmlProperty(property_cell.getName(), property_cell.getOwner(), "block1");
            XmlProperty property_cell_block2   = new XmlProperty(property_cell.getName(), property_cell.getOwner(), "block2");
            XmlProperty property_cell_block3   = new XmlProperty(property_cell.getName(), property_cell.getOwner(), "block3");
            XmlProperty property_cell_block4   = new XmlProperty(property_cell.getName(), property_cell.getOwner(), "block4");
            XmlProperty property_cell_block5   = new XmlProperty(property_cell.getName(), property_cell.getOwner(), "block5");

            channel_ghi001_properties_tags.addProperty(property_cell_block1);
            channel_ghi002_properties_tags.addProperty(property_cell_block2);
            channel_ghi003_properties_tags.addProperty(property_cell_block3);
            channel_ghi010_properties_tags.addProperty(property_cell_block4);
            channel_ghi011_properties_tags.addProperty(property_cell_block5);
            channel_xyz001_properties_tags.addProperty(property_cell_block1);
            channel_xyz002_properties_tags.addProperty(property_cell_block2);
            channel_xyz003_properties_tags.addProperty(property_cell_block3);
            channel_xyz010_properties_tags.addProperty(property_cell_block4);
            channel_xyz011_properties_tags.addProperty(property_cell_block5);
            property_cell_channels.getChannels().add(channel_ghi001_properties_tags);
            property_cell_channels.getChannels().add(channel_ghi002_properties_tags);
            property_cell_channels.getChannels().add(channel_ghi003_properties_tags);
            property_cell_channels.getChannels().add(channel_ghi010_properties_tags);
            property_cell_channels.getChannels().add(channel_ghi011_properties_tags);
            property_cell_channels.getChannels().add(channel_xyz001_properties_tags);
            property_cell_channels.getChannels().add(channel_xyz002_properties_tags);
            property_cell_channels.getChannels().add(channel_xyz003_properties_tags);
            property_cell_channels.getChannels().add(channel_xyz010_properties_tags);
            property_cell_channels.getChannels().add(channel_xyz011_properties_tags);

            ITUtilProperties.assertAddPropertyMultipleChannels("/cell", property_cell_channels);

            ITUtilProperties.assertListProperties(4,
                    property_cell,
                    property_domain,
                    property_element,
                    property_type);

            ITUtilProperties.assertRetrieveProperty("/cell",                    property_cell_channels);
            ITUtilProperties.assertRetrieveProperty("/cell?withChannels=true",  property_cell_channels);
            ITUtilProperties.assertRetrieveProperty("/cell?withChannels=false", property_cell);

            // --------------------------------------------------------------------------------
            // check channels
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertListChannels(10);

            // ensure that channel is there, value not important at this stage
            ITUtilChannels.assertRetrieveChannel("/ABC:DEF-GHI:JKL:002",  HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Set up test fixture, add tags to channels.
     */
    private static void setupAddTagsToChannels() {
        // tags
        //     archived
        //     handle_this
        //     noteworthy
        //     not_used    (NOT add tag (not_used) to any channel)

        try {
            // --------------------------------------------------------------------------------
            // add tag (archived) to multiple channels
            // --------------------------------------------------------------------------------

            channel_ghi001_properties_tags.addTag(tag_archived);
            channel_ghi002_properties_tags.addTag(tag_archived);
            channel_xyz001_properties_tags.addTag(tag_archived);
            channel_xyz002_properties_tags.addTag(tag_archived);
            tag_archived_channels.getChannels().add(channel_ghi001_properties_tags);
            tag_archived_channels.getChannels().add(channel_ghi002_properties_tags);
            tag_archived_channels.getChannels().add(channel_xyz001_properties_tags);
            tag_archived_channels.getChannels().add(channel_xyz002_properties_tags);

            ITUtilTags.assertAddTagMultipleChannels("/archived", tag_archived_channels);

            ITUtilTags.assertListTags(4,
                    tag_archived,
                    tag_handle_this,
                    tag_not_used,
                    tag_noteworthy);

            ITUtilTags.assertRetrieveTag("/archived",                    tag_archived_channels);
            ITUtilTags.assertRetrieveTag("/archived?withChannels=true",  tag_archived_channels);
            ITUtilTags.assertRetrieveTag("/archived?withChannels=false", tag_archived);

            // --------------------------------------------------------------------------------
            // add tag (handle_this) to multiple channels
            // --------------------------------------------------------------------------------

            channel_ghi010_properties_tags.addTag(tag_handle_this);
            channel_ghi011_properties_tags.addTag(tag_handle_this);
            channel_xyz001_properties_tags.addTag(tag_handle_this);
            tag_handle_this_channels.getChannels().add(channel_ghi010_properties_tags);
            tag_handle_this_channels.getChannels().add(channel_ghi011_properties_tags);
            tag_handle_this_channels.getChannels().add(channel_xyz001_properties_tags);

            ITUtilTags.assertAddTagMultipleChannels("/handle_this", tag_handle_this_channels);

            ITUtilTags.assertListTags(4,
                    tag_archived,
                    tag_handle_this,
                    tag_not_used,
                    tag_noteworthy);

            ITUtilTags.assertRetrieveTag("/handle_this",                    tag_handle_this_channels);
            ITUtilTags.assertRetrieveTag("/handle_this?withChannels=true",  tag_handle_this_channels);
            ITUtilTags.assertRetrieveTag("/handle_this?withChannels=false", tag_handle_this);

            // --------------------------------------------------------------------------------
            // add tag (noteworthy) to multiple channels
            // --------------------------------------------------------------------------------

            channel_ghi001_properties_tags.addTag(tag_noteworthy);
            channel_ghi002_properties_tags.addTag(tag_noteworthy);
            channel_ghi003_properties_tags.addTag(tag_noteworthy);
            channel_ghi010_properties_tags.addTag(tag_noteworthy);
            channel_ghi011_properties_tags.addTag(tag_noteworthy);
            channel_xyz001_properties_tags.addTag(tag_noteworthy);
            channel_xyz002_properties_tags.addTag(tag_noteworthy);
            channel_xyz003_properties_tags.addTag(tag_noteworthy);
            channel_xyz010_properties_tags.addTag(tag_noteworthy);
            channel_xyz011_properties_tags.addTag(tag_noteworthy);
            tag_noteworthy_channels.getChannels().add(channel_ghi001_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_ghi002_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_ghi003_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_ghi010_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_ghi011_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_xyz001_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_xyz002_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_xyz003_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_xyz010_properties_tags);
            tag_noteworthy_channels.getChannels().add(channel_xyz011_properties_tags);

            ITUtilTags.assertAddTagMultipleChannels("/noteworthy", tag_noteworthy_channels);

            ITUtilTags.assertListTags(4,
                    tag_archived,
                    tag_handle_this,
                    tag_not_used,
                    tag_noteworthy);

            ITUtilTags.assertRetrieveTag("/noteworthy",                    tag_noteworthy_channels);
            ITUtilTags.assertRetrieveTag("/noteworthy?withChannels=true",  tag_noteworthy_channels);
            ITUtilTags.assertRetrieveTag("/noteworthy?withChannels=false", tag_noteworthy);

            // --------------------------------------------------------------------------------
            // NOT add tag (not_used) to any channel
            // --------------------------------------------------------------------------------

            // --------------------------------------------------------------------------------
            // check channels
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertListChannels(10);

            // ensure that channel is there, value not important at this stage
            ITUtilChannels.assertRetrieveChannel("/ABC:DEF-XYZ:JKL:011",  HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Tear down test fixture, properties.
     */
    private static void tearDownProperties() {
        try {
            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertRemoveProperty("/domain");
            ITUtilProperties.assertRemoveProperty("/element");
            ITUtilProperties.assertRemoveProperty("/type");
            ITUtilProperties.assertRemoveProperty("/cell");

            ITUtilProperties.assertListProperties(0);

            property_domain = null;
            property_element = null;
            property_type = null;
            property_cell = null;

            property_domain_channels = null;
            property_element_channels = null;
            property_type_channels = null;
            property_cell_channels = null;
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Tear down test fixture, tags.
     */
    private static void tearDownTags() {
        try {
            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            ITUtilTags.assertRemoveTag("/archived");
            ITUtilTags.assertRemoveTag("/handle_this");
            ITUtilTags.assertRemoveTag("/noteworthy");
            ITUtilTags.assertRemoveTag("/not_used");

            ITUtilTags.assertListTags(0);

            tag_archived = null;
            tag_handle_this = null;
            tag_noteworthy = null;
            tag_not_used = null;

            tag_archived_channels = null;
            tag_handle_this_channels = null;
            tag_noteworthy_channels = null;
            tag_not_used_channels = null;
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Tear down test fixture, channels.
     */
    private static void tearDownChannels() {
        try {
            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertDeleteChannel("/ABC:DEF-GHI:JKL:001");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-GHI:JKL:002");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-GHI:JKL:003");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-GHI:JKL:010");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-GHI:JKL:011");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-XYZ:JKL:001");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-XYZ:JKL:002");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-XYZ:JKL:003");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-XYZ:JKL:010");
            ITUtilChannels.assertDeleteChannel("/ABC:DEF-XYZ:JKL:011");

            ITUtilChannels.assertListChannels(0);

            channel_ghi001 = null;
            channel_ghi002 = null;
            channel_ghi003 = null;
            channel_ghi010 = null;
            channel_ghi011 = null;
            channel_xyz001 = null;
            channel_xyz002 = null;
            channel_xyz003 = null;
            channel_xyz010 = null;
            channel_xyz011 = null;

            channel_ghi001_properties_tags = null;
            channel_ghi002_properties_tags = null;
            channel_ghi003_properties_tags = null;
            channel_ghi010_properties_tags = null;
            channel_ghi011_properties_tags = null;
            channel_xyz001_properties_tags = null;
            channel_xyz002_properties_tags = null;
            channel_xyz003_properties_tags = null;
            channel_xyz010_properties_tags = null;
            channel_xyz011_properties_tags = null;

            channels_all_properties_tags = null;
        } catch (Exception e) {
            fail();
        }
    }

}
