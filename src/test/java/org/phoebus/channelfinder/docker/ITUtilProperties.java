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

import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.docker.ITUtil.EndpointChoice;
import org.phoebus.channelfinder.docker.ITUtil.MethodChoice;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITUtilProperties {

    static final ObjectMapper  mapper          = new ObjectMapper();
    static final Property[] PROPERTIES_NULL = null;
    static final Property PROPERTY_NULL   = null;

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
            return mapper.writeValueAsString(value);
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
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static void assertRetrieveProperty(String path, int responseCode) {
        assertRetrieveProperty(path, responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static void assertRetrieveProperty(String path, Property expected) {
        assertRetrieveProperty(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the property with the given name, listing all channels with that property in an embedded <channels> structure.
     *
     * @param path path
     * @param responseCode expected response code
     * @param expected expected response property
     */
    public static void assertRetrieveProperty(String path, int responseCode, Property expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Property actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES + path);
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Property.class);
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
     * Utility method to return the list of all properties in the directory.
     *
     * @param expectedEqual (if non-negative number) equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static Integer assertListProperties(int expectedEqual, Property... expected) {
    	return assertListProperties(expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all properties in the directory.
     *
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static Integer assertListProperties(int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Property... expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Property[] actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            actual = mapper.readValue(response[1], Property[].class);

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
                assertEqualsXmlProperties(actual, expected);
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
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertCreateReplaceProperty(String path, Property value) {
        assertCreateReplaceProperty(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, Property value) {
        assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, Property value, int responseCode) {
        assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertCreateReplaceProperty(authorizationChoice, path, json, responseCode, PROPERTY_NULL);
    }
    /**
     * Utility method to create or completely replace the existing property name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response property
     */
    public static void assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Property expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Property actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertAddPropertySingleChannel(String, Property, Property)
     */
    public static void assertAddPropertySingleChannel(String path, Property value) {
        assertAddPropertySingleChannel(path, value, PROPERTY_NULL);
    }
    /**
     * Utility method to add property with the given property_name to the channel with the given channel_name.
     *
     * @param path path
     * @param value property
     * @param expected expected response property
     */
    public static void assertAddPropertySingleChannel(String path, Property value, Property expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Property actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, mapper.writeValueAsString(value)));
            ITUtil.assertResponseLength2CodeOK(response);

            if (expected != null) {
                actual = mapper.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static void assertCreateReplaceProperties(String path, Property[] value) {
        assertCreateReplaceProperties(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static void assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertCreateReplaceProperties(authorizationChoice, path, json, responseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add the properties in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response properties
     */
    public static void assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Property[] expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                assertEqualsXmlProperties(mapper.readValue(response[1], Property[].class), expected);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertAddPropertyMultipleChannels(String path, Property value) {
        assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, Property value, int responseCode) {
        assertAddPropertyMultipleChannels(authorizationChoice, path, object2Json(value), responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, Property)
     */
    public static void assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        assertAddPropertyMultipleChannels(authorizationChoice, path, json, responseCode, PROPERTY_NULL);
    }
    /**
     * Utility method to add property with the given name to all channels in the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response property
     */
    public static void assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, Property expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;
            Property actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                actual = mapper.readValue(response[1], Property.class);
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
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, Property[])
     */
    public static void assertAddMultipleProperties(String path, Property[] value) {
        assertAddMultipleProperties(path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, Property[])
     */
    public static void assertAddMultipleProperties(String path, String json, int responseCode) {
        assertAddMultipleProperties(path, json, responseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add properties in the payload to all channels in the payload data.
     *
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response properties
     */
    public static void assertAddMultipleProperties(String path, String json, int responseCode, Property[] expected) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);

            if (expected != null) {
                assertEqualsXmlProperties(mapper.readValue(response[1], Property[].class), expected);
            }
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to remove property with the given property_name from the channel with the given channel_name.
     *
     * @param path path
     */
    public static void assertRemovePropertySingleChannel(String path) {
        try {
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.DELETE, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, null));
            ITUtil.assertResponseLength2CodeOK(response);
        } catch (IOException e) {
            fail();
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
     * @param responseCode expected response code
     */
    public static void assertRemoveProperty(AuthorizationChoice authorizationChoice, String path, int responseCode) {
        try {
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.DELETE, authorizationChoice, EndpointChoice.PROPERTIES, path, null));
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
