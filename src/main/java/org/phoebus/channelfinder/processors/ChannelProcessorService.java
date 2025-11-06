package org.phoebus.channelfinder.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.phoebus.channelfinder.entity.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class ChannelProcessorService {

  private static final Logger logger = Logger.getLogger(ChannelProcessorService.class.getName());

  private final List<ChannelProcessor> channelProcessors;

  private final TaskExecutor taskExecutor;

  private final int chunkSize;

  public ChannelProcessorService(
      @Autowired List<ChannelProcessor> channelProcessors,
      @Autowired TaskExecutor taskExecutor,
      @Value("${processors.chunking.size:10000}") int chunkSize) {
    this.channelProcessors = channelProcessors;
    this.taskExecutor = taskExecutor;
    this.chunkSize = chunkSize;
  }

  long getProcessorCount() {
    return channelProcessors.size();
  }

  List<ChannelProcessorInfo> getProcessorsInfo() {
    return channelProcessors.stream().map(ChannelProcessor::processorInfo).toList();
  }

  void setProcessorEnabled(String name, boolean enabled) {
    Optional<ChannelProcessor> processor =
        channelProcessors.stream()
            .filter(p -> Objects.equals(p.processorInfo().name(), name))
            .findFirst();
    processor.ifPresent(channelProcessor -> channelProcessor.setEnabled(enabled));
  }

  /**
   * {@link ChannelProcessor} providers are called for the specified list of channels. Since a
   * provider implementation may need some time to do it's job, calling them is done asynchronously.
   * Any error handling or logging has to be done in the {@link ChannelProcessor}, but exceptions
   * are handled here in order to not abort if any of the providers fails.
   *
   * @param channels list of channels to be processed
   */
  public void sendToProcessors(List<Channel> channels) {
    logger.log(
        Level.FINEST, () -> channels.stream().map(Channel::toLog).collect(Collectors.joining()));
    if (channelProcessors.isEmpty()) {
      return;
    }
    taskExecutor.execute(
        () ->
            channelProcessors.stream()
                .filter(ChannelProcessor::enabled)
                .forEach(
                    channelProcessor -> {
                      try {
                        Spliterator<Channel> split = channels.stream().spliterator();

                        while (true) {
                          List<Channel> chunk = new ArrayList<>(chunkSize);
                          for (int i = 0; i < chunkSize && split.tryAdvance(chunk::add); i++) {}
                          if (chunk.isEmpty()) break;
                          channelProcessor.process(chunk);
                        }

                      } catch (Exception e) {
                        logger.log(
                            Level.WARNING,
                            "ChannelProcessor "
                                + channelProcessor.getClass().getName()
                                + " throws exception",
                            e);
                      }
                    }));
  }
}
