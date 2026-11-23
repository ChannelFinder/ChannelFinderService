package org.phoebus.channelfinder.processor.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.processor.ChannelPreProcessor;
import org.phoebus.channelfinder.processor.ChannelPreProcessorInfo;
import org.phoebus.channelfinder.processor.PreProcessResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * A {@link ChannelPreProcessor} that reports incoming channels which already exist in Channel
 * Finder and are currently marked active (by default {@code pvStatus=Active}), e.g. two IOCs
 * publishing the same PV name.
 *
 * <p>An incoming channel that sets the status to a non-active value (e.g. Active to Inactive) is
 * not reported. Each warning includes the properties the incoming channel would change, for
 * reporting only. The create/update operation is never blocked.
 */
@Configuration
@ConditionalOnProperty(name = "preprocessors.active_channel.enabled", havingValue = "true")
public class ActiveChannelPreProcessor implements ChannelPreProcessor {

  private static final Logger logger = Logger.getLogger(ActiveChannelPreProcessor.class.getName());

  public static final String NAME = "ActiveChannelPreProcessor";

  private final String statusPropertyName;
  private final String activeValue;
  private volatile boolean enabled = true;

  public ActiveChannelPreProcessor(
      @Value("${preprocessors.active_channel.status_property_name:pvStatus}")
          String statusPropertyName,
      @Value("${preprocessors.active_channel.active_value:Active}") String activeValue) {
    this.statusPropertyName = statusPropertyName;
    this.activeValue = activeValue;
  }

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public ChannelPreProcessorInfo preProcessorInfo() {
    return new ChannelPreProcessorInfo(
        NAME, enabled, Map.of("statusProperty", statusPropertyName, "activeValue", activeValue));
  }

  @Override
  public PreProcessResult preProcess(
      List<Channel> channels, Map<String, Channel> existingChannels) {
    Map<String, List<String>> warnings = new HashMap<>();
    for (Channel channel : channels) {
      Channel existing = existingChannels.get(channel.getName());
      if (existing == null || !isActive(existing) || setsInactive(channel)) {
        continue;
      }
      String warning =
          String.format(
              "Channel already exists and is active (%s=%s); changes: %s",
              statusPropertyName, activeValue, propertyDiff(existing, channel));
      warnings.computeIfAbsent(channel.getName(), k -> new ArrayList<>()).add(warning);
      logger.log(Level.FINE, () -> String.format("Channel '%s': %s", channel.getName(), warning));
    }
    return new PreProcessResult(warnings, NAME);
  }

  private boolean isActive(Channel channel) {
    return statusOf(channel).map(activeValue::equalsIgnoreCase).orElse(false);
  }

  private boolean setsInactive(Channel channel) {
    return statusOf(channel).map(s -> !activeValue.equalsIgnoreCase(s)).orElse(false);
  }

  private Optional<String> statusOf(Channel channel) {
    return valueOf(channel, statusPropertyName);
  }

  /** Changed or added properties as {@code name: old -> new}, or {@code none}. */
  private static String propertyDiff(Channel existing, Channel incoming) {
    List<String> diff = new ArrayList<>();
    for (Property p : properties(incoming)) {
      String oldValue = valueOf(existing, p.getName()).orElse(null);
      if (!Objects.equals(oldValue, p.getValue())) {
        diff.add(String.format("%s: %s -> %s", p.getName(), oldValue, p.getValue()));
      }
    }
    return diff.isEmpty() ? "none" : String.join(", ", diff);
  }

  private static Optional<String> valueOf(Channel channel, String propertyName) {
    return properties(channel).stream()
        .filter(p -> propertyName.equalsIgnoreCase(p.getName()))
        .map(Property::getValue)
        .filter(Objects::nonNull)
        .findFirst();
  }

  private static List<Property> properties(Channel channel) {
    return channel.getProperties() == null ? List.of() : channel.getProperties();
  }
}
