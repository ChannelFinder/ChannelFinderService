package org.phoebus.channelfinder.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.exceptions.ArchiverServiceException;
import org.phoebus.channelfinder.service.external.ArchiverService;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;

/**
 * A post processor which uses the channel property *archive* to add pv's to the archiver appliance
 * The archive parameters are specified in the property value, they consist of 2 parts the sampling
 * method which can be scan or monitor the sampling rate defined in seconds
 *
 * <p>e.g. archive=monitor@1.0
 */
@Configuration
@ConditionalOnProperty(name = "aa.enabled", havingValue = "true")
public class AAChannelProcessor implements ChannelProcessor {

  private static final Logger logger = Logger.getLogger(AAChannelProcessor.class.getName());

  private static final String PV_STATUS_PROPERTY_NAME = "pvStatus"; // Matches in recsync
  private static final String PV_STATUS_INACTIVE = "Inactive";
  public static final String PV_STATUS_ACTIVE = "Active";

  @Value("${aa.enabled:true}")
  private boolean aaEnabled;

  @Value("#{${aa.urls:{'default': 'http://localhost:17665'}}}")
  private Map<String, String> aaURLs;

  @Value("${aa.default_alias:default}")
  private List<String> defaultArchivers;

  @Value("${aa.pva:false}")
  private boolean aaPVA;

  @Value("${aa.archive_property_name:archive}")
  private String archivePropertyName;

  @Value("${aa.archiver_property_name:archiver}")
  private String archiverPropertyName;

  @Value("${aa.auto_pause:}")
  private volatile List<String> autoPauseOptions;

  @Value("${aa.post_support:}")
  private volatile List<String> postSupportArchivers;

  @Autowired private ArchiverService archiverService;

  @Override
  public boolean enabled() {
    return aaEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.aaEnabled = enabled;
  }

  @Override
  public ChannelProcessorInfo processorInfo() {
    return new ChannelProcessorInfo(
        "AAChannelProcessor",
        aaEnabled,
        Map.of(
            "archiveProperty",
            archivePropertyName,
            "archiverProperty",
            archiverPropertyName,
            "Archivers",
            aaURLs.keySet().toString(),
            "AutoPauseOn",
            autoPauseOptions.toString()));
  }

  /**
   * Processes a list of channels through the archiver workflow. First the status of each pv is
   * checked against the archiver. If the pv is not being archived and is not paused then the pv
   * will be submitted to be archived. If the pvStatus auto pause is set, then the pv will be auto
   * pause resumed as well.
   *
   * @param channels List of channels
   * @return Return number of channels processed
   * @throws JacksonException If processing archiver responses fail.
   */
  @Override
  public long process(List<Channel> channels) throws JacksonException {
    if (channels.isEmpty()) {
      return 0;
    }

    Map<String, ArchiverInfo> archiversInfo = getArchiversInfo(aaURLs);
    if (archiversInfo.isEmpty()) {
      logger.log(
          Level.WARNING,
          () ->
              String.format(
                  "No reachable archivers configured; skipping %d channels.", channels.size()));
      return 0;
    }

    logger.log(Level.INFO, () -> String.format("Processing %d channels.", channels.size()));
    Map<String, List<ArchivePVOptions>> pvsByArchiver = buildArchivePVMap(channels, archiversInfo);

    long count = submitToArchivers(pvsByArchiver, archiversInfo);
    logger.log(Level.INFO, () -> String.format("Configured %d channels.", count));
    return count;
  }

