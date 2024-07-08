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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.docker.ITUtil.EndpointChoice;
import org.phoebus.channelfinder.docker.ITUtil.MethodChoice;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support test of behavior for channel endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.docker.ITUtil
 */
public class ITUtilChannels {

    static final ObjectMapper mapper        = new ObjectMapper();
    static final Channel[] CHANNELS_NULL = null;
    static final Channel   CHANNEL_NULL  = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilChannels() {
        throw new IllegalStateException("Utility class");
    }

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------
    //     CHANNELFINDER API                                            ChannelManager
    //     --------------------                                         --------------------
    //     Retrieve a Channel                    .../channels/<name>    (GET)       read(String)
    //     List Channels / Query by Pattern      .../channels?prop1=patt1&prop2=patt2&~tag=patt3&~name=patt4...
    //                                                                  (GET)       query(MultiValueMap<String, String>)
    //     Create / Replace Channel              .../channels/<name>    (PUT)       create(String, Channel)
    //     Create / Replace Multiple Channels    .../channels           (PUT)       create(Iterable<Channel>)
    //     Update Channel                        .../channels/<name>    (POST)      update(String, Channel)
    //     Update Channels                       .../channels           (POST)      update(Iterable<Channel>)
    //     Delete a Channel                      .../channels/<name>    (DELETE)    remove(String)
    //     ------------------------------------------------------------------------------------------------

    /**
     * Return string for channel.
     *
     * @param value channel
     * @return string for channel
     */
    static String object2Json(Channel value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for channel array.
     *
     * @param value channel array
     * @return string for channel array
     */
    static String object2Json(Channel[] value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertRetrieveChannel(String, int, Channel)
     */
    public static Channel assertRetrieveChannel(String path, int expectedResponseCode) {
        return assertRetrieveChannel(path, expectedResponseCode, CHANNEL_NULL);
    }
    /**
     * @see ITUtilChannels#assertRetrieveChannel(String, int, Channel)
     */
    public static Channel assertRetrieveChannel(String path, Channel expected) {
        return assertRetrieveChannel(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the full listing of a single channel with the given name.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response channel
     */
    public static Channel assertRetrieveChannel(String path, int expectedResponseCode, Channel expected) {
        Channel actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS + path);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel.class);
            }
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertListChannels(String, int, int, int, Channel...)
     */
    public static Channel[] assertListChannels(int expectedEqual) {
        return assertListChannels("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, CHANNELS_NULL);
    }
    /**
     * @see ITUtilChannels#assertListChannels(String, int, int, int, Channel...)
     */
    public static Channel[] assertListChannels(int expectedEqual, Channel... expected) {
        return assertListChannels("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * @see ITUtilChannels#assertListChannels(String, int, int, int, Channel...)
     */
    public static Channel[] assertListChannels(String queryString, Channel... expected) {
        return assertListChannels(queryString, HttpURLConnection.HTTP_OK, -1, -1, expected);
    }
    /**
     * @see ITUtilChannels#assertListChannels(String, int, int, int, Channel...)
     */
    public static Channel[] assertListChannels(String queryString, int expectedEqual) {
        return assertListChannels(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, CHANNELS_NULL);
    }
    /**
     * @see ITUtilChannels#assertListChannels(String, int, int, int, Channel...)
     */
    public static Channel[] assertListChannels(String queryString, int expectedResponseCode, int expectedEqual) {
        return assertListChannels(queryString, expectedResponseCode, expectedEqual, expectedEqual, CHANNELS_NULL);
    }
    /**
     * Utility method to return the list of channels which match all given expressions, i.e. the expressions are combined in a logical AND.
     *
     * @param queryString query string
     * @param expectedResponseCode response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response channels
     * @return number of channels
     */
    public static Channel[] assertListChannels(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Channel... expected) {
        Channel[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS + queryString);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel[].class);
            }
            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.length >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.length <= expectedLessThanOrEqual);
            }
            if (expected != null && expected.length > 0) {
                assertEqualsChannels(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertCountChannels(String, int, int, int)
     */
    public static Integer assertCountChannels(int expectedEqual) {
        return assertCountChannels("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual);
    }
    /**
     * @see ITUtilChannels#assertCountChannels(String, int, int, int)
     */
    public static Integer assertCountChannels(String queryString, int expectedEqual) {
        return assertCountChannels(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual);
    }
    /**
     * Utility method to return the count of channels which match all given expressions, i.e. the expressions are combined in a logical AND.
     *
     * @param queryString query string
     * @param expectedResponseCode response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response channels
     * @return number of channels
     */
    public static Integer assertCountChannels(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual) {
        Integer actual = -1;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS + "/count" + queryString);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = Integer.parseInt(response[1]);
            }
            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual <= expectedLessThanOrEqual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertCreateReplaceChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertCreateReplaceChannel(String path, Channel value) {
        return assertCreateReplaceChannel(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, CHANNEL_NULL);
    }
    /**
     * @see ITUtilChannels#assertCreateReplaceChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertCreateReplaceChannel(AuthorizationChoice authorizationChoice, String path, Channel value, int expectedResponseCode) {
        return assertCreateReplaceChannel(authorizationChoice, path, object2Json(value), expectedResponseCode, CHANNEL_NULL);
    }
    /**
     * @see ITUtilChannels#assertCreateReplaceChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertCreateReplaceChannel(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceChannel(authorizationChoice, path, json, expectedResponseCode, CHANNEL_NULL);
    }
    /**
     * Utility method to create or completely replace the existing channel name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response channel
     */
    public static Channel assertCreateReplaceChannel(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Channel expected) {
        Channel actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.CHANNELS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel.class);
            }
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertCreateReplaceMultipleChannels(AuthorizationChoice, String, String, int, Channel[])
     */
    public static Channel[] assertCreateReplaceMultipleChannels(String path, Channel[] value) {
        return assertCreateReplaceMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, CHANNELS_NULL);
    }
    /**
     * @see ITUtilChannels#assertCreateReplaceMultipleChannels(AuthorizationChoice, String, String, int, Channel[])
     */
    public static Channel[] assertCreateReplaceMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceMultipleChannels(authorizationChoice, path, json, expectedResponseCode, CHANNELS_NULL);
    }
    /**
     * Utility method to add the channels in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response channels
     */
    public static Channel[] assertCreateReplaceMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Channel[] expected) {
        Channel[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.CHANNELS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel[].class);
            }

