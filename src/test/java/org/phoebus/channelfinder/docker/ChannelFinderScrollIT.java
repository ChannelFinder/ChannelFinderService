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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for ChannelFinder and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have ChannelFinder and Elasticsearch up and running together with usage of
 * {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.ChannelScroll
 */
@Testcontainers
class ChannelFinderScrollIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for ChannelFinder, ensured at start-up
    //             environment
    //                 default ports, 8080 for ChannelFinder, 9200 for Elasticsearch
    //                 demo_auth enabled
    //         docker containers shared for tests
    //             each test to leave ChannelFinder, Elasticsearch in clean state - not disturb other tests
    //         each test uses multiple endpoints in ChannelFinder API
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------

	@Container
    public static final DockerComposeContainer<?> ENVIRONMENT =
        new DockerComposeContainer<>(new File("docker-compose-integrationtest.yml"))
            .waitingFor(ITUtil.CHANNELFINDER, Wait.forLogMessage(".*Started Application.*", 1));

    @Test
    void channelfinderUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_CHANNELFINDER;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
     */
    @Test
    void handleScrollQueryChannels() {
        // what
        //     check scroll

        try {
            ITUtilScroll.assertQueryChannels("/search?foo123",           HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?domain=cryo123",   HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?~tag=archived123", HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?~name=*001123",    HttpURLConnection.HTTP_OK, 0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
     */
    @Test
    void handleScrollContinueChannelsQuery() {
        // what
        //     check scroll

        try {
            ITUtilScroll.assertContinueChannelsQuery("/search/foo", HttpURLConnection.HTTP_NOT_FOUND, -1);
            ITUtilScroll.assertContinueChannelsQuery("/search/123", HttpURLConnection.HTTP_NOT_FOUND, -1);
            ITUtilScroll.assertContinueChannelsQuery("/123",        HttpURLConnection.HTTP_OK,        -1);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
     */
    @Test
    void handleScrollDataByTestFixture() {
        // what
        //     check scroll

        // --------------------------------------------------------------------------------
        // set up test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.setup();

        try {
            ITUtilScroll.assertQueryChannels("/search?foo",           HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?domain=cryo",   HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?~tag=archived", HttpURLConnection.HTTP_OK, 0);
            ITUtilScroll.assertQueryChannels("/search?~name=*1",      HttpURLConnection.HTTP_OK, 0);
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.tearDown();
    }

}
