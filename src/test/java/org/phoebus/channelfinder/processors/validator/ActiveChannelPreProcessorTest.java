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

  private static Channel channel(String name, String pvStatus) {
    return new Channel(
        name, "owner", List.of(new Property("pvStatus", "owner", pvStatus)), List.of());
  }

  @Test
  void warnsForExistingActiveChannel() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(new Channel("ch1", "owner")), Map.of("ch1", channel("ch1", "Active")));

    Assertions.assertEquals(ActiveChannelPreProcessor.NAME, result.preprocessorName());
    Assertions.assertEquals(List.of("ch1"), List.copyOf(result.warnings().keySet()));
    Assertions.assertEquals(1, result.warnings().get("ch1").size());
    Assertions.assertTrue(result.warnings().get("ch1").get(0).contains("pvStatus=Active"));
  }

  @Test
  void noWarningForExistingInactiveChannel() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(new Channel("ch1", "owner")), Map.of("ch1", channel("ch1", "Inactive")));

    Assertions.assertFalse(result.hasWarnings());
  }

  @Test
  void noWarningForNewChannel() {
    PreProcessResult result = preProcessor.preProcess(List.of(channel("ch1", "Active")), Map.of());

    Assertions.assertFalse(result.hasWarnings());
  }

  @Test
  void warnsOnlyForExistingActiveChannelsInBatch() {
    PreProcessResult result =
        preProcessor.preProcess(
            List.of(
                new Channel("active", "owner"),
                new Channel("inactive", "owner"),
                new Channel("new", "owner")),
            Map.of(
                "active",
                channel("active", "Active"),
                "inactive",
                channel("inactive", "Inactive")));

    Assertions.assertEquals(Set.of("active"), result.warnings().keySet());
  }
}
