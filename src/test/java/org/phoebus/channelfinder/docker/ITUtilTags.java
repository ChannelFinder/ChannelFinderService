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

import java.io.IOException;
import java.net.HttpURLConnection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.docker.ITUtil.EndpointChoice;
import org.phoebus.channelfinder.docker.ITUtil.MethodChoice;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITUtilTags {

    static final ObjectMapper mapper    = new ObjectMapper();
    static final Tag[]     TAGS_NULL = null;
    static final Tag TAG_NULL  = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilTags() {
        throw new IllegalStateException("Utility class");
    }

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------
    //     CHANNELFINDER API                                                       TagManager
    //     --------------------                                                    --------------------
    //     Retrieve a Tag                    .../tags/<name>                       (GET)    read(String, boolean)
    //     List Tags                         .../tags                              (GET)    list()
    //     Create/Replace a Tag              .../tags/<name>                       (PUT)    create(String, Tag)
    //     Add Tag to a Single Channel       .../tags/<tag_name>/<channel_name>    (PUT)    addSingle(String, String, Tag)
    //     Create/Replace Tags               .../tags/<name>                       (PUT)    create(Iterable<Tag>)
    //     Add Tag to Multiple Channels      .../tags/<name>                       (POST)   update(String, Tag)
    //     Add Multiple Tags                 .../tags                              (POST)   update(Iterable<Tag>)
    //     Remove Tag from Single Channel    .../tags/<tag_name>/<channel_name>    (DELETE) removeSingle(String, String)
    //     Remove Tag                        .../tags/<name>                       (DELETE) remove(String)
    //     ------------------------------------------------------------------------------------------------

    /**
     * Return string for tag.
     *
     * @param value tag
     * @return string for tag
     */
    static String object2Json(Tag value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for tag array.
     *
     * @param value tag array
     * @return string for tag array
     */
    static String object2Json(Tag[] value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertRetrieveTag(String, int, Tag)
     */
    public static void assertRetrieveTag(String path, int responseCode) {
        assertRetrieveTag(path, responseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertRetrieveTag(String, int, Tag)
     */
    public static void assertRetrieveTag(String path, Tag expected) {
        assertRetrieveTag(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the tag with the given name, listing all tagged channels in an embedded <channels> structure.
     *
     * @param path path
     * @param responseCode expected response code
     * @param expected expected response tag
     */
    public static void assertRetrieveTag(String path, int responseCode, Tag expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Tag actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS + path);
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Tag.class);
                assertEquals(expected, actual);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return the list of all tags in the directory.
     *
     * @param expectedEqual (if non-negative number) equal to this number of items
     * @param expected expected response tags
     * @return number of tags
     */
    public static Integer assertListTags(int expectedEqual, Tag... expected) {
    	return assertListTags(expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all tags in the directory.
     *
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response tags
     * @return number of tags
     */
    public static Integer assertListTags(int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Tag... expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Tag[] actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            actual = mapper.readValue(response[1], Tag[].class);

            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.length >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.length <= expectedLessThanOrEqual);
            }

            // expected content
            if (expected != null) {
                assertEqualsXmlTags(actual, expected);
            }

            return actual != null ? actual.length : -1;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertCreateReplaceTag(String path, Tag value) {
        assertCreateReplaceTag(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, Tag value) {
        assertCreateReplaceTag(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, Tag value, int responseCode) {
        assertCreateReplaceTag(authorizationChoice, path, object2Json(value), responseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertCreateReplaceTag(authorizationChoice, path, json, responseCode, TAG_NULL);
    }
    /**
     * Utility method to create or completely replace the existing tag name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response tag
     */
    public static void assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Tag expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Tag actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Tag.class);
                assertEquals(expected, actual);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertAddTagSingleChannel(String, Tag, Tag)
     */
    public static void assertAddTagSingleChannel(String path, Tag value) {
        assertAddTagSingleChannel(path, value, TAG_NULL);
    }
    /**
     * Utility method to add tag with the given tag_name to the channel with the given channel_name.
     *
     * @param path path
     * @param value tag
     * @param expected expected response tag
     */
    public static void assertAddTagSingleChannel(String path, Tag value, Tag expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Tag actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, mapper.writeValueAsString(value)));
            ITUtil.assertResponseLength2CodeOK(response);

            if (expected != null) {
                actual = mapper.readValue(response[1], Tag.class);
                assertEquals(expected, actual);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertCreateReplaceTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static void assertCreateReplaceTags(String path, Tag[] value) {
        assertCreateReplaceTags(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static void assertCreateReplaceTags(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertCreateReplaceTags(authorizationChoice, path, json, responseCode, TAGS_NULL);
    }
    /**
     * Utility method to add the tags in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response tags
     */
    public static void assertCreateReplaceTags(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Tag[] expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                assertEqualsXmlTags(mapper.readValue(response[1], Tag[].class), expected);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertAddTagMultipleChannels(String path, Tag value) {
        assertAddTagMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, Tag value, int responseCode) {
        assertAddTagMultipleChannels(authorizationChoice, path, object2Json(value), responseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static void assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertAddTagMultipleChannels(authorizationChoice, path, json, responseCode, TAG_NULL);
    }
    /**
     * Utility method to add tag with the given name to all channels in the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response tag
     */
    public static void assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Tag expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Tag actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, authorizationChoice, EndpointChoice.TAGS, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Tag.class);
                assertEquals(expected, actual);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertAddMultipleTags(String, String, int, Tag[])
     */
    public static void assertAddMultipleTags(String path, Tag[] value) {
        assertAddMultipleTags(path, object2Json(value), HttpURLConnection.HTTP_OK, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertAddMultipleTags(String, String, int, Tag[])
     */
    public static void assertAddMultipleTags(String path, String json, int responseCode) {
        assertAddMultipleTags(path, json, responseCode, TAGS_NULL);
    }
    /**
     * Utility method to add the tags in the payload to the directory.
     *
     * @param path path
     * @param value tags
     * @param responseCode expected response code
     * @param expected expected response tags
     */
    public static void assertAddMultipleTags(String path, String json, int responseCode, Tag[] expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                assertEqualsXmlTags(mapper.readValue(response[1], Tag[].class), expected);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to remove tag with the given tag_name from the channel with the given channel_name.
     *
     * @param path path
     */
    public static void assertRemoveTagSingleChannel(String path) {
        try {
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.DELETE, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, null));
            ITUtil.assertResponseLength2CodeOK(response);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertRemoveTag(AuthorizationChoice, String, int)
     */
    public static void assertRemoveTag(String path) {
        assertRemoveTag(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to remove tag with the given name from all channels.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param responseCode expected response code
     */
    public static void assertRemoveTag(AuthorizationChoice authorizationChoice, String path, int responseCode) {
        try {
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.DELETE, authorizationChoice, EndpointChoice.TAGS, path, null));
            ITUtil.assertResponseLength2Code(response, responseCode);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Tag objects
     * @param expected expected arbitrary number of Tag objects
     */
    static void assertEqualsXmlTags(Tag[] actual, Tag... expected) {
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
