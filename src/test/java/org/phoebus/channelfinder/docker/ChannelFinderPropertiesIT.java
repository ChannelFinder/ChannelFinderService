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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for ChannelFinder and Elasticsearch with focus on usage of
 * {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
 * Existing dockerization is used with <tt>docker-compose-integrationtest.yml</tt> and <tt>Dockerfile.integrationtest</tt>.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.PropertyManager
 * @see org.phoebus.channelfinder.docker.ITUtil
 * @see org.phoebus.channelfinder.docker.ITUtilProperties
 */
@Testcontainers
class ChannelFinderPropertiesIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for ChannelFinder, ensured at start-up
    //             environment
    //                 default ports, can be exposed differently externally to avoid interference with any running instance
    //                 demo_auth enabled
    //         docker containers shared for tests
    //             each test to leave ChannelFinder, Elasticsearch in clean state - not disturb other tests
    //         each test uses multiple endpoints in ChannelFinder API
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------
    //     CHANNELFINDER API
    //     --------------------
    //     Retrieve a Property                        .../properties/<name>                                GET
    //     List Properties                            .../properties                                       GET
    //     Create/Replace a Property                  .../properties/<name>                                PUT
    //     Add Property to a Single Channel           .../properties/<property_name>/<channel_name>        PUT
    //     Create/Replace Properties                  .../properties                                       PUT
    //     Add Property to Multiple Channels          .../properties/<name>                                POST
    //     Add Multiple Properties                    .../properties                                       POST
    //     Remove Property from Single Channel        .../properties/<property_name>/<channel_name>        DELETE
    //     Remove Property                            .../properties/<name>                                DELETE
    //     ------------------------------------------------------------------------------------------------

    // test data
    //     properties p1 - p10, owner o1
    //     property   p1,       owner o2

    static Property property_p1_owner_o1;
    static Property property_p2_owner_o1;
    static Property property_p3_owner_o1;
    static Property property_p4_owner_o1;
    static Property property_p5_owner_o1;
    static Property property_p6_owner_o1;
    static Property property_p7_owner_o1;
    static Property property_p8_owner_o1;
    static Property property_p9_owner_o1;
    static Property property_p10_owner_o1;

    static Property property_p1_owner_o2;
    static Property property_p2_owner_o2;
    static Property property_p3_owner_o2;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        property_p1_owner_o1 = new Property("p1", "o1", null);
        property_p2_owner_o1 = new Property("p2", "o1", null);
        property_p3_owner_o1 = new Property("p3", "o1", null);
        property_p4_owner_o1 = new Property("p4", "o1", null);
        property_p5_owner_o1 = new Property("p5", "o1", null);
        property_p6_owner_o1 = new Property("p6", "o1", null);
        property_p7_owner_o1 = new Property("p7", "o1", null);
        property_p8_owner_o1 = new Property("p8", "o1", null);
        property_p9_owner_o1 = new Property("p9", "o1", null);
        property_p10_owner_o1 = new Property("p10", "o1", null);

        property_p1_owner_o2 = new Property("p1", "o2", null);
        property_p2_owner_o2 = new Property("p2", "o2", null);
        property_p3_owner_o2 = new Property("p3", "o2", null);
    }

    @AfterAll
    public static void tearDownObjects() {
        property_p1_owner_o1 = null;
        property_p2_owner_o1 = null;
        property_p3_owner_o1 = null;
        property_p4_owner_o1 = null;
        property_p5_owner_o1 = null;
        property_p6_owner_o1 = null;
        property_p7_owner_o1 = null;
        property_p8_owner_o1 = null;
        property_p9_owner_o1 = null;
        property_p10_owner_o1 = null;

        property_p1_owner_o2 = null;
        property_p2_owner_o2 = null;
        property_p3_owner_o2 = null;
    }

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + ChannelFinderPropertiesIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
    }

    @Test
    void channelfinderUp() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyRetrieveCheck() {
        // what
        //     check(s) for retrieve tag
        //         e.g.
        //             retrieve non-existing property

        ITUtilProperties.assertRetrieveProperty("/p11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyRemoveCheck() {
        // what
        //     check(s) for remove property
        //         e.g.
        //             remove non-existing property

        try {
            // might be both 401, 404
            //     401 UNAUTHORIZED
            //     404 NOT_FOUND

            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.NONE,  "/p11", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER,  "/p11", HttpURLConnection.HTTP_NOT_FOUND);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.ADMIN, "/p11", HttpURLConnection.HTTP_NOT_FOUND);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyCreateUpdateCheckJson() {
        // what
        //     check(s) for create / update property
        //         e.g.
        //             user without required role PropertyMod
        //             content not ok
        //                 json    - incomplete
        //                 name    - null, empty
        //                 owner   - null, empty
        //                 channel - exists

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_property_p1_name_na     = "{\"na\":\"p1\",\"owner\":\"o1\"}";
        String json_property_p1_owner_ow    = "{\"name\":\"p1\",\"ow\":\"o1\"}";

        String json_property_p1_channels_c1 = "{\"name\":\"p1\",\"owner\":\"o1\",\"value\":null,\"channels\":["
                + "{\"name\":\"c1\",\"owner\":\"o1\",\"properties\":[{\"name\":\"p1\",\"owner\":\"o1\",\"value\":\"asdf\",\"channels\":[]}],\"tags\":[]}"
                + "]}";

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete1,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete2,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete3,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete4,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete5,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete6,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete7,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete8,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete9,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_name_na,     HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_owner_ow,    HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_channels_c1, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete1,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete2,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete3,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete4,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete5,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete6,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete7,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete8,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_incomplete9,             HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_property_p1_name_na,     HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_property_p1_owner_ow,    HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/p1", json_property_p1_channels_c1, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyCreateUpdateCheck() {
        // what
        //     check(s) for create / update property
        //         e.g.
        //             user without required role PropertyMod
        //             content not ok
        //                 json    - incomplete
        //                 name    - null, empty
        //                 owner   - null, empty
        //                 channel - exists

        Property property_check = new Property();

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.NONE,  "/p1", property_p1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.NONE,  "/p1", property_p1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.USER,  "/p1", property_p1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.USER,  "/p1", property_p1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName(null);

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("");

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("asdf");
            property_check.setOwner(null);

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("asdf");
            property_check.setOwner("");

            ITUtilProperties.assertCreateReplaceProperty      (AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilProperties.assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty() {
        // what
        //     user with required role PropertyMod
        //     create property
        //         list, create property, list, retrieve, delete (unauthorized), delete, list

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/t1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1",                    property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p1?withChannels=true",  property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p1?withChannels=false", property_p1_owner_o1);

            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.NONE,  "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER,  "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.ADMIN, "/p1", HttpURLConnection.HTTP_OK);

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty2() {
        // what
        //     create properties, one by one
        //         list, create property * 2, list, retrieve, retrieve, delete, list, retrieve, delete, list

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty("/p1", property_p1_owner_o1);
            ITUtilProperties.assertCreateReplaceProperty("/p2", property_p2_owner_o1);

            ITUtilProperties.assertListProperties(2,
                    property_p1_owner_o1,
                    property_p2_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p2", property_p2_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p1");

            ITUtilProperties.assertListProperties(1,
                    property_p2_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p2", property_p2_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p2");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty3RenameOwner() {
        // what
        //     replace property, rename owner
        //         list, create property, list, retrieve, update, retrieve, delete, list

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty("/p1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);

            ITUtilProperties.assertAddPropertyMultipleChannels("/p1", property_p1_owner_o2);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o2);

            ITUtilProperties.assertRemoveProperty("/p1");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty4ReplaceNonExisting() {
        // what
        //     replace non-existing property
        //         list, update, list, retrieve, delete, list

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertAddPropertyMultipleChannels("/p1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p1");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty5SingleChannel() {
        // what
        //     add property to single channel
        //         clean start, create property, create channel,
        //         add property to single channel,
        //         list, retrieve
        //         remove property from single channel,
        //         delete channel, delete property, clean end

        Channel channel_c1 = new Channel("c1", "o1");

        Property property_p1_value    = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner(), "asdf");

        Channel channel_c1_properties = new Channel("c1", "o1");
        channel_c1_properties.addProperty(property_p1_value);

        Property property_p1_channels = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner());
        property_p1_channels.getChannels().add(channel_c1_properties);

        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertListProperties(0);

            ITUtilTags.assertListTags(0);

            ITUtilChannels.assertListChannels(0);

            // --------------------------------------------------------------------------------
            // put
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertCreateReplaceProperty("/p1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);

            ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1);

            ITUtilChannels.assertListChannels(1, channel_c1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);

            // --------------------------------------------------------------------------------
            // complex tests, add property to single channel
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertAddPropertySingleChannel("/p1/c1", property_p1_value);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_channels);

            ITUtilChannels.assertListChannels(1, channel_c1_properties);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_properties);

            // --------------------------------------------------------------------------------
            // complex tests, remove property from single channel
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertRemovePropertySingleChannel("/p1/c1");

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);

            ITUtilChannels.assertListChannels(1, channel_c1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);

            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertDeleteChannel("/c1");

            ITUtilChannels.assertListChannels(0);

            ITUtilTags.assertListTags(0);

            ITUtilProperties.assertRemoveProperty("/p1");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty6MultipleChannels() {
        // what
        //     add property to multiple channels
        //         clean start, create property, create channel(s),
        //         add property to multiple channel(s),
        //         list, retrieve
        //         delete property, delete channel(s), clean end

        Channel channel_c1 = new Channel("c1", "o1");
        Channel channel_c2 = new Channel("c2", "o1");
        Channel channel_c3 = new Channel("c3", "o1");

        Property property_p1_value1 = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner(), "qwer");
        Property property_p1_value2 = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner(), "asdf");
        Property property_p1_value3 = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner(), "zxcv");

        Channel channel_c1_properties = new Channel("c1", "o1");
        Channel channel_c2_properties = new Channel("c2", "o1");
        Channel channel_c3_properties = new Channel("c3", "o1");
        channel_c1_properties.addProperty(property_p1_value1);
        channel_c2_properties.addProperty(property_p1_value2);
        channel_c3_properties.addProperty(property_p1_value3);

        Property property_p1_channels = new Property(property_p1_owner_o1.getName(), property_p1_owner_o1.getOwner(), null);
        property_p1_channels.getChannels().add(channel_c1_properties);
        property_p1_channels.getChannels().add(channel_c2_properties);
        property_p1_channels.getChannels().add(channel_c3_properties);

        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertListProperties(0);

            ITUtilTags.assertListTags(0);

            ITUtilChannels.assertListChannels(0);

            // --------------------------------------------------------------------------------
            // put
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertCreateReplaceProperty("/p1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_o1);

            ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1);
            ITUtilChannels.assertCreateReplaceChannel("/c2", channel_c2);
            ITUtilChannels.assertCreateReplaceChannel("/c3", channel_c3);

            ITUtilChannels.assertListChannels(3,
                    channel_c1,
                    channel_c2,
                    channel_c3);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);
            ITUtilChannels.assertRetrieveChannel("/c2", channel_c2);
            ITUtilChannels.assertRetrieveChannel("/c3", channel_c3);

            // --------------------------------------------------------------------------------
            // complex tests, add property to multiple channels
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertAddPropertyMultipleChannels("/p1", property_p1_channels);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_channels);

            ITUtilChannels.assertListChannels(3,
                    channel_c1_properties,
                    channel_c2_properties,
                    channel_c3_properties);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_properties);
            ITUtilChannels.assertRetrieveChannel("/c2", channel_c2_properties);
            ITUtilChannels.assertRetrieveChannel("/c3", channel_c3_properties);

            // --------------------------------------------------------------------------------
            // clean end
            // --------------------------------------------------------------------------------

            ITUtilChannels.assertDeleteChannel("/c3");
            ITUtilChannels.assertDeleteChannel("/c2");
            ITUtilChannels.assertDeleteChannel("/c1");

            ITUtilChannels.assertListChannels(0);

            ITUtilTags.assertListTags(0);

            ITUtilProperties.assertRemoveProperty("/p1");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertiesCreateUpdateCheck() {
        // what
        //     check(s) for create properties
        //         e.g.
        //             user without required role PropertyMod
        //             content not ok
        //                 json    - incomplete
        //                 name    - null, empty
        //                 owner   - null, empty
        //                 channel - exists

        String json_incomplete1          = "{\"incomplete\"}";
        String json_property_p1_name_na  = "{\"na\":\"p1\",\"owner\":\"o1\"}";
        String json_property_p1_owner_ow = "{\"name\":\"p1\",\"ow\":\"o1\"}";

        ObjectMapper mapper = new ObjectMapper();

        try {
            ITUtilProperties.assertListProperties(0);

            String json_multiple = "["
                    +       mapper.writeValueAsString(property_p1_owner_o1)
                    + "," + mapper.writeValueAsString(property_p2_owner_o1)
                    + "," + mapper.writeValueAsString(property_p3_owner_o1)
                    + "," + mapper.writeValueAsString(property_p4_owner_o1)
                    + "," + mapper.writeValueAsString(property_p5_owner_o1)
                    + "," + mapper.writeValueAsString(property_p6_owner_o1)
                    + "," + mapper.writeValueAsString(property_p7_owner_o1)
                    + "," + mapper.writeValueAsString(property_p8_owner_o1)
                    + "," + mapper.writeValueAsString(property_p9_owner_o1)
                    + "," + mapper.writeValueAsString(property_p10_owner_o1)
                    + "," + json_incomplete1 + "]";

            ITUtilProperties.assertCreateReplaceProperties(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertAddMultipleProperties("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            json_multiple = "["
                    +       mapper.writeValueAsString(property_p1_owner_o1)
                    + "," + mapper.writeValueAsString(property_p2_owner_o1)
                    + "," + mapper.writeValueAsString(property_p3_owner_o1)
                    + "," + mapper.writeValueAsString(property_p4_owner_o1)
                    + "," + mapper.writeValueAsString(property_p5_owner_o1)
                    + "," + mapper.writeValueAsString(property_p6_owner_o1)
                    + "," + mapper.writeValueAsString(property_p7_owner_o1)
                    + "," + mapper.writeValueAsString(property_p8_owner_o1)
                    + "," + mapper.writeValueAsString(property_p9_owner_o1)
                    + "," + mapper.writeValueAsString(property_p10_owner_o1)
                    + "," + json_property_p1_name_na + "]";

            ITUtilProperties.assertCreateReplaceProperties(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

            ITUtilProperties.assertAddMultipleProperties("", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

            json_multiple = "["
                    +       mapper.writeValueAsString(property_p1_owner_o1)
                    + "," + mapper.writeValueAsString(property_p2_owner_o1)
                    + "," + mapper.writeValueAsString(property_p3_owner_o1)
                    + "," + mapper.writeValueAsString(property_p4_owner_o1)
                    + "," + mapper.writeValueAsString(property_p5_owner_o1)
                    + "," + mapper.writeValueAsString(property_p6_owner_o1)
                    + "," + mapper.writeValueAsString(property_p7_owner_o1)
                    + "," + mapper.writeValueAsString(property_p8_owner_o1)
                    + "," + mapper.writeValueAsString(property_p9_owner_o1)
                    + "," + mapper.writeValueAsString(property_p10_owner_o1)
                    + "," + json_property_p1_owner_ow + "]";

            ITUtilProperties.assertCreateReplaceProperties(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertAddMultipleProperties("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperties() {
        // what
        //     create properties
        //         list, create properties (10), list, retrieve (10), delete (5), list, delete (5), list

        Property[] properties_10 = new Property[] {
                property_p1_owner_o1,
                property_p2_owner_o1,
                property_p3_owner_o1,
                property_p4_owner_o1,
                property_p5_owner_o1,
                property_p6_owner_o1,
                property_p7_owner_o1,
                property_p8_owner_o1,
                property_p9_owner_o1,
                property_p10_owner_o1
        };

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperties("", properties_10);

            ITUtilProperties.assertListProperties(10,
                    property_p1_owner_o1,
                    property_p10_owner_o1,
                    property_p2_owner_o1,
                    property_p3_owner_o1,
                    property_p4_owner_o1,
                    property_p5_owner_o1,
                    property_p6_owner_o1,
                    property_p7_owner_o1,
                    property_p8_owner_o1,
                    property_p9_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1",  property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p2",  property_p2_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p3",  property_p3_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p4",  property_p4_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p5",  property_p5_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p6",  property_p6_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p7",  property_p7_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p8",  property_p8_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p9",  property_p9_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p10", property_p10_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p1");
            ITUtilProperties.assertRemoveProperty("/p2");
            ITUtilProperties.assertRemoveProperty("/p3");
            ITUtilProperties.assertRemoveProperty("/p4");
            ITUtilProperties.assertRemoveProperty("/p5");

            ITUtilProperties.assertListProperties(5,
                    property_p10_owner_o1,
                    property_p6_owner_o1,
                    property_p7_owner_o1,
                    property_p8_owner_o1,
                    property_p9_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p6");
            ITUtilProperties.assertRemoveProperty("/p7");
            ITUtilProperties.assertRemoveProperty("/p8");
            ITUtilProperties.assertRemoveProperty("/p9");
            ITUtilProperties.assertRemoveProperty("/p10");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperties2ReplaceNonExisting() {
        // what
        //     replace non-existing properties
        //         list, update properties (10), list, retrieve (10), delete (5), list, delete (5), list

        Property[] properties_10 = new Property[] {
                property_p1_owner_o1,
                property_p2_owner_o1,
                property_p3_owner_o1,
                property_p4_owner_o1,
                property_p5_owner_o1,
                property_p6_owner_o1,
                property_p7_owner_o1,
                property_p8_owner_o1,
                property_p9_owner_o1,
                property_p10_owner_o1
        };

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertAddMultipleProperties("", properties_10);

            ITUtilProperties.assertListProperties(10,
                    property_p1_owner_o1,
                    property_p10_owner_o1,
                    property_p2_owner_o1,
                    property_p3_owner_o1,
                    property_p4_owner_o1,
                    property_p5_owner_o1,
                    property_p6_owner_o1,
                    property_p7_owner_o1,
                    property_p8_owner_o1,
                    property_p9_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1",  property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p2",  property_p2_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p3",  property_p3_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p4",  property_p4_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p5",  property_p5_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p6",  property_p6_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p7",  property_p7_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p8",  property_p8_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p9",  property_p9_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p10", property_p10_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p1");
            ITUtilProperties.assertRemoveProperty("/p2");
            ITUtilProperties.assertRemoveProperty("/p3");
            ITUtilProperties.assertRemoveProperty("/p4");
            ITUtilProperties.assertRemoveProperty("/p5");

            ITUtilProperties.assertListProperties(5,
                    property_p10_owner_o1,
                    property_p6_owner_o1,
                    property_p7_owner_o1,
                    property_p8_owner_o1,
                    property_p9_owner_o1);

            ITUtilProperties.assertRemoveProperty("/p6");
            ITUtilProperties.assertRemoveProperty("/p7");
            ITUtilProperties.assertRemoveProperty("/p8");
            ITUtilProperties.assertRemoveProperty("/p9");
            ITUtilProperties.assertRemoveProperty("/p10");

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }

}
