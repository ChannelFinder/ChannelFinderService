package org.phoebus.channelfinder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.phoebus.channelfinder.entity.SearchResult;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelRepository.class)
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelRepositoryIT {

  @Autowired ElasticConfig esService;

  @Autowired ChannelRepository channelRepository;

  @Autowired TagRepository tagRepository;

  @Autowired PropertyRepository propertyRepository;

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
  }

  /** index a single channel */
  @Test
  void indexXmlChannel() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel);

    Channel createdChannel = channelRepository.index(testChannel);
    // verify the channel was created as expected
    Assertions.assertEquals(testChannel, createdChannel, "Failed to create the channel");
  }

  /** index multiple channels */
  @Test
  void indexXmlChannels() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    cleanupTestChannels = testChannels;

    Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
    // verify the channels were created as expected
    Assertions.assertTrue(
        Iterables.elementsEqual(testChannels, createdChannels), "Failed to create the channels");
  }

  /** save a single channel */
  @Test
  void saveXmlChannel() {
    Channel testChannel = new Channel("testChannel", "testOwner");
    Channel updateTestChannel =
        new Channel(
            "testChannel", "updateTestOwner", testProperties.subList(0, 1), testTags.subList(0, 1));
    Channel updateTestChannel1 =
        new Channel(
            "testChannel", "updateTestOwner", testProperties.subList(1, 2), testTags.subList(1, 2));
    Channel updateTestChannel2 =
        new Channel(
            "updateTestChannel1", "updateTestOwner1", testUpdatedProperties, testUpdatedTags);
    Channel createdChannel = channelRepository.index(testChannel);
    cleanupTestChannels =
        Arrays.asList(testChannel, updateTestChannel, updateTestChannel1, updateTestChannel2);

    // Update Channel with new owner a new property and a new tag
    Channel updatedTestChannel = channelRepository.save(updateTestChannel);
    // verify that the channel was updated as expected
    Assertions.assertEquals(
        updateTestChannel, updatedTestChannel, "Failed to update the channel with the same name");

    // Update Channel with a second property and tag
    Channel updatedTestChannel1 = channelRepository.save(updateTestChannel1);
    // verify that the channel was updated with the new tags and properties while preserving the old
    // ones
    Channel expectedChannel = new Channel("testChannel", "updateTestOwner");
    expectedChannel.addProperties(testProperties);
    expectedChannel.addTags(testTags);
    Assertions.assertEquals(
        updateTestChannel, updatedTestChannel, "Failed to update the channel with the same name");
  }

  /** save multiple channels */
  @Test
  void saveXmlChannels() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    Channel updateTestChannel =
        new Channel("testChannel", "updateTestOwner", testUpdatedProperties, testUpdatedTags);
    Channel updateTestChannel1 =
        new Channel("testChannel1", "updateTestOwner1", testUpdatedProperties, testUpdatedTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    List<Channel> updateTestChannels = Arrays.asList(updateTestChannel, updateTestChannel1);
    Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
    cleanupTestChannels =
        Arrays.asList(testChannel, testChannel1, updateTestChannel, updateTestChannel1);

    Iterable<Channel> updatedTestChannels = channelRepository.saveAll(updateTestChannels);
    // verify the channels were updated as expected
    List<Channel> expectedChannels =
        Arrays.asList(
            new Channel("testChannel", "updateTestOwner", testUpdatedProperties, testUpdatedTags),
            new Channel(
                "testChannel1", "updateTestOwner1", testUpdatedProperties, testUpdatedTags));
    Assertions.assertEquals(
        expectedChannels, updatedTestChannels, "Failed to update the channels: ");
  }

  /** find a single channel */
  @Test
  void findXmlChannel() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    cleanupTestChannels = Arrays.asList(testChannel);

    Optional<Channel> notFoundChannel = channelRepository.findById(testChannel.getName());
    // verify the channel was not found as expected
    Assertions.assertNotEquals(Optional.of(testChannel), notFoundChannel, "Found the channel");

    Channel createdChannel = channelRepository.index(testChannel);

    Optional<Channel> foundChannel = channelRepository.findById(createdChannel.getName());
    // verify the channel was found as expected
    if (foundChannel.isPresent()) {
      Assertions.assertEquals(createdChannel, foundChannel.get(), "Failed to find the channel");
    } else Assertions.fail("Failed to find the channel");
  }

  /** check if a channel exists */
  @Test
  void testChannelExists() {
    Iterable<Tag> createdTags = tagRepository.indexAll(testTags);
    Iterable<Property> createdProperties = propertyRepository.indexAll(testProperties);
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel createdChannel = channelRepository.index(testChannel);
    cleanupTestChannels = Arrays.asList(testChannel);

    // verify the channel exists as expected
    Assertions.assertTrue(
        channelRepository.existsById(testChannel.getName()),
        "Failed to check the existance of " + testChannel.getName());
    // verify the channel does not exist, as expected
    Assertions.assertFalse(
        channelRepository.existsById("non-existant-channel"),
        "Failed to check the non-existance of 'non-existant-channel'");
  }

  /** check if channels exist */
  @Test
  void testChannelsExist() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
    cleanupTestChannels = Arrays.asList(testChannel, testChannel1);

    // verify the channels exist as expected
    Assertions.assertTrue(
        channelRepository.existsByIds(Arrays.asList("testChannel", "testChannel1")),
        "Failed to check the existance of channels");
    // verify the channel does not exist, as expected
    Assertions.assertFalse(
        channelRepository.existsByIds(Arrays.asList("test-channel1", "non-existant-channel")),
        "Failed to check the non-existance of 'non-existant-channel'");
  }

  /** find multiple channels */
  @Test
  void findXmlChannels() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    List<String> channelNames = Arrays.asList(testChannel.getName(), testChannel1.getName());
    Iterable<Channel> notFoundChannels = null;
    Iterable<Channel> foundChannels = null;

    try {
      notFoundChannels = channelRepository.findAllById(channelNames);
    } catch (ResponseStatusException e) {
    } finally {
      // verify the channels were not found as expected
      Assertions.assertNotEquals(testChannels, notFoundChannels, "Found the channels");
    }

    Iterable<Channel> createdChannels = channelRepository.indexAll(testChannels);
    cleanupTestChannels = Arrays.asList(testChannel, testChannel1);

    try {
      foundChannels = channelRepository.findAllById(channelNames);
    } catch (ResponseStatusException e) {
    } finally {
      // verify the channels were found as expected
      Assertions.assertEquals(createdChannels, foundChannels, "Failed to find the tags");
    }
  }

  /** find channels using case insensitive tag and property names searches */
  @Test
  void findChannels() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    SearchResult foundChannelsResponse = null;

    List<Channel> createdChannels = channelRepository.indexAll(testChannels);
    SearchResult createdSearchResult = new SearchResult(createdChannels, 2);
    cleanupTestChannels = Arrays.asList(testChannel, testChannel1);

    try {
      MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
      searchParameters.set(testProperties.get(0).getName().toLowerCase(), "*");
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on property name search (all lower case)");

      searchParameters.set(testProperties.get(0).getName().toUpperCase(), "*");
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on property name search (all upper case)");

      searchParameters.clear();
      searchParameters.set("~tag", testTags.get(0).getName().toLowerCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on tags name search (all lower case)");

      searchParameters.set("~tag", testTags.get(0).getName().toUpperCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on tags name search (all upper case)");

    } catch (ResponseStatusException e) {
      Assertions.fail(e);
    }
  }

  /** find channels using not modifier */
  @Test
  void findLackOfChannels() {
    Property extraTestProperty =
        new Property(testProperties.get(0).getName(), "testOwner", "value2");
    Channel testChannel =
        new Channel("testChannel", "testOwner", List.of(testProperties.get(0)), testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    Channel testChannel2 = new Channel("testChannel2", "testOwner2", List.of(), List.of());
    Channel testChannel3 =
        new Channel("testChannel3", "testOwner3", List.of(extraTestProperty), List.of());
    List<Channel> testChannels =
        Arrays.asList(testChannel, testChannel1, testChannel2, testChannel3);
    SearchResult foundChannelsResponse = null;

    List<Channel> createdChannels = channelRepository.indexAll(testChannels);
    SearchResult createdSearchResult = new SearchResult(createdChannels, 4);
    cleanupTestChannels = testChannels;

    try {
      MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
      searchParameters.set(testProperties.get(0).getName().toLowerCase() + "!", "*");
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          new SearchResult(List.of(createdChannels.get(2)), 1), foundChannelsResponse);

    } catch (ResponseStatusException e) {
      Assertions.fail(e);
    }
  }

  /** find channels using case insensitive names searches */
  @Test
  void findChannelByCaseInsensitiveSearch() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel testChannel1 = new Channel("testChannel1", "testOwner1", testProperties, testTags);
    List<Channel> testChannels = Arrays.asList(testChannel, testChannel1);
    SearchResult foundChannelsResponse = null;

    List<Channel> createdChannels = channelRepository.indexAll(testChannels);
    SearchResult createdSearchResult = new SearchResult(createdChannels, 2);
    cleanupTestChannels = Arrays.asList(testChannel, testChannel1);

    try {
      MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

      // Search for a single channel
      searchParameters.set("~name", "testChannel");
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          new SearchResult(List.of(testChannel), 1),
          foundChannelsResponse,
          "Failed to find the based on channel name search (exact)");

      searchParameters.set("~name", "testChannel".toLowerCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          new SearchResult(List.of(testChannel), 1),
          foundChannelsResponse,
          "Failed to find the based on channel name search (all lower case)");

      searchParameters.set("~name", "testChannel".toUpperCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          new SearchResult(List.of(testChannel), 1),
          foundChannelsResponse,
          "Failed to find the based on channel name search (all upper case)");

      // Search for multiple channels using case insensitive name searches
      searchParameters.clear();
      searchParameters.set("~name", "testChannel*");
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on channel name search (exact)");

      searchParameters.set("~name", "testChannel*".toLowerCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on channel name search (all lower case)");

      searchParameters.set("~name", "testChannel*".toUpperCase());
      foundChannelsResponse = channelRepository.search(searchParameters);
      Assertions.assertEquals(
          createdSearchResult,
          foundChannelsResponse,
          "Failed to find the based on channel name search (all upper case)");

    } catch (ResponseStatusException e) {
    }
  }

  /** delete a single tag */
  @Test
  void deleteXmlTag() {
    Channel testChannel = new Channel("testChannel", "testOwner", testProperties, testTags);
    Channel createdChannel = channelRepository.index(testChannel);
    cleanupTestChannels = Arrays.asList(testChannel);

    channelRepository.deleteById(createdChannel.getName());
    // verify the channel was deleted as expected
    Assertions.assertNotEquals(
        Optional.of(testChannel),
        channelRepository.findById(testChannel.getName()),
        "Failed to delete the channel");
  }

  /**
   * Update a channel with 1. additional list of tags and properties 2. update the values of
   * existing properties
   */
  @Test
  void updateChannelWithTagsAndProperties() {
    Channel testChannel = new Channel();
    testChannel.setName("test-channel1");
    testChannel.setOwner("test-owner");
    cleanupTestChannels = Arrays.asList(testChannel);

    List<Property> props = createTestProperties();
    try {
      testChannel.addProperty(testProperties.get(0));
      testChannel.addTag(testTags.get(0));
      Channel createdChannel = channelRepository.index(testChannel);
      // verify the tag was created as expected
      Assertions.assertEquals(
          testChannel,
          createdChannel,
          "Failed to create the test channel with a list of tags & properties");
      // update the channel with new tags and properties
      testChannel.setTags(testTags);
      testChannel.setProperties(testProperties);
      Channel updatedChannel = channelRepository.save(testChannel);
      Assertions.assertEquals(
          testChannel,
          updatedChannel,
          "Failed to create the test channel with a list of tags & properties");
      Assertions.assertTrue(
          testChannel.getTags().containsAll(testTags), "Failed updated the channel with new tags");
      Assertions.assertTrue(
          testChannel.getProperties().containsAll(testProperties),
          "Failed updated the channel with new properties");
      // update the channel with new property values
      testProperties.get(0).setValue("new-value0");
      testProperties.get(1).setValue("new-value1");
      testChannel.setProperties(testProperties);
      Channel updatedValueChannel = channelRepository.save(testChannel);
      Assertions.assertEquals(
          testChannel,
          updatedValueChannel,
          "Failed to create the test channel with a list of tags & properties");
      Assertions.assertTrue(
          testChannel.getTags().containsAll(testTags), "Failed updated the channel with new tags");
      Assertions.assertTrue(
          testChannel.getProperties().containsAll(testProperties),
          "Failed updated the channel with new properties");
    } catch (Exception e) {
    }
    props.forEach(
        testProperty -> {
          propertyRepository.deleteById(testProperty.getName());
        });
  }

  /**
   * Update a channel with partial objects, this is needed when you want to add a single tag or
   * property
   */
  @Test
  void updateChannelWithPartialObjects() {
    Channel testChannel = new Channel();
    testChannel.setName("testChannel");
    testChannel.setOwner("testOwner");
    cleanupTestChannels = Arrays.asList(testChannel);

    List<Property> props = createTestProperties();
    try {
      testChannel.addTag(testTags.get(0));
      testChannel.addProperty(testProperties.get(4));
      Channel createdChannel = channelRepository.index(testChannel);
      // verify the tag was created as expected
      Assertions.assertEquals(
          testChannel,
          createdChannel,
          "Failed to create the test channel with a list of tags & properties");
      // update the channel with new tags and properties provided via partial object

      Channel updateTestChannel = new Channel();
      updateTestChannel.setName("test-update-channel1");
      updateTestChannel.setOwner("test-owner");
      updateTestChannel.addTag(testTags.get(1));
      updateTestChannel.addProperty(testProperties.get(1));
      cleanupTestChannels.add(updateTestChannel);

      Channel updatedChannel = channelRepository.save(updateTestChannel);

      Channel expectedTestChannel = new Channel();
      expectedTestChannel.setName("test-update-channel1");
      expectedTestChannel.setOwner("test-owner");
      expectedTestChannel.addTag(testTags.get(0));
      expectedTestChannel.addTag(testTags.get(1));
      expectedTestChannel.addProperty(testProperties.get(0));
      expectedTestChannel.addProperty(testProperties.get(1));
      Assertions.assertEquals(
          expectedTestChannel,
          updatedChannel,
          "Failed to create the test channel with a list of tags & properties");
    } catch (Exception e) {
    }
    props.forEach(
        testProperty -> {
          propertyRepository.deleteById(testProperty.getName());
        });
  }

  /**
   * A utility class which will create the requested number of test properties named
   * 'test-property#'
   *
   * @return list of created properties
   */
  private List<Property> createTestProperties() {
    List<Property> testProperties = new ArrayList<Property>();
    for (int i = 0; i < 2; i++) {
      Property testProperty = new Property();
      testProperty.setName("test-property" + i);
      testProperty.setOwner("test-owner");
      testProperty.setValue("test-property" + i + "-value");
      testProperties.add(testProperty);
    }
    try {
      return Lists.newArrayList(propertyRepository.indexAll(testProperties));
    } catch (Exception e) {
      propertyRepository.deleteAll(testProperties);
      return Collections.emptyList();
    }
  }

  /**
   * A utility class which will create the requested number of test tags named 'test-tag#'
   *
   * @return list of created tags
   */
  private List<Tag> createTestTags(int count) {
    List<Tag> testTags = new ArrayList<Tag>();
    for (int i = 0; i < count; i++) {
      Tag testTag = new Tag();
      testTag.setName("test-tag" + i);
      testTag.setOwner("test-owner");
      testTags.add(testTag);
    }
    try {
      return Lists.newArrayList(tagRepository.indexAll(testTags));
    } catch (Exception e) {
      tagRepository.deleteAll(testTags);
      return Collections.emptyList();
    }
  }

  // Helper operations to create and clean up the resources needed for successful
  // testing of the channelRepository operations

  private final List<Tag> testTags =
      Arrays.asList(new Tag("testTag", "testOwner"), new Tag("testTag1", "testOwner1"));

  private final List<Tag> testUpdatedTags =
      Arrays.asList(new Tag("testTag", "updateTestOwner"), new Tag("testTag1", "updateTestOwner1"));

  private final List<Property> testProperties =
      Arrays.asList(
          new Property("testProperty", "testOwner", "value"),
          new Property("testProperty1", "testOwner1", "value"));

  private final List<Property> testUpdatedProperties =
      Arrays.asList(
          new Property("testProperty", "updateTestOwner", "updatedValue"),
          new Property("testProperty1", "updateTestOwner1", "updatedValue"));

  private List<Channel> cleanupTestChannels = Collections.emptyList();

  @BeforeEach
  public void setup() {
    tagRepository.indexAll(testTags);
    propertyRepository.indexAll(testProperties);
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
    propertyRepository.findAll().forEach(p -> propertyRepository.deleteById(p.getName()));
  }

  @AfterAll
  void tearDown() throws IOException {
    ElasticConfigIT.teardown(esService);
  }
}
