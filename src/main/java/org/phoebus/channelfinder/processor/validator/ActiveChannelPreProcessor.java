package org.phoebus.channelfinder.processor.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.processor.ChannelPreProcessor;
import org.phoebus.channelfinder.processor.ChannelPreProcessorInfo;
import org.phoebus.channelfinder.processor.PreProcessResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * A {@link ChannelPreProcessor} that detects incoming channels which already exist in Channel
 * Finder and are currently marked active (by default {@code pvStatus=Active}).
 *
 * <p>Such channels are reported as non-blocking warnings in the returned {@link PreProcessResult}
 * (individual channels are also logged at FINE). The create/update operation is never blocked.
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
      if (existing != null && isActive(existing)) {
        String warning =
            String.format(
                "Channel already exists and is active (%s=%s)", statusPropertyName, activeValue);
        warnings.computeIfAbsent(channel.getName(), k -> new ArrayList<>()).add(warning);
        logger.log(Level.INFO, () -> String.format("Channel '%s': %s", channel.getName(), warning));
      }
    }
    return new PreProcessResult(warnings, NAME);
  }

  private boolean isActive(Channel channel) {
    return channel.getProperties() != null
        && channel.getProperties().stream()
            .anyMatch(
                p ->
                    statusPropertyName.equalsIgnoreCase(p.getName())
                        && activeValue.equalsIgnoreCase(p.getValue()));
  }
}
