package org.phoebus.channelfinder.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.processors.aa.AAChannelProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(value = "classpath:application_test.properties")
class AAChannelProcessorIT {

  protected static Property archiveProperty = new Property("archive", "owner", "default");
  protected static Property activeProperty = new Property("pvStatus", "owner", "Active");
  protected static Property inactiveProperty = new Property("pvStatus", "owner", "Inactive");

  @Autowired AAChannelProcessor aaChannelProcessor;

  MockWebServer mockArchiverAppliance;
  ObjectMapper objectMapper;

  @NotNull
  private static Stream<Arguments> processSource() {
    return Stream.of(
        Arguments.of(
            new Channel(
                "PVArchivedActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            "Being archived",
            "",
            ""),
        Arguments.of(
            new Channel(
                "PVPausedActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            "Paused",
            "resumeArchivingPV",
            "[\"PVPausedActive\"]"),
        Arguments.of(
            new Channel(
                "PVNoneActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            "Not being archived",
            "archivePV",
            "[{\"pv\":\"PVNoneActive\"}]"),
        Arguments.of(
            new Channel(
                "PVArchivedInactive",
                "owner",
                List.of(archiveProperty, inactiveProperty),
                List.of()),
            "Being archived",
            "pauseArchivingPV",
            "[\"PVArchivedInactive\"]"),
        Arguments.of(
            new Channel(
                "PVPausedInactive", "owner", List.of(archiveProperty, inactiveProperty), List.of()),
            "Paused",
            "",
            ""),
        Arguments.of(
            new Channel(
                "PVNoneInactive", "owner", List.of(archiveProperty, inactiveProperty), List.of()),
            "Not being archived",
            "",
            ""),
        Arguments.of(
            new Channel("PVArchivedNotag", "owner", List.of(), List.of()),
            "Being archived",
            "pauseArchivingPV",
            "[\"PVArchivedNotag\"]"));
  }

  public static void paramableAAChannelProcessorTest(
      MockWebServer mockArchiverAppliance,
      ObjectMapper objectMapper,
      ChannelProcessor aaChannelProcessor,
      List<Channel> channels,
      String archiveStatus,
      String archiverEndpoint,
      String submissionBody)
      throws JsonProcessingException, InterruptedException {
    // Request to version
    Map<String, String> versions = Map.of("mgmt_version", "Archiver Appliance Version 1.1.0");
    mockArchiverAppliance.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(versions))
            .addHeader("Content-Type", "application/json"));

    // Request to policies
    Map<String, String> policyList = Map.of("policy", "description");
    mockArchiverAppliance.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(policyList))
            .addHeader("Content-Type", "application/json"));

    if (!archiveStatus.isEmpty()) {

      // Request to archiver status
      List<Map<String, String>> archivePVStatuses =
          channels.stream()
              .map(channel -> Map.of("pvName", channel.getName(), "status", archiveStatus))
              .toList();
      mockArchiverAppliance.enqueue(
          new MockResponse()
              .setBody(objectMapper.writeValueAsString(archivePVStatuses))
              .addHeader("Content-Type", "application/json"));
    }
    if (!archiverEndpoint.isEmpty()) {
      // Request to archiver to archive
      List<Map<String, String>> archiverResponse =
          channels.stream()
              .map(
                  channel ->
                      Map.of("pvName", channel.getName(), "status", "Archive request submitted"))
              .toList();
      mockArchiverAppliance.enqueue(
          new MockResponse()
              .setBody(objectMapper.writeValueAsString(archiverResponse))
              .addHeader("Content-Type", "application/json"));
    }

    long count = aaChannelProcessor.process(channels);
    assertEquals(count, archiverEndpoint.isEmpty() ? 0 : channels.size());

    int expectedRequests = 1;
    RecordedRequest requestVersion = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
    assert requestVersion != null;
    assertEquals("/mgmt/bpl/getVersions", requestVersion.getPath());

    expectedRequests += 1;
    RecordedRequest requestPolicy = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
    assert requestPolicy != null;
    assertEquals("/mgmt/bpl/getPolicyList", requestPolicy.getPath());

    if (!archiveStatus.isEmpty()) {
      expectedRequests += 1;
      RecordedRequest requestStatus = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
      assert requestStatus != null;
      assert requestStatus.getRequestUrl() != null;
      assertEquals("/mgmt/bpl/getPVStatus", requestStatus.getRequestUrl().encodedPath());
    }

    if (!archiverEndpoint.isEmpty()) {
      expectedRequests += 1;
      RecordedRequest requestAction = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
      assert requestAction != null;
      assertEquals("/mgmt/bpl/" + archiverEndpoint, requestAction.getPath());
      assertEquals(submissionBody, requestAction.getBody().readUtf8());
    }

    assertEquals(mockArchiverAppliance.getRequestCount(), expectedRequests);
  }

  @BeforeEach
  void setUp() throws IOException {
    mockArchiverAppliance = new MockWebServer();
    mockArchiverAppliance.start(17665);

    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void teardown() throws IOException {
    mockArchiverAppliance.shutdown();
  }

  @Test
  void testProcessNoPVs() throws JsonProcessingException {
    aaChannelProcessor.process(List.of());

    assertEquals(mockArchiverAppliance.getRequestCount(), 0);
  }

  @ParameterizedTest
  @MethodSource("processSource")
  void testProcessNotArchivedActive(
      Channel channel, String archiveStatus, String archiverEndpoint, String submissionBody)
      throws JsonProcessingException, InterruptedException {
    paramableAAChannelProcessorTest(
        mockArchiverAppliance,
        objectMapper,
        aaChannelProcessor,
        List.of(channel),
        archiveStatus,
        archiverEndpoint,
        submissionBody);
  }
}
