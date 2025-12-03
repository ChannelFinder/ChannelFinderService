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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.common.CFResourceDescriptors;
import org.phoebus.channelfinder.docker.ITUtil.AuthorizationChoice;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.rest.controller.TagManager;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for ChannelFinder and Elasticsearch with focus on usage of {@link
 * CFResourceDescriptors#TAG_RESOURCE_URI}.
 *
 * @author Lars Johansson
 * @see TagManager
 * @see org.phoebus.channelfinder.docker.ITUtil
 * @see org.phoebus.channelfinder.docker.ITUtilTags
 */
@Testcontainers
class ChannelFinderTagsIT {

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
  //     CHANNELFINDER API
  //     --------------------
  //     Retrieve a Tag                        .../tags/<name>                           GET
  //     List Tags                             .../tags                                  GET
  //     Create/Replace a Tag                  .../tags/<name>                           PUT
  //     Add Tag to a Single Channel           .../tags/<tag_name>/<channel_name>        PUT
  //     Create/Replace Tags                   .../tags/<name>                           PUT
  //     Add Tag to Multiple Channels          .../tags/<name>                           POST
  //     Add Multiple Tags                     .../tags                                  POST
  //     Delete Tag from Single Channel        .../tags/<tag_name>/<channel_name>        DELETE
  //     Delete Tag                            .../tags/<name>                           DELETE
  //
  // ------------------------------------------------------------------------------------------------

  // test data
  //     tags t1 - t10, owner o1
  //     tag  t1,       owner o2

  static Tag tag_t1_owner_o1;
  static Tag tag_t2_owner_o1;
  static Tag tag_t3_owner_o1;
  static Tag tag_t4_owner_o1;
  static Tag tag_t5_owner_o1;
  static Tag tag_t6_owner_o1;
  static Tag tag_t7_owner_o1;
  static Tag tag_t8_owner_o1;
  static Tag tag_t9_owner_o1;
  static Tag tag_t10_owner_o1;

  static Tag tag_t1_owner_o2;

  @Container public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

  @BeforeAll
  public static void setupObjects() {
    tag_t1_owner_o1 = new Tag("t1", "o1");
    tag_t2_owner_o1 = new Tag("t2", "o1");
    tag_t3_owner_o1 = new Tag("t3", "o1");
    tag_t4_owner_o1 = new Tag("t4", "o1");
    tag_t5_owner_o1 = new Tag("t5", "o1");
    tag_t6_owner_o1 = new Tag("t6", "o1");
    tag_t7_owner_o1 = new Tag("t7", "o1");
    tag_t8_owner_o1 = new Tag("t8", "o1");
    tag_t9_owner_o1 = new Tag("t9", "o1");
    tag_t10_owner_o1 = new Tag("t10", "o1");

    tag_t1_owner_o2 = new Tag("t1", "o2");
  }

  @AfterAll
  public static void tearDownObjects() {
    tag_t1_owner_o1 = null;
    tag_t2_owner_o1 = null;
    tag_t3_owner_o1 = null;
    tag_t4_owner_o1 = null;
    tag_t5_owner_o1 = null;
    tag_t6_owner_o1 = null;
    tag_t7_owner_o1 = null;
    tag_t8_owner_o1 = null;
    tag_t9_owner_o1 = null;
    tag_t10_owner_o1 = null;

    tag_t1_owner_o2 = null;
  }

  @AfterAll
  public static void extractJacocoReport() {
    // extract jacoco report from container file system
    ITUtil.extractJacocoReport(
        ENVIRONMENT,
        ITUtil.JACOCO_TARGET_PREFIX
            + ChannelFinderTagsIT.class.getSimpleName()
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

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTagRetrieveCheck() {
    // what
    //     check(s) for retrieve tag
    //         e.g.
    //             retrieve non-existing tag

    ITUtilTags.assertRetrieveTag("/t11", HttpURLConnection.HTTP_NOT_FOUND);
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTagRemoveCheck() {
    // what
    //     check(s) for remove tag
    //         e.g.
    //             remove non-existing tag

    try {
      // might be both 401, 404
      //     401 UNAUTHORIZED
      //     404 NOT_FOUND

      ITUtilTags.assertRemoveTag(
          AuthorizationChoice.NONE, "/t11", HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertRemoveTag(
          AuthorizationChoice.USER, "/t11", HttpURLConnection.HTTP_NOT_FOUND);
      ITUtilTags.assertRemoveTag(
          AuthorizationChoice.ADMIN, "/t11", HttpURLConnection.HTTP_NOT_FOUND);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTagCreateUpdateCheckJson() {
    // what
    //     check(s) for create / update tag
    //         e.g.
    //             user without required role TagMod
    //             content
    //                 json    - incomplete
    //                 name    - null, empty
    //                 owner   - null, empty
    //                 channel - exists

    String json_incomplete1 = "{\"incomplete\"}";
    String json_incomplete2 = "{\"incomplete\"";
    String json_incomplete3 = "{\"incomplete}";
    String json_incomplete4 = "{\"\"}";
    String json_incomplete5 = "{incomplete\"}";
    String json_incomplete6 = "\"incomplete\"}";
    String json_incomplete7 = "{";
    String json_incomplete8 = "}";
    String json_incomplete9 = "\"";

    String json_tag_t1_name_na = "{\"na\":\"t1\",\"owner\":\"o1\"}";
    String json_tag_t1_owner_ow = "{\"name\":\"t1\",\"ow\":\"o1\"}";

    String json_tag_t1_channels_c1 =
        "{\"name\":\"t1\",\"owner\":\"o1\",\"channels\":[{\"name\":\"c1\",\"owner\":\"o1\",\"properties\":[],\"tags\":[{\"name\":\"t1\",\"owner\":\"o1\",\"channels\":[]}]}]}";

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete6, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_name_na,
          HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_owner_ow,
          HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_channels_c1,
          HttpURLConnection.HTTP_NOT_FOUND);

      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete6, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/t1", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_name_na,
          HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_owner_ow,
          HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN,
          "/t1",
          json_tag_t1_channels_c1,
          HttpURLConnection.HTTP_NOT_FOUND);

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTagCreateUpdateCheck() {
    // what
    //     check(s) for create / update tag
    //         e.g.
    //             user without required role TagMod
    //             content
    //                 json    - incomplete
    //                 name    - null, empty
    //                 owner   - null, empty
    //                 channel - exists

    Tag tag_check = new Tag();

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.NONE, "/t1", tag_t1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.NONE, "/t1", tag_t1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.USER, "/t1", tag_t1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.USER, "/t1", tag_t1_owner_o1, HttpURLConnection.HTTP_UNAUTHORIZED);

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

      tag_check.setName(null);

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

      tag_check.setName("");

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

      tag_check.setName("asdf");
      tag_check.setOwner(null);

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

      tag_check.setName("asdf");
      tag_check.setOwner("");

      ITUtilTags.assertCreateReplaceTag(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
      ITUtilTags.assertAddTagMultipleChannels(
          AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTag() {
    // what
    //     user with required role TagMod
    //     create tag
    //         list, create tag, list, retrieve, delete (unauthorized), delete, list

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTag(AuthorizationChoice.ADMIN, "/t1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);
      ITUtilTags.assertRetrieveTag("/t1?withChannels=true", tag_t1_owner_o1);
      ITUtilTags.assertRetrieveTag("/t1?withChannels=false", tag_t1_owner_o1);

      ITUtilTags.assertRemoveTag(
          AuthorizationChoice.NONE, "/t1", HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertRemoveTag(
          AuthorizationChoice.USER, "/t1", HttpURLConnection.HTTP_UNAUTHORIZED);
      ITUtilTags.assertRemoveTag(AuthorizationChoice.ADMIN, "/t1", HttpURLConnection.HTTP_OK);

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTag2() {
    // what
    //     create tags, one by one
    //         list, create tag * 2, list, retrieve, retrieve, delete, list, retrieve, delete, list

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_owner_o1);
      ITUtilTags.assertCreateReplaceTag("/t2", tag_t2_owner_o1);

      ITUtilTags.assertListTags(2, tag_t1_owner_o1, tag_t2_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);
      ITUtilTags.assertRetrieveTag("/t2", tag_t2_owner_o1);

      ITUtilTags.assertRemoveTag("/t1");

      ITUtilTags.assertListTags(1, tag_t2_owner_o1);

      ITUtilTags.assertRetrieveTag("/t2", tag_t2_owner_o1);

      ITUtilTags.assertRemoveTag("/t2");

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTag3RenameOwner() {
    // what
    //     replace tag, rename owner
    //         list, create tag, list, retrieve, update, retrieve, delete, list

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);

      ITUtilTags.assertAddTagMultipleChannels("/t1", tag_t1_owner_o2);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o2);

      ITUtilTags.assertRemoveTag("/t1");

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTag4ReplaceNonExisting() {
    // what
    //     replace non-existing tag
    //         list, update, list, retrieve, delete, list

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertAddTagMultipleChannels("/t1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);

      ITUtilTags.assertRemoveTag("/t1");

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#PROPERTY_RESOURCE_URI}. */
  @Test
  void handleTag5SingleChannel() {
    // what
    //     add tag to single channel
    //         clean start, create tag, create channel,
    //         add tag to single channel,
    //         list, retrieve
    //         remove tag from single channel,
    //         delete channel, delete tag, clean end

    Channel channel_c1 = new Channel("c1", "o1");

    Tag tag_t1 = new Tag("t1", "o1");

    Channel channel_c1_tags = new Channel("c1", "o1");
    channel_c1_tags.addTag(tag_t1);

    Tag tag_t1_channels = new Tag("t1", "o1");
    tag_t1_channels.getChannels().add(channel_c1_tags);

    try {
      // --------------------------------------------------------------------------------
      // clean start
      // --------------------------------------------------------------------------------

      ITUtilProperties.assertListProperties(0);

      ITUtilTags.assertListTags(0);

      ITUtilChannels.assertListChannels(0);

      // --------------------------------------------------------------------------------
      // put
      // --------------------------------------------------------------------------------

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);

      ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1);

      ITUtilChannels.assertListChannels(1, channel_c1);

      ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);

      // --------------------------------------------------------------------------------
      // complex tests, add tag to single channel
      // --------------------------------------------------------------------------------

      ITUtilTags.assertAddTagSingleChannel("/t1/c1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_channels);

      ITUtilChannels.assertListChannels(1, channel_c1_tags);

      ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_tags);

      // --------------------------------------------------------------------------------
      // complex tests, remove tag from single channel
      // --------------------------------------------------------------------------------

      ITUtilTags.assertRemoveTagSingleChannel("/t1/c1");

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);

      ITUtilChannels.assertListChannels(1, channel_c1);

      ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);

      // --------------------------------------------------------------------------------
      // clean end
      // --------------------------------------------------------------------------------

      ITUtilChannels.assertDeleteChannel("/c1");

      ITUtilChannels.assertListChannels(0);

      ITUtilTags.assertRemoveTag("/t1");

      ITUtilTags.assertListTags(0);

      ITUtilProperties.assertListProperties(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#PROPERTY_RESOURCE_URI}. */
  @Test
  void handleTag6MultipleChannels() {
    // what
    //     add tag to multiple channels
    //         clean start, create tag, create channel(s),
    //         add tag to multiple channel(s),
    //         list, retrieve
    //         delete tag, delete channel(s), clean end

    Channel channel_c1 = new Channel("c1", "o1");
    Channel channel_c2 = new Channel("c2", "o1");
    Channel channel_c3 = new Channel("c3", "o1");

    Tag tag_t1 = new Tag("t1", "o1");

    Channel channel_c1_tags = new Channel("c1", "o1");
    Channel channel_c2_tags = new Channel("c2", "o1");
    Channel channel_c3_tags = new Channel("c3", "o1");
    channel_c1_tags.addTag(tag_t1);
    channel_c2_tags.addTag(tag_t1);
    channel_c3_tags.addTag(tag_t1);

    Tag tag_t1_channels = new Tag("t1", "o1");
    tag_t1_channels.getChannels().add(channel_c1);
    tag_t1_channels.getChannels().add(channel_c2);
    tag_t1_channels.getChannels().add(channel_c3);

    try {
      // --------------------------------------------------------------------------------
      // clean start
      // --------------------------------------------------------------------------------

      ITUtilProperties.assertListProperties(0);

      ITUtilTags.assertListTags(0);

      ITUtilChannels.assertListChannels(0);

      // --------------------------------------------------------------------------------
      // put
      // --------------------------------------------------------------------------------

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_owner_o1);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_owner_o1);

      ITUtilChannels.assertCreateReplaceChannel("/c1", channel_c1);
      ITUtilChannels.assertCreateReplaceChannel("/c2", channel_c2);
      ITUtilChannels.assertCreateReplaceChannel("/c3", channel_c3);

      ITUtilChannels.assertListChannels(3, channel_c1, channel_c2, channel_c3);

      ITUtilChannels.assertRetrieveChannel("/c1", channel_c1);
      ITUtilChannels.assertRetrieveChannel("/c2", channel_c2);
      ITUtilChannels.assertRetrieveChannel("/c3", channel_c3);

      // --------------------------------------------------------------------------------
      // complex tests, add tag to multiple channels
      // --------------------------------------------------------------------------------

      ITUtilTags.assertAddTagMultipleChannels("/t1", tag_t1_channels);

      ITUtilTags.assertListTags(1, tag_t1_owner_o1);

      ITUtilTags.assertCreateReplaceTag("/t1", tag_t1_channels);

      ITUtilChannels.assertListChannels(3, channel_c1_tags, channel_c2_tags, channel_c3_tags);

      ITUtilChannels.assertRetrieveChannel("/c1", channel_c1_tags);
      ITUtilChannels.assertRetrieveChannel("/c2", channel_c2_tags);
      ITUtilChannels.assertRetrieveChannel("/c3", channel_c3_tags);

      // --------------------------------------------------------------------------------
      // clean end
      // --------------------------------------------------------------------------------

      ITUtilChannels.assertDeleteChannel("/c3");
      ITUtilChannels.assertDeleteChannel("/c2");
      ITUtilChannels.assertDeleteChannel("/c1");

      ITUtilChannels.assertListChannels(0);

      ITUtilTags.assertRemoveTag("/t1");

      ITUtilTags.assertListTags(0);

      ITUtilProperties.assertListProperties(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTagsCreateUpdateCheck() {
    // what
    //     check(s) for create tags
    //         e.g.
    //             user without required role TagMod
    //             content
    //                 json    - incomplete
    //                 name    - null, empty
    //                 owner   - null, empty
    //                 channel - exists

    String json_incomplete1 = "{\"incomplete\"}";
    String json_tag_t1_name_na = "{\"na\":\"t1\",\"owner\":\"o1\"}";
    String json_tag_t1_owner_ow = "{\"name\":\"t1\",\"ow\":\"o1\"}";

    ObjectMapper mapper = new ObjectMapper();

    try {
      ITUtilTags.assertListTags(0);

      String json_multiple =
          "["
              + mapper.writeValueAsString(tag_t1_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t2_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t3_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t4_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t5_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t6_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t7_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t8_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t9_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t10_owner_o1)
              + ","
              + json_incomplete1
              + "]";

      ITUtilTags.assertCreateReplaceTags(
          AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

      ITUtilTags.assertAddMultipleTags("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

      json_multiple =
          "["
              + mapper.writeValueAsString(tag_t1_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t2_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t3_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t4_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t5_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t6_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t7_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t8_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t9_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t10_owner_o1)
              + ","
              + json_tag_t1_name_na
              + "]";

      ITUtilTags.assertCreateReplaceTags(
          AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

      ITUtilTags.assertAddMultipleTags("", json_multiple, HttpURLConnection.HTTP_INTERNAL_ERROR);

      json_multiple =
          "["
              + mapper.writeValueAsString(tag_t1_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t2_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t3_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t4_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t5_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t6_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t7_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t8_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t9_owner_o1)
              + ","
              + mapper.writeValueAsString(tag_t10_owner_o1)
              + ","
              + json_tag_t1_owner_ow
              + "]";

      ITUtilTags.assertCreateReplaceTags(
          AuthorizationChoice.ADMIN, "", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

      ITUtilTags.assertAddMultipleTags("", json_multiple, HttpURLConnection.HTTP_BAD_REQUEST);

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTags() {
    // what
    //     create tags
    //         list, create tags (10), list, retrieve (10), delete (5), list, delete (5), list

    Tag[] tags_10 =
        new Tag[] {
          tag_t1_owner_o1,
          tag_t2_owner_o1,
          tag_t3_owner_o1,
          tag_t4_owner_o1,
          tag_t5_owner_o1,
          tag_t6_owner_o1,
          tag_t7_owner_o1,
          tag_t8_owner_o1,
          tag_t9_owner_o1,
          tag_t10_owner_o1
        };

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertCreateReplaceTags("", tags_10);

      ITUtilTags.assertListTags(
          10,
          tag_t1_owner_o1,
          tag_t10_owner_o1,
          tag_t2_owner_o1,
          tag_t3_owner_o1,
          tag_t4_owner_o1,
          tag_t5_owner_o1,
          tag_t6_owner_o1,
          tag_t7_owner_o1,
          tag_t8_owner_o1,
          tag_t9_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);
      ITUtilTags.assertRetrieveTag("/t2", tag_t2_owner_o1);
      ITUtilTags.assertRetrieveTag("/t3", tag_t3_owner_o1);
      ITUtilTags.assertRetrieveTag("/t4", tag_t4_owner_o1);
      ITUtilTags.assertRetrieveTag("/t5", tag_t5_owner_o1);
      ITUtilTags.assertRetrieveTag("/t6", tag_t6_owner_o1);
      ITUtilTags.assertRetrieveTag("/t7", tag_t7_owner_o1);
      ITUtilTags.assertRetrieveTag("/t8", tag_t8_owner_o1);
      ITUtilTags.assertRetrieveTag("/t9", tag_t9_owner_o1);
      ITUtilTags.assertRetrieveTag("/t10", tag_t10_owner_o1);

      ITUtilTags.assertRemoveTag("/t1");
      ITUtilTags.assertRemoveTag("/t2");
      ITUtilTags.assertRemoveTag("/t3");
      ITUtilTags.assertRemoveTag("/t4");
      ITUtilTags.assertRemoveTag("/t5");

      ITUtilTags.assertListTags(
          5, tag_t10_owner_o1, tag_t6_owner_o1, tag_t7_owner_o1, tag_t8_owner_o1, tag_t9_owner_o1);

      ITUtilTags.assertRemoveTag("/t6");
      ITUtilTags.assertRemoveTag("/t7");
      ITUtilTags.assertRemoveTag("/t8");
      ITUtilTags.assertRemoveTag("/t9");
      ITUtilTags.assertRemoveTag("/t10");

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }

  /** Test {@link CFResourceDescriptors#TAG_RESOURCE_URI}. */
  @Test
  void handleTags2ReplaceNonExisting() {
    // what
    //     replace non-existing tags
    //         list, update tags (10), list, retrieve (10), delete (5), list, delete (5), list

    Tag[] tags_10 =
        new Tag[] {
          tag_t1_owner_o1,
          tag_t2_owner_o1,
          tag_t3_owner_o1,
          tag_t4_owner_o1,
          tag_t5_owner_o1,
          tag_t6_owner_o1,
          tag_t7_owner_o1,
          tag_t8_owner_o1,
          tag_t9_owner_o1,
          tag_t10_owner_o1
        };

    try {
      ITUtilTags.assertListTags(0);

      ITUtilTags.assertAddMultipleTags("", tags_10);

      ITUtilTags.assertListTags(
          10,
          tag_t1_owner_o1,
          tag_t10_owner_o1,
          tag_t2_owner_o1,
          tag_t3_owner_o1,
          tag_t4_owner_o1,
          tag_t5_owner_o1,
          tag_t6_owner_o1,
          tag_t7_owner_o1,
          tag_t8_owner_o1,
          tag_t9_owner_o1);

      ITUtilTags.assertRetrieveTag("/t1", tag_t1_owner_o1);
      ITUtilTags.assertRetrieveTag("/t2", tag_t2_owner_o1);
      ITUtilTags.assertRetrieveTag("/t3", tag_t3_owner_o1);
      ITUtilTags.assertRetrieveTag("/t4", tag_t4_owner_o1);
      ITUtilTags.assertRetrieveTag("/t5", tag_t5_owner_o1);
      ITUtilTags.assertRetrieveTag("/t6", tag_t6_owner_o1);
      ITUtilTags.assertRetrieveTag("/t7", tag_t7_owner_o1);
      ITUtilTags.assertRetrieveTag("/t8", tag_t8_owner_o1);
      ITUtilTags.assertRetrieveTag("/t9", tag_t9_owner_o1);
      ITUtilTags.assertRetrieveTag("/t10", tag_t10_owner_o1);

      ITUtilTags.assertRemoveTag("/t1");
      ITUtilTags.assertRemoveTag("/t2");
      ITUtilTags.assertRemoveTag("/t3");
      ITUtilTags.assertRemoveTag("/t4");
      ITUtilTags.assertRemoveTag("/t5");

      ITUtilTags.assertListTags(
          5, tag_t10_owner_o1, tag_t6_owner_o1, tag_t7_owner_o1, tag_t8_owner_o1, tag_t9_owner_o1);

      ITUtilTags.assertRemoveTag("/t6");
      ITUtilTags.assertRemoveTag("/t7");
      ITUtilTags.assertRemoveTag("/t8");
      ITUtilTags.assertRemoveTag("/t9");
      ITUtilTags.assertRemoveTag("/t10");

      ITUtilTags.assertListTags(0);
    } catch (Exception e) {
      fail();
    }
  }
}
