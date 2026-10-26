package org.phoebus.channelfinder.processors.aa;

import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.core.JacksonException;

@WebMvcTest(AAChannelProcessor.class)
@ExtendWith(MockitoExtension.class)
@TestPropertySource(value = "classpath:application_aa_proc_test.properties")
class AAChannelProcessorIT extends AAChannelProcessorBaseIT {

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

  @Test
  void testProcessNoPVs() throws JacksonException {
    aaChannelProcessor.process(List.of());
  }

  @ParameterizedTest
  @MethodSource("processSource")
  void testProcessNotArchivedActive(Channel channel, String archiveStatus, String archiverEndpoint)
      throws JacksonException {
    paramableAAChannelProcessorTest(List.of(channel), archiveStatus, archiverEndpoint);
  }
}
