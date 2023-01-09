package org.phoebus.channelfinder.processors;

import org.phoebus.channelfinder.ChannelScroll;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlScroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
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

    // TODO replace with PIT and search_after
    @Autowired
    ChannelScroll channelScroll;

    @Value("${elasticsearch.query.size:10000}")
    private int defaultMaxSize;

    @GetMapping("/count")
    public long processorCount() {
        return channelProcessorService.getProcessorCount();
    }

    @GetMapping("/info")
    public List<String> processorInfo() {
        return channelProcessorService.getProcessorsNames();
    }

    @GetMapping("/process/all")
    public long processAllChannels() {
        // Only allow authorized users to trigger this operation

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.add("~name", "*");
        return processChannels(searchParameters);
    }

    @GetMapping("/process/query")
    public long processChannels(@RequestParam MultiValueMap<String, String> allRequestParams) {
        long channelCount = 0;
        XmlScroll scrollResult = channelScroll.query(allRequestParams);
        channelCount += scrollResult.getChannels().size();
        processChannels(scrollResult.getChannels());
        while(scrollResult.getChannels().size() == defaultMaxSize) {
            scrollResult = channelScroll.search(scrollResult.getId(), allRequestParams);
            channelCount += scrollResult.getChannels().size();
            processChannels(scrollResult.getChannels());
        }
        return channelCount;
    }

    @GetMapping("/process/channels")
    public void processChannels(List<XmlChannel> channels) {
        channelProcessorService.sendToProcessors(channels);
    }
}