            if (expected != null) {
                assertEqualsChannels(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertUpdateChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertUpdateChannel(String path, Channel value) {
        return assertUpdateChannel(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, CHANNEL_NULL);
    }
    /**
     * @see ITUtilChannels#assertUpdateChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertUpdateChannel(AuthorizationChoice authorizationChoice, String path, Channel value, int expectedResponseCode) {
        return assertUpdateChannel(authorizationChoice, path, object2Json(value), expectedResponseCode, CHANNEL_NULL);
    }
    /**
     * @see ITUtilChannels#assertUpdateChannel(AuthorizationChoice, String, String, int, Channel)
     */
    public static Channel assertUpdateChannel(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertUpdateChannel(authorizationChoice, path, json, expectedResponseCode, CHANNEL_NULL);
    }
    /**
     * Utility method to merge properties and tags of the channel identified by the payload into an existing channel.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response channel
     */
    public static Channel assertUpdateChannel(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Channel expected) {
        Channel actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, authorizationChoice, EndpointChoice.CHANNELS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel.class);
            }
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertUpdateChannels(String, String, int, Channel[])
     */
    public static Channel[] assertUpdateChannels(String path, Channel[] value) {
        return assertUpdateChannels(path, object2Json(value), HttpURLConnection.HTTP_OK, CHANNELS_NULL);
    }
    /**
     * @see ITUtilChannels#assertUpdateChannels(String, String, int, Channel[])
     */
    public static Channel[] assertUpdateChannels(String path, String json, int expectedResponseCode) {
        return assertUpdateChannels(path, json, expectedResponseCode, CHANNELS_NULL);
    }
    /**
     * Utility method to merge properties and tags of the channels identified by the payload into existing channels.
     *
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response channels
     */
    public static Channel[] assertUpdateChannels(String path, String json, int expectedResponseCode, Channel[] expected) {
        Channel[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.CHANNELS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Channel[].class);
            }
            if (expected != null) {
                assertEqualsChannels(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilChannels#assertDeleteChannel(AuthorizationChoice, String, int)
     */
    public static void assertDeleteChannel(String path) {
        assertDeleteChannel(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to delete the existing channel name and all its properties and tags.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param expected expected response channel
     */
    public static void assertDeleteChannel(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, authorizationChoice, EndpointChoice.CHANNELS, path, null));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Channel objects
     * @param expected expected arbitray number of Channel objects
     */
    static void assertEqualsChannels(Channel[] actual, Channel... expected) {
        if (expected != null) {
            assertNotNull(actual);
            assertEquals(expected.length, actual.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } else {
            assertNull(actual);
        }
    }

}
