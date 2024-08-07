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

import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.docker.ITUtil.EndpointChoice;
import org.phoebus.channelfinder.docker.ITUtil.MethodChoice;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support test of behavior for property endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.docker.ITUtil
 */
public class ITUtilProperties {

    private static final Property[] PROPERTIES_NULL = null;
    private static final Property   PROPERTY_NULL   = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilProperties() {
        throw new IllegalStateException("Utility class");
    }

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------
    //     CHANNELFINDER API                                                                       PropertyManager
    //     --------------------                                                                    --------------------
    //     Retrieve a Property                    .../properties/<name>                            (GET)       read(String, boolean)
    //     List Properties                        .../properties                                   (GET)       list()
    //     Create/Replace a Property              .../properties/<name>                            (PUT)       create(String, Property)
    //     Add Property to a Single Channel       .../properties/<property_name>/<channel_name>    (PUT)       addSingle(String, String, Property)
    //     Create/Replace Properties              .../properties                                   (PUT)       create(Iterable<Property>)
    //     Add Property to Multiple Channels      .../properties/<name>                            (POST)      update(String, Property)
    //     Add Multiple Properties                .../properties                                   (POST)      update(Iterable<Property>)
    //     Remove Property from Single Channel    .../properties/<property_name>/<channel_name>    (DELETE)    removeSingle(String, String)
    //     Remove Property                        .../properties/<name>                            (DELETE)    remove(String)
    //     ------------------------------------------------------------------------------------------------

    /**
     * Return string for property.
     *
     * @param value property
     * @return string for property
     */
    static String object2Json(Property value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for property array.
     *
     * @param value property array
     * @return string for property array
     */
    static String object2Json(Property[] value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static Property assertRetrieveProperty(String path, int expectedResponseCode) {
        return assertRetrieveProperty(path, expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static Property assertRetrieveProperty(String path, Property expected) {
        return assertRetrieveProperty(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the property with the given name, listing all channels with that property in an embedded <channels> structure.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertRetrieveProperty(String path, int expectedResponseCode, Property expected) {
        Property actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES + path);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertListProperties(int, int, int, Property...)
     */
    public static Property[] assertListProperties(int expectedEqual, Property... expected) {
        return assertListProperties(HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all properties in the directory.
     *
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static Property[] assertListProperties(int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Property... expected) {
        Property[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property[].class);
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
                assertEqualsXmlProperties(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateReplaceProperty(String path, Property value) {
        return assertCreateReplaceProperty(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, Property value) {
        return assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, Property value, int expectedResponseCode) {
        return assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceProperty(authorizationChoice, path, json, expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * Utility method to create or completely replace the existing property name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Property expected) {
        Property actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertAddPropertySingleChannel(String, Property, Property)
     */
    public static Property assertAddPropertySingleChannel(String path, Property value) {
        return assertAddPropertySingleChannel(path, value, HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * Utility method to add property with the given property_name to the channel with the given channel_name.
     *
     * @param path path
     * @param value property
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertAddPropertySingleChannel(String path, Property value, int expectedResponseCode, Property expected) {
        Property actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, ITUtil.MAPPER.writeValueAsString(value)));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static Property[] assertCreateReplaceProperties(String path, Property[] value) {
        return assertCreateReplaceProperties(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static Property[] assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateReplaceProperties(authorizationChoice, path, json, expectedResponseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add the properties in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response properties
     */
    public static Property[] assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Property[] expected) {
        Property[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property[].class);
            }
            if (expected != null) {
                assertEqualsXmlProperties(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertAddPropertyMultipleChannels(String path, Property value) {
        return assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, Property value, int expectedResponseCode) {
        return assertAddPropertyMultipleChannels(authorizationChoice, path, object2Json(value), expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertAddPropertyMultipleChannels(authorizationChoice, path, json, expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * Utility method to add property with the given name to all channels in the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Property expected) {
        Property actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, authorizationChoice, EndpointChoice.PROPERTIES, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, Property[])
     */
    public static Property[] assertAddMultipleProperties(String path, Property[] value) {
        return assertAddMultipleProperties(path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, Property[])
     */
    public static Property[] assertAddMultipleProperties(String path, String json, int expectedResponseCode) {
        return assertAddMultipleProperties(path, json, expectedResponseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add properties in the payload to all channels in the payload data.
     *
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response properties
     */
    public static Property[] assertAddMultipleProperties(String path, String json, int expectedResponseCode, Property[] expected) {
        Property[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Property[].class);
            }
            if (expected != null) {
                assertEqualsXmlProperties(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to remove property with the given property_name from the channel with the given channel_name.
     *
     * @param path path
     */
    public static void assertRemovePropertySingleChannel(String path) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, null));

            ITUtil.assertResponseLength2CodeOK(response);
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRemoveProperty(AuthorizationChoice, String, int)
     */
    public static void assertRemoveProperty(String path) {
        assertRemoveProperty(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to remove property with the given name from all channels.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param expectedResponseCode expected response code
     */
    public static void assertRemoveProperty(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, authorizationChoice, EndpointChoice.PROPERTIES, path, null));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Assert that arrays are equal with same length and same content in each array position.
     *
     * @param actual actual array of Property objects
     * @param expected expected arbitrary number of Property objects
     */
    static void assertEqualsXmlProperties(Property[] actual, Property... expected) {
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
