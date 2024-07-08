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

import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.docker.ITUtil.EndpointChoice;
import org.phoebus.channelfinder.docker.ITUtil.MethodChoice;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support test of behavior for tag endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.docker.ITUtil
 */
public class ITUtilTags {

    static final ObjectMapper mapper    = new ObjectMapper();
    static final Tag[]     TAGS_NULL = null;
    static final Tag       TAG_NULL  = null;

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
    public static Tag assertRetrieveTag(String path, int expectedResponseCode) {
        return assertRetrieveTag(path, expectedResponseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertRetrieveTag(String, int, Tag)
     */
    public static Tag assertRetrieveTag(String path, Tag expected) {
        return assertRetrieveTag(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the tag with the given name, listing all tagged channels in an embedded <channels> structure.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertRetrieveTag(String path, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS + path);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertListTags(int, int, int, Tag...)
     */
    public static Tag[] assertListTags(int expectedEqual, Tag... expected) {
        return assertListTags(HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all tags in the directory.
     *
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response tags
     * @return number of tags
     */
    public static Tag[] assertListTags(int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Tag... expected) {
        Tag[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag[].class);
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
                assertEqualsTags(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateReplaceTag(String path, Tag value) {
        return assertCreateReplaceTag(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, Tag value) {
        return assertCreateReplaceTag(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, Tag value, int expectedResponseCode) {
        return assertCreateReplaceTag(authorizationChoice, path, object2Json(value), expectedResponseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceTag(authorizationChoice, path, json, expectedResponseCode, TAG_NULL);
    }
    /**
     * Utility method to create or completely replace the existing tag name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertCreateReplaceTag(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertAddTagSingleChannel(String, Tag, Tag)
     */
    public static Tag assertAddTagSingleChannel(String path, Tag value) {
        return assertAddTagSingleChannel(path, value, HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * Utility method to add tag with the given tag_name to the channel with the given channel_name.
     *
     * @param path path
     * @param value tag
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertAddTagSingleChannel(String path, Tag value, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, mapper.writeValueAsString(value)));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertCreateReplaceTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static Tag[] assertCreateReplaceTags(String path, Tag[] value) {
        return assertCreateReplaceTags(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateReplaceTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static Tag[] assertCreateReplaceTags(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceTags(authorizationChoice, path, json, expectedResponseCode, TAGS_NULL);
    }
    /**
     * Utility method to add the tags in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response tags
     */
    public static Tag[] assertCreateReplaceTags(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Tag[] expected) {
        Tag[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag[].class);
            }
            if (expected != null) {
                assertEqualsTags(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertAddTagMultipleChannels(String path, Tag value) {
        return assertAddTagMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, Tag value, int expectedResponseCode) {
        return assertAddTagMultipleChannels(authorizationChoice, path, object2Json(value), expectedResponseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertAddTagMultipleChannels(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertAddTagMultipleChannels(authorizationChoice, path, json, expectedResponseCode, TAG_NULL);
    }
    /**
     * Utility method to add tag with the given name to all channels in the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertAddTagMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, authorizationChoice, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertAddMultipleTags(String, String, int, Tag[])
     */
    public static Tag[] assertAddMultipleTags(String path, Tag[] value) {
        return assertAddMultipleTags(path, object2Json(value), HttpURLConnection.HTTP_OK, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertAddMultipleTags(String, String, int, Tag[])
     */
    public static Tag[] assertAddMultipleTags(String path, String json, int expectedResponseCode) {
        return assertAddMultipleTags(path, json, expectedResponseCode, TAGS_NULL);
    }
    /**
     * Utility method to add the tags in the payload to the directory.
     *
     * @param path path
     * @param json tags
     * @param expectedResponseCode expected response code
     * @param expected expected response tags
     */
    public static Tag[] assertAddMultipleTags(String path, String json, int expectedResponseCode, Tag[] expected) {
        Tag[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Tag[].class);
            }
            if (expected != null) {
                assertEqualsTags(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to remove tag with the given tag_name from the channel with the given channel_name.
     *
     * @param path path
     */
    public static void assertRemoveTagSingleChannel(String path) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, AuthorizationChoice.ADMIN, EndpointChoice.TAGS, path, null));

            ITUtil.assertResponseLength2CodeOK(response);
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
     * @param expectedResponseCode expected response code
     */
    public static void assertRemoveTag(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, authorizationChoice, EndpointChoice.TAGS, path, null));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
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
    static void assertEqualsTags(Tag[] actual, Tag... expected) {
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
