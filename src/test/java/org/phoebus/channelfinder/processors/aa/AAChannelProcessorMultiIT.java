package org.phoebus.channelfinder.processors.aa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.activeProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.inactiveProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(value = "classpath:application_aa_proc_test.properties")
class AAChannelProcessorMultiIT {

  public static final String BEING_ARCHIVED = "Being archived";
  public static final String PAUSED = "Paused";
  public static final String NOT_BEING_ARCHIVED = "Not being archived";
  public static final String OWNER = "owner";
  @Autowired AAChannelProcessor aaChannelProcessor;
  @MockitoBean ArchiverService archiverService;

  static Stream<Arguments> provideArguments() {
    List<Channel> channels =
        List.of(
            new Channel(
                "PVArchivedActive", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel(
                "PVPausedActive", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel(
                "PVNoneActive0", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel(
                "PVArchivedInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel(
                "PVPausedInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel(
                "PVNoneInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVArchivedNotag", OWNER, List.of(), List.of()),
            new Channel(
                "PVNoneActive1", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel(
                "PVNoneActive2", OWNER, List.of(archiveProperty, activeProperty), List.of()));

    Map<String, String> namesToStatuses =
        Map.of(
            "PVArchivedActive", BEING_ARCHIVED,
            "PVPausedActive", PAUSED,
            "PVNoneActive0", NOT_BEING_ARCHIVED,
            "PVArchivedInactive", BEING_ARCHIVED,
            "PVPausedInactive", PAUSED,
            "PVNoneInactive", NOT_BEING_ARCHIVED,
            "PVArchivedNotag", BEING_ARCHIVED,
            "PVNoneActive1", NOT_BEING_ARCHIVED,
            "PVNoneActive2", NOT_BEING_ARCHIVED);
    Map<ArchiveAction, List<String>> actionsToNames =
        Map.of(
            ArchiveAction.RESUME, List.of("PVPausedActive"),
            ArchiveAction.PAUSE, List.of("PVArchivedInactive", "PVArchivedNotag"),
            ArchiveAction.ARCHIVE, List.of("PVNoneActive0", "PVNoneActive1", "PVNoneActive2"));
    int expectedProcessedChannels = 6;

    List<String> massPVNames = IntStream.range(1, 100).mapToObj(i -> "PV" + i).toList();
    return Stream.of(
        Arguments.of(channels, namesToStatuses, actionsToNames, expectedProcessedChannels),
        Arguments.of(
            massPVNames.stream()
                .map(
                    s -> new Channel(s, OWNER, List.of(archiveProperty, activeProperty), List.of()))
                .toList(),
            massPVNames.stream()
                .collect(Collectors.toMap(String::toString, e -> NOT_BEING_ARCHIVED)),
            Map.of(ArchiveAction.ARCHIVE, massPVNames),
            massPVNames.size()));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testProcessMulti(
      List<Channel> channels,
      Map<String, String> namesToStatuses,
      Map<ArchiveAction, List<String>> actionsToNames,
      int expectedProcessedChannels)
      throws JsonProcessingException {

    // Mock getAAPolicies
    when(archiverService.getAAPolicies(anyString())).thenReturn(List.of("policy"));

    // Mock getStatuses
    List<Map<String, String>> archivePVStatuses =
        namesToStatuses.entrySet().stream()
            .map(entry -> Map.of("pvName", entry.getKey(), "status", entry.getValue()))
            .toList();
    when(archiverService.getStatuses(anyMap(), anyString(), anyString()))
        .thenReturn(archivePVStatuses);

    // Mock configureAA
    when(archiverService.configureAA(anyMap(), anyString()))
        .thenReturn((long) expectedProcessedChannels);

    long count = aaChannelProcessor.process(channels);
    assertEquals(expectedProcessedChannels, count);

    verify(archiverService).getAAPolicies(anyString());
    verify(archiverService).getStatuses(anyMap(), anyString(), anyString());

    ArgumentCaptor<Map<ArchiveAction, List<ArchivePVOptions>>> captor =
        ArgumentCaptor.forClass(Map.class);
    verify(archiverService).configureAA(captor.capture(), anyString());
    Map<ArchiveAction, List<ArchivePVOptions>> capturedMap = captor.getValue();

    actionsToNames.forEach(
        (action, names) -> {
          if (names.isEmpty()) {
            return;
          }
          assertTrue(capturedMap.containsKey(action));
          List<String> capturedNames =
              capturedMap.get(action).stream()
                  .map(ArchivePVOptions::getPv)
                  .collect(Collectors.toList());
          assertTrue(capturedNames.containsAll(names));
          assertTrue(names.containsAll(capturedNames));
        });
  }
}
