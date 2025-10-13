package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.phoebus.channelfinder.entity.Channel;

public interface ChannelProcessor {

  boolean enabled();

  String processorInfo();

  long process(List<Channel> channels) throws JsonProcessingException;
}
