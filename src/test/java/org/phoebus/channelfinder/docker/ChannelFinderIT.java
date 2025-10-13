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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for ChannelFinder and Elasticsearch with focus on endpoints being available.
 *
 * @author Lars Johansson
 * @see ITUtil
 */
@Testcontainers
class ChannelFinderIT {

  // Note
  //
  // ------------------------------------------------------------------------------------------------
  //     About
  //         requires
  //             elastic indices for ChannelFinder, ensured at start-up
  //             environment
  //                 default ports, can be exposed differently externally to avoid interference with
  // any running instance
  //                 demo_auth enabled
  //         docker containers shared for tests
  //             each test to leave ChannelFinder, Elasticsearch in clean state - not disturb other
  // tests
  //         each test uses multiple endpoints in ChannelFinder API
  //
  // ------------------------------------------------------------------------------------------------
  //     ChannelFinder - Enhanced Directory Service
  //         https://channelfinder.readthedocs.io/en/latest/api.html
  //
  // ------------------------------------------------------------------------------------------------

  @Container public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

  @AfterAll
  public static void extractJacocoReport() {
    // extract jacoco report from container file system
    ITUtil.extractJacocoReport(
        ENVIRONMENT,
        ITUtil.JACOCO_TARGET_PREFIX
            + ChannelFinderIT.class.getSimpleName()
            + ITUtil.JACOCO_TARGET_SUFFIX);
  }

  @Test
  void channelfinderUp() {
    try {
      int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER);

      assertEquals(HttpURLConnection.HTTP_OK, responseCode);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void channelfinderUpTags() {
    try {
      int responseCode =
          ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/tags");

      assertEquals(HttpURLConnection.HTTP_OK, responseCode);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void channelfinderUpProperties() {
    try {
      int responseCode =
          ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/properties");

      assertEquals(HttpURLConnection.HTTP_OK, responseCode);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void channelfinderUpChannels() {
    try {
      int responseCode =
          ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/channels");

      assertEquals(HttpURLConnection.HTTP_OK, responseCode);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void channelfinderUpScroll() {
    try {
      int responseCode =
          ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER + "/resources/scroll");

      assertEquals(HttpURLConnection.HTTP_OK, responseCode);
    } catch (Exception e) {
      fail();
    }
  }
}
