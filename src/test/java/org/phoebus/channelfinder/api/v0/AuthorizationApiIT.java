package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.phoebus.channelfinder.api.v0.ApiJson.array;
import static org.phoebus.channelfinder.api.v0.ApiJson.channel;
import static org.phoebus.channelfinder.api.v0.ApiJson.property;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyNames;
import static org.phoebus.channelfinder.api.v0.ApiJson.propertyValue;
import static org.phoebus.channelfinder.api.v0.ApiJson.tag;
import static org.phoebus.channelfinder.api.v0.ApiJson.withChannels;
import static org.phoebus.channelfinder.api.v0.ApiJson.withProperties;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;

/**
 * Tests anonymous reads, authenticated writes and owner enforcement.
 *
 * <p>The demo users are {@code admin} (role {@code ADMIN}, an admin group), {@code user} (role
 * {@code USER}, which grants channel, property and tag writes) and {@code guest} (no group).
 */
class AuthorizationApiIT extends ApiTestBase {

  @BeforeEach
  void createAdminOwnedData() {
    createProperties("admin", "area");
    createTags("admin", "archived");
    putChannels(
        withProperties(channel("adminChannel", "admin"), property("area", "admin", "north")));
  }

  private void assertAdminChannelIsUnchanged() {
    ApiClient.Response response = cf.get("/resources/channels/adminChannel");
    assertEquals(200, response.status(), response.body());
    assertEquals("admin", response.json().get("owner").asString());
    assertEquals("north", propertyValue(response.json(), "area"));
  }

  @Test
  void readsNeedNoCredentials() {
    assertEquals(200, cf.get("/resources/channels").status());
    assertEquals(200, cf.get("/resources/channels/adminChannel").status());
    assertEquals(200, cf.get("/resources/channels/count").status());
    assertEquals(200, cf.get("/resources/channels/combined").status());
    assertEquals(200, cf.get("/resources/properties").status());
    assertEquals(200, cf.get("/resources/tags").status());
    assertEquals(200, cf.get("/resources/scroll").status());
  }

  @Test
  void writesWithoutCredentialsAreRejectedWith401() {
    String ch = channel("ch1", "admin").toString();

    assertEquals(401, cf.put("/resources/channels/ch1", ch, null).status());
    assertEquals(401, cf.post("/resources/channels/ch1", ch, null).status());
    assertEquals(401, cf.delete("/resources/channels/adminChannel", null).status());
    assertEquals(401, cf.put("/resources/tags/t", tag("t", "admin").toString(), null).status());
  }

  @Test
  void wrongPasswordIsRejectedWith401() {
    String ch = channel("ch1", "admin").toString();

    assertEquals(401, cf.put("/resources/channels/ch1", ch, "admin:wrong").status());
  }

  @Test
  void userWithoutAGroupIsRejectedWith403() {
    assertEquals(
        403,
        cf.put("/resources/channels/ch1", channel("ch1", "guest").toString(), ApiClient.GUEST)
            .status());
    assertEquals(
        403, cf.put("/resources/tags/t", tag("t", "guest").toString(), ApiClient.GUEST).status());
    assertEquals(
        403,
        cf.put("/resources/properties/p", property("p", "guest").toString(), ApiClient.GUEST)
            .status());
  }

  @Test
  void knownUserWithoutTheRightIsRejectedWith403() {
    assertEquals(
        403,
        cf.put("/resources/channels/ch1", channel("ch1", "guest").toString(), ApiClient.GUEST)
            .status());
    assertEquals(
        403,
        cf.put(
                "/resources/channels/adminChannel",
                channel("adminChannel", "user").toString(),
                ApiClient.USER)
            .status());
  }

  @Test
  void userWithoutTheAdminRoleCannotProcessAllChannels() {
    assertEquals(403, cf.put("/resources/processors/process/all", "", ApiClient.USER).status());
  }

  @Test
  void userCanWriteChannelsItOwnsDirectlyOrThroughAGroup() {
    assertEquals(
        200,
        cf.put("/resources/channels/ch1", channel("ch1", "user").toString(), ApiClient.USER)
            .status());
    assertEquals(
        200,
        cf.put("/resources/channels/ch2", channel("ch2", "USER").toString(), ApiClient.USER)
            .status());
    assertEquals(200, cf.delete("/resources/channels/ch1", ApiClient.USER).status());
  }

  @Test
  void userCanBatchPostChannelsItOwns() {
    putChannels(channel("userChannel", "user"));
    String batch =
        array(
                withProperties(channel("userChannel", "user"), property("area", "user", "south")),
                channel("newChannel", "user"))
            .toString();

    assertEquals(200, cf.post("/resources/channels", batch, ApiClient.USER).status());
    assertEquals("south", propertyValue(cf.get("/resources/channels/userChannel").json(), "area"));
    assertEquals(200, cf.get("/resources/channels/newChannel").status());
  }

  @Test
  void userCannotCreateAChannelForAnotherOwner() {
    assertEquals(
        403,
        cf.put("/resources/channels/ch1", channel("ch1", "admin").toString(), ApiClient.USER)
            .status());
  }

