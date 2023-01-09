package org.phoebus.channelfinder.processors;

import org.phoebus.channelfinder.XmlChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ChannelProcessorService {
    private static final Logger log = Logger.getLogger(ChannelProcessorService.class.getName());

    @Autowired
    private List<ChannelProcessor> channelProcessors;

    @Autowired
    private TaskExecutor taskExecutor;

    long getProcessorCount() {
        return channelProcessors.stream().count();
    }

    List<String> getProcessorsNames() {
        return channelProcessors.stream().map(ChannelProcessor::processorName).collect(Collectors.toList());
    }
    /**
     * {@link ChannelProcessor} providers are called for the specified list of channels. Since a provider
     * implementation may need some time to do it's job, calling them is done asynchronously. Any
     * error handling or logging has to be done in the {@link ChannelProcessor}, but exceptions are
     * handled here in order to not abort if any of the providers fails.
     *
     * @param channels list of channels to be processed
     */
    public void sendToProcessors(List<XmlChannel> channels) {
        log.info(channels.stream().map(XmlChannel::toLog).collect(Collectors.joining()));
        if (channelProcessors.isEmpty()) {
            return;
        }
        taskExecutor.execute(() -> channelProcessors.stream()
                .filter(ChannelProcessor::enabled)
                .forEach(channelProcessor -> {
                    try {
                        channelProcessor.process(channels);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "ChannelProcessor " + channelProcessor.getClass().getName() + " throws exception", e);
                    }
                }));
    }
}
