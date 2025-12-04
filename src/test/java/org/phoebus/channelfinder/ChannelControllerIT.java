package org.phoebus.channelfinder;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.PropertyRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.rest.api.IChannel;
import org.phoebus.channelfinder.rest.controller.ChannelController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelController.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelControllerIT {

  @Autowired IChannel channelManager;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @Autowired ChannelRepository channelRepository;

  /** read a single channel */
  @Test
  void readXmlChannel() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel0);

    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);

    Channel readChannel = channelManager.read(createdChannel.getName());
    // verify the channel was read as expected
    Assertions.assertEquals(createdChannel, readChannel, "Failed to read the channel");
  }

  /** attempt to read a single non existent channel */
  @Test
  void readNonExistingXmlChannel() {
    // verify the channel failed to be read, as expected
    Assertions.assertThrows(
        ResponseStatusException.class, () -> channelManager.read("fakeChannel"));
  }

  /** create a simple channel */
  @Test
  void createXmlChannel() {
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    cleanupTestChannels = Arrays.asList(testChannel0);

    // Create a simple channel
    Channel createdChannel0 = channelManager.create(testChannel0.getName(), testChannel0);
    // verify the channel was created as expected
    Assertions.assertEquals(testChannel0, createdChannel0, "Failed to create the channel");

    //      Tag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
    //      // verify the tag was created as expected
    //      assertEquals("Failed to create the tag",testTag1,createdTag1);

    // Update the test channel with a new owner
    testChannel0.setOwner("updateTestOwner");
    Channel updatedChannel0 = channelManager.create(testChannel0.getName(), testChannel0);
    // verify the channel was created as expected
    Assertions.assertEquals(testChannel0, updatedChannel0, "Failed to create the channel");
  }

  /** Rename a simple channel using create */
  @Test
  void renameByCreateXmlChannel() {
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    Channel testChannel1 = new Channel("testChannel1", "testOwner");
    cleanupTestChannels = Arrays.asList(testChannel0, testChannel1);

    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
    createdChannel = channelManager.create(testChannel0.getName(), testChannel1);
    // verify that the old channel "testChannel0" was replaced with the new "testChannel1"
    Assertions.assertEquals(testChannel1, createdChannel, "Failed to create the channel");
    // verify that the old channel is no longer present
    Assertions.assertFalse(
        channelRepository.existsById(testChannel0.getName()), "Failed to replace the old channel");
  }

  /** create a single channel with tags and properties */
  @Test
  void createXmlChannel2() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel0);

    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
    try {
      Channel foundChannel = channelRepository.findById(testChannel0.getName()).get();
      Assertions.assertEquals(
          testChannel0,
          foundChannel,
          "Failed to create the channel. Expected "
              + testChannel0.toLog()
              + " found "
              + foundChannel.toLog());
    } catch (Exception e) {
      Assertions.fail("Failed to create/find the channel");
    }

    //      Tag createdTag1 = tagManager.create("fakeTag", copy(testTag1));
    //      // verify the tag was created as expected
    //      assertEquals("Failed to create the tag",testTag1,createdTag1);

    // Update the test channel with a new owner
    testChannel0.setOwner("updateTestOwner");
    createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
    try {
      Channel foundChannel = channelRepository.findById(testChannel0.getName()).get();
      Assertions.assertEquals(
          testChannel0,
          foundChannel,
          "Failed to create the channel. Expected "
              + testChannel0.toLog()
              + " found "
              + foundChannel.toLog());
    } catch (Exception e) {
      Assertions.fail("Failed to create/find the channel");
    }
  }

  /** Rename a single channel with tags and properties using create */
  @Test
  void renameByCreateXmlChannel2() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel0, testChannel1);

    // Create the testChannel0
    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
    // update the testChannel0 with testChannel1
    createdChannel = channelManager.create(testChannel0.getName(), testChannel1);
    // verify that the old channel "testChannel0" was replaced with the new "testChannel1"
    try {
      Channel foundChannel = channelRepository.findById(testChannel1.getName()).get();
      Assertions.assertEquals(testChannel1, foundChannel, "Failed to create the channel");
    } catch (Exception e) {
      Assertions.fail("Failed to create/find the channel");
    }
    // verify that the old channel is no longer present
    Assertions.assertFalse(
        channelRepository.existsById(testChannel0.getName()), "Failed to replace the old channel");
  }

  /** create multiple channels */
  @Test
  void createXmlChannels() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner", testProperties, testTags);
    Channel testChannel2 = new Channel("testChannel2", "testOwner");
    List<Channel> testChannels = Arrays.asList(testChannel0, testChannel1, testChannel2);
    cleanupTestChannels = testChannels;

    Iterable<Channel> createdChannels = channelManager.create(testChannels);
    List<Channel> foundChannels = new ArrayList<Channel>();
    testChannels.forEach(
        chan -> foundChannels.add(channelRepository.findById(chan.getName()).get()));
    // verify the channels were created as expected
    Assertions.assertTrue(
        Iterables.elementsEqual(testChannels, foundChannels), "Failed to create the channels");
  }

  /** create by overriding multiple channels */
  @Test
  void createXmlChannelsWithOverride() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    Channel testChannel1 = new Channel("testChannel1", "testOwner", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel0, testChannel1);
    cleanupTestChannels = testChannels;

    // Create a set of original channels to be overriden
    channelManager.create(testChannels);
    // Now update the test channels
    testChannel0.setOwner("testOwner-updated");
    testChannel1.setTags(Collections.emptyList());
    testChannel1.setProperties(Collections.emptyList());

    List<Channel> updatedTestChannels = Arrays.asList(testChannel0, testChannel1);
    channelManager.create(updatedTestChannels);
    List<Channel> foundChannels = new ArrayList<Channel>();
    testChannels.forEach(
        chan -> foundChannels.add(channelRepository.findById(chan.getName()).get()));
    // verify the channels were created as expected
    Assertions.assertTrue(
        Iterables.elementsEqual(updatedTestChannels, foundChannels),
        "Failed to create the channels");
  }

  /** update a channel */
  @Test
  void updateXmlChannel() {
    testProperties.forEach(prop -> prop.setValue("value"));
    // A test channel with only name and owner
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    // A test channel with name, owner, tags and props
    Channel testChannel1 = new Channel("testChannel1", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel0, testChannel1);

    // Update on a non-existing channel should result in the creation of that channel
    // 1. Test a simple channel
    Channel returnedChannel = channelManager.update(testChannel0.getName(), testChannel0);
    Assertions.assertEquals(
        testChannel0, returnedChannel, "Failed to update channel " + testChannel0);
    Assertions.assertEquals(
        testChannel0,
        channelRepository.findById(testChannel0.getName()).get(),
        "Failed to update channel " + testChannel0);
    // 2. Test a channel with tags and props
    returnedChannel = channelManager.update(testChannel1.getName(), testChannel1);
    Assertions.assertEquals(
        testChannel1, returnedChannel, "Failed to update channel " + testChannel1);
    Assertions.assertEquals(
        testChannel1,
        channelRepository.findById(testChannel1.getName()).get(),
        "Failed to update channel " + testChannel1);

    // Update the channel owner
    testChannel0.setOwner("newTestOwner");
    returnedChannel = channelManager.update(testChannel0.getName(), testChannel0);
    Assertions.assertEquals(
        testChannel0, returnedChannel, "Failed to update channel " + testChannel0);
    Assertions.assertEquals(
        testChannel0,
        channelRepository.findById(testChannel0.getName()).get(),
        "Failed to update channel " + testChannel0);
    testChannel1.setOwner("newTestOwner");
    returnedChannel = channelManager.update(testChannel1.getName(), testChannel1);
    Assertions.assertEquals(
        testChannel1, returnedChannel, "Failed to update channel " + testChannel1);
    Assertions.assertEquals(
        testChannel1,
        channelRepository.findById(testChannel1.getName()).get(),
        "Failed to update channel " + testChannel1);
  }

  /** Rename a channel using update */
  @Test
  void renameByUpdateXmlChannel() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    Channel testChannel1 = new Channel("testChannel1", "testOwner");
    Channel testChannel2 = new Channel("testChannel2", "testOwner", testProperties, testTags);
    Channel testChannel3 = new Channel("testChannel3", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel0, testChannel1, testChannel2, testChannel3);

    // Create the testChannels
    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);
    Channel createdChannelWithItems = channelManager.create(testChannel2.getName(), testChannel2);
    // update the testChannels
    Channel renamedChannel = channelManager.update(testChannel0.getName(), testChannel1);
    Channel renamedChannelWithItems = channelManager.update(testChannel2.getName(), testChannel3);

    // verify that the old channels were replaced by the new ones
    try {
      Channel foundChannel = channelRepository.findById(testChannel1.getName()).get();
      Assertions.assertEquals(testChannel1, foundChannel, "Failed to create the channel");
    } catch (Exception e) {
      Assertions.fail("Failed to create/find the channel");
    }
    // verify that the old channel is no longer present
    Assertions.assertFalse(
        channelRepository.existsById(testChannel0.getName()), "Failed to replace the old channel");

    try {
      Channel foundChannel = channelRepository.findById(testChannel3.getName()).get();
      Assertions.assertEquals(testChannel3, foundChannel, "Failed to create the channel");
    } catch (Exception e) {
      Assertions.fail("Failed to create/find the channel");
    }
    // verify that the old channel is no longer present
    Assertions.assertFalse(
        channelRepository.existsById(testChannel2.getName()), "Failed to replace the old channel");

    // TODO add test for failure case
  }

  /** update a channel by adding tags and adding properties and changing properties */
  @Test
  void updateXmlChannelItems() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 =
        new Channel(
            "testChannel0",
            "testOwner",
            Arrays.asList(testProperties.get(0), testProperties.get(1)),
            Arrays.asList(testTags.get(0), testTags.get(1)));
    cleanupTestChannels = Arrays.asList(testChannel0);

    // Create the testChannel
    Channel createdChannel = channelManager.create(testChannel0.getName(), testChannel0);

    // set up the new testChannel
    testProperties.get(1).setValue("newValue");
    testChannel0 =
        new Channel(
            "testChannel0",
            "testOwner",
            Arrays.asList(testProperties.get(1), testProperties.get(2)),
            Arrays.asList(testTags.get(1), testTags.get(2)));

    // update the testChannel
    Channel updatedChannel = channelManager.update(testChannel0.getName(), testChannel0);

    Channel expectedChannel = new Channel("testChannel0", "testOwner", testProperties, testTags);
    Channel foundChannel = channelRepository.findById("testChannel0").get();
    foundChannel
        .getTags()
        .sort(
            (Tag o1, Tag o2) -> {
              return o1.getName().compareTo(o2.getName());
            });
    foundChannel
        .getProperties()
        .sort(
            (Property o1, Property o2) -> {
              return o1.getName().compareTo(o2.getName());
            });
    Assertions.assertEquals(
        expectedChannel,
        foundChannel,
        "Did not update channel correctly, expected "
            + expectedChannel.toLog()
            + " but actual was "
            + foundChannel.toLog());
  }

  /**
   * update multiple channels first update non-existing channels which should create them second
   * update the newly created channels which should change them
   */
  @Test
  void updateMultipleXmlChannels() {
    testProperties.forEach(prop -> prop.setValue("value"));
    // A test channel with only name and owner
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    // A test channel with name, owner, tags and props
    Channel testChannel1 = new Channel("testChannel1", "testOwner", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel0, testChannel1);
    cleanupTestChannels = testChannels;

    // Update on non-existing channels should result in the creation of those channels
    Iterable<Channel> returnedChannels = channelManager.update(testChannels);
    // 1. Test a simple channel
    Assertions.assertEquals(
        testChannel0,
        channelRepository.findById(testChannel0.getName()).get(),
        "Failed to update channel " + testChannel0);
    // 2. Test a channel with tags and props
    Assertions.assertEquals(
        testChannel1,
        channelRepository.findById(testChannel1.getName()).get(),
        "Failed to update channel " + testChannel1);

    // Update the channel owner
    testChannel0.setOwner("newTestOwner");
    testChannel1.setOwner("newTestOwner");
    returnedChannels = channelManager.update(testChannels);
    Assertions.assertEquals(
        testChannel0,
        channelRepository.findById(testChannel0.getName()).get(),
        "Failed to update channel " + testChannel0);
    Assertions.assertEquals(
        testChannel1,
        channelRepository.findById(testChannel1.getName()).get(),
        "Failed to update channel " + testChannel1);
  }

  /** update multiple channels by adding tags and adding properties and changing properties */
  @Test
  void updateMultipleXmlChannelsWithItems() {
    testProperties.forEach(prop -> prop.setValue("value"));
    Channel testChannel0 =
        new Channel(
            "testChannel0",
            "testOwner",
            Arrays.asList(testProperties.get(0), testProperties.get(1)),
            Arrays.asList(testTags.get(0), testTags.get(1)));
    Channel testChannel1 =
        new Channel(
            "testChannel1",
            "testOwner",
            Arrays.asList(testProperties.get(0), testProperties.get(2)),
            Arrays.asList(testTags.get(0), testTags.get(2)));
    List<Channel> testChannels = Arrays.asList(testChannel0, testChannel1);
    cleanupTestChannels = testChannels;

    // Create the testChannel
    Iterable<Channel> createdChannels = channelManager.create(testChannels);

    // set up the new testChannel
    testProperties.forEach(prop -> prop.setValue("newValue"));
    testChannel0 =
        new Channel(
            "testChannel0",
            "testOwner",
            Arrays.asList(testProperties.get(0), testProperties.get(2)),
            Arrays.asList(testTags.get(0), testTags.get(2)));
    testChannel1 =
        new Channel(
            "testChannel1",
            "testOwner",
            Arrays.asList(testProperties.get(0), testProperties.get(1)),
            Arrays.asList(testTags.get(0), testTags.get(1)));
    testChannels = Arrays.asList(testChannel0, testChannel1);

    // update the testChannel
    Iterable<Channel> updatedChannels = channelManager.update(testChannels);

    // set up the expected testChannels
    testChannel0 =
        new Channel(
            "testChannel0",
            "testOwner",
            Arrays.asList(
                testProperties.get(0),
                new Property("testProperty1", "testPropertyOwner1", "value"),
                testProperties.get(2)),
            testTags);
    testChannel1 =
        new Channel(
            "testChannel1",
            "testOwner",
            Arrays.asList(
                testProperties.get(0),
                testProperties.get(1),
                new Property("testProperty2", "testPropertyOwner2", "value")),
            testTags);
    Iterable<Channel> expectedChannels = Arrays.asList(testChannel0, testChannel1);

    Iterable<Channel> foundChannels =
        channelRepository.findAllById(Arrays.asList("testChannel0", "testChannel1"));
    foundChannels.forEach(
        chan ->
            chan.getTags()
                .sort(
                    (Tag o1, Tag o2) -> {
                      return o1.getName().compareTo(o2.getName());
                    }));
    foundChannels.forEach(
        chan ->
            chan.getProperties()
                .sort(
                    (Property o1, Property o2) -> {
                      return o1.getName().compareTo(o2.getName());
                    }));

    Assertions.assertEquals(
        expectedChannels,
        foundChannels,
        "Did not update channel correctly, expected "
            + testChannel0.toLog()
            + " and "
            + testChannel1.toLog()
            + " but actual was "
            + foundChannels.iterator().next().toLog()
            + " and "
            + foundChannels.iterator().next().toLog());
  }

  /** delete a channel */
  @Test
  void deleteXmlChannel() {
    Channel testChannel0 = new Channel("testChannel0", "testOwner");
    cleanupTestChannels = Arrays.asList(testChannel0);

    channelManager.create(testChannel0.getName(), testChannel0);
    channelManager.remove(testChannel0.getName());
    // verify the channel was deleted as expected
    Assertions.assertFalse(
        channelRepository.existsById(testChannel0.getName()), "Failed to delete the channel");
  }

  // Helper operations to create and clean up the resources needed for successful
  // testing of the ChannelManager operations

  private final List<Tag> testTags =
      Arrays.asList(
          new Tag("testTag0", "testTagOwner0"),
          new Tag("testTag1", "testTagOwner1"),
          new Tag("testTag2", "testTagOwner2"));

  private final List<Property> testProperties =
      Arrays.asList(
          new Property("testProperty0", "testPropertyOwner0"),
          new Property("testProperty1", "testPropertyOwner1"),
          new Property("testProperty2", "testPropertyOwner2"));

  private List<Channel> cleanupTestChannels = Collections.emptyList();

  @BeforeEach
  public void setup() {
    tagRepository.indexAll(testTags);
    propertyRepository.indexAll(testProperties);
  }

  @AfterEach
  public void cleanup() {
    // clean up
    testTags.forEach(
        tag -> {
          try {
            tagRepository.deleteById(tag.getName());
          } catch (Exception e) {
            System.out.println("Failed to clean up tag: " + tag.getName());
          }
        });
    testProperties.forEach(
        property -> {
          try {
            propertyRepository.deleteById(property.getName());
          } catch (Exception e) {
            System.out.println("Failed to clean up property: " + property.getName());
          }
        });
    cleanupTestChannels.forEach(
        channel -> {
          if (channelRepository.existsById(channel.getName())) {
            channelRepository.deleteById(channel.getName());
          }
        });
  }

  @Autowired ElasticConfig esService;

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }
}
