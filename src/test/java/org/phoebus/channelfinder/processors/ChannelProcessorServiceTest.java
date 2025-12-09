package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.configuration.ChannelProcessor;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.service.ChannelProcessorService;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest()
@TestPropertySource(value = "classpath:application_test.properties")
class ChannelProcessorServiceTest {

  private ChannelProcessorService channelProcessorService;

  @Autowired private DummyProcessor dummyProcessor;

  @Configuration
  static class TestConfig {
    @Bean
    public DummyProcessor dummyProcessor() {
      return new DummyProcessor();
    }
  }

  static class DummyProcessor implements ChannelProcessor {
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean processed = new AtomicBoolean(false);

    @Override
    public boolean enabled() {
      return enabled.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled.set(enabled);
    }

    @Override
    public ChannelProcessorInfo processorInfo() {
      return new ChannelProcessorInfo("DummyProcessor", enabled.get(), Map.of());
    }

    @Override
    public long process(List<Channel> channels) throws JsonProcessingException {
      processed.set(true);
      return channels.size();
    }

    public boolean hasBeenProcessed() {
      return processed.get();
    }

    public void reset() {
      processed.set(false);
    }
  }

  @BeforeEach
  void setUp() {
    channelProcessorService =
        new ChannelProcessorService(List.of(dummyProcessor), Runnable::run, 10);
  }

  @Test
  void testEnableAndDisableDummyProcessor() {
    Assertions.assertFalse(
        dummyProcessor.enabled(), "Dummy processor should be disabled initially");

    channelProcessorService.setProcessorEnabled("DummyProcessor", true);
    Assertions.assertTrue(dummyProcessor.enabled(), "Dummy processor should be enabled");

    channelProcessorService.setProcessorEnabled("DummyProcessor", false);
    Assertions.assertFalse(dummyProcessor.enabled(), "Dummy processor should be disabled");
  }

  @Test
  void testDummyProcessorProcessing() {
    dummyProcessor.reset();
    channelProcessorService.setProcessorEnabled("DummyProcessor", false);
    Assertions.assertFalse(dummyProcessor.enabled(), "Dummy processor should be disabled");

    channelProcessorService.sendToProcessors(
        Collections.singletonList(new Channel("test-channel")));
    Assertions.assertFalse(
        dummyProcessor.hasBeenProcessed(), "Dummy processor should not have been called");

    channelProcessorService.setProcessorEnabled("DummyProcessor", true);
    Assertions.assertTrue(dummyProcessor.enabled(), "Dummy processor should be enabled");

    channelProcessorService.sendToProcessors(
        Collections.singletonList(new Channel("test-channel")));
    Assertions.assertTrue(
        dummyProcessor.hasBeenProcessed(), "Dummy processor should have been called");
  }
}
