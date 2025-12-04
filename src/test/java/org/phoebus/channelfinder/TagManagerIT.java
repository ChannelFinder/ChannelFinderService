package org.phoebus.channelfinder;

import static java.util.Collections.EMPTY_LIST;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.respository.ChannelRepository;
import org.phoebus.channelfinder.respository.TagRepository;
import org.phoebus.channelfinder.rest.api.ITagManager;
import org.phoebus.channelfinder.rest.controller.TagManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(TagManager.class)
@WithMockUser(roles = "CF-ADMINS")
@ContextConfiguration(classes = {TagRepository.class, ElasticConfig.class})
@TestPropertySource(value = "classpath:application_test.properties")
class TagManagerIT {

  @Autowired ITagManager tagManager;

  @Autowired TagRepository tagRepository;

  @Autowired ChannelRepository channelRepository;

  @Autowired ElasticConfig esService;
  private static final Logger logger = Logger.getLogger(TagManagerIT.class.getName());

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }

  /** list all tags */
  @Test
  void listXmlTags() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(testChannels());

    List<Tag> testTags = Arrays.asList(testTag0, testTag1);
    Iterable<Tag> createdTags = tagManager.create(testTags);

    Iterable<Tag> tagList = tagManager.list();
    for (Tag tag : createdTags) {
      tag.setChannels(new ArrayList<>());
    }
    // verify the tags were listed as expected
    Assertions.assertEquals(createdTags, tagList, "Failed to list all tags");
  }

  /** read a single tag test the "withChannels" flag */
  @Test
  void readXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(testChannels());

    Tag createdTag0 = tagManager.create(testTag0.getName(), testTag0);
    Tag createdTag1 = tagManager.create(testTag1.getName(), testTag1);

    // verify the created tags are read as expected
    // Retrieve the testTag0 without channels
    Tag retrievedTag = tagManager.read(createdTag0.getName(), false);
    Assertions.assertEquals(createdTag0, retrievedTag, "Failed to read the tag");
    // Retrieve the testTag0 with channels
    retrievedTag = tagManager.read(createdTag0.getName(), true);
    Assertions.assertEquals(createdTag0, retrievedTag, "Failed to read the tag w/ channels");

    // Retrieve the testTag1 without channels
    retrievedTag = tagManager.read(createdTag1.getName(), false);
    testTag1.setChannels(new ArrayList<>());
    Assertions.assertEquals(testTag1, retrievedTag, "Failed to read the tag");
    // Retrieve the testTag1 with channels
    retrievedTag = tagManager.read(createdTag1.getName(), true);
    Assertions.assertEquals(createdTag1, retrievedTag, "Failed to read the tag w/ channels");
  }

  /** attempt to read a single non existent tag */
  @Test
  void readNonExistingXmlTag() {
    // verify the tag failed to be read, as expected
    Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.read("fakeTag", false));
  }

  /** attempt to read a single non existent tag with channels */
  @Test
  void readNonExistingXmlTag2() {
    // verify the tag failed to be read, as expected
    Assertions.assertThrows(ResponseStatusException.class, () -> tagManager.read("fakeTag", true));
  }

  /** create a simple tag */
  @Test
  void createXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");

    // Create a simple tag
    Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
    Assertions.assertEquals(testTag0, createdTag, "Failed to create the tag");

    // Update the test tag with a new owner
    Tag updatedTestTag0 = new Tag("testTag0", "updateTestOwner");
    createdTag = tagManager.create(testTag0.getName(), copy(updatedTestTag0));
    Assertions.assertEquals(updatedTestTag0, createdTag, "Failed to create the tag");
  }

  /** Rename a simple tag using create */
  @Test
  void renameByCreateXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");

    tagManager.create(testTag0.getName(), testTag0);
    Tag createdTag = tagManager.create(testTag0.getName(), testTag1);
    // verify that the old tag "testTag0" was replaced with the new "testTag1"
    Assertions.assertEquals(testTag1, createdTag, "Failed to create the tag");
    // verify that the old tag is no longer present
    Assertions.assertFalse(
        tagRepository.existsById(testTag0.getName()), "Failed to replace the old tag");
  }

  /** Create a single tag with channels */
  @Test
  void createXmlTag2() {
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(testChannels());

    tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
    try {
      Tag foundTag = tagRepository.findById(testTag0WithChannels.getName(), true).get();
      Tag expectedTag = new Tag("testTag0WithChannels", "testOwner");
      expectedTag.setChannels(
          Arrays.asList(
              new Channel(
                  "testChannel0",
                  "testOwner",
                  EMPTY_LIST,
                  List.of(new Tag("testTag0WithChannels", "testOwner"))),
              new Channel(
                  "testChannel1",
                  "testOwner",
                  EMPTY_LIST,
                  List.of(new Tag("testTag0WithChannels", "testOwner")))));
      Assertions.assertEquals(
          expectedTag,
          foundTag,
          "Failed to create the tag w/ channels. Expected "
              + expectedTag.toLog()
              + " found "
              + foundTag.toLog());

    } catch (Exception e) {
      fail("Failed to create/find the tag w/ channels due to exception " + e.getMessage());
    }

    Tag updatedTestTag0WithChannels = new Tag("testTag0WithChannels", "updateTestOwner");

    tagManager.create(testTag0WithChannels.getName(), copy(updatedTestTag0WithChannels));
    try {
      Tag foundTag = tagRepository.findById(updatedTestTag0WithChannels.getName(), true).get();
      // verify the tag was created as expected
      Assertions.assertTrue(
          tagCompare(updatedTestTag0WithChannels, foundTag),
          "Failed to create the tag w/ channels");
    } catch (Exception e) {
      fail("Failed to create/find the tag w/ channels");
    }
  }

  /** Rename a single tag with channels using create */
  @Test
  void renameByCreateXmlTag2() {
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(testChannels());
    Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
    testTag1WithChannels.setChannels(testChannels());

    // Create the testTag0WithChannels
    tagManager.create(testTag0WithChannels.getName(), copy(testTag0WithChannels));
    // update the testTag0WithChannels with testTag1WithChannels
    tagManager.create(testTag0WithChannels.getName(), copy(testTag1WithChannels));
    try {
      Tag foundTag = tagRepository.findById(testTag1WithChannels.getName(), true).get();
      Assertions.assertFalse(
          tagRepository.existsById("testTag0WithChannels"),
          "Failed to rename the Tag - the old tag still exists");
      Assertions.assertTrue(
          tagRepository.existsById("testTag1WithChannels"),
          "Failed to rename the Tag - the new tag does not exists");
      Assertions.assertSame(
          2,
          tagRepository.findById("testTag1WithChannels", true).get().getChannels().size(),
          "Failed to rename the Tag - the new tag does have the channels");
    } catch (Exception e) {
      fail("Failed to create/find the tag w/ channels");
    }
    Assertions.assertFalse(
        tagRepository.existsById(testTag0WithChannels.getName()), "Failed to replace the old tag");
  }

  /** create multiple tags */
  @Test
  void createXmlTags() {

    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");
    Tag testTag2 = new Tag("testTag2", "testOwner");

    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(testChannels());
    Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
    testTag1WithChannels.setChannels(testChannels());
    Tag testTag2WithChannels = new Tag("testTag2WithChannels", "testOwner");
    testTag2WithChannels.setChannels(testChannels());

    List<Tag> testTags =
        Arrays.asList(
            testTag0,
            testTag1,
            testTag2,
            testTag0WithChannels,
            testTag1WithChannels,
            testTag2WithChannels);

    tagManager.create(copy(testTags));
    List<Tag> foundTags = new ArrayList<Tag>();
    testTags.forEach(tag -> foundTags.add(tagRepository.findById(tag.getName(), true).get()));
    Assertions.assertTrue(foundTags.contains(testTag0), "Failed to create the tags testTag0 ");
    Assertions.assertTrue(foundTags.contains(testTag1), "Failed to create the tags testTag1 ");
    Assertions.assertTrue(foundTags.contains(testTag2), "Failed to create the tags testTag2 ");
    // Check for creation of tags with channels
    testTags.stream()
        .filter(tag -> tag.getName().endsWith("WithChannels"))
        .forEach(
            (t) -> {
              Optional<Tag> testTagWithchannels = tagRepository.findById(t.getName(), true);
              Assertions.assertTrue(
                  testTagWithchannels.isPresent(), "failed to create test tag : " + t.getName());
              Assertions.assertSame(
                  2, t.getChannels().size(), "failed to create tag with channels : " + t.getName());
            });
  }

  /**
   * create by overriding multiple tags
   *
   * <p>attempting to change owners will have no effect changing channels will have an effect
   */
  @Test
  void createXmlTagsWithOverride() {
    Tag testTag0 = new Tag("testTag0", "testOwner");

    Channel testChannel1 = testChannels().get(1);

    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(Arrays.asList(testChannel1));

    List<Tag> testTags = Arrays.asList(testTag0, testTag0WithChannels);

    // Create a set of original tags to be overriden
    tagManager.create("testTag0", copy(testTag0));
    tagManager.create("testTag0WithChannels", copy(testTag0WithChannels));
    // Now update the test tags
    testTag0.setOwner("testOwner-updated");
    testTag0WithChannels.setOwner("testOwner-updated");
    testTag0WithChannels.setChannels(Arrays.asList(testChannel1));

    List<Tag> updatedTestTags = Arrays.asList(testTag0, testTag0WithChannels);
    Iterable<Tag> createdTags = tagManager.create(copy(updatedTestTags));

    // set owner back to original since it shouldn't change
    testTag0.setOwner("testOwner");
    testTag0WithChannels.setOwner("testOwner");
    // verify the tags were updated as expected
    Optional<Tag> foundTag = tagRepository.findById(testTag0.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent() && foundTag.get().equals(testTag0),
        "Failed to update tag " + testTag0);
    foundTag = tagRepository.findById(testTag0WithChannels.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent() && foundTag.get().getChannels().size() == 1,
        "Failed to update tag " + testTag0WithChannels);

    testTag0WithChannels.setChannels(new ArrayList<Channel>());
    testChannel1.setTags(Arrays.asList(testTag0WithChannels));
    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
    params.add("~tag", testTag0WithChannels.getName());
    // verify the tag was removed from the old channels
    Assertions.assertEquals(
        Arrays.asList(testChannel1),
        channelRepository.search(params).channels(),
        "Failed to change the channels the tag is attached to correctly");
  }

  /** add a single tag to a single channel */
  @Test
  void addSingleXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    tagRepository.index(testTag0);

    tagManager.addSingle(testTag0.getName(), "testChannel0");
    Assertions.assertTrue(
        channelRepository.findById("testChannel0").get().getTags().stream()
            .anyMatch(t -> t.getName().equals(testTag0.getName())),
        "Failed to add tag");
  }

  /** update a tag */
  @Test
  void updateXmlTag() {
    // A test tag with only name and owner
    Tag testTag0 = new Tag("testTag0", "testOwner");
    // A test tag with name, owner, and a single test channel
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(Arrays.asList(testChannels().get(0)));

    // Update on a non-existing tag should result in the creation of that tag
    // 1. Test a simple tag
    Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
    Assertions.assertEquals(testTag0, returnedTag, "Failed to update tag " + testTag0);
    Assertions.assertEquals(
        testTag0,
        tagRepository.findById(testTag0.getName()).get(),
        "Failed to update tag " + testTag0);
    // 2. Test a tag with channels
    returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
    Assertions.assertTrue(
        returnedTag.getName().equalsIgnoreCase(testTag0WithChannels.getName())
            && returnedTag.getChannels().size() == 1,
        "Failed to update tag " + testTag0WithChannels);
    Assertions.assertSame(
        1,
        tagRepository.findById(testTag0WithChannels.getName(), true).get().getChannels().size(),
        "Failed to update tag " + testTag0WithChannels);

    // Update the tag owner
    testTag0.setOwner("newTestOwner");
    returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
    Assertions.assertEquals(testTag0, returnedTag, "Failed to update tag " + testTag0);
    Assertions.assertEquals(
        testTag0,
        tagRepository.findById(testTag0.getName()).get(),
        "Failed to update tag " + testTag0);
    testTag0WithChannels.setOwner("newTestOwner");
    returnedTag = tagManager.update(testTag0WithChannels.getName(), copy(testTag0WithChannels));
    Assertions.assertTrue(
        returnedTag.getName().equalsIgnoreCase(testTag0WithChannels.getName())
            && returnedTag.getChannels().size() == 1,
        "Failed to update tag " + testTag0WithChannels);
    Optional<Tag> queriedTag = tagRepository.findById(testTag0WithChannels.getName(), true);
    Assertions.assertTrue(
        queriedTag.isPresent()
            && queriedTag.get().getName().equalsIgnoreCase(testTag0WithChannels.getName())
            && queriedTag.get().getChannels().size() == 1,
        "Failed to update tag " + testTag0WithChannels);
  }

  /** update a tag's name and owner on its channels */
  @Test
  void updateXmlTagOnChan() {
    // extra channel for this test
    Channel testChannelX = new Channel("testChannelX", "testOwner");
    channelRepository.index(testChannelX);
    // A test tag with name, owner, and 2 test channels
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(Arrays.asList(testChannels().get(0), testChannelX));
    // test tag with different name, owner, and 1 different channel & 1 existing channel
    Tag testTag1WithChannels = new Tag("testTag1WithChannels", "updateTestOwner");
    testTag1WithChannels.setChannels(
        Arrays.asList(testChannels().get(1), new Channel("testChannelX", "testOwner")));

    tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
    // change name and owner on existing channel, add to new channel
    tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);

    Tag expectedTag = new Tag("testTag1WithChannels", "updateTestOwner");
    expectedTag.setChannels(
        Arrays.asList(
            new Channel("testChannel0", "testOwner"),
            new Channel("testChannel1", "testOwner"),
            new Channel("testChannelX", "testOwner")));
    // verify that the old tag "testTag0WithChannels" was replaced with the new
    // "testTag1WithChannels" and lists of channels were combined

    Optional<Tag> foundTag = tagRepository.findById(testTag1WithChannels.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent()
            && foundTag.get().getName().equalsIgnoreCase("testTag1WithChannels")
            && foundTag.get().getChannels().size() == 3,
        "Failed to update the tag");

    // verify that the old tag is no longer present
    Assertions.assertFalse(
        tagRepository.existsById(testTag0WithChannels.getName()), "Failed to replace the old tag");

    expectedTag = new Tag("testTag1WithChannels", "updateTestOwner");
    // test tag of old channel not in update
    Assertions.assertTrue(
        channelRepository
            .findById(testChannels().get(0).getName())
            .get()
            .getTags()
            .contains(expectedTag),
        "The tag attached to the channel "
            + testChannels().get(0).toString()
            + " doesn't match the new tag");
    // test tag of old channel and in update
    Assertions.assertTrue(
        channelRepository
            .findById(testChannels().get(1).getName())
            .get()
            .getTags()
            .contains(expectedTag),
        "The tag attached to the channel "
            + testChannels().get(1).toString()
            + " doesn't match the new tag");
    // test tag of new channel
    Assertions.assertTrue(
        channelRepository.findById(testChannelX.getName()).get().getTags().contains(expectedTag),
        "The tag attached to the channel " + testChannelX + " doesn't match the new tag");

    // clean extra channel
    channelRepository.deleteById(testChannelX.getName());
  }

  /** Rename a tag using update */
  @Test
  void renameByUpdateXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(testChannels());
    Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
    testTag1WithChannels.setChannels(testChannels());

    // Create the original tags
    Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
    Tag createdTagWithChannels =
        tagManager.create(testTag0WithChannels.getName(), testTag0WithChannels);
    // update the tags with new names, 0 -> 1
    Tag updatedTag = tagManager.update(testTag0.getName(), testTag1);
    Tag updatedTagWithChannels =
        tagManager.update(testTag0WithChannels.getName(), testTag1WithChannels);

    // verify that the old tag "testTag0" was replaced with the new "testTag1"
    Optional<Tag> foundTag = tagRepository.findById(testTag1.getName());
    Assertions.assertTrue(foundTag.isPresent(), "Failed to update the tag");
    // verify that the old tag is no longer present
    Assertions.assertFalse(
        tagRepository.existsById(testTag0.getName()), "Failed to replace the old tag");

    // verify that the old tag "testTag0WithChannels" was replaced with the new
    // "testTag1WithChannels"
    foundTag = tagRepository.findById(testTag1WithChannels.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent() && foundTag.get().getChannels().size() == 2,
        "Failed to update the tag w/ channels");
    // verify that the old tag is no longer present
    Assertions.assertFalse(
        tagRepository.existsById(testTag0WithChannels.getName()), "Failed to replace the old tag");

    // TODO add test for failure case
  }

  /**
   * Update the channels associated with a tag Existing tag channels: none | update tag channels:
   * testChannel0 Resultant tag channels: testChannel0
   */
  @Test
  void updateTagTest1() {
    // A test tag with only name and owner
    Tag testTag0 = new Tag("testTag0", "testOwner");
    tagManager.create(testTag0.getName(), testTag0);
    // Updating a tag with no channels, the new channels should be added to the tag
    // Add testChannel0 to testTag0 which has no channels
    testTag0.setChannels(Arrays.asList(testChannels().get(0)));
    Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
    Assertions.assertEquals(
        returnedTag,
        tagRepository.findById(testTag0.getName(), true).get(),
        "Failed to update tag " + returnedTag);
  }

  /**
   * Update the channels associated with a tag Existing tag channels: testChannel0 | update tag
   * channels: testChannel1 Resultant tag channels: testChannel0,testChannel1
   */
  @Test
  void updateTagTest2() {
    // A test tag with testChannel0
    Tag testTag0 = new Tag("testTag0", "testOwner");
    testTag0.setChannels(Arrays.asList(testChannels().get(0)));
    Tag createdTag = tagManager.create(testTag0.getName(), testTag0);
    Assertions.assertSame(1, createdTag.getChannels().size(), "Failed to update tag " + testTag0);
    // Updating a tag with existing channels, the new channels should be added without affecting
    // existing channels
    // testTag0 already has testChannel0, the update operation should append the testChannel1 while
    // leaving the existing channel unaffected.
    testTag0.setChannels(Arrays.asList(testChannels().get(1)));
    Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));

    // Query ChannelFinder and verify updated channels and tags
    Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();
    Assertions.assertSame(2, foundTag.getChannels().size(), "Failed to update tag " + testTag0);
  }

  /**
   * Update the channels associated with a tag. Existing tag channels: testChannel0 | update tag
   * channels: testChannel0,testChannel1 Resultant tag channels: testChannel0,testChannel1
   */
  @Test
  void updateTagTest3() {
    // A test tag with testChannel0
    Tag testTag0 = new Tag("testTag0", "testOwner");
    testTag0.setChannels(Arrays.asList(testChannels().get(0)));

    tagManager.create(testTag0.getName(), testTag0);
    // testTag0 already has testChannel0, the update request (which repeats the testChannel0) should
    // append
    // the testChannel1 while leaving the existing channel unaffected.
    testTag0.setChannels(testChannels());
    tagManager.update(testTag0.getName(), copy(testTag0));

    // Query ChannelFinder and verify updated channels and tags
    Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();

    Tag expectedChannelTag = new Tag("testTag0", "testOwner");
    Assertions.assertTrue(
        foundTag
            .getChannels()
            .containsAll(
                Arrays.asList(
                    new Channel(
                        "testChannel0", "testOwner", EMPTY_LIST, Arrays.asList(expectedChannelTag)),
                    new Channel(
                        "testChannel1",
                        "testOwner",
                        EMPTY_LIST,
                        Arrays.asList(expectedChannelTag)))),
        "Failed to update tag " + testTag0.toLog());
  }

  /**
   * Update the channels associated with a tag Existing tag channels: testChannel0,testChannel1 |
   * update tag channels: testChannel0,testChannel1 Resultant tag channels:
   * testChannel0,testChannel1
   */
  @Test
  void updateTagTest4() {
    // A test tag with testChannel0,testChannel1
    Tag testTag0 = new Tag("testTag0", "testOwner");
    testTag0.setChannels(testChannels());

    // Updating a tag with existing channels, the new channels should be added without affecting
    // existing channels
    // testTag0 already has testChannel0 & testChannel1, the update request should be a NOP.
    testTag0.setChannels(testChannels());
    Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
    Assertions.assertSame(2, returnedTag.getChannels().size(), "Failed to update tag " + testTag0);

    // Query ChannelFinder and verify updated channels and tags
    Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();
    Tag expectedTag = tagManager.create(testTag0.getName(), testTag0);
    expectedTag.setChannels(
        Arrays.asList(
            new Channel(
                "testChannel0",
                "testOwner",
                EMPTY_LIST,
                Arrays.asList(new Tag("testTag0", "testOwner"))),
            new Channel(
                "testChannel1",
                "testOwner",
                EMPTY_LIST,
                Arrays.asList(new Tag("testTag0", "testOwner")))));
    Assertions.assertEquals(expectedTag, foundTag, "Failed to update tag " + testTag0);
  }

  /**
   * Update the channels associated with a tag Existing tag channels: testChannel0,testChannel1 |
   * update tag channels: testChannel0 Resultant tag channels: testChannel0,testChannel1
   */
  @Test
  void updateTagTest5() {
    // A test tag with testChannel0,testChannel1
    Tag testTag0 = new Tag("testTag0", "testOwner");
    testTag0.setChannels(testChannels());
    Tag expectedTag = tagManager.create(testTag0.getName(), testTag0);

    // Updating a tag with existing channels, the new channels should be added without affecting
    // existing channels
    // testTag0 already has testChannel0 & testChannel1, the update operation should be a NOP.
    testTag0.setChannels(Arrays.asList(testChannels().get(0)));
    Tag returnedTag = tagManager.update(testTag0.getName(), copy(testTag0));
    Assertions.assertSame(2, returnedTag.getChannels().size(), "Failed to update tag " + testTag0);
    // Query ChannelFinder and verify updated channels and tags
    Tag foundTag = tagRepository.findById(testTag0.getName(), true).get();

    expectedTag.setChannels(Arrays.asList(new Channel("testChannel0", "testOwner")));
    expectedTag.setChannels(
        Arrays.asList(
            new Channel(
                "testChannel0",
                "testOwner",
                EMPTY_LIST,
                Arrays.asList(new Tag("testTag0", "testOwner"))),
            new Channel(
                "testChannel1",
                "testOwner",
                EMPTY_LIST,
                Arrays.asList(new Tag("testTag0", "testOwner")))));
    Assertions.assertEquals(expectedTag, foundTag, "Failed to update tag " + testTag0);
  }

  /** Update multiple tags Update on non-existing tags should result in the creation of the tags */
  @Test
  void updateMultipleTags() {
    // A test tag with only name and owner
    Tag testTag0 = new Tag("testTag0", "testOwner");
    // A test tag with name, owner, and test channels
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(testChannels());

    Iterable<Tag> returnedTag =
        tagManager.update(Arrays.asList(copy(testTag0), copy(testTag0WithChannels)));
    // Query ChannelFinder and verify updated channels and tags
    Optional<Tag> foundTag = tagRepository.findById(testTag0.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent() && foundTag.get().equals(testTag0),
        "Failed to update tag " + testTag0.toLog());
    foundTag = tagRepository.findById(testTag0WithChannels.getName(), true);
    Assertions.assertTrue(
        foundTag.isPresent() && foundTag.get().getChannels().size() == 2,
        "Failed to update tag with channels " + testTag0WithChannels.toLog());
  }

  /** update tags' names and attempt to change owners on their channels */
  @Test
  void updateMultipleXmlTagsOnChan() {
    // extra channel for this test
    Channel testChannelX = new Channel("testChannelX", "testOwner");
    channelRepository.index(testChannelX);

    // 2 test tags with name, owner, and channels
    Tag testTag0WithChannels = new Tag("testTag0WithChannels", "testOwner");
    testTag0WithChannels.setChannels(Arrays.asList(testChannels().get(0), testChannelX));
    Tag testTag1WithChannels = new Tag("testTag1WithChannels", "testOwner");
    testTag1WithChannels.setChannels(Arrays.asList(testChannels().get(1), testChannelX));

    tagManager.create(Arrays.asList(testTag0WithChannels, testTag1WithChannels));
    // change owners and add channels
    testTag0WithChannels.setOwner("updateTestOwner");
    testTag0WithChannels.setChannels(Arrays.asList(testChannels().get(1), testChannelX));
    testTag1WithChannels.setOwner("updateTestOwner");
    testTag1WithChannels.setChannels(Arrays.asList(testChannels().get(0), testChannelX));

    // update both tags
    tagManager.update(Arrays.asList(testTag0WithChannels, testTag1WithChannels));

    // verify that the tags were updated
    Tag foundTag0 = tagRepository.findById(testTag0WithChannels.getName(), true).get();
    Assertions.assertSame(
        3,
        foundTag0.getChannels().size(),
        "Failed to update the tag " + testTag0WithChannels.getName());
    Tag foundTag1 = tagRepository.findById(testTag1WithChannels.getName(), true).get();
    Assertions.assertSame(
        3,
        foundTag0.getChannels().size(),
        "Failed to update the tag " + testTag0WithChannels.getName());

    Tag expectedTag0 = new Tag("testTag0WithChannels", "testOwner");
    Tag expectedTag1 = new Tag("testTag1WithChannels", "testOwner");
    List<Tag> expectedTags = Arrays.asList(expectedTag0, expectedTag1);
    // check if tags attached to channels are correct
    // test tag of channel0
    Assertions.assertEquals(
        expectedTags,
        channelRepository.findById(testChannels().get(0).getName()).get().getTags(),
        "The tags attached to the channel "
            + testChannels().get(0).toString()
            + " doesn't match the expected tags");
    // test tag of channel1 (tags need to be sorted because they are not sorted when gotten from
    // channel)
    List<Tag> sortedTags =
        channelRepository.findById(testChannels().get(1).getName()).get().getTags();
    sortedTags.sort(Comparator.comparing(Tag::getName));
    Assertions.assertEquals(
        expectedTags,
        sortedTags,
        "The tags attached to the channel "
            + testChannels().get(1).toString()
            + " doesn't match the expected tags");
    // test tag of channelX
    Assertions.assertEquals(
        expectedTags,
        channelRepository.findById(testChannelX.getName()).get().getTags(),
        "The tags attached to the channel " + testChannelX + " doesn't match the expected tags");
  }

  /** delete a single tag */
  @Test
  void deleteXmlTag() {
    Tag testTag0 = new Tag("testTag0", "testOwner");
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(testChannels());
    List<Tag> testTags = Arrays.asList(testTag0, testTag1);

    Iterable<Tag> createdTags = tagManager.create(testTags);

    tagManager.remove(testTag0.getName());
    // verify the tag was deleted as expected
    Assertions.assertFalse(
        tagRepository.existsById(testTag0.getName()), "Failed to delete the tag");

    tagManager.remove(testTag1.getName());
    MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
    params.add("~tag", testTag1.getName());
    // verify the tag was deleted and removed from all associated channels
    Assertions.assertFalse(
        tagRepository.existsById(testTag1.getName()), "Failed to delete the tag");
    Assertions.assertEquals(
        new ArrayList<Channel>(),
        channelRepository.search(params).channels(),
        "Failed to delete the tag from channels");
  }

  /** delete a single tag from a single channel */
  @Test
  void deleteXmlTagFromChannel() {
    Tag testTag1 = new Tag("testTag1", "testOwner");
    testTag1.setChannels(testChannels());

    Tag createdTag = tagManager.create(testTag1.getName(), testTag1);

    tagManager.removeSingle(testTag1.getName(), testChannels().get(0).getName());
    // verify the tag was only removed from the single test channel
    Assertions.assertTrue(
        tagRepository.existsById(testTag1.getName()), "Failed to not delete the tag");

    // Verify the tag is removed from the testChannel0
    MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
    searchParameters.add("~tag", testTag1.getName());
    Assertions.assertFalse(
        channelRepository.search(searchParameters).channels().stream()
            .anyMatch(
                ch -> {
                  return ch.getName().equals(testChannels().get(0).getName());
                }),
        "Failed to delete the tag from channel");
  }

  // A set of test channels populated into channelfinder for test purposes. These
  // channels are added and removed before each test

  private static List<Channel> testChannels() {
    return Arrays.asList(
        new Channel("testChannel0", "testOwner"), new Channel("testChannel1", "testOwner"));
  }

  @BeforeEach
  public void setup() {
    channelRepository.indexAll(testChannels());
  }

  @AfterEach
  public void cleanup() {

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.set("~name", "*");
    channelRepository
        .search(map)
        .channels()
        .forEach(c -> channelRepository.deleteById(c.getName()));
    tagRepository.findAll().forEach(t -> tagRepository.deleteById(t.getName()));
  }

  /**
   * Compare the two tags ignoring the order of the associated channels
   *
   * @param tag1
   * @param tag2
   * @return true is the tags match
   */
  private static boolean tagCompare(Tag tag1, Tag tag2) {
    if (!(tag1.getName().equals(tag2.getName())) || !(tag1.getOwner().equals(tag2.getOwner())))
      return false;
    return tag1.getChannels().containsAll(tag2.getChannels())
        && tag2.getChannels().containsAll(tag1.getChannels());
  }

  private static Tag copy(Tag tag) {
    Tag copy = new Tag(tag.getName(), tag.getOwner());
    List<Channel> channels = new ArrayList<Channel>();
    tag.getChannels().forEach(chan -> channels.add(new Channel(chan.getName(), chan.getOwner())));
    copy.setChannels(channels);
    return copy;
  }

  private static List<Tag> copy(List<Tag> tags) {
    List<Tag> copy = new ArrayList<Tag>();
    tags.forEach(tag -> copy.add(copy(tag)));
    return copy;
  }
}
