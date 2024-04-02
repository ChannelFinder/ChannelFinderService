package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.phoebus.channelfinder.entity.Channel;

import java.util.List;

public interface ChannelProcessor {

    boolean enabled();

    String processorInfo();

    void process(List<Channel> channels) throws JsonProcessingException;

}
