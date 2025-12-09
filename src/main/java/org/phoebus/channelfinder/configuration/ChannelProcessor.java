package org.phoebus.channelfinder.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;

public interface ChannelProcessor {

  boolean enabled();

  void setEnabled(boolean enabled);

  ChannelProcessorInfo processorInfo();

  long process(List<Channel> channels) throws JsonProcessingException;
}
