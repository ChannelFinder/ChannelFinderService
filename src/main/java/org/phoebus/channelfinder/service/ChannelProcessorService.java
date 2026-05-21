package org.phoebus.channelfinder.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.phoebus.channelfinder.configuration.ChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.exceptions.UnauthorizedException;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class ChannelProcessorService {

  private static final Logger logger = Logger.getLogger(ChannelProcessorService.class.getName());

  private final List<ChannelProcessor> channelProcessors;
  private final TaskExecutor channelFinderTaskExecutor;
  private final AuthorizationService authorizationService;
  private final ChannelScrollService channelScrollService;
  private final int chunkSize;
  private final int defaultMaxSize;

  public ChannelProcessorService(
      @Autowired List<ChannelProcessor> channelProcessors,
      @Autowired @Qualifier("channelFinderTaskExecutor") TaskExecutor channelFinderTaskExecutor,
      @Autowired AuthorizationService authorizationService,
      @Autowired ChannelScrollService channelScrollService,
      @Value("${processors.chunking.size:10000}") int chunkSize,
      @Value("${elasticsearch.query.size:10000}") int defaultMaxSize) {
    this.channelProcessors = channelProcessors;
    this.channelFinderTaskExecutor = channelFinderTaskExecutor;
    this.authorizationService = authorizationService;
    this.channelScrollService = channelScrollService;
    this.chunkSize = chunkSize;
    this.defaultMaxSize = defaultMaxSize;
  }

  public long processAllChannels() {
    if (!authorizationService.isAuthorizedRole(
        SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN)) {
      throw new UnauthorizedException(
          "User does not have the proper authorization to perform this operation: /process/all");
    }
    logger.log(Level.INFO, "Calling processor on ALL channels in ChannelFinder");
    MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
    searchParameters.add("~name", "*");
    return processChannelsByQuery(searchParameters);
  }

  public long processChannelsByQuery(MultiValueMap<String, String> allRequestParams) {
    long channelCount = 0;
    Scroll scrollResult = channelScrollService.search(null, allRequestParams);
    channelCount += scrollResult.getChannels().size();
    sendToProcessors(scrollResult.getChannels());
    while (scrollResult.getChannels().size() == defaultMaxSize) {
      scrollResult = channelScrollService.search(scrollResult.getId(), allRequestParams);
      channelCount += scrollResult.getChannels().size();
      sendToProcessors(scrollResult.getChannels());
    }
    return channelCount;
  }

  public long getProcessorCount() {
    return channelProcessors.size();
  }

  public List<ChannelProcessorInfo> getProcessorsInfo() {
    return channelProcessors.stream().map(ChannelProcessor::processorInfo).toList();
  }

  public void setProcessorEnabled(String name, boolean enabled) {
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
    channelFinderTaskExecutor.execute(
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
