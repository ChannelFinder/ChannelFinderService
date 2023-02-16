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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.phoebus.channelfinder.XmlScroll;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITUtilScroll {

    static final ObjectMapper mapper      = new ObjectMapper();
    static final XmlScroll    SCROLL_NULL = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilScroll() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the first 100(current default size) channels.
     *
     * @param queryString query string
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertQueryChannels(String queryString, int expectedLength) {
        return assertQueryChannels(queryString, HttpURLConnection.HTTP_OK, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the first 100(current default size) channels.
     *
     * @param queryString query string
     * @param responseCode expected response code
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertQueryChannels(String queryString, int responseCode, int expectedLength) {
        return assertQueryChannels(queryString, responseCode, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the first 100(current default size) channels.
     *
     * @param queryString query string
     * @param responseCode expected response code
     * @param expectedId (if non-null) expected id
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertQueryChannels(String queryString, int responseCode, String expectedId, int expectedLength) {
        try {
            String[] response = null;
            XmlScroll actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL + queryString);
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
            	actual = mapper.readValue(response[1], XmlScroll.class);
            }

            // (if non-null) expected id
            if (expectedId != null) {
            	assertEquals(expectedId, actual.getId());
            }
            // (if non-negative number) expected length of channels
            if (expectedLength >= 0) {
                assertEquals(expectedLength, actual.getChannels().size());
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
     * Utility method to return scroll object, including scroll id for the next query and a list of the next 100(current default size) channels.
     *
     * @param path path
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertContinueChannelsQuery(String path, int expectedLength) {
        return assertContinueChannelsQuery(path, HttpURLConnection.HTTP_OK, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the next 100(current default size) channels.
     *
     * @param path path
     * @param responseCode expected response code
     * @param expectedId (if non-null) expected id
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertContinueChannelsQuery(String path, int responseCode, int expectedLength) {
        return assertContinueChannelsQuery(path, responseCode, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the next 100(current default size) channels.
     *
     * @param path path
     * @param responseCode expected response code
     * @param expectedId (if non-null) expected id
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static XmlScroll assertContinueChannelsQuery(String path, int responseCode, String expectedId, int expectedLength) {
        try {
            String[] response = null;
            XmlScroll actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL + path);
            ITUtil.assertResponseLength2Code(response, responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
            	actual = mapper.readValue(response[1], XmlScroll.class);
            }

            // (if non-null) expected id
            if (expectedId != null) {
            	assertEquals(expectedId, actual.getId());
            }
            // (if non-negative number) expected length of channels
            if (expectedLength >= 0) {
                assertEquals(expectedLength, actual.getChannels().size());
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

}
