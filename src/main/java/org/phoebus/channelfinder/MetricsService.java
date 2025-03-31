package org.phoebus.channelfinder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@PropertySource(value = "classpath:application.properties")
public class MetricsService {

    public static final String CF_TOTAL_CHANNEL_COUNT = "cf.total.channel.count";
    public static final String CF_PROPERTY_COUNT = "cf.property.count";
    public static final String CF_TAG_COUNT = "cf.tag.count";
    public static final String CF_PROPERTY_FORMAT_STRING = "cf.%s.channel.count";
    public static final String CF_TAG_ON_CHANNELS_COUNT = "cf.tag_on_channels.count";
    private static final String METRIC_DESCRIPTION_TOTAL_CHANNEL_COUNT = "Count of all ChannelFinder channels";
    private static final String METRIC_DESCRIPTION_PROPERTY_COUNT = "Count of all ChannelFinder properties";
    private static final String METRIC_DESCRIPTION_TAG_COUNT = "Count of all ChannelFinder tags";
    private static final String BASE_UNIT = "channels";

    private final ChannelRepository channelRepository;
    private final PropertyRepository propertyRepository;
    private final TagRepository tagRepository;
    private final MeterRegistry meterRegistry;

    @Value("${metrics.tags}")
    private String[] tags;

    @Value("${metrics.properties}")
    private String metricProperties;

    Map<String, List<String>> parseProperties() {
        if (metricProperties == null || metricProperties.isEmpty()) {
            return new LinkedMultiValueMap<>();
        }
        return Arrays.stream(metricProperties.split(";")).map(s ->
        {
            String[] split = s.split(":");
            String k = split[0].trim();
            List<String> v = Arrays.stream(split[1].split(",")).map(String::trim).toList();
            return Map.entry(k, v);
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

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
    }

    @PostConstruct
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
        registerTagMetrics();
        registerPropertyMetrics();
    }

    private void registerTagMetrics() {
        // Add tags
        for (String tag : tags) {
            Gauge.builder(CF_TAG_ON_CHANNELS_COUNT, () -> channelRepository.countByTag(tag))
                .description("Number of channels with tag")
                .tag("tag", tag)
                .baseUnit(BASE_UNIT)
                .register(meterRegistry);
        }
    }

    private void registerPropertyMetrics() {
        Map<String, List<String>> properties = parseProperties();

        properties.forEach((propertyName, propertyValues) -> propertyValues.forEach(propertyValue ->
            Gauge.builder(String.format(CF_PROPERTY_FORMAT_STRING, propertyName), () -> channelRepository.countByProperty(propertyName, propertyValue))
                .description(String.format("Number of channels with property '%s'", propertyName))
                .tag(propertyName, propertyValue)
                .baseUnit(BASE_UNIT)
                .register(meterRegistry))
        );
    }
}
