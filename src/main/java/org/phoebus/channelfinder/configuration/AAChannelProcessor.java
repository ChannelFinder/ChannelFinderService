package org.phoebus.channelfinder.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.apache.commons.lang3.StringUtils;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.service.external.ArchiverClient;
import org.phoebus.channelfinder.service.model.archiver.ChannelProcessorInfo;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * A post processor which uses the channel property *archive* to add pv's to the archiver appliance
 * The archive parameters are specified in the property value, they consist of 2 parts the sampling
 * method which can be scan or monitor the sampling rate defined in seconds
 *
 * <p>e.g. archive=monitor@1.0
 */
@Configuration
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
  private List<String> autoPauseOptions;

  @Autowired private final ArchiverClient archiverClient = new ArchiverClient();

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
   * @throws JsonProcessingException If processing archiver responses fail.
   */
  @Override
  public long process(List<Channel> channels) throws JsonProcessingException {
    if (channels.isEmpty()) {
      return 0;
    }

    // Get Info (policy and version) of each archiver
    Map<String, ArchiverInfo> archiversInfo = getArchiversInfo(aaURLs);
    if (archiversInfo.isEmpty()) {
      return 0;
    }

    Map<String, List<ArchivePVOptions>> archiverAliasToArchivePVOptions =
        new HashMap<>(); // AA identifier, ArchivePVOptions
    for (String alias : archiversInfo.keySet()) {
      archiverAliasToArchivePVOptions.put(alias, new ArrayList<>());
    }

    logger.log(Level.INFO, "Get channelfinder properties for aa processor.");
    channels.forEach(
        channel -> {
          Optional<Property> archiveProperty =
              channel.getProperties().stream()
                  .filter(
                      xmlProperty -> archivePropertyName.equalsIgnoreCase(xmlProperty.getName()))
                  .findFirst();
          if (archiveProperty.isPresent()) {
            channel.getProperties().stream()
                .filter(xmlProperty -> archiverPropertyName.equalsIgnoreCase(xmlProperty.getName()))
                .findFirst()
                .map(
                    xmlProperty -> {
                      String archiverValue = xmlProperty.getValue();
                      // archiver property can be comma separated list of archivers
                      if (archiverValue != null && !archiverValue.isEmpty()) {
                        return Arrays.stream(archiverValue.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty());
                      } else {
                        return defaultArchivers.stream();
                      }
                    })
                .orElse(
                    defaultArchivers
                        .stream()) // Use defaultArchivers list if no matching property found
                .forEach(
                    archiverAlias -> {
                      try {
                        addChannelChange(
                            channel,
                            archiverAliasToArchivePVOptions,
                            archiversInfo,
                            archiveProperty,
                            archiverAlias);
                      } catch (Exception e) {
                        logger.log(
                            Level.WARNING, String.format("Failed to process %s", channel), e);
                      }
                    });
          } else if (autoPauseOptions.contains(archivePropertyName)) {
            aaURLs
                .keySet()
                .forEach(
                    archiverAlias ->
                        archiverAliasToArchivePVOptions
                            .get(archiverAlias)
                            .add(createArchivePV(List.of(), channel, "", PV_STATUS_INACTIVE)));
          }
        });
    long count = 0;
    for (Map.Entry<String, List<ArchivePVOptions>> e : archiverAliasToArchivePVOptions.entrySet()) {
      ArchiverInfo archiverInfo = archiversInfo.get(e.getKey());
      if (archiverInfo == null) {
        logger.log(Level.WARNING, String.format("Failed to process %s", e.getKey()));
        continue;
      }
      Map<String, ArchivePVOptions> archivePVSList =
          e.getValue().stream()
              .collect(
                  Collectors.toMap(ArchivePVOptions::getPv, archivePVOptions -> archivePVOptions));
      Map<ArchiveAction, List<ArchivePVOptions>> archiveActionArchivePVMap =
          getArchiveActions(archivePVSList, archiverInfo);
      count += archiverClient.configureAA(archiveActionArchivePVMap, archiverInfo.url());
    }
    long finalCount = count;
    logger.log(Level.INFO, () -> String.format("Configured %s channels.", finalCount));
    return finalCount;
  }

  private void addChannelChange(
      Channel channel,
      Map<String, List<ArchivePVOptions>> aaArchivePVS,
      Map<String, ArchiverInfo> archiversInfo,
      Optional<Property> archiveProperty,
      String archiverAlias) {
    String pvStatus =
        channel.getProperties().stream()
            .filter(xmlProperty -> PV_STATUS_PROPERTY_NAME.equalsIgnoreCase(xmlProperty.getName()))
            .findFirst()
            .map(Property::getValue)
            .orElse(PV_STATUS_INACTIVE);
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

  private Map<ArchiveAction, List<ArchivePVOptions>> getArchiveActions(
      Map<String, ArchivePVOptions> archivePVS, ArchiverInfo archiverInfo) {
    if (archiverInfo == null) {
      return Map.of();
    }

    logger.log(Level.INFO, () -> String.format("Get archiver status in archiver %s", archiverInfo));

    Map<ArchiveAction, List<ArchivePVOptions>> result = new EnumMap<>(ArchiveAction.class);
    Arrays.stream(ArchiveAction.values())
        .forEach(archiveAction -> result.put(archiveAction, new ArrayList<>()));
    // Don't request to archive an empty list.
    if (archivePVS.isEmpty()) {
      return result;
    }
    List<Map<String, String>> statuses =
        archiverClient.getStatuses(archivePVS, archiverInfo.url(), archiverInfo.alias());
    logger.log(Level.FINER, "Statuses {0}", statuses);
    statuses.forEach(
        archivePVStatusJsonMap -> {
          String archiveStatus = archivePVStatusJsonMap.get("status");
          String pvName = archivePVStatusJsonMap.get("pvName");

          if (archiveStatus == null || pvName == null) {
            logger.log(
                Level.WARNING,
                "Missing status or pvName in archivePVStatusJsonMap: {0}",
                archivePVStatusJsonMap);
            return;
          }

          ArchivePVOptions archivePVOptions = archivePVS.get(pvName);
          if (archivePVOptions == null) {
            logger.log(Level.WARNING, "archivePVS does not contain pvName: {0}", pvName);
            return;
          }

          String pvStatus = archivePVOptions.getPvStatus();
          ArchiveAction action = pickArchiveAction(archiveStatus, pvStatus);

          List<ArchivePVOptions> archivePVOptionsList = result.get(action);
          archivePVOptionsList.add(archivePVOptions);
        });
    return result;
  }

  private ArchivePVOptions createArchivePV(
      List<String> policyList, Channel channel, String archiveProperty, String pvStaus) {
    ArchivePVOptions newArchiverPV = new ArchivePVOptions();
    if (aaPVA && !channel.getName().contains("://")) {
      newArchiverPV.setPv("pva://" + channel.getName());
    } else {
      newArchiverPV.setPv(channel.getName());
    }
    newArchiverPV.setSamplingParameters(archiveProperty, policyList);
    newArchiverPV.setPvStatus(pvStaus);
    return newArchiverPV;
  }

  private Map<String, ArchiverInfo> getArchiversInfo(Map<String, String> aaURLs) {
    Map<String, ArchiverInfo> result = new HashMap<>();
    for (Map.Entry<String, String> aa : aaURLs.entrySet()) {
      if (StringUtils.isEmpty(aa.getValue())) {
        // Empty archiver tagged
        continue;
      }
      String version = archiverClient.getVersion(aa.getValue());
      List<String> policies = archiverClient.getAAPolicies(aa.getValue());
      result.put(aa.getKey(), new ArchiverInfo(aa.getKey(), aa.getValue(), version, policies));
    }
    return result;
  }
}
