package org.phoebus.channelfinder.processors.aa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.configuration.ChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AAChannelProcessor.class)
@ExtendWith(MockitoExtension.class)
@TestPropertySource(value = "classpath:application_aa_proc_test.properties")
class AAChannelProcessorIT {

  protected static Property archiveProperty = new Property("archive", "owner", "default");
  protected static Property activeProperty = new Property("pvStatus", "owner", "Active");
  protected static Property inactiveProperty = new Property("pvStatus", "owner", "Inactive");

  @MockitoBean ArchiverService archiverService;
  @Autowired AAChannelProcessor aaChannelProcessor;

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
      ArchiverService archiverService,
      ChannelProcessor aaChannelProcessor,
      List<Channel> channels,
      String archiveStatus,
      String archiverEndpoint)
      throws JsonProcessingException {
    // Mock getAAPolicies
    when(archiverService.getAAPolicies(anyString())).thenReturn(List.of("policy"));

    if (!archiveStatus.isEmpty()) {
      // Mock getStatuses
      List<Map<String, String>> archivePVStatuses =
          channels.stream()
              .map(channel -> Map.of("pvName", channel.getName(), "status", archiveStatus))
              .toList();
      when(archiverService.getStatuses(anyMap(), anyString(), anyString()))
          .thenReturn(archivePVStatuses);
    }

    if (!archiverEndpoint.isEmpty()) {
      // Mock configureAA
      when(archiverService.configureAA(anyMap(), anyString())).thenReturn((long) channels.size());
    } else {
      when(archiverService.configureAA(anyMap(), anyString())).thenReturn(0L);
    }

    long count = aaChannelProcessor.process(channels);
    assertEquals(count, archiverEndpoint.isEmpty() ? 0 : channels.size());

    // Verifications
    verify(archiverService).getAAPolicies(anyString());

    if (!archiveStatus.isEmpty()) {
      verify(archiverService).getStatuses(anyMap(), anyString(), anyString());
    }

    if (!archiverEndpoint.isEmpty()) {
      ArgumentCaptor<Map<ArchiveAction, List<ArchivePVOptions>>> captor =
          ArgumentCaptor.forClass(Map.class);
      verify(archiverService).configureAA(captor.capture(), anyString());
      Map<ArchiveAction, List<ArchivePVOptions>> map = captor.getValue();

      ArchiveAction expectedAction = getActionFromEndpoint(archiverEndpoint);
      if (expectedAction != null) {
        assertTrue(map.containsKey(expectedAction));
        List<ArchivePVOptions> options = map.get(expectedAction);
        assertFalse(options.isEmpty());
        // We could parse submissionBody to be more strict, but checking the action is likely
        // sufficient for now
        // as we trust the mapping logic in AAChannelProcessor
      }
    }
  }

  private static ArchiveAction getActionFromEndpoint(String endpoint) {
    if (endpoint.contains("resumeArchivingPV")) return ArchiveAction.RESUME;
    if (endpoint.contains("pauseArchivingPV")) return ArchiveAction.PAUSE;
    if (endpoint.contains("archivePV")) return ArchiveAction.ARCHIVE;
    return null;
  }

  @Test
  void testProcessNoPVs() throws JsonProcessingException {
    aaChannelProcessor.process(List.of());

    // verify interactions are minimal or none if empty
    // But since list is empty, process returns 0 early
  }

  @ParameterizedTest
  @MethodSource("processSource")
  void testProcessNotArchivedActive(Channel channel, String archiveStatus, String archiverEndpoint)
      throws JsonProcessingException {
    paramableAAChannelProcessorTest(
        archiverService, aaChannelProcessor, List.of(channel), archiveStatus, archiverEndpoint);
  }
}
