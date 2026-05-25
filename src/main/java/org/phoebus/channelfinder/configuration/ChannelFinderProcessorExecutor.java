package org.phoebus.channelfinder.configuration;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * {@link ThreadPoolTaskExecutor} used when calling {@link ChannelProcessor}s.
 *
 * <p>Pool parameters are derived from {@code processors.max_concurrent_updates}. Individual values
 * can be overridden via the {@code processors.task_executor.*} properties (values ≤ 0 mean "use the
 * derived value").
 */
@Component("channelFinderTaskExecutor")
public class ChannelFinderProcessorExecutor extends ThreadPoolTaskExecutor {

  private static final Logger logger =
      Logger.getLogger(ChannelFinderProcessorExecutor.class.getName());

  public ChannelFinderProcessorExecutor(
      @Value("${processors.max_concurrent_updates:10}") int maxConcurrent,
      @Value("${processors.task_executor.core_pool_size:-1}") int overrideCore,
      @Value("${processors.task_executor.max_pool_size:-1}") int overrideMax,
      @Value("${processors.task_executor.queue_capacity:-1}") int overrideQueue) {

    int core = overrideCore > 0 ? overrideCore : maxConcurrent;
    int max = overrideMax > 0 ? overrideMax : maxConcurrent;
    int queue = overrideQueue > 0 ? overrideQueue : Math.max(1, maxConcurrent / 4);

    setCorePoolSize(core);
    setMaxPoolSize(max);
    setQueueCapacity(queue);
    setRejectedExecutionHandler(
        (runnable, executor) -> {
          if (!executor.isShutdown()) {
            executor.getQueue().poll(); // evict oldest (stale) task to make room
            executor.getQueue().offer(runnable);
            logger.log(
                Level.WARNING,
                () ->
                    "ChannelFinderProcessorExecutor task queue full — evicted oldest task to admit fresher update"
                        + " (active="
                        + executor.getActiveCount()
                        + ", queued="
                        + executor.getQueue().size()
                        + ")");
          }
        });
  }
}