  private Map<String, List<ArchivePVOptions>> buildArchivePVMap(
      List<Channel> channels, Map<String, ArchiverInfo> archiversInfo) {
    Map<String, List<ArchivePVOptions>> result = new HashMap<>();
    archiversInfo.keySet().forEach(alias -> result.put(alias, new ArrayList<>()));

    channels.forEach(
        channel -> {
          Optional<Property> archiveProperty = findProperty(channel, archivePropertyName);
          if (archiveProperty.isPresent()) {
            resolveArchiverAliases(channel)
                .forEach(
                    archiverAlias -> {
                      try {
                        addChannelChange(
                            channel, result, archiversInfo, archiveProperty, archiverAlias);
                      } catch (Exception e) {
                        logger.log(
                            Level.WARNING,
                            () ->
                                String.format(
                                    "Failed to add channel '%s' to archiver '%s': %s",
                                    channel.getName(), archiverAlias, e.getMessage()));
                      }
                    });
          } else if (autoPauseOptions.contains(archivePropertyName)) {
            archiversInfo
                .keySet()
                .forEach(
                    archiverAlias ->
                        result
                            .get(archiverAlias)
                            .add(createArchivePV(List.of(), channel, "", PV_STATUS_INACTIVE)));
          }
        });
    return result;
  }

  private long submitToArchivers(
      Map<String, List<ArchivePVOptions>> pvsByArchiver, Map<String, ArchiverInfo> archiversInfo)
      throws JacksonException {
    long count = 0;
    for (Map.Entry<String, List<ArchivePVOptions>> e : pvsByArchiver.entrySet()) {
      ArchiverInfo archiverInfo = archiversInfo.get(e.getKey());
      if (archiverInfo == null) {
        logger.log(
            Level.WARNING,
            () ->
                String.format(
                    "Archiver alias '%s' present in PV map but missing from archiver info; skipping.",
                    e.getKey()));
        continue;
      }
      Map<String, ArchivePVOptions> pvMap =
          e.getValue().stream().collect(Collectors.toMap(ArchivePVOptions::getPv, pv -> pv));
      Optional<Map<ArchiveAction, List<ArchivePVOptions>>> actions =
          getArchiveActions(pvMap, archiverInfo);
      if (actions.isEmpty()) continue;
      count += archiverService.configureAA(actions.get(), archiverInfo.url());
    }
    return count;
  }

