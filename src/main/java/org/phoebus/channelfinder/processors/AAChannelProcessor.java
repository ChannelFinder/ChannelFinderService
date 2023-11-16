package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A post processor which uses the channel property *archive* to add pv's to the archiver appliance The archive
 * parameters are specified in the property value, they consist of 2 parts the sampling method which can be scan or
 * monitor the sampling rate defined in seconds
 * <p>
 * e.g. archive=monitor@1.0
 */
@Configuration
public class AAChannelProcessor implements ChannelProcessor {

    private static final Logger logger = Logger.getLogger(AAChannelProcessor.class.getName());
    private static final String MGMT_RESOURCE = "/mgmt/bpl";
    private static final String POLICY_RESOURCE = MGMT_RESOURCE + "/getPolicyList";
    private static final String PV_STATUS_RESOURCE = MGMT_RESOURCE + "/getPVStatus";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PV_STATUS_PROPERTY_NAME = "pvStatus"; // Matches in recsync
    private static final String PV_STATUS_INACTIVE = "Inactive";
    public static final String PV_STATUS_ACTIVE = "Active";
    private final WebClient client = WebClient.create();
    @Value("${aa.enabled:true}")
    private boolean aaEnabled;
    @Value("#{${aa.urls:{'default': 'http://localhost:17665'}}}")
    private Map<String, String> aaURLs;
    @Value("${aa.default_alias:default}")
    private String defaultArchiver;
    @Value("${aa.pva:false}")
    private boolean aaPVA;
    @Value("${aa.archive_property_name:archive}")
    private String archivePropertyName;
    @Value("${aa.archiver_property_name:archiver}")
    private String archiverPropertyName;
    @Value("${aa.auto_pause:pvStatus,archive}")
    private List<String> autoPauseOptions;

    @Override
    public boolean enabled() {
        return aaEnabled;
    }

    @Override
    public String processorName() {
        return "Process " + archivePropertyName + " properties on channels";
    }

