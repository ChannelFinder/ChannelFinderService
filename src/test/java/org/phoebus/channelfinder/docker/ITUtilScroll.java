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

import java.net.HttpURLConnection;

import org.phoebus.channelfinder.entity.Scroll;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support test of behavior for scroll endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.docker.ITUtil
 */
public class ITUtilScroll {

    /**
     * This class is not to be instantiated.
     */
    private ITUtilScroll() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilScroll#assertQueryChannels(String, int, String, int)
     */
    public static Scroll assertQueryChannels(String queryString, int expectedLength) {
        return assertQueryChannels(queryString, HttpURLConnection.HTTP_OK, null, expectedLength);
    }
    /**
     * @see ITUtilScroll#assertQueryChannels(String, int, String, int)
     */
    public static Scroll assertQueryChannels(String queryString, int expectedResponseCode, int expectedLength) {
        return assertQueryChannels(queryString, expectedResponseCode, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the first 100(current default size) channels.
     *
     * @param queryString query string
     * @param expectedResponseCode expected response code
     * @param expectedId (if non-null) expected id
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static Scroll assertQueryChannels(String queryString, int expectedResponseCode, String expectedId, int expectedLength) {
        Scroll actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL + queryString);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (expectedResponseCode == HttpURLConnection.HTTP_OK) {
                actual = ITUtil.MAPPER.readValue(response[1], Scroll.class);
            }
            if (expectedId != null) {
                assertEquals(expectedId, actual.getId());
            }
            // (if non-negative number) expected length of channels
            if (expectedLength >= 0) {
                assertEquals(expectedLength, actual.getChannels().size());
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilScroll#assertContinueChannelsQuery(String, int, String, int)
     */
    public static Scroll assertContinueChannelsQuery(String path, int expectedLength) {
        return assertContinueChannelsQuery(path, HttpURLConnection.HTTP_OK, null, expectedLength);
    }
    /**
     * @see ITUtilScroll#assertContinueChannelsQuery(String, int, String, int)
     */
    public static Scroll assertContinueChannelsQuery(String path, int expectedResponseCode, int expectedLength) {
        return assertContinueChannelsQuery(path, expectedResponseCode, null, expectedLength);
    }
    /**
     * Utility method to return scroll object, including scroll id for the next query and a list of the next 100(current default size) channels.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expectedId (if non-null) expected id
     * @param expectedLength (if non-negative number) expected length of channels
     * @return scroll object
     */
    public static Scroll assertContinueChannelsQuery(String path, int expectedResponseCode, String expectedId, int expectedLength) {
        Scroll actual = null;
        try {
            String[] response = null;

            response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL + path);
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (expectedResponseCode == HttpURLConnection.HTTP_OK) {
                actual = ITUtil.MAPPER.readValue(response[1], Scroll.class);
            }
            if (expectedId != null) {
                assertEquals(expectedId, actual.getId());
            }
            // (if non-negative number) expected length of channels
            if (expectedLength >= 0) {
                assertEquals(expectedLength, actual.getChannels().size());
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

}
