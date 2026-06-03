package org.phoebus.channelfinder.processors.aa;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.core.JacksonException;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(
    locations = "classpath:application_aa_proc_test.properties",
    properties = "aa.auto_pause=pvStatus")
class AAChannelProcessorStatusPauseIT extends AAChannelProcessorBaseIT {

  private static Stream<Arguments> processNoPauseSource() {
    return Stream.of(
        Arguments.of(
            new Channel(
                "PVArchivedInactive",
                "owner",
                List.of(archiveProperty, inactiveProperty),
                List.of()),
            "Being archived",
            "pauseArchivingPV",
            "[\"PVArchivedInactive\"]"),
        Arguments.of(new Channel("PVArchivedNotag", "owner", List.of(), List.of()), "", "", ""));
  }

  @ParameterizedTest
  @MethodSource("processNoPauseSource")
  void testProcessNotArchivedActive(
      Channel channel, String archiveStatus, String archiverEndpoint, String submissionBody)
      throws JacksonException {
    paramableAAChannelProcessorTest(List.of(channel), archiveStatus, archiverEndpoint);
  }
}
