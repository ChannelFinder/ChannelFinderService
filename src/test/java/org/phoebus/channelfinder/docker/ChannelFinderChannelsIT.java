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
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for ChannelFinder and Elasticsearch with focus on usage of
 * {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.ChannelManager
 * @see org.phoebus.channelfinder.docker.ITTestFixture
 * @see org.phoebus.channelfinder.docker.ITUtil
 * @see org.phoebus.channelfinder.docker.ITUtilChannels
 */
@Testcontainers
public class ChannelFinderChannelsIT {

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
    //         see test QueryByPattern for list of channels which match given expressions
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------
    //     CHANNELFINDER API
    //     --------------------
    //     Retrieve a Channel                        .../channels/<name>                                                            GET
    //     List Channels / Query by Pattern          .../channels?prop1=patt1&prop2=patt2&~tag=patt3&~name=patt4...                 GET
    //     Query Count                               .../channels/count?prop1=patt1&prop2=patt2&~tag=patt3&~name=patt4...           GET
    //     Query Combined                            .../channels/combined?prop1=patt1&prop2=patt2&~tag=patt3&~name=patt4...        GET
    //     Create / Replace Channel                  .../channels/<name>                                                            PUT
    //     Create / Replace Multiple Channels        .../channels                                                                   PUT
    //     Update Channel                            .../channels/<name>                                                            POST
    //     Update Channels                           .../channels                                                                   POST
    //     Delete a Channel                          .../channels/<name>                                                            DELETE
    //     ------------------------------------------------------------------------------------------------

    // test data
    //     channels c1 - c10, owner o1
    //     channel  c1,       owner o2

    static Channel channel_c1_owner_o1;
    static Channel channel_c2_owner_o1;
    static Channel channel_c3_owner_o1;
    static Channel channel_c4_owner_o1;
    static Channel channel_c5_owner_o1;
    static Channel channel_c6_owner_o1;
    static Channel channel_c7_owner_o1;
    static Channel channel_c8_owner_o1;
    static Channel channel_c9_owner_o1;
    static Channel channel_c10_owner_o1;

    static Channel channel_c1_owner_o2;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        channel_c1_owner_o1 = new Channel("c1", "o1");
        channel_c2_owner_o1 = new Channel("c2", "o1");
        channel_c3_owner_o1 = new Channel("c3", "o1");
        channel_c4_owner_o1 = new Channel("c4", "o1");
        channel_c5_owner_o1 = new Channel("c5", "o1");
        channel_c6_owner_o1 = new Channel("c6", "o1");
        channel_c7_owner_o1 = new Channel("c7", "o1");
        channel_c8_owner_o1 = new Channel("c8", "o1");
        channel_c9_owner_o1 = new Channel("c9", "o1");
        channel_c10_owner_o1 = new Channel("c10", "o1");

        channel_c1_owner_o2 = new Channel("c1", "o2");
    }

    @AfterAll
    public static void tearDownObjects() {
        channel_c1_owner_o1 = null;
        channel_c2_owner_o1 = null;
        channel_c3_owner_o1 = null;
        channel_c4_owner_o1 = null;
        channel_c5_owner_o1 = null;
        channel_c6_owner_o1 = null;
        channel_c7_owner_o1 = null;
        channel_c8_owner_o1 = null;
        channel_c9_owner_o1 = null;
        channel_c10_owner_o1 = null;

        channel_c1_owner_o2 = null;
    }

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + ChannelFinderChannelsIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
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
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannelRetrieveCheck() {
        // what
        //     check(s) for retrieve channel
        //         e.g.
        //             retrieve non-existing channel

        ITUtilChannels.assertRetrieveChannel("/c11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleChannelDeleteCheck() {
        // what
        //     check(s) for delete channel
        //         e.g.
        //             remove non-existing channel

        try {
            // might be both 401, 404
            //     401 UNAUTHORIZED
            //     404 NOT_FOUND

            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.NONE,  "/c11", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.USER,  "/c11", HttpURLConnection.HTTP_NOT_FOUND);
            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.ADMIN, "/c11", HttpURLConnection.HTTP_NOT_FOUND);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannelCreateUpdateCheckJson() {
        // what
        //     check(s) for create / update channel
        //         e.g.
        //             user without required role ChannelMod
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 properties - exists
        //                 tags       - exists

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_channel_c1_name_na  = "{\"na\":\"c1\",\"owner\":\"o1\"}";
        String json_channel_c1_owner_ow = "{\"name\":\"c1\",\"ow\":\"o1\"}";

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete1,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete2,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete3,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete4,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete5,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete6,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete7,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete8,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_incomplete9,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_channel_c1_name_na,  HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/c1", json_channel_c1_owner_ow, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete1,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete2,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete3,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete4,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete5,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete6,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete7,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete8,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_incomplete9,         HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_channel_c1_name_na,  HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel(AuthorizationChoice.ADMIN, "/t1", json_channel_c1_owner_ow, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannelCreateUpdateCheck() {
        // what
        //     check(s) for create / update channel
        //         e.g.
        //             user without required role ChannelMod
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 properties - exists
        //                 tags       - exists

        Channel channel_check = new Channel();

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.NONE,  "/c1", channel_c1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.NONE,  "/c1", channel_c1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.USER,  "/c1", channel_c1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.USER,  "/c1", channel_c1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);

            channel_check.setName(null);

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);

            channel_check.setName("");

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);

            channel_check.setName("asdf");
            channel_check.setOwner(null);

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);

            channel_check.setName("asdf");
            channel_check.setOwner("");

            ITUtilChannels.assertCreateReplaceChannel(AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);
            ITUtilChannels.assertUpdateChannel       (AuthorizationChoice.ADMIN, "/asdf", channel_check, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannel() {
        // what
        //     user with required role ChannelMod
        //     create channel
        //         list, create channel, list, retrieve, delete (unauthorized), delete, list

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertCountChannels(1);
            ITUtilChannels.assertListChannels(1, channel_c1_owner_o1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.NONE,  "/c1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.USER,  "/c1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilChannels.assertDeleteChannel(AuthorizationChoice.ADMIN, "/c1", HttpURLConnection.HTTP_OK);

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannel2() {
        // what
        //     create channels, one by one
        //         list, create channel * 2, list, retrieve, retrieve, delete, list, retrieve, delete, list

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1_owner_o1);
            ITUtilChannels.assertCreateReplaceChannel("/c2", channel_c2_owner_o1);

            ITUtilChannels.assertCountChannels(2);
            ITUtilChannels.assertListChannels(2,
                    channel_c1_owner_o1,
                    channel_c2_owner_o1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c2", channel_c2_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c1");

            ITUtilChannels.assertCountChannels(1);
            ITUtilChannels.assertListChannels(1, channel_c2_owner_o1);

            ITUtilChannels.assertRetrieveChannel("/c2", channel_c2_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c2");

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannel3RenameOwner() {
        // what
        //     replace channel, rename channel
        //         list, create channel, list, retrieve, update, retrieve, delete, list

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertCountChannels(1);
            ITUtilChannels.assertListChannels(1, channel_c1_owner_o1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertUpdateChannel("/c1", channel_c1_owner_o2);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_owner_o2);

            ITUtilChannels.assertDeleteChannel("/c1");

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannel4ReplaceNonExisting() {
        // what
        //     replace non-existing channel
        //         list, update, list, retrieve, delete, list

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertUpdateChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertCountChannels(1);
            ITUtilChannels.assertListChannels(1, channel_c1_owner_o1);

            ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c1");

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannelsCreateUpdateCheck() {
        // what
        //     check(s) for create tag
        //         e.g.
        //             user without required role ChannelMod
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 properties - exists
        //                 tags       - exists

        String json_incomplete1         = "{\"incomplete\"}";
        String json_channel_c1_name_na  = "{\"na\":\"c1\",\"owner\":\"o1\"}";
        String json_channel_c1_owner_ow = "{\"name\":\"c1\",\"ow\":\"o1\"}";

        ObjectMapper mapper = new ObjectMapper();

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            String json_multiple = "["
                    +       mapper.writeValueAsString(channel_c1_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c2_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c3_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c4_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c5_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c6_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c7_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c8_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c9_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c10_owner_o1)
                    + "," + json_incomplete1 + "]";

            ITUtilChannels.assertCreateReplaceMultipleChannels(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertUpdateChannels("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            json_multiple = "["
                    +       mapper.writeValueAsString(channel_c1_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c2_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c3_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c4_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c5_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c6_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c7_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c8_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c9_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c10_owner_o1)
                    + "," + json_channel_c1_name_na + "]";

            ITUtilChannels.assertCreateReplaceMultipleChannels(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

            ITUtilChannels.assertUpdateChannels("", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

            json_multiple = "["
                    +       mapper.writeValueAsString(channel_c1_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c2_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c3_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c4_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c5_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c6_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c7_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c8_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c9_owner_o1)
                    + "," + mapper.writeValueAsString(channel_c10_owner_o1)
                    + "," + json_channel_c1_owner_ow + "]";

            ITUtilChannels.assertCreateReplaceMultipleChannels(AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertUpdateChannels("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannels() {
        // what
        //     create channels
        //         list, create channels (10), list, retrieve (10), delete (5), list, delete (5), list

        Channel[] channels_10 = new Channel[] {
                channel_c1_owner_o1,
                channel_c10_owner_o1,
                channel_c2_owner_o1,
                channel_c3_owner_o1,
                channel_c4_owner_o1,
                channel_c5_owner_o1,
                channel_c6_owner_o1,
                channel_c7_owner_o1,
                channel_c8_owner_o1,
                channel_c9_owner_o1
        };

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertCreateReplaceMultipleChannels("", channels_10);

            ITUtilChannels.assertCountChannels(10);
            ITUtilChannels.assertListChannels(10, channels_10);

            ITUtilChannels.assertRetrieveChannel("/c1",  channel_c1_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c2",  channel_c2_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c3",  channel_c3_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c4",  channel_c4_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c5",  channel_c5_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c6",  channel_c6_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c7",  channel_c7_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c8",  channel_c8_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c9",  channel_c9_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c10", channel_c10_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c1");
            ITUtilChannels.assertDeleteChannel("/c2");
            ITUtilChannels.assertDeleteChannel("/c3");
            ITUtilChannels.assertDeleteChannel("/c4");
            ITUtilChannels.assertDeleteChannel("/c5");

            ITUtilChannels.assertCountChannels(5);
            ITUtilChannels.assertListChannels(5,
                    channel_c10_owner_o1,
                    channel_c6_owner_o1,
                    channel_c7_owner_o1,
                    channel_c8_owner_o1,
                    channel_c9_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c6");
            ITUtilChannels.assertDeleteChannel("/c7");
            ITUtilChannels.assertDeleteChannel("/c8");
            ITUtilChannels.assertDeleteChannel("/c9");
            ITUtilChannels.assertDeleteChannel("/c10");

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannels2ReplaceNonExisting() {
        // what
        //     replace non-existing channels
        //         list, update channels (10), list, retrieve (10), delete (5), list, delete (5), list

        Channel[] channels_10 = new Channel[] {
                channel_c1_owner_o1,
                channel_c10_owner_o1,
                channel_c2_owner_o1,
                channel_c3_owner_o1,
                channel_c4_owner_o1,
                channel_c5_owner_o1,
                channel_c6_owner_o1,
                channel_c7_owner_o1,
                channel_c8_owner_o1,
                channel_c9_owner_o1
        };

        try {
            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);

            ITUtilChannels.assertUpdateChannels("", channels_10);

            ITUtilChannels.assertCountChannels(10);
            ITUtilChannels.assertListChannels(10, channels_10);

            ITUtilChannels.assertRetrieveChannel("/c1",  channel_c1_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c2",  channel_c2_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c3",  channel_c3_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c4",  channel_c4_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c5",  channel_c5_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c6",  channel_c6_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c7",  channel_c7_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c8",  channel_c8_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c9",  channel_c9_owner_o1);
            ITUtilChannels.assertRetrieveChannel("/c10", channel_c10_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c1");
            ITUtilChannels.assertDeleteChannel("/c2");
            ITUtilChannels.assertDeleteChannel("/c3");
            ITUtilChannels.assertDeleteChannel("/c4");
            ITUtilChannels.assertDeleteChannel("/c5");

            ITUtilChannels.assertCountChannels(5);
            ITUtilChannels.assertListChannels(5,
                    channel_c10_owner_o1,
                    channel_c6_owner_o1,
                    channel_c7_owner_o1,
                    channel_c8_owner_o1,
                    channel_c9_owner_o1);

            ITUtilChannels.assertDeleteChannel("/c6");
            ITUtilChannels.assertDeleteChannel("/c7");
            ITUtilChannels.assertDeleteChannel("/c8");
            ITUtilChannels.assertDeleteChannel("/c9");
            ITUtilChannels.assertDeleteChannel("/c10");

            ITUtilChannels.assertCountChannels(0);
            ITUtilChannels.assertListChannels(0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     */
    @Test
    void handleChannels3QueryByPattern() {
        // what
        //     query by pattern
        //     --------------------------------------------------------------------------------
        //     set up test fixture
        //     test
        //         query by pattern
        //             combine search parameters and channels, properties, tags
        //     tear down test fixture

        // --------------------------------------------------------------------------------
        // set up test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.setup();

        // --------------------------------------------------------------------------------
        // query by pattern
        //     --------------------------------------------------------------------------------
        //     patterns
        //         ?    single character
        //         *    any number of characters
        //     search parameters
        //         keyword
        //             ~name
        //             ~tag
        //             propertyName
        //         pagination
        //             ~size
        //             ~from
        //     --------------------------------------------------------------------------------
        //     query for pattern
        //         non-existing
        //         exact
        //         pagination
        //         regex, regex pagination
        //         or, or regex, or regex pagination
        //         not, not regex, not regex pagination, not or, not regex or, not or regex pagination
        //         combinations
        //     --------------------------------------------------------------------------------
        //     channels (10)
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
        //     properties (4)
        //         domain      (10 channels, values - cryo, power)
        //         element     (10 channels, values - source, initial, radio, magnet, supra)
        //         type        (10 channels, values - read, write, readwrite)
        //         cell        (10 channels, values - block1, block2, block3, block4, block5)
        //     tags (4)
        //         archived    ( 4 channels)
        //         handle_this ( 3 channels)
        //         noteworthy  (10 channels)
        //         not_used    ( 0 channels)
        //     --------------------------------------------------------------------------------
        //     note
        //         regex          regex-like, not regex
        //         ?              means 1 character, not 0 or 1 character
        //         ~from          causes exception if used without ~size
        //                        causes exception if not a number
        //                        if negative, then handled as 0
        //         ~size          causes exception if negative number or not a number
        //         ~size&~from    if result without (~from) considered as array of 10 elements (0,1,2,3,4 5 6,7,8,9),
        //                        then ~from=2 means element 2, i.e. 3rd element
        //         ----------------------------------------------------------------------------
        //         or             property value, not channel name, not tag name
        //                        -------------------------------------------------------------
        //                        propertyName=value1&propertyName=value
        //                            search for propertyName with value1 or value2
        //                        may be used with regex
        //                        may be used with pagination
        //                        care to be taken in usage as expression becomes complicated
        //         ----------------------------------------------------------------------------
        //         not            tag name, property value, not channel name
        //                        -------------------------------------------------------------
        //                        propertyName!=value1
        //                        ~tag!=value1
        //                        may be used with regex
        //                        may be used with pagination
        //                        care to be taken in usage as expression becomes complicated
        //                            if property values are (1, 2, 3, 4, 5)
        //                            with expression propertyName!=1&propertyName!=2
        //                                then result is all values - not 1 or not 2
        //     --------------------------------------------------------------------------------
        //     search for non-existing property or tag gives exception
        //     search for non-existing channel         gives empty result
        //     --------------------------------------------------------------------------------
        //     tests may expanded by (give possibilities for broader search)
        //         all properties not present on all channels
        //         channel names have different lengths
        //         characters need escaping
        //     --------------------------------------------------------------------------------

        try {
            // channels (all)
            ITUtilChannels.assertCountChannels(ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels(10, ITTestFixture.channels_all_properties_tags);

            // channel (name)
            //     query for pattern
            //         non-existing
            //         exact
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
            //         pagination
            //         regex, regex pagination

            ITUtilChannels.assertCountChannels("?~name=asdf", 0);
            ITUtilChannels.assertListChannels("?~name=asdf", 0);

            ITUtilChannels.assertCountChannels("?~name=ABC:DEF-GHI:JKL:001", 1);
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

            ITUtilChannels.assertCountChannels("?~name=*001", 2);
            ITUtilChannels.assertListChannels("?~name=*001",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?~name=ABC:DEF-XYZ:JKL:01?", 2);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-XYZ:JKL:01?",
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?~name=ABC:DEF-???:JKL:003", 2);
            ITUtilChannels.assertListChannels("?~name=ABC:DEF-???:JKL:003",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=0", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=0", 0);

            ITUtilChannels.assertCountChannels("?~size=5", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=5",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=100", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=100", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=3&~from=-1", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=3&~from=-1", HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            ITUtilChannels.assertCountChannels("?~size=3&~from=0", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=3&~from=0",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=3&~from=1", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=3&~from=1",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=3&~from=2", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=3&~from=2",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?~size=3&~from=3", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~size=3&~from=3",
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?~name=*1*&~size=4&~from=2", 6);
            ITUtilChannels.assertListChannels("?~name=*1*&~size=4&~from=2",
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            // property name (domain)
            //     query for pattern
            //         non-existing
            //         exact - cryo, power
            //         regex, regex pagination
            //         or, or regex, or regex pagination
            //         not, not with regex, not with regex and pagination
            ITUtilChannels.assertCountChannels("?domain=asdf", 0);
            ITUtilChannels.assertListChannels("?domain=asdf", 0);

            ITUtilChannels.assertCountChannels("?domain=cryo", 5);
            ITUtilChannels.assertListChannels("?domain=cryo",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=power", 5);
            ITUtilChannels.assertListChannels("?domain=power",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=cry?", 5);
            ITUtilChannels.assertListChannels("?domain=cry?",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=?????r?????", 0);
            ITUtilChannels.assertListChannels("?domain=?????r?????", 0);

            ITUtilChannels.assertCountChannels("?domain=?r??", 5);
            ITUtilChannels.assertListChannels("?domain=?r??",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=?r???", 0);
            ITUtilChannels.assertListChannels("?domain=?r???", 0);

            ITUtilChannels.assertCountChannels("?domain=*a*", 0);
            ITUtilChannels.assertListChannels("?domain=*a*", 0);

            ITUtilChannels.assertCountChannels("?domain=?ow*&~size=4&~from=2", 5);
            ITUtilChannels.assertListChannels("?domain=?ow*&~size=4&~from=2",
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=cryo&domain=power", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?domain=cryo&domain=power",
                    ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=*o&domain=asdf?", 5);
            ITUtilChannels.assertListChannels("?domain=*o&domain=asdf?",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=*o&domain=asdf?&~size=3", 5);
            ITUtilChannels.assertListChannels("?domain=*o&domain=asdf?&~size=3",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=*o&domain=asdf?&~size=3&~from=1", 5);
            ITUtilChannels.assertListChannels("?domain=*o&domain=asdf?&~size=3&~from=1",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags);

            ITUtilChannels.assertCountChannels("?domain!=cryo", 5);
            ITUtilChannels.assertListChannels("?domain!=cryo",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain!=*r", 5);
            ITUtilChannels.assertListChannels("?domain!=*r",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags);

            ITUtilChannels.assertCountChannels("?domain!=cryo&~size=4", 5);
            ITUtilChannels.assertListChannels("?domain!=cryo&~size=4",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags);

            ITUtilChannels.assertCountChannels("?domain!=cryo&~size=4&~from=0", 5);
            ITUtilChannels.assertListChannels("?domain!=cryo&~size=4&~from=0",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags);

            // property name (element)
            //     query for pattern
            //         non-existing
            //         exact - source, initial, radio, magnet, supra
            //         regex, regex pagination
            //         or, or regex, or regex pagination
            //         not, not regex, not regex pagination, not or, not regex or, not or regex pagination
            ITUtilChannels.assertCountChannels("?element=asdf", 0);
            ITUtilChannels.assertListChannels("?element=asdf", 0);

            ITUtilChannels.assertCountChannels("?element=source", 2);
            ITUtilChannels.assertListChannels("?element=source",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?element=initial", 2);
            ITUtilChannels.assertListChannels("?element=initial",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?element=radio", 2);
            ITUtilChannels.assertListChannels("?element=radio",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?element=magnet", 2);
            ITUtilChannels.assertListChannels("?element=magnet",
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags);

            ITUtilChannels.assertCountChannels("?element=supra", 2);
            ITUtilChannels.assertListChannels("?element=supra",
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?element=*i?", 2);
            ITUtilChannels.assertListChannels("?element=*i?",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?element=?i*", 0);
            ITUtilChannels.assertListChannels("?element=?i*", 0);

            ITUtilChannels.assertCountChannels("?element=*a*&~size=2&~from=4", 8);
            ITUtilChannels.assertListChannels("?element=*a*&~size=2&~from=4",
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?element=initial&element=radio&element=supra", 6);
            ITUtilChannels.assertListChannels("?element=initial&element=radio&element=supra",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?element=rad?o&element=asdf?", 2);
            ITUtilChannels.assertListChannels("?element=rad?o&element=asdf?",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?element=initial&element=radio&element=supra&~size=4", 6);
            ITUtilChannels.assertListChannels("?element=initial&element=radio&element=supra&~size=4",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?element=initial&element=radio&element=supra&~size=4&~from=3", 6);
            ITUtilChannels.assertListChannels("?element=initial&element=radio&element=supra&~size=4&~from=3",
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?element!=initial", 8);
            ITUtilChannels.assertListChannels("?element!=initial",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?element!=source&element!=initial", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?element!=source&element!=initial", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?element!=source&element!=initial&~size=6", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?element!=source&element!=initial&~size=6",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?element!=source&element!=initial&~size=6&~from=5", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?element!=source&element!=initial&~size=6&~from=5",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            // property name (type)
            //     query for pattern
            //         non-existing
            //         exact - read, write, readwrite, regex
            //         regex, regex pagination
            //         or, or regex, or regex pagination
            //         not, not regex, not regex pagination, not or, not regex or, not or regex pagination
            ITUtilChannels.assertCountChannels("?type=asdf", 0);
            ITUtilChannels.assertListChannels("?type=asdf", 0);

            ITUtilChannels.assertCountChannels("?type=read", 4);
            ITUtilChannels.assertListChannels("?type=read",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?type=write", 4);
            ITUtilChannels.assertListChannels("?type=write",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?type=readwrite", 2);
            ITUtilChannels.assertListChannels("?type=readwrite",
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?type=read*", 6);
            ITUtilChannels.assertListChannels("?type=read*",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?type=*write", 6);
            ITUtilChannels.assertListChannels("?type=*write",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?type=*r*", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?type=*r*", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?type=??a?&~size=2&~from=4", 4);
            ITUtilChannels.assertListChannels("?type=??a?&~size=2&~from=4", 0);

            ITUtilChannels.assertCountChannels("?type=write&type=readwrite", 6);
            ITUtilChannels.assertListChannels("?type=write&type=readwrite",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?type=writ?&type=writ*", 4);
            ITUtilChannels.assertListChannels("?type=writ?&type=writ*",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?type=write&type=readwrite&~size=10", 6);
            ITUtilChannels.assertListChannels("?type=write&type=readwrite&~size=10",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?type=write&type=readwrite&~size=10&~from=7", 6);
            ITUtilChannels.assertListChannels("?type=write&type=readwrite&~size=10&~from=7", 0);

            ITUtilChannels.assertCountChannels("?type!=asdf", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?type!=asdf", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?type!=asdf&~size=100", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?type!=asdf&~size=100", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?type!=asdf&~size=100&~from=0", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?type!=asdf&~size=100&~from=0", ITTestFixture.channels_all_properties_tags);

            // property name (cell)
            //     query for pattern
            //         non-existing
            //         exact - block1, block2, block3, block4, block5
            //         regex, regex pagination
            //         or, or regex, or regex pagination
            //         not, not regex, not regex pagination, not or, not regex or, not or regex pagination
            ITUtilChannels.assertCountChannels("?cell=asdf", 0);
            ITUtilChannels.assertListChannels("?cell=asdf", 0);

            ITUtilChannels.assertCountChannels("?cell=block1", 2);
            ITUtilChannels.assertListChannels("?cell=block1",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block2", 2);
            ITUtilChannels.assertListChannels("?cell=block2",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block3", 2);
            ITUtilChannels.assertListChannels("?cell=block3",
                    ITTestFixture.channel_ghi003_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block4", 2);
            ITUtilChannels.assertListChannels("?cell=block4",
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block5", 2);
            ITUtilChannels.assertListChannels("?cell=block5",
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block?", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?cell=block?", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=*2", 2);
            ITUtilChannels.assertListChannels("?cell=*2",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block?&~size=5&~from=5", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?cell=block?&~size=5&~from=5",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags,
                    ITTestFixture.channel_xyz010_properties_tags,
                    ITTestFixture.channel_xyz011_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block1&cell=block2", 4);
            ITUtilChannels.assertListChannels("?cell=block1&cell=block2",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=*1&cell=*2", 4);
            ITUtilChannels.assertListChannels("?cell=*1&cell=*2",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block1&cell=block2&cell=block2&~size=1", 4);
            ITUtilChannels.assertListChannels("?cell=block1&cell=block2&cell=block2&~size=1",
                    ITTestFixture.channel_ghi001_properties_tags);

            ITUtilChannels.assertCountChannels("?cell=block1&cell=block2&cell=block2&~size=1&~from=0", 4);
            ITUtilChannels.assertListChannels("?cell=block1&cell=block2&cell=block2&~size=1&~from=0",
                    ITTestFixture.channel_ghi001_properties_tags);

            ITUtilChannels.assertCountChannels("?cell!=block", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?cell!=block", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?cell!=block?", 0);
            ITUtilChannels.assertListChannels("?cell!=block?", 0);

            ITUtilChannels.assertCountChannels("?cell!=block*&size=10", 0);
            ITUtilChannels.assertListChannels("?cell!=block*&size=10", 0);

            ITUtilChannels.assertCountChannels("?cell!=block?*&size=10&~from=0", 0);
            ITUtilChannels.assertListChannels("?cell!=block?*&size=10&~from=0", 0);

            // tag (name)
            //     query for pattern
            //         non-existing
            //         exact - archived, handle_this, noteworthy, not_used
            //         regex, regex pagination
            //         not, not regex, not regex pagination
            ITUtilChannels.assertCountChannels("?~tag=asdf", 0);
            ITUtilChannels.assertListChannels("?~tag=asdf", 0);

            ITUtilChannels.assertCountChannels("?~tag=archived", 4);
            ITUtilChannels.assertListChannels("?~tag=archived",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag=handle_this", 3);
            ITUtilChannels.assertListChannels("?~tag=handle_this",
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag=noteworthy", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~tag=noteworthy", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag=not_used", 0);
            ITUtilChannels.assertListChannels("?~tag=not_used", 0);

            ITUtilChannels.assertCountChannels("?~tag=*_*", 3);
            ITUtilChannels.assertListChannels("?~tag=*_*",
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag=*i*", 6);
            ITUtilChannels.assertListChannels("?~tag=*i*",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag=*i*&~size=4&~from=1", 6);
            ITUtilChannels.assertListChannels("?~tag=*i*&~size=4&~from=1",
                    ITTestFixture.channel_ghi002_properties_tags,
                    ITTestFixture.channel_ghi010_properties_tags,
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag!=noteworthy", 0);
            ITUtilChannels.assertListChannels("?~tag!=noteworthy", 0);

            ITUtilChannels.assertCountChannels("?~tag!=not_used", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~tag!=not_used", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag!=not_used&~size=10", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~tag!=not_used&~size=10", ITTestFixture.channels_all_properties_tags);

            ITUtilChannels.assertCountChannels("?~tag!=not_used&~size=10&~from=10", ITTestFixture.channels_all_properties_tags.length);
            ITUtilChannels.assertListChannels("?~tag!=not_used&~size=10&~from=10", 0);

            // combinations
            //     query for pattern
            //         complex
            //             3 properties
            //             2 properties, 1 tag
            //             2 properties, 1 tag, or
            //             2 properties, 2 tags
            //             2 properties, 1 tag, pagination
            ITUtilChannels.assertCountChannels("?domain=cryo&element=source&cell=block1", 1);
            ITUtilChannels.assertListChannels("?domain=cryo&element=source&cell=block1", ITTestFixture.channel_ghi001_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=power&type=write&~tag=noteworthy", 2);
            ITUtilChannels.assertListChannels("?domain=power&type=write&~tag=noteworthy",
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=power&type=write&type=????write&~tag=noteworthy", 3);
            ITUtilChannels.assertListChannels("?domain=power&type=write&type=????write&~tag=noteworthy",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags,
                    ITTestFixture.channel_xyz003_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=*r*&type=*write&~tag=archived&~tag=noteworthy", 2);
            ITUtilChannels.assertListChannels("?domain=*r*&type=*write&~tag=archived&~tag=noteworthy",
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);

            ITUtilChannels.assertCountChannels("?domain=*r*&type=*write&~tag=noteworthy&~size=3&~from=2", 6);
            ITUtilChannels.assertListChannels("?domain=*r*&type=*write&~tag=noteworthy&~size=3&~from=2",
                    ITTestFixture.channel_ghi011_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags,
                    ITTestFixture.channel_xyz002_properties_tags);
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.tearDown();
    }

}
