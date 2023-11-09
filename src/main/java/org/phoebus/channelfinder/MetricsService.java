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

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Service
@PropertySource(value = "classpath:application.properties")
public class MetricsService {

    @Value("${metrics.tags}")
    private String[] tags;

    @Value("#{${metrics.properties:{'pvStatus': 'Active'}}}")
    private Map<String, String> properties;

    private static final Logger logger = Logger.getLogger(MetricsService.class.getName());
    private static final String METRIC_NAME_CHANNEL_COUNT = "cf_channel_count";
    private static final String METRIC_NAME_PROPERTIES_COUNT = "cf_properties_count";
    private static final String METRIC_NAME_TAGS_COUNT = "cf_tags_count";
    private static final String METRIC_NAME_CHANNEL_COUNT_PER_PROPERTY = "cf_channel_count_per_property";
    private static final String METRIC_NAME_CHANNEL_COUNT_PER_TAG = "cf_channel_count_per_tag";
    private static final String METRIC_DESCRIPTION_CHANNEL_COUNT = "Count of channel finder channels";
    private static final String METRIC_DESCRIPTION_PROPERTIES_COUNT = "Count of channel finder properties";
    private static final String METRIC_DESCRIPTION_TAGS_COUNT = "Count of channel finder tags";
    private static final String METRIC_DESCRIPTION_CHANNEL_COUNT_PER_PROPERTY = "Count of channels with specific property with and specific value";
    private static final String METRIC_DESCRIPTION_CHANNEL_COUNT_PER_TAG = "Count of channels with specific tag";

    private final ChannelRepository channelRepository;
    private final PropertyRepository propertyRepository;
    private final TagRepository tagRepository;
    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsService(final ChannelRepository channelRepository, final PropertyRepository propertyRepository, final TagRepository tagRepository,
                          final MeterRegistry meterRegistry) {
        this.channelRepository = channelRepository;
        this.propertyRepository = propertyRepository;
        this.tagRepository = tagRepository;
        this.meterRegistry = meterRegistry;
        registerGaugeMetrics();
    }

    MultiGauge propertyCounts;
    MultiGauge tagCounts;

    private void registerGaugeMetrics(){
        Gauge.builder(METRIC_NAME_CHANNEL_COUNT, () -> channelRepository.count(new LinkedMultiValueMap<>()))
                .description(METRIC_DESCRIPTION_CHANNEL_COUNT)
                .register(meterRegistry);
        Gauge.builder(METRIC_NAME_PROPERTIES_COUNT,
                        propertyRepository::count)
                .description(METRIC_DESCRIPTION_PROPERTIES_COUNT)
                .register(meterRegistry);
        Gauge.builder(METRIC_NAME_TAGS_COUNT,
                        tagRepository::count)
                .description(METRIC_DESCRIPTION_TAGS_COUNT)
                .register(meterRegistry);

        propertyCounts = MultiGauge.builder(METRIC_NAME_CHANNEL_COUNT_PER_PROPERTY)
                .description(METRIC_DESCRIPTION_CHANNEL_COUNT_PER_PROPERTY)
                .register(meterRegistry);

        tagCounts = MultiGauge.builder(METRIC_NAME_CHANNEL_COUNT_PER_TAG)
                .description(METRIC_DESCRIPTION_CHANNEL_COUNT_PER_TAG)
                .register(meterRegistry);

    }

    @Scheduled(fixedRate = 10000)
    public void updateMetrics() {
        logger.log(Level.INFO, "Updating metrics");
        propertyCounts.register(
                properties.entrySet().stream().map(property -> MultiGauge.Row.of(Tags.of(property.getKey(), property.getValue()),
                                channelRepository.countByProperty(property.getKey(), property.getValue()))).collect(Collectors.toList())
        );
        tagCounts.register(
                Arrays.stream(tags).map(tag -> MultiGauge.Row.of(Tags.of("tag", tag),
                                channelRepository.countByTag(tag))).collect(Collectors.toList())
        );
    }
}
