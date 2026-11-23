package org.phoebus.channelfinder.processors.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.processor.validator.ActiveChannelPreProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActiveChannelPreProcessorConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(ActiveChannelPreProcessor.class);

  @Test
  void notCreatedWhenPropertyMissing() {
    runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ActiveChannelPreProcessor.class));
  }

  @Test
  void notCreatedWhenDisabled() {
    runner
        .withPropertyValues("preprocessors.active_channel.enabled=false")
        .run(ctx -> assertThat(ctx).doesNotHaveBean(ActiveChannelPreProcessor.class));
  }

  @Test
  void createdAndConfiguredWhenEnabled() {
    runner
        .withPropertyValues(
            "preprocessors.active_channel.enabled=true",
            "preprocessors.active_channel.status_property_name=status",
            "preprocessors.active_channel.active_value=On")
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(ActiveChannelPreProcessor.class);
              ActiveChannelPreProcessor p = ctx.getBean(ActiveChannelPreProcessor.class);
              assertThat(p.enabled()).isTrue();
              assertThat(p.preProcessorInfo().properties())
                  .containsEntry("statusProperty", "status")
                  .containsEntry("activeValue", "On");
            });
  }
}
