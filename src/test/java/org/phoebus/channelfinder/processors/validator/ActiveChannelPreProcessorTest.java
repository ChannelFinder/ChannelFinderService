package org.phoebus.channelfinder.processors.validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.processor.PreProcessResult;
import org.phoebus.channelfinder.processor.validator.ActiveChannelPreProcessor;

class ActiveChannelPreProcessorTest {

  private final ActiveChannelPreProcessor preProcessor =
      new ActiveChannelPreProcessor("pvStatus", "Active");

  private static Channel channel(String name, String pvStatus, String hostName) {
    return new Channel(
        name,
        "owner",
        List.of(
            new Property("pvStatus", "owner", pvStatus),
            new Property("hostName", "owner", hostName)),
        List.of());
  }

  @Test
  void warnsWithDiffForExistingActiveChannel() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(channel("ch1", "Active", "ioc-b")),
            Map.of("ch1", channel("ch1", "Active", "ioc-a")));

    Assertions.assertEquals(ActiveChannelPreProcessor.NAME, result.preprocessorName());
    Assertions.assertEquals(Set.of("ch1"), result.warnings().keySet());
    String warning = result.warnings().get("ch1").get(0);
    Assertions.assertTrue(warning.contains("hostName: ioc-a -> ioc-b"), warning);
  }

  @Test
  void warnsWithNoChangesForIdenticalActiveChannel() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(channel("ch1", "Active", "ioc-a")),
            Map.of("ch1", channel("ch1", "Active", "ioc-a")));

    String warning = result.warnings().get("ch1").get(0);
    Assertions.assertTrue(warning.endsWith("changes: none"), warning);
  }

  @Test
  void noWarningWhenSettingActiveChannelInactive() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(channel("ch1", "Inactive", "ioc-a")),
            Map.of("ch1", channel("ch1", "Active", "ioc-a")));

    Assertions.assertFalse(result.hasWarnings());
  }

  @Test
  void noWarningForExistingInactiveChannel() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(channel("ch1", "Active", "ioc-b")),
            Map.of("ch1", channel("ch1", "Inactive", "ioc-a")));

    Assertions.assertFalse(result.hasWarnings());
  }

  @Test
  void noWarningForNewChannel() {
    PreProcessResult result =
        preProcessor.preProcess(List.of(channel("ch1", "Active", "ioc-a")), Map.of());

    Assertions.assertFalse(result.hasWarnings());
  }
}
