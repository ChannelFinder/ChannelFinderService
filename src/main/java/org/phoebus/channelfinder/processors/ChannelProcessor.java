package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.phoebus.channelfinder.XmlChannel;

import java.util.List;

public interface ChannelProcessor {

    boolean enabled();

    void process(List<XmlChannel> channels) throws JsonProcessingException;
}
