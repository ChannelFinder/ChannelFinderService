package org.phoebus.channelfinder.processors;

import org.phoebus.channelfinder.AuthorizationService;
import org.phoebus.channelfinder.ChannelScroll;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.channelfinder.CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI;

/**
 * A rest end point for retrieving information about the various channel processors included
 * in this installation of ChannelFinder and end points for manually triggering their processing.
 */

@RestController
@RequestMapping(CHANNEL_PROCESSOR_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelProcessorManager {

    private static final Logger logger = Logger.getLogger(ChannelProcessorManager.class.getName());

    @Autowired
    ChannelProcessorService channelProcessorService;
    @Autowired
    AuthorizationService authorizationService;

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

    @PutMapping("/process/all")
    public long processAllChannels() {
        logger.log(Level.INFO, "Calling processor on ALL channels in ChannelFinder");
        // Only allow authorized users to trigger this operation
        if(authorizationService
                .isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(),
                                  AuthorizationService.ROLES.CF_ADMIN)) {
            MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
            searchParameters.add("~name", "*");
            return processChannels(searchParameters);
        } else {
            logger.log(Level.SEVERE,
                    "User does not have the proper authorization to perform this operation: /process/all",
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform this operation: /process/all");
        }
    }

    @PutMapping("/process/query")
    public long processChannels(@RequestParam MultiValueMap<String, String> allRequestParams) {
        long channelCount = 0;
        Scroll scrollResult = channelScroll.query(allRequestParams);
        channelCount += scrollResult.getChannels().size();
        processChannels(scrollResult.getChannels());
        while(scrollResult.getChannels().size() == defaultMaxSize) {
            scrollResult = channelScroll.search(scrollResult.getId(), allRequestParams);
            channelCount += scrollResult.getChannels().size();
            processChannels(scrollResult.getChannels());
        }
        return channelCount;
    }

    @PutMapping("/process/channels")
    public void processChannels(List<Channel> channels) {
        channelProcessorService.sendToProcessors(channels);
    }
}
