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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.dockerjava.api.DockerClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for ChannelFinder and Elasticsearch with focus on endpoints being available.
 * Existing dockerization is used with <tt>docker-compose-integrationtest.yml</tt> and <tt>Dockerfile.integrationtest</tt>.
 *
 * @author Lars Johansson
 *
 * @see ITUtil
 */
@Testcontainers
class ChannelFinderIT {

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
    //     ------------------------------------------------------------------------------------------------
    //     ChannelFinder - Enhanced Directory Service
    //         https://channelfinder.readthedocs.io/en/latest/api.html
    //     ------------------------------------------------------------------------------------------------

	@Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        //     stop jvm to make data available

        if (!Boolean.FALSE.toString().equals(System.getProperty(ITUtil.JACOCO_SKIPITCOVERAGE))) {
            return;
        }

        Optional<ContainerState> container = ENVIRONMENT.getContainerByServiceName(ITUtil.CHANNELFINDER);
        if (container.isPresent()) {
            ContainerState cs = container.get();
            DockerClient dc = cs.getDockerClient();
            dc.stopContainerCmd(cs.getContainerId()).exec();
            try {
                cs.copyFileFromContainer(ITUtil.JACOCO_EXEC_PATH, ITUtil.JACOCO_TARGET_PREFIX + ChannelFinderIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
            } catch (Exception e) {
                // proceed if file cannot be copied
            }
        }
    }

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
    @Test
    void channelfinderUpTags() {
        try {
            String address = ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/tags";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    void channelfinderUpProperties() {
        try {
            String address = ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/properties";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    void channelfinderUpChannels() {
        try {
            String address = ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/channels";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }
    @Test
    void channelfinderUpScroll() {
        try {
            String address = ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/scroll";
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

}
