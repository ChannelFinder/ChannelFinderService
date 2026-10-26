package org.phoebus.channelfinder.processors.aa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.JacksonException;

abstract class AAChannelProcessorBaseIT {

  protected static Property archiveProperty = new Property("archive", "owner", "default");
  protected static Property activeProperty = new Property("pvStatus", "owner", "Active");
  protected static Property inactiveProperty = new Property("pvStatus", "owner", "Inactive");

  @MockitoBean protected ArchiverService archiverService;
  @Autowired protected AAChannelProcessor aaChannelProcessor;

  @BeforeEach
  void primeCache() {
    when(archiverService.getAAPolicies(anyString())).thenReturn(List.of("policy"));
    aaChannelProcessor.scheduledPolicyRefresh();
  }

  protected void paramableAAChannelProcessorTest(
      List<Channel> channels, String archiveStatus, String archiverEndpoint)
      throws JacksonException {
    if (!archiveStatus.isEmpty()) {
      List<Map<String, String>> archivePVStatuses =
          channels.stream()
              .map(channel -> Map.of("pvName", channel.getName(), "status", archiveStatus))
              .toList();
      when(archiverService.getStatusesViaGet(anyString(), anyList())).thenReturn(archivePVStatuses);
    }

    if (!archiverEndpoint.isEmpty()) {
      when(archiverService.configureAA(anyMap(), anyString())).thenReturn((long) channels.size());
    } else {
      when(archiverService.configureAA(anyMap(), anyString())).thenReturn(0L);
    }

    long count = aaChannelProcessor.process(channels);
    assertEquals(count, archiverEndpoint.isEmpty() ? 0 : channels.size());

    if (!archiveStatus.isEmpty()) {
      verify(archiverService).getStatusesViaGet(anyString(), anyList());
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
      }
    }
  }

  private static ArchiveAction getActionFromEndpoint(String endpoint) {
    if (endpoint.contains("resumeArchivingPV")) return ArchiveAction.RESUME;
    if (endpoint.contains("pauseArchivingPV")) return ArchiveAction.PAUSE;
    if (endpoint.contains("archivePV")) return ArchiveAction.ARCHIVE;
    return null;
  }
}
