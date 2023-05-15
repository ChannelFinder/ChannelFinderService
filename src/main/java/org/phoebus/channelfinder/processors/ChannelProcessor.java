package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.phoebus.channelfinder.XmlChannel;

import java.util.List;

public interface ChannelProcessor {

    /**
     *
     * @return true if this processor is enabled
     */
    boolean enabled();

    /**
     *
     * @return The name of the processor
     */
    String processorName();

    /**
     *
     * @param channels list of channel to be processed
     * @throws JsonProcessingException
     */
    void process(List<XmlChannel> channels) throws JsonProcessingException;

}
