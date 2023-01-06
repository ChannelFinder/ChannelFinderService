package org.phoebus.channelfinder.processors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.phoebus.channelfinder.CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI;

/**
 * A rest end point for retrieving information about the various channel processors included
 * in this installation of ChannelFinder and end points for manually triggering their processing.
 */
@RestController
@RequestMapping(CHANNEL_PROCESSOR_RESOURCE_URI)
public class ChannelProcessorManager {
    @Autowired
    ChannelProcessorService channelProcessorService;

    @GetMapping("/count")
    public long processorCount() {
        return channelProcessorService.getProcessorCount();
    }

    @GetMapping("/info")
    public List<String> processorInfo() {
        return channelProcessorService.getProcessorsNames();
    }
}
