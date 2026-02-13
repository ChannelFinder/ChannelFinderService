package org.phoebus.channelfinder.processors.aa;

import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.inactiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.paramableAAChannelProcessorTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(
    locations = "classpath:application_aa_proc_test.properties",
    properties = "aa.auto_pause=archive")
class AAChannelProcessorTagPauseIT {

  @Autowired AAChannelProcessor aaChannelProcessor;

  @MockitoBean ArchiverService archiverService;

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

  @ParameterizedTest
  @MethodSource("processNoPauseSource")
  void testProcessNotArchivedActive(
      Channel channel, String archiveStatus, String archiverEndpoint, String submissionBody)
      throws JsonProcessingException {
    paramableAAChannelProcessorTest(
        archiverService, aaChannelProcessor, List.of(channel), archiveStatus, archiverEndpoint);
  }
}