  @Test
  void userCannotChangeOrDeleteAChannelOwnedBySomeoneElse() {
    String takeOver = channel("adminChannel", "user").toString();

    assertEquals(
        403, cf.put("/resources/channels/adminChannel", takeOver, ApiClient.USER).status());
    assertEquals(
        403, cf.post("/resources/channels/adminChannel", takeOver, ApiClient.USER).status());
    assertEquals(403, cf.delete("/resources/channels/adminChannel", ApiClient.USER).status());
    assertEquals(
        "admin", cf.get("/resources/channels/adminChannel").json().get("owner").asString());
  }

  @Test
  void userCannotBatchPostAChannelOwnedBySomeoneElse() {
    putChannels(channel("userChannel", "user"));
    String batch =
        array(
                withProperties(channel("userChannel", "user"), property("area", "user", "south")),
                withProperties(channel("adminChannel", "user"), property("area", "user", "south")))
            .toString();

    assertEquals(403, cf.post("/resources/channels", batch, ApiClient.USER).status());
    assertAdminChannelIsUnchanged();
    assertEquals(List.of(), propertyNames(cf.get("/resources/channels/userChannel").json()));
  }

  @Test
  void userCannotPutAChannelOwnedBySomeoneElseThroughAnotherPath() {
    String adminChannel = channel("adminChannel", "user").toString();

    assertEquals(
        403, cf.put("/resources/channels/notThere", adminChannel, ApiClient.USER).status());
    assertAdminChannelIsUnchanged();
  }

  @Test
  void userCanPutItsChannelThroughThePathOfAnotherOfItsChannels() {
    putChannels(channel("userChannel", "user"), channel("otherUserChannel", "user"));
    String otherUserChannel = channel("otherUserChannel", "user").toString();

    assertEquals(
        200, cf.put("/resources/channels/userChannel", otherUserChannel, ApiClient.USER).status());
    assertEquals(404, cf.get("/resources/channels/userChannel").status());
    assertEquals(200, cf.get("/resources/channels/otherUserChannel").status());
  }

  @Test
  void userCannotRenameItsChannelOntoAChannelOwnedBySomeoneElse() {
    putChannels(channel("userChannel", "user"));
    String adminChannel = channel("adminChannel", "user").toString();

    assertEquals(
        403, cf.post("/resources/channels/userChannel", adminChannel, ApiClient.USER).status());
    assertAdminChannelIsUnchanged();
    assertEquals(200, cf.get("/resources/channels/userChannel").status());
  }

  @Test
  void userCannotPostAChannelOwnedBySomeoneElseThroughAnotherPath() {
    String adminChannel = channel("adminChannel", "user").toString();

    assertEquals(
        403, cf.post("/resources/channels/notThere", adminChannel, ApiClient.USER).status());
    assertAdminChannelIsUnchanged();
  }

  @Test
  void userCanRenameItsChannelOntoAnotherOfItsChannels() {
    putChannels(channel("userChannel", "user"), channel("otherUserChannel", "user"));
    String otherUserChannel = channel("otherUserChannel", "user").toString();

    assertEquals(
        200, cf.post("/resources/channels/userChannel", otherUserChannel, ApiClient.USER).status());
    assertEquals(404, cf.get("/resources/channels/userChannel").status());
    assertEquals(200, cf.get("/resources/channels/otherUserChannel").status());
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value = "POST to a property writes the owner of each embedded channel",
      withExceptions = AssertionFailedError.class)
  void userCannotChangeTheOwnerOfAChannelThroughItsOwnProperty() {
    // api.rst lets the owner of userProp add it to any channel. The service checks the owner of
    // userProp and then writes the embedded channel as it is, owner included.
    createProperties("user", "userProp");
    String userProp =
        withChannels(
                property("userProp", "user"),
                withProperties(channel("adminChannel", "user"), property("userProp", "user", "v")))
            .toString();

    // today: returns 200, and adminChannel belongs to user
    cf.post("/resources/properties/userProp", userProp, ApiClient.USER);
    assertAdminChannelIsUnchanged();
  }

  @Test
  void userCanUseAPropertyItDoesNotOwnOnItsOwnChannel() {
    // This is the model api.rst documents. A channel write needs a member of the channel's owner
    // group. The property endpoints need a member of the property's owner group. The channel PUT
    // therefore checks the channel owner only.
    String ch1 =
        withProperties(channel("ch1", "user"), property("area", "user", "south")).toString();

    assertEquals(200, cf.put("/resources/channels/ch1", ch1, ApiClient.USER).status());
    assertEquals("south", propertyValue(cf.get("/resources/channels/ch1").json(), "area"));
  }

  @Test
  void userCannotChangeADefinitionOwnedBySomeoneElse() {
    assertEquals(
        403,
        cf.put("/resources/properties/area", property("area", "user").toString(), ApiClient.USER)
            .status());
    assertEquals(
        403,
        cf.put(
                "/resources/properties/area/adminChannel",
                property("area", "user", "south").toString(),
                ApiClient.USER)
            .status());
    assertEquals(403, cf.delete("/resources/tags/archived", ApiClient.USER).status());
  }

  @Test
  void adminCanChangeAnything() {
    putChannels(channel("userChannel", "user"));

    assertEquals(
        200,
        cf.post(
                "/resources/channels/userChannel",
                channel("userChannel", "admin").toString(),
                ApiClient.ADMIN)
            .status());
    assertEquals(200, cf.delete("/resources/channels/userChannel", ApiClient.ADMIN).status());
  }
}
