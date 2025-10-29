package org.phoebus.channelfinder.processors.aa;

import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.inactiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.paramableAAChannelProcessorTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.entity.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(
    locations = "classpath:application_test.properties",
    properties = "aa.auto_pause=archive")
class AAChannelProcessorTagPauseIT {

  @Autowired AAChannelProcessor aaChannelProcessor;

  MockWebServer mockArchiverAppliance;
  ObjectMapper objectMapper;

  private static Stream<Arguments> processNoPauseSource() {

    return Stream.of(
        Arguments.of(
            new Channel(
                "PVArchivedInactive",
                "owner",
                List.of(archiveProperty, inactiveProperty),
                List.of()),
            "Being archived",
            "",
            ""),
        Arguments.of(
            new Channel("PVArchivedNotag", "owner", List.of(), List.of()),
            "Being archived",
            "pauseArchivingPV",
            "[\"PVArchivedNotag\"]"));
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

  @ParameterizedTest
  @MethodSource("processNoPauseSource")
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
