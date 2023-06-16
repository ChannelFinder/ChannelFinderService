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

import org.phoebus.channelfinder.XmlProperty;
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
    static final XmlProperty[] PROPERTIES_NULL = null;
    static final XmlProperty   PROPERTY_NULL   = null;

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
    //     Create/Replace a Property              .../properties/<name>                            (PUT)       create(String, XmlProperty)
    //     Add Property to a Single Channel       .../properties/<property_name>/<channel_name>    (PUT)       addSingle(String, String, XmlProperty)
    //     Create/Replace Properties              .../properties                                   (PUT)       create(Iterable<XmlProperty>)
    //     Add Property to Multiple Channels      .../properties/<name>                            (POST)      update(String, XmlProperty)
    //     Add Multiple Properties                .../properties                                   (POST)      update(Iterable<XmlProperty>)
    //     Remove Property from Single Channel    .../properties/<property_name>/<channel_name>    (DELETE)    removeSingle(String, String)
    //     Remove Property                        .../properties/<name>                            (DELETE)    remove(String)
    //     ------------------------------------------------------------------------------------------------

    /**
     * Return string for property.
     *
     * @param value property
     * @return string for property
     */
    static String object2Json(XmlProperty value) {
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
    static String object2Json(XmlProperty[] value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, XmlProperty)
     */
    public static XmlProperty assertRetrieveProperty(String path, int responseCode) {
        return assertRetrieveProperty(path, responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, XmlProperty)
     */
    public static XmlProperty assertRetrieveProperty(String path, XmlProperty expected) {
        return assertRetrieveProperty(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the property with the given name, listing all channels with that property in an embedded <channels> structure.
     *
     * @param path path
     * @param responseCode expected response code
     * @param expected expected response property
     */
    public static XmlProperty assertRetrieveProperty(String path, int responseCode, XmlProperty expected) {
        try {
            String[] response = null;
            XmlProperty actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES + path);
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return the list of all properties in the directory.
     *
     * @param expectedEqual (if non-negative number) equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static XmlProperty[] assertListProperties(int expectedEqual, XmlProperty... expected) {
        return assertListProperties(HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all properties in the directory.
     *
     * @param responseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static XmlProperty[] assertListProperties(int responseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, XmlProperty... expected) {
        try {
            String[] response = null;
            XmlProperty[] actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty[].class);
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

            // expected content
            if (expected != null) {
                assertEqualsXmlProperties(actual, expected);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertCreateReplaceProperty(String path, XmlProperty value) {
        return assertCreateReplaceProperty(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, XmlProperty value) {
        return assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, XmlProperty value, int responseCode) {
        return assertCreateReplaceProperty(authorizationChoice, path, object2Json(value), responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperty(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        return assertCreateReplaceProperty(authorizationChoice, path, json, responseCode, PROPERTY_NULL);
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
    public static XmlProperty assertCreateReplaceProperty(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, XmlProperty expected) {
        try {
            String[] response = null;
            XmlProperty actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertAddPropertySingleChannel(String, XmlProperty, XmlProperty)
     */
    public static XmlProperty assertAddPropertySingleChannel(String path, XmlProperty value) {
        return assertAddPropertySingleChannel(path, value, HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * Utility method to add property with the given property_name to the channel with the given channel_name.
     *
     * @param path path
     * @param value property
     * @param responseCode expected response code
     * @param expected expected response property
     */
    public static XmlProperty assertAddPropertySingleChannel(String path, XmlProperty value, int responseCode, XmlProperty expected) {
        try {
            String[] response = null;
            XmlProperty actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, mapper.writeValueAsString(value)));
            ITUtil.assertResponseLength2CodeOK(response);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, XmlProperty[])
     */
    public static XmlProperty[] assertCreateReplaceProperties(String path, XmlProperty[] value) {
        return assertCreateReplaceProperties(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateReplaceProperties(AuthorizationChoice, String, String, int, XmlProperty[])
     */
    public static XmlProperty[] assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        return assertCreateReplaceProperties(authorizationChoice, path, json, responseCode, PROPERTIES_NULL);
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
    public static XmlProperty[] assertCreateReplaceProperties(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, XmlProperty[] expected) {
        try {
            String[] response = null;
            XmlProperty[] actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty[].class);
            }

            if (expected != null) {
                assertEqualsXmlProperties(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertAddPropertyMultipleChannels(String path, XmlProperty value) {
        return assertAddPropertyMultipleChannels(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, XmlProperty value, int responseCode) {
        return assertAddPropertyMultipleChannels(authorizationChoice, path, object2Json(value), responseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddPropertyMultipleChannels(AuthorizationChoice, String, String, int, XmlProperty)
     */
    public static XmlProperty assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode) {
        return assertAddPropertyMultipleChannels(authorizationChoice, path, json, responseCode, PROPERTY_NULL);
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
    public static XmlProperty assertAddPropertyMultipleChannels(AuthorizationChoice authorizationChoice, String path, String json, int responseCode, XmlProperty expected) {
        try {
            String[] response = null;
            XmlProperty actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, XmlProperty[])
     */
    public static XmlProperty[] assertAddMultipleProperties(String path, XmlProperty[] value) {
        return assertAddMultipleProperties(path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertAddMultipleProperties(String, String, int, XmlProperty[])
     */
    public static XmlProperty[] assertAddMultipleProperties(String path, String json, int responseCode) {
        return assertAddMultipleProperties(path, json, responseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add properties in the payload to all channels in the payload data.
     *
     * @param path path
     * @param json json
     * @param responseCode expected response code
     * @param expected expected response properties
     */
    public static XmlProperty[] assertAddMultipleProperties(String path, String json, int responseCode, XmlProperty[] expected) {
        try {
            String[] response = null;
            XmlProperty[] actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, AuthorizationChoice.ADMIN, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], XmlProperty[].class);
            }

            if (expected != null) {
                assertEqualsXmlProperties(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
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
     * @param actual actual array of XmlProperty objects
     * @param expected expected arbitrary number of XmlProperty objects
     */
    static void assertEqualsXmlProperties(XmlProperty[] actual, XmlProperty... expected) {
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
