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
import org.phoebus.channelfinder.entity.Scroll;
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
 * Integration tests for ChannelFinder and Elasticsearch with focus on usage of 
 * {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
 * Existing dockerization is used with <tt>docker-compose-integrationtest.yml</tt> and <tt>Dockerfile.integrationtest</tt>.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.channelfinder.ChannelScroll
 * @see org.phoebus.channelfinder.docker.ITUtil
 * @see org.phoebus.channelfinder.docker.ITUtilScroll
 */
@Testcontainers
class ChannelFinderScrollIT {

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
                cs.copyFileFromContainer(ITUtil.JACOCO_EXEC_PATH, ITUtil.JACOCO_TARGET_PREFIX + ChannelFinderScrollIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
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

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
     */
    @Test
    void handleScrollQueryChannels() {
        // what
        //     check scroll

        try {
            ITUtilScroll.assertQueryChannels("?foo123",           0);
            ITUtilScroll.assertQueryChannels("?domain=cryo123",   0);
            ITUtilScroll.assertQueryChannels("?~tag=archived123", 0);
            ITUtilScroll.assertQueryChannels("?~name=*001123",    0);
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
            ITUtilScroll.assertContinueChannelsQuery("/foo",      0);
            ITUtilScroll.assertContinueChannelsQuery("/123",      0);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#SCROLL_RESOURCE_URI}.
     *
     * @see ChannelFinderChannelsIT#handleChannels3QueryByPattern()
     * @see ITTestFixture
     */
    @Test
    void handleScrollDataByTestFixture() {
        // what
        //     check scroll

        // --------------------------------------------------------------------------------
        // set up test fixture
        // --------------------------------------------------------------------------------

        // --------------------------------------------------------------------------------
        // note
        //     option
        //         ~name
        //         ~tag
        //         ~size
        //         ~from
        //         other
        //     see ChannelFinderChannelsIT#handleChannels3QueryByPattern()
        //         channels (all)
        //         channel (name)
        //         property name (domain)
        //         property name (element)
        //         property name (type)
        //         property name (cell)
        //         tag (name)
        //         combinations
        // --------------------------------------------------------------------------------

        ITTestFixture.setup();

        try {
            Scroll actual = null;

            // channels (all)

            actual = ITUtilScroll.assertQueryChannels("",                                                               ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId(),                                     0);

            // channel (name)

            actual = ITUtilScroll.assertQueryChannels("?~name=asdf",                                                    0);

            actual = ITUtilScroll.assertQueryChannels("?~name=ABC:DEF-GHI:JKL:001",                                     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~name=ABC:DEF-GHI:JKL:001",      0);

            actual = ITUtilScroll.assertQueryChannels("?~name=*001",                                                    2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~name=*001",                     0);

            actual = ITUtilScroll.assertQueryChannels("?~name=*ABC:DEF-XYZ:JKL:01?",                                    2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~name=*ABC:DEF-XYZ:JKL:01?",     0);

            actual = ITUtilScroll.assertQueryChannels("?~name=ABC:DEF-???:JKL:003",                                     2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~name=ABC:DEF-???:JKL:003",      0);

            actual = ITUtilScroll.assertQueryChannels("?~size=0",                                                       0);

            actual = ITUtilScroll.assertQueryChannels("?~size=5",                                                       5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=5",                        5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=5",                        0);

            actual = ITUtilScroll.assertQueryChannels("?~size=100",                                                     ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=100",                      0);

            actual = ITUtilScroll.assertQueryChannels("?~size=3&~from=-1",                                              HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?~size=3&~from=0",                                               3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=0",                3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=0",                3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=0",                1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=0",                0);

            actual = ITUtilScroll.assertQueryChannels("?~size=3&~from=1",                                               3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=1",                HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?~size=3&~from=2",                                               3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=2",                HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?~size=3&~from=3",                                               3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~size=3&~from=3",                HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?~name=*1*&~size=4&~from=2",                                     4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~name=*1*&~size=4&~from=2",      HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            //         property name (domain)

            actual = ITUtilScroll.assertQueryChannels("?domain=asdf",                                                   0);

            actual = ITUtilScroll.assertQueryChannels("?domain=cryo",                                                   5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=cryo",                    0);

            actual = ITUtilScroll.assertQueryChannels("?domain=power",                                                  5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=power",                   0);

            actual = ITUtilScroll.assertQueryChannels("?domain=cry?",                                                   5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=cry?",                    0);

            actual = ITUtilScroll.assertQueryChannels("?domain=?????r?????",                                            0);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=?????r?????",             0);

            actual = ITUtilScroll.assertQueryChannels("?domain=?r??",                                                   5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=?r??",                    0);

            actual = ITUtilScroll.assertQueryChannels("?domain=?r???",                                                  0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*a*",                                                    0);

            actual = ITUtilScroll.assertQueryChannels("?domain=?ow*&~size=4&~from=2",                                   3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=?ow*&~size=4&~from=2",    HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?domain=cryo&domain=power",                                      ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=cryo&domain=power",       0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*o&domain=asdf?",                                        5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*o&domain=asdf?",         0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*o&domain=asdf?&~size=3",                                3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*o&domain=asdf?&~size=3", 2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*o&domain=asdf?&~size=3", 0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*o&domain=asdf?&~size=3&~from=1",                                    3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*o&domain=asdf?&~size=3&~from=1",     HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?domain!=cryo",                                                  5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=cryo",                   0);

            actual = ITUtilScroll.assertQueryChannels("?domain!=*r",                                                    5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=*r",                     0);

            actual = ITUtilScroll.assertQueryChannels("?domain!=cryo&~size=4",                                          4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=cryo&~size=4",           1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=cryo&~size=4",           0);

            actual = ITUtilScroll.assertQueryChannels("?domain!=cryo&~size=4&~from=0",                                  4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=cryo&~size=4&~from=0",   1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain!=cryo&~size=4&~from=0",   0);

            //         property name (element)

            actual = ITUtilScroll.assertQueryChannels("?element=asdf",                                                  0);

            actual = ITUtilScroll.assertQueryChannels("?element=source",                                                2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=source",                 0);

            actual = ITUtilScroll.assertQueryChannels("?element=initial",                                               2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=initial",                0);

            actual = ITUtilScroll.assertQueryChannels("?element=radio",                                                 2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=radio",                  0);

            actual = ITUtilScroll.assertQueryChannels("?element=magnet",                                                2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=magnet",                 0);

            actual = ITUtilScroll.assertQueryChannels("?element=supra",                                                 2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=supra",                  0);

            actual = ITUtilScroll.assertQueryChannels("?element=*i?",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=*i?",                    0);

            actual = ITUtilScroll.assertQueryChannels("?element=?i*",                                                   0);

            actual = ITUtilScroll.assertQueryChannels("?element=*a*&~size=2&~from=4",                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=*a*&~size=2&~from=4",    HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?element=initial&element=radio&element=supra",                                    6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=initial&element=radio&element=supra",     0);

            actual = ITUtilScroll.assertQueryChannels("?element=rad?o&element=asdf?",                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=rad?o&element=asdf?",    0);

            actual = ITUtilScroll.assertQueryChannels("?element=initial&element=radio&element=supra&~size=4",                                    4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=initial&element=radio&element=supra&~size=4",     2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=initial&element=radio&element=supra&~size=4",     0);

            actual = ITUtilScroll.assertQueryChannels("?element=initial&element=radio&element=supra&~size=4&~from=3",                                    3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element=initial&element=radio&element=supra&~size=4&~from=3",     HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?element!=initial",                                              8);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element!=initial",               0);

            actual = ITUtilScroll.assertQueryChannels("?element!=source&element!=initial",                                    ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element!=source&element!=initial",     0);

            actual = ITUtilScroll.assertQueryChannels("?element!=source&element!=initial&~size=6", 6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element!=source&element!=initial&~size=6",     4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element!=source&element!=initial&~size=6",     0);

            actual = ITUtilScroll.assertQueryChannels("?element!=source&element!=initial&~size=6&~from=5",                                    5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?element!=source&element!=initial&~size=6&~from=5",     HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            //         property name (type)

            actual = ITUtilScroll.assertQueryChannels("?type=asdf",                                                     0);

            actual = ITUtilScroll.assertQueryChannels("?type=read",                                                     4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=read",                      0);

            actual = ITUtilScroll.assertQueryChannels("?type=write",                                                    4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=write",                     0);

            actual = ITUtilScroll.assertQueryChannels("?type=readwrite",                                                2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=readwrite",                 0);

            actual = ITUtilScroll.assertQueryChannels("?type=read*",                                                    6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=read*",                     0);

            actual = ITUtilScroll.assertQueryChannels("?type=*write",                                                   6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=*write",                    0);

            actual = ITUtilScroll.assertQueryChannels("?type=*r*",                                                      ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=*r*",                       0);

            actual = ITUtilScroll.assertQueryChannels("?type=??a?&~size=2&~from=4",                                     0);

            actual = ITUtilScroll.assertQueryChannels("?type=write&type=readwrite",                                     6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=write&type=readwrite",      0);

            actual = ITUtilScroll.assertQueryChannels("?type=writ?&type=writ*",                                         4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=writ?&type=writ*",          0);

            actual = ITUtilScroll.assertQueryChannels("?type=write&type=readwrite&~size=10",                                    6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type=write&type=readwrite&~size=10",     0);

            actual = ITUtilScroll.assertQueryChannels("?type=write&type=readwrite&~size=10&~from=7",                    0);

            actual = ITUtilScroll.assertQueryChannels("?type!=asdf",                                                    ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type!=asdf",                     0);

            actual = ITUtilScroll.assertQueryChannels("?type!=asdf&~size=100",                                          ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type!=asdf&~size=100",           0);

            actual = ITUtilScroll.assertQueryChannels("?type!=asdf&~size=100&~from=0",                                  ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?type!=asdf&~size=100&~from=0",   0);

            //         property name (cell)

            actual = ITUtilScroll.assertQueryChannels("?cell=asdf",                                                     0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block1",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block2",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block2",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block3",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block3",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block4",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block4",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block5",                                                   2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block5",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block?",                                                   ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block?",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell=*2",                                                       2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=*2",                        0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block?&~size=5&~from=5",                                   5);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block?&~size=5&~from=5",    HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?cell=block1&cell=block2",                                       4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2",        0);

            actual = ITUtilScroll.assertQueryChannels("?cell=*1&cell=*2",                                               4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=*1&cell=*2",                0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block1&cell=block2&cell=block2&~size=1",                                    1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1",     0);

            actual = ITUtilScroll.assertQueryChannels("?cell=block1&cell=block2&cell=block2&~size=1&~from=0",                                    1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1&~from=0",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1&~from=0",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1&~from=0",     1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell=block1&cell=block2&cell=block2&~size=1&~from=0",     0);

            actual = ITUtilScroll.assertQueryChannels("?cell!=block",                                                   ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?cell!=block",                    0);

            actual = ITUtilScroll.assertQueryChannels("?cell!=block?",                                                  0);

            actual = ITUtilScroll.assertQueryChannels("?cell!=block*&size=10",                                          0);

            actual = ITUtilScroll.assertQueryChannels("?cell!=block?*&size=10&~from=0",                                 0);

            //         tag (name)

            actual = ITUtilScroll.assertQueryChannels("?~tag=asdf",                                                     0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=archived",                                                 4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=archived",                  0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=handle_this",                                              3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=handle_this",               0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=noteworthy",                                               ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=noteworthy",                0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=not_used",                                                 0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=*_*",                                                      3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=*_*",                       0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=*i*",                                                      6);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=*i*",                       0);

            actual = ITUtilScroll.assertQueryChannels("?~tag=*i*&~size=4&~from=1",                                      4);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag=*i*&~size=4&~from=1",       HttpURLConnection.HTTP_INTERNAL_ERROR, -1);

            actual = ITUtilScroll.assertQueryChannels("?~tag!=noteworthy",                                              0);

            actual = ITUtilScroll.assertQueryChannels("?~tag!=not_used",                                                ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag!=not_used",                 0);

            actual = ITUtilScroll.assertQueryChannels("?~tag!=not_used&~size=10",                                       ITTestFixture.channels_all_properties_tags.length);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?~tag!=not_used&~size=10",        0);

            actual = ITUtilScroll.assertQueryChannels("?~tag!=not_used&~size=10&~from=10",                              0);

            //         combinations

            actual = ITUtilScroll.assertQueryChannels("?domain=cryo&element=source&cell=block1",                                                1);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=cryo&element=source&cell=block1",                 0);

            actual = ITUtilScroll.assertQueryChannels("?domain=power&type=write&~tag=noteworthy",                                               2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=power&type=write&~tag=noteworthy",                0);

            actual = ITUtilScroll.assertQueryChannels("?domain=power&type=write&type=????write&~tag=noteworthy",                                3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=power&type=write&type=????write&~tag=noteworthy", 0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*r*&type=*write&~tag=archived&~tag=noteworthy",                                  2);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*r*&type=*write&~tag=archived&~tag=noteworthy",   0);

            actual = ITUtilScroll.assertQueryChannels("?domain=*r*&type=*write&~tag=noteworthy&~size=3&~from=2",                                3);
            actual = ITUtilScroll.assertContinueChannelsQuery("/" + actual.getId() + "?domain=*r*&type=*write&~tag=noteworthy&~size=3&~from=2", HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixture.tearDown();
    }

}