  private Stream<String> resolveArchiverAliases(Channel channel) {
    return findPropertyValue(channel, archiverPropertyName)
        .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()))
        .orElseGet(defaultArchivers::stream);
  }

  private Optional<Property> findProperty(Channel channel, String propertyName) {
    return channel.getProperties().stream()
        .filter(p -> propertyName.equalsIgnoreCase(p.getName()))
        .findFirst();
  }

  private Optional<String> findPropertyValue(Channel channel, String propertyName) {
    return findProperty(channel, propertyName)
        .map(Property::getValue)
        .filter(v -> v != null && !v.isEmpty());
  }

  private void addChannelChange(
      Channel channel,
      Map<String, List<ArchivePVOptions>> aaArchivePVS,
      Map<String, ArchiverInfo> archiversInfo,
      Optional<Property> archiveProperty,
      String archiverAlias) {
    String pvStatus =
        findPropertyValue(channel, PV_STATUS_PROPERTY_NAME).orElse(PV_STATUS_INACTIVE);
    if (aaArchivePVS.containsKey(archiverAlias) && archiveProperty.isPresent()) {
      ArchivePVOptions newArchiverPV =
          createArchivePV(
              archiversInfo.get(archiverAlias).policies(),
              channel,
              archiveProperty.get().getValue(),
              autoPauseOptions.contains(PV_STATUS_PROPERTY_NAME) ? pvStatus : PV_STATUS_ACTIVE);
      aaArchivePVS.get(archiverAlias).add(newArchiverPV);
    }
  }

  private ArchiveAction pickArchiveAction(String archiveStatus, String pvStatus) {
    if (archiveStatus.equals("Being archived") && (pvStatus.equals(PV_STATUS_INACTIVE))) {
      return ArchiveAction.PAUSE;
    } else if (archiveStatus.equals("Paused") && (pvStatus.equals(PV_STATUS_ACTIVE))) {
      return ArchiveAction.RESUME;
    } else if (!archiveStatus.equals("Being archived")
        && !archiveStatus.equals("Paused")
        && pvStatus.equals(PV_STATUS_ACTIVE)) { // If archive status anything else
      return ArchiveAction.ARCHIVE;
    }

    return ArchiveAction.NONE;
  }

  private Optional<Map<ArchiveAction, List<ArchivePVOptions>>> getArchiveActions(
      Map<String, ArchivePVOptions> archivePVS, ArchiverInfo archiverInfo) {
    if (archiverInfo == null) {
      return Optional.of(Map.of());
    }

    logger.log(
        Level.FINE,
        () ->
            String.format(
                "Querying status of %d PVs from archiver '%s'.",
                archivePVS.size(), archiverInfo.alias()));

    Map<ArchiveAction, List<ArchivePVOptions>> result = new EnumMap<>(ArchiveAction.class);
    Arrays.stream(ArchiveAction.values())
        .forEach(archiveAction -> result.put(archiveAction, new ArrayList<>()));
    // Don't request to archive an empty list.
    if (archivePVS.isEmpty()) {
      return Optional.of(result);
    }
    List<String> pvList = new ArrayList<>(archivePVS.keySet());
    List<Map<String, String>> statuses;
    try {
      statuses =
          postSupportArchivers.contains(archiverInfo.alias())
              ? archiverService.getStatusesViaPost(archiverInfo.url(), pvList)
              : archiverService.getStatusesViaGet(archiverInfo.url(), pvList);
    } catch (ArchiverServiceException e) {
      logger.log(
          Level.WARNING,
          () ->
              String.format(
                  "Status fetch failed for archiver '%s'; skipping %d PVs to avoid spurious ARCHIVE submissions: %s",
                  archiverInfo.alias(), archivePVS.size(), e.getMessage()));
      return Optional.empty();
    }
    logger.log(
        Level.FINER,
        () ->
            String.format(
                "Status response from archiver '%s': %s", archiverInfo.alias(), statuses));
    statuses.forEach(
        archivePVStatusJsonMap -> {
          String archiveStatus = archivePVStatusJsonMap.get("status");
          String pvName = archivePVStatusJsonMap.get("pvName");

          if (archiveStatus == null || pvName == null) {
            logger.log(
                Level.WARNING,
                () ->
                    String.format(
                        "Archiver '%s' returned entry with missing 'status' or 'pvName': %s",
                        archiverInfo.alias(), archivePVStatusJsonMap));
            return;
          }

          ArchivePVOptions archivePVOptions = archivePVS.get(pvName);
          if (archivePVOptions == null) {
            logger.log(
                Level.WARNING,
                () ->
                    String.format(
                        "Archiver '%s' returned status for unknown PV '%s'; ignoring.",
                        archiverInfo.alias(), pvName));
            return;
          }

          String pvStatus = archivePVOptions.getPvStatus();
          ArchiveAction action = pickArchiveAction(archiveStatus, pvStatus);

          List<ArchivePVOptions> archivePVOptionsList = result.get(action);
          archivePVOptionsList.add(archivePVOptions);
        });
    return Optional.of(result);
  }

  private ArchivePVOptions createArchivePV(
      List<String> policyList, Channel channel, String archiveProperty, String pvStatus) {
    ArchivePVOptions newArchiverPV = new ArchivePVOptions();
    if (aaPVA && !channel.getName().contains("://")) {
      newArchiverPV.setPv("pva://" + channel.getName());
    } else {
      newArchiverPV.setPv(channel.getName());
    }
    newArchiverPV.setSamplingParameters(archiveProperty, policyList);
    newArchiverPV.setPvStatus(pvStatus);
    return newArchiverPV;
  }

  private Map<String, ArchiverInfo> getArchiversInfo(Map<String, String> aaURLs) {
    return aaURLs.entrySet().stream()
        .filter(aa -> !StringUtils.isEmpty(aa.getValue()))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                aa ->
                    new ArchiverInfo(
                        aa.getKey(), aa.getValue(), archiverService.getAAPolicies(aa.getValue()))));
  }
}
