package org.phoebus.channelfinder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@PropertySource(value = "classpath:application.properties")
public class MetricsService {

    private static final Logger logger = Logger.getLogger(MetricsService.class.getName());
    public static final String CF_TOTAL_CHANNEL_COUNT = "cf.total.channel.count";
    public static final String CF_PROPERTY_COUNT = "cf.property.count";
    public static final String CF_TAG_COUNT = "cf.tag.count";
    public static final String CF_CHANNEL_COUNT = "cf.channel.count";
    private static final String METRIC_DESCRIPTION_TOTAL_CHANNEL_COUNT = "Count of all ChannelFinder channels";
    private static final String METRIC_DESCRIPTION_PROPERTY_COUNT = "Count of all ChannelFinder properties";
    private static final String METRIC_DESCRIPTION_TAG_COUNT = "Count of all ChannelFinder tags";
    private static final String METRIC_DESCRIPTION_CHANNEL_COUNT =
            "Count of channels with specific property with and specific value";
    private final ChannelRepository channelRepository;
    private final PropertyRepository propertyRepository;
    private final TagRepository tagRepository;
    private final MeterRegistry meterRegistry;

    MultiGauge channelCounts;

    @Value("${metrics.tags}")
    private String[] tags;

    @Value("#{${metrics.properties:{{'pvStatus', 'Active'}, {'pvStatus', 'Inactive'}}}}")
    private String[][] properties;

    @Autowired
    public MetricsService(
            final ChannelRepository channelRepository,
            final PropertyRepository propertyRepository,
            final TagRepository tagRepository,
            final MeterRegistry meterRegistry) {
        this.channelRepository = channelRepository;
        this.propertyRepository = propertyRepository;
        this.tagRepository = tagRepository;
        this.meterRegistry = meterRegistry;
        registerGaugeMetrics();
    }

    private void registerGaugeMetrics() {
        Gauge.builder(CF_TOTAL_CHANNEL_COUNT, () -> channelRepository.count(new LinkedMultiValueMap<>()))
                .description(METRIC_DESCRIPTION_TOTAL_CHANNEL_COUNT)
                .register(meterRegistry);
        Gauge.builder(CF_PROPERTY_COUNT, propertyRepository::count)
                .description(METRIC_DESCRIPTION_PROPERTY_COUNT)
                .register(meterRegistry);
        Gauge.builder(CF_TAG_COUNT, tagRepository::count)
                .description(METRIC_DESCRIPTION_TAG_COUNT)
                .register(meterRegistry);
        channelCounts = MultiGauge.builder(CF_CHANNEL_COUNT)
                .description(METRIC_DESCRIPTION_CHANNEL_COUNT)
                .baseUnit("channels")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 5000)
    public void updateMetrics() {
        logger.log(
                Level.FINER,
                () -> "Updating metrics for properties " + Arrays.deepToString(properties) + " and tags " + Arrays.toString(tags));
        ArrayList<MultiGauge.Row<?>> rows = new ArrayList<>();

        // Add tags
        for (String tag: tags) {
            long count = channelRepository.countByTag(tag);
            rows.add(MultiGauge.Row.of(Tags.of("tag", tag), count ));
            logger.log(
                    Level.FINER,
                    () -> "Updating metrics for tag " + tag + " to " + count);
        }

        // Add properties
        for (String[] propertyValue: properties) {
            long count = channelRepository.countByProperty(propertyValue[0], propertyValue[1]);
            rows.add(MultiGauge.Row.of(Tags.of(propertyValue[0], propertyValue[1]), count));
            logger.log(
                    Level.FINER,
                    () -> "Updating metrics for property " + propertyValue[0]  + ":" + propertyValue[1] + " to " + count);
        }

        channelCounts.register(rows, true);
    }
}
