package org.phoebus.channelfinder.processors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.configuration.ChannelFinderProcessorExecutor;

class ChannelFinderProcessorExecutorTest {

  @Test
  void testDefaultPoolSizing() throws Exception {
    ChannelFinderProcessorExecutor ex = new ChannelFinderProcessorExecutor(4, -1, -1, -1);
    ex.initialize();
    Assertions.assertEquals(4, ex.getCorePoolSize());
    Assertions.assertEquals(4, ex.getMaxPoolSize());
    Assertions.assertEquals(1, ex.getQueueCapacity()); // max(1, 4/4) = 1
    ex.shutdown();
  }

  @Test
  void testMinQueueCapacity() throws Exception {
    // max(1, 1/4) = max(1, 0) = 1
    ChannelFinderProcessorExecutor ex = new ChannelFinderProcessorExecutor(1, -1, -1, -1);
    ex.initialize();
    Assertions.assertEquals(1, ex.getQueueCapacity());
    ex.shutdown();
  }

  @Test
  void testOverridesTakePrecedence() throws Exception {
    ChannelFinderProcessorExecutor ex = new ChannelFinderProcessorExecutor(10, 2, 6, 8);
    ex.initialize();
    Assertions.assertEquals(2, ex.getCorePoolSize());
    Assertions.assertEquals(6, ex.getMaxPoolSize());
    Assertions.assertEquals(8, ex.getQueueCapacity());
    ex.shutdown();
  }

  @Test
  void testRejectionHandlerEvictsOldestAndAdmitsNew() throws Exception {
    // 1 thread, queue=1 → third submit triggers the rejection handler
    ChannelFinderProcessorExecutor ex = new ChannelFinderProcessorExecutor(1, -1, -1, 1);
    ex.initialize();

    CountDownLatch blocker = new CountDownLatch(1);
    CountDownLatch task1Ready = new CountDownLatch(1);
    AtomicBoolean task2Ran = new AtomicBoolean(false);
    AtomicBoolean task3Ran = new AtomicBoolean(false);
    CountDownLatch task3Done = new CountDownLatch(1);

    // task1: blocks the sole thread
    ex.execute(
        () -> {
          task1Ready.countDown();
          try {
            blocker.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });
    task1Ready.await(2, TimeUnit.SECONDS);

    // task2: sits in queue (the stale task to be evicted)
    ex.execute(() -> task2Ran.set(true));

    // task3: triggers rejection handler → evicts task2, admits task3
    ex.execute(
        () -> {
          task3Ran.set(true);
          task3Done.countDown();
        });

    blocker.countDown(); // release the blocked thread
    Assertions.assertTrue(task3Done.await(2, TimeUnit.SECONDS), "task3 did not complete in time");

    Assertions.assertTrue(task3Ran.get(), "Newer task must run after eviction");
    Assertions.assertFalse(task2Ran.get(), "Older queued task must be evicted");

    ex.shutdown();
  }
}
