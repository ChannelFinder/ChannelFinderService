package org.phoebus.channelfinder.processors.aa;

import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.activeProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.aa.AAChannelProcessorIT.paramableAAChannelProcessorTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.configuration.AAChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(
    locations = "classpath:application_aa_proc_test.properties",
    properties = "aa.urls:{'default': '','aa': 'http://localhost:17665'}")
class AAChannelProcessorNoDefaultIT {
  protected static Property archiverProperty = new Property("archiver", "owner", "aa");

  @Autowired AAChannelProcessor aaChannelProcessor;

  @MockitoBean ArchiverService archiverService;

  private static Stream<Arguments> processNoPauseSource() {

    return Stream.of(
        Arguments.of(
            new Channel(
                "PVNoneActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            "",
            "",
            ""),
        Arguments.of(
            new Channel(
                "PVNoneActiveArchiver",
                "owner",
                List.of(archiveProperty, activeProperty, archiverProperty),
                List.of()),
            "Not being archived",
            "archivePV",
            "[{\"pv\":\"PVNoneActiveArchiver\"}]"));
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
