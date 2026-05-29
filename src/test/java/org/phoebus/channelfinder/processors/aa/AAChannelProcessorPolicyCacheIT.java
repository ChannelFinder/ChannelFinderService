package org.phoebus.channelfinder.processors.aa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.JacksonException;

@WebMvcTest(AAChannelProcessor.class)
@ExtendWith(MockitoExtension.class)
@TestPropertySource(value = "classpath:application_aa_proc_test.properties")
class AAChannelProcessorPolicyCacheIT {

  @MockitoBean ArchiverService archiverService;
  @Autowired AAChannelProcessor aaChannelProcessor;

  @BeforeEach
  void primeCache() {
    when(archiverService.getAAPolicies(anyString())).thenReturn(List.of("policy"));
    aaChannelProcessor.scheduledPolicyRefresh();
  }

  @Test
  void testProcessDoesNotCallGetAAPolicies() throws JacksonException {
    when(archiverService.getStatusesViaGet(anyString(), anyList()))
        .thenReturn(List.of(Map.of("pvName", "PVNoneActive", "status", "Not being archived")));
    when(archiverService.configureAA(anyMap(), anyString())).thenReturn(1L);
    clearInvocations(archiverService);

    Channel channel =
        new Channel(
            "PVNoneActive",
            "owner",
            List.of(
                new Property("archive", "owner", "default"),
                new Property("pvStatus", "owner", "Active")),
            List.of());
    aaChannelProcessor.process(List.of(channel));

    verify(archiverService, never()).getAAPolicies(anyString());
  }

  @Test
  void testProcessorInfoShowsCacheMetadata() {
    ChannelProcessorInfo info = aaChannelProcessor.processorInfo();

    assertNotEquals("never", info.properties().get("LastPolicyRefresh"));
    assertTrue(info.properties().get("CachedPoliciesPerArchiver").contains("default="));
    assertEquals("3600", info.properties().get("PolicyRefreshIntervalSeconds"));
  }

  @Test
  void testScheduledRefreshUpdatesCache() {
    when(archiverService.getAAPolicies(anyString())).thenReturn(List.of("p1", "p2", "p3"));
    clearInvocations(archiverService);

    aaChannelProcessor.scheduledPolicyRefresh();

    verify(archiverService).getAAPolicies(anyString());
    assertTrue(
        aaChannelProcessor
            .processorInfo()
            .properties()
            .get("CachedPoliciesPerArchiver")
            .contains("default=3"));
  }
}