    @Override
    public void process(List<Channel> channels) throws JsonProcessingException {
        if (channels.isEmpty()) {
            return;
        }
        Map<String, List<ArchivePV>> aaArchivePVS = new HashMap<>(); // AA identifier, ArchivePV
        for (String alias : aaURLs.keySet()) {
            aaArchivePVS.put(alias, new ArrayList<>());
        }
        Map<String, List<String>> policyLists = getAAsPolicies(aaURLs);

        logger.log(Level.INFO, "Get channelfinder properties for aa processor.");
        channels.forEach(channel -> {
            Optional<Property> archiveProperty = channel.getProperties().stream()
                    .filter(xmlProperty -> archivePropertyName.equalsIgnoreCase(xmlProperty.getName()))
                    .findFirst();
            if (archiveProperty.isPresent()) {
                String pvStatus = channel.getProperties().stream()
                        .filter(xmlProperty -> PV_STATUS_PROPERTY_NAME.equalsIgnoreCase(xmlProperty.getName()))
                        .findFirst()
                        .map(Property::getValue)
                        .orElse(PV_STATUS_INACTIVE);
                String archiverAlias = channel.getProperties().stream()
                        .filter(xmlProperty -> archiverPropertyName.equalsIgnoreCase(xmlProperty.getName()))
                        .findFirst()
                        .map(Property::getValue)
                        .orElse(defaultArchiver);
                ArchivePV newArchiverPV = createArchivePV(
                        policyLists.get(archiverAlias),
                        channel,
                        archiveProperty.get().getValue(),
                        autoPauseOptions.contains(PV_STATUS_PROPERTY_NAME) ? pvStatus : PV_STATUS_ACTIVE);
                aaArchivePVS.get(archiverAlias).add(newArchiverPV);
            } else if (autoPauseOptions.contains(archivePropertyName)) {
                aaURLs.keySet().forEach(archiverAlias -> aaArchivePVS
                        .get(archiverAlias)
                        .add(createArchivePV(List.of(), channel, "", PV_STATUS_INACTIVE)));
            }
        });

        for (Map.Entry<String, List<ArchivePV>> e : aaArchivePVS.entrySet()) {
            String archiverURL = aaURLs.get(e.getKey());
            Map<String, ArchivePV> archivePVSList =
                    e.getValue().stream().collect(Collectors.toMap(archivePV -> archivePV.pv, archivePV -> archivePV));
            Map<ArchiveAction, List<ArchivePV>> archiveActionArchivePVMap =
                    getArchiveActions(archivePVSList, archiverURL);
            configureAA(archiveActionArchivePVMap, archiverURL);
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

    private Map<ArchiveAction, List<ArchivePV>> getArchiveActions(
            Map<String, ArchivePV> archivePVS, String archiverURL) {
        logger.log(Level.INFO, () -> String.format("Get archiver status in archiver %s", archiverURL));

        Map<ArchiveAction, List<ArchivePV>> result = new EnumMap<>(ArchiveAction.class);
        Arrays.stream(ArchiveAction.values()).forEach(archiveAction -> result.put(archiveAction, new ArrayList<>()));
        // Don't request to archive an empty list.
        if (archivePVS.isEmpty()) {
            return result;
        }

        try {
            URI pvStatusURI = UriComponentsBuilder.fromUri(URI.create(archiverURL + PV_STATUS_RESOURCE))
                    .queryParam("pv", archivePVS.keySet())
                    .build()
                    .toUri();

            String response = client.post()
                    .uri(pvStatusURI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .block();

            // Structure of response is
            // [{"pvName":"PV:1", "status":"Paused", ... }, {"pvName": "PV:2"}, {"status": "Being archived"}, ...}, ...
            // ]

            objectMapper
                    .readValue(response, new TypeReference<List<Map<String, String>>>() {})
                    .forEach(archivePVStatusJsonMap -> {
                        String archiveStatus = archivePVStatusJsonMap.get("status");
                        String pvName = archivePVStatusJsonMap.get("pvName");
                        String pvStatus = archivePVS.get(pvName).getPvStatus();
                        ArchiveAction action = pickArchiveAction(archiveStatus, pvStatus);
                        result.get(action).add(archivePVS.get(pvName));
                    });
            return result;

        } catch (JsonProcessingException e) {
            // problem collecting policies from AA, so warn and return empty list
            logger.log(Level.WARNING, "Could not get AA pv Status list: " + e.getMessage());
            return result;
        }
    }

    private ArchivePV createArchivePV(
            List<String> policyList, Channel channel, String archiveProperty, String pvStaus) {
        ArchivePV newArchiverPV = new ArchivePV();
        if (aaPVA && !channel.getName().contains("://")) {
            newArchiverPV.setPv("pva://" + channel.getName());
        } else {
            newArchiverPV.setPv(channel.getName());
        }
        newArchiverPV.setSamplingParameters(archiveProperty, policyList);
        newArchiverPV.setPvStatus(pvStaus);
        return newArchiverPV;
    }

    private void configureAA(Map<ArchiveAction, List<ArchivePV>> archivePVS, String aaURL)
            throws JsonProcessingException {
        logger.log(Level.INFO, () -> String.format("Configure PVs %s in %s", archivePVS.toString(), aaURL));

        // Don't request to archive an empty list.
        if (archivePVS.isEmpty()) {
            return;
        }
        if (!archivePVS.get(ArchiveAction.ARCHIVE).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be archived "
                            + archivePVS.get(ArchiveAction.ARCHIVE).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.ARCHIVE)),
                    ArchiveAction.ARCHIVE.endpoint,
                    aaURL);
        }
        if (!archivePVS.get(ArchiveAction.PAUSE).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be paused "
                            + archivePVS.get(ArchiveAction.PAUSE).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.PAUSE).stream()
                            .map(ArchivePV::getPv)
                            .collect(Collectors.toList())),
                    ArchiveAction.PAUSE.endpoint,
                    aaURL);
        }
        if (!archivePVS.get(ArchiveAction.RESUME).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be resumed "
                            + archivePVS.get(ArchiveAction.RESUME).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.RESUME).stream()
                            .map(ArchivePV::getPv)
                            .collect(Collectors.toList())),
                    ArchiveAction.RESUME.endpoint,
                    aaURL);
        }
    }

    private void submitAction(String values, String endpoint, String aaURL) {

        String response = client.post()
                .uri(URI.create(aaURL + MGMT_RESOURCE + endpoint))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(values)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .block();
        logger.log(Level.FINE, () -> response);
    }

    private Map<String, List<String>> getAAsPolicies(Map<String, String> aaURLs) {
        Map<String, List<String>> result = new HashMap<>();
        for (String aaAlias : aaURLs.keySet()) {
            result.put(aaAlias, getAAPolicies(aaURLs.get(aaAlias)));
        }
        return result;
    }

    private List<String> getAAPolicies(String aaURL) {
        try {
            String response = client.get()
                    .uri(URI.create(aaURL + POLICY_RESOURCE))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .block();
            Map<String, String> policyMap = objectMapper.readValue(response, Map.class);
            return new ArrayList<>(policyMap.keySet());
        } catch (Exception e) {
            // problem collecting policies from AA, so warn and return empty list
            logger.log(Level.WARNING, "Could not get AA policies list: " + e.getMessage());
            return List.of();
        }
    }

    enum ArchiveAction {
        ARCHIVE("/archivePV"),
        PAUSE("/pauseArchivingPV"),
        RESUME("/resumeArchivingPV"),
        NONE("");

        private final String endpoint;

        ArchiveAction(final String endpoint) {
            this.endpoint = endpoint;
        }

        public String getEndpoint() {
            return this.endpoint;
        }
    }

    @JsonInclude(Include.NON_NULL)
    public static class ArchivePV {
        private String pv;
        private String samplingMethod;
        private String samplingPeriod;
        private String policy;
        @JsonIgnore
        private String pvStatus;

        @Override
        public String toString() {
            return "ArchivePV{" + "pv='"
                    + pv + '\'' + ", samplingMethod='"
                    + samplingMethod + '\'' + ", samplingPeriod='"
                    + samplingPeriod + '\'' + ", policy='"
                    + policy + '\'' + ", pvStatus='"
                    + pvStatus + '\'' + '}';
        }

        public String getPv() {
            return pv;
        }

        public void setPv(String pv) {
            this.pv = pv;
        }

        /**
         * process the archive property value string to configure the sampling method and rate
         *
         * @param parameters string expected in the form monitor@1.0
         */
        public void setSamplingParameters(String parameters, List<String> policyList) {
            if (parameters.equalsIgnoreCase("default")) {
                return;
            }
            if (policyList.contains(parameters)) {
                setPolicy(parameters);
                return;
            }
            String[] p = parameters.split("@");
            if (p.length == 2) {
                switch (p[0].toUpperCase()) {
                    case "MONITOR":
                        setSamplingMethod("MONITOR");
                        break;
                    case "SCAN":
                        setSamplingMethod("SCAN");
                        break;
                    default:
                        // invalid sampling method
                        logger.log(Level.WARNING, "Invalid sampling method " + p[0]);
                        return;
                }
                // ignore anything after first space
                String sp = p[1].split("\\s")[0];
                // catch number format errors
                try {
                    Float.parseFloat(sp);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid sampling period" + sp);
                    setSamplingMethod(null);
                    return;
                }
                setSamplingPeriod(sp);
            }
        }

        public String getSamplingMethod() {
            return samplingMethod;
        }

        public void setSamplingMethod(String samplingMethod) {
            this.samplingMethod = samplingMethod;
        }

        public String getSamplingPeriod() {
            return samplingPeriod;
        }

        public void setSamplingPeriod(String samplingPeriod) {
            this.samplingPeriod = samplingPeriod;
        }

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }

        public String getPvStatus() {
            return pvStatus;
        }

        public void setPvStatus(String pvStatus) {
            this.pvStatus = pvStatus;
        }
    }
}
