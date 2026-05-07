package org.phoebus.channelfinder.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.PropertyRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
  public static final String CF_TOTAL_CHANNEL_COUNT = "cf.total.channel.count";
  public static final String CF_PROPERTY_COUNT = "cf.property.count";
  public static final String CF_TAG_COUNT = "cf.tag.count";
  public static final String CF_CHANNEL_COUNT = "cf.channel.count";
  public static final String CF_TAG_ON_CHANNELS_COUNT = "cf.tag_on_channels.count";
  private static final String METRIC_DESCRIPTION_TOTAL_CHANNEL_COUNT =
      "Count of all ChannelFinder channels";
  private static final String METRIC_DESCRIPTION_PROPERTY_COUNT =
      "Count of all ChannelFinder properties";
  private static final String METRIC_DESCRIPTION_TAG_COUNT = "Count of all ChannelFinder tags";
  private static final String METRIC_DESCRIPTION_CHANNEL_COUNT =
      "Count of all ChannelFinder channels with set properties";
  private static final String BASE_UNIT = "channels";
  private static final String NEGATE = "!";
  public static final String NOT_SET = "-";

  private final ChannelRepository channelRepository;
  private final PropertyRepository propertyRepository;
  private final TagRepository tagRepository;
  private final MeterRegistry meterRegistry;

  private Map<Map<String, List<String>>, AtomicLong> propertyMetrics;
  private Map<String, AtomicLong> tagMetrics;

  @Value("${metrics.tags}")
  private String[] tags;

  @Value("${metrics.properties}")
  private String metricProperties;

  Map<String, List<String>> parseProperties() {
    if (metricProperties == null || metricProperties.isEmpty()) {
      return Map.of();
    }
    return Arrays.stream(metricProperties.split(";"))
        .map(
            s -> {
              String[] split = s.split(":");
              String k = split[0].trim();
              List<String> v = Arrays.stream(split[1].split(",")).map(String::trim).toList();
              return Map.entry(k, v);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
    Gauge.builder(CF_TOTAL_CHANNEL_COUNT, () -> channelRepository.count(Map.of()))
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
    tagMetrics =
        Arrays.stream(tags)
            .map(t -> Map.entry(t, new AtomicLong(0)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    for (String tag : tags) {
      Gauge.builder(CF_TAG_ON_CHANNELS_COUNT, tagMetrics, m -> m.get(tag).doubleValue())
          .description("Number of channels with tag")
          .tag("tag", tag)
          .baseUnit(BASE_UNIT)
          .register(meterRegistry);
    }
  }

  public static List<Map<String, List<String>>> generateAllMultiValueMaps(
      Map<String, List<String>> properties) {
    List<Map<String, List<String>>> allQueryParams = new ArrayList<>();

    if (properties.isEmpty()) {
      return List.of();
    }

    List<Entry<String, List<String>>> entries = new ArrayList<>(properties.entrySet());
    generateCombinations(entries, 0, new LinkedHashMap<>(), allQueryParams);

    return allQueryParams;
  }

  private static void generateCombinations(
      List<Entry<String, List<String>>> entries,
      int index,
      Map<String, List<String>> currentMap,
      List<Map<String, List<String>>> allQueryParams) {

    if (index == entries.size()) {
      allQueryParams.add(new LinkedHashMap<>(currentMap));
      return;
    }

    Entry<String, List<String>> currentEntry = entries.get(index);
    String key = currentEntry.getKey();
    List<String> values = currentEntry.getValue();

    for (String value : values) {
      Map<String, List<String>> nextMap = new LinkedHashMap<>(currentMap);
      if (value.startsWith(NEGATE)) {
        nextMap.computeIfAbsent(key + NEGATE, k -> new ArrayList<>()).add(value.substring(1));
      } else {
        nextMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
      }
      generateCombinations(entries, index + 1, nextMap, allQueryParams);
    }
  }

  private List<Tag> metricTagsFrom(Map<String, List<String>> queryParams) {
    List<Tag> metricTags = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
      String firstValue = entry.getValue().isEmpty() ? "" : entry.getValue().get(0);
      if (entry.getKey().endsWith(NEGATE)) {
        String tagKey = entry.getKey().substring(0, entry.getKey().length() - 1);
        metricTags.add(
            new ImmutableTag(tagKey, firstValue.equals("*") ? NOT_SET : NEGATE + firstValue));
      } else {
        metricTags.add(new ImmutableTag(entry.getKey(), firstValue));
      }
    }
    return metricTags;
  }

  private void registerPropertyMetrics() {
    Map<String, List<String>> properties = parseProperties();

    List<Map<String, List<String>>> combinations = generateAllMultiValueMaps(properties);

    propertyMetrics =
        combinations.stream()
            .map(t -> Map.entry(t, new AtomicLong(0)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    combinations.forEach(
        map ->
            Gauge.builder(CF_CHANNEL_COUNT, propertyMetrics, m -> m.get(map).doubleValue())
                .description(METRIC_DESCRIPTION_CHANNEL_COUNT)
                .tags(metricTagsFrom(map))
                .register(meterRegistry));
  }

  private void updateTagMetrics() {
    for (Map.Entry<String, AtomicLong> tagMetricEntry : tagMetrics.entrySet()) {
      tagMetricEntry.getValue().set(channelRepository.countByTag(tagMetricEntry.getKey()));
    }
  }

  private void updatePropertyMetrics() {
    for (Map.Entry<Map<String, List<String>>, AtomicLong> propertyMetricEntry :
        propertyMetrics.entrySet()) {
      propertyMetricEntry.getValue().set(channelRepository.count(propertyMetricEntry.getKey()));
    }
  }

  @Scheduled(fixedRateString = "${metrics.updateInterval}", timeUnit = TimeUnit.SECONDS)
  public void updateMetrics() {
    if (tagMetrics != null && !tagMetrics.isEmpty()) {
      updateTagMetrics();
    }
    if (propertyMetrics != null && !propertyMetrics.isEmpty()) {
      updatePropertyMetrics();
    }
  }
}
