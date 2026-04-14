package org.phoebus.channelfinder.configuration;

import java.util.List;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import tools.jackson.core.JacksonException;

public interface ChannelProcessor {

  boolean enabled();

  void setEnabled(boolean enabled);

  ChannelProcessorInfo processorInfo();

  long process(List<Channel> channels) throws JacksonException;
}
