package org.phoebus.channelfinder.processors.aa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
public class ArchiverClient {
    private static final Logger logger = Logger.getLogger(ArchiverClient.class.getName());
    private static final int STATUS_BATCH_SIZE = 100; // Limit comes from tomcat server maxHttpHeaderSize which by default is a header of size 8k
    private static final List<String> AA_STATUS_ENDPOINT_ONLY_SUPPORT_QUERY_VERSION = List.of("1.1.0", 
            "Before_JDK_12_Upgrade", 
            "v0.0.1_SNAPSHOT_03-November-2015", 
            "v0.0.1_SNAPSHOT_09-Oct-2018", 
            "v0.0.1_SNAPSHOT_10-June-2017", 
            "v0.0.1_SNAPSHOT_10-Sep-2015", 
            "v0.0.1_SNAPSHOT_12-May-2016", 
            "v0.0.1_SNAPSHOT_12-Oct-2016", 
            "v0.0.1_SNAPSHOT_13-Nov-2019", 
            "v0.0.1_SNAPSHOT_14-Jun-2018", 
            "v0.0.1_SNAPSHOT_15-Nov-2018", 
            "v0.0.1_SNAPSHOT_20-Sept-2016", 
            "v0.0.1_SNAPSHOT_22-June-2016", 
            "v0.0.1_SNAPSHOT_22-June-2017", 
            "v0.0.1_SNAPSHOT_23-Sep-2015", 
            "v0.0.1_SNAPSHOT_26-January-2016", 
            "v0.0.1_SNAPSHOT_27-Nov-2017", 
            "v0.0.1_SNAPSHOT_29-July-2015", 
            "v0.0.1_SNAPSHOT_30-March-2016", 
            "v0.0.1_SNAPSHOT_30-September-2021");

    private final WebClient client = WebClient.create();

    private static final String MGMT_RESOURCE = "/mgmt/bpl";
    private static final String POLICY_RESOURCE = MGMT_RESOURCE + "/getPolicyList";
    private static final String PV_STATUS_RESOURCE = MGMT_RESOURCE + "/getPVStatus";
    private static final String ARCHIVER_VERSIONS_RESOURCE = MGMT_RESOURCE + "/getVersions";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int TIMEOUT_SECONDS = 15;

    private Stream<List<String>> partitionSet(Set<String> pvSet, int pageSize) {
        List<String> list = new ArrayList<>(pvSet);
        return IntStream.range(0, (list.size() + pageSize - 1) / pageSize)
                .mapToObj(i -> list.subList(i * pageSize, Math.min(pageSize * (i + 1), list.size())));
    }

    List<Map<String, String>> getStatuses(Map<String, ArchivePVOptions> archivePVS, String archiverURL, String archiverVersion) throws JsonProcessingException {
        Set<String> pvs = archivePVS.keySet();
        if (AA_STATUS_ENDPOINT_ONLY_SUPPORT_QUERY_VERSION.contains(archiverVersion)) {

            Stream<List<String>> stream = partitionSet(pvs, STATUS_BATCH_SIZE);

            return stream.map(pvList -> getStatusesFromPvListQuery(archiverURL, pvList)).flatMap(List::stream).toList();
        } else {
            return getStatusesFromPvListBody(archiverURL, pvs.stream().toList());
        }
    }

    private List<Map<String, String>> getStatusesFromPvListQuery(String archiverURL, List<String> pvs) {
        URI pvStatusURI = UriComponentsBuilder.fromUri(URI.create(archiverURL + PV_STATUS_RESOURCE))
                .queryParam("pv", pvs)
                .build()
                .toUri();

        String response = client.get()
                .uri(pvStatusURI)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                .block();

        try {
            return objectMapper
                    .readValue(response, new TypeReference<>() {
                    });
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Could not parse pv status response: " + e.getMessage());
        }
        return List.of();
    }

    private List<Map<String, String>> getStatusesFromPvListBody(String archiverURL, List<String> pvs) {
        String response = client.post()
                    .uri(URI.create(archiverURL + PV_STATUS_RESOURCE))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(pvs)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                    .block();

        // Structure of response is
        // [{"pvName":"PV:1", "status":"Paused", ... }, {"pvName": "PV:2"}, {"status": "Being archived"}, ...}, ...
        // ]

        try {
            return objectMapper
                    .readValue(response, new TypeReference<>() {
                    });
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Could not parse pv status response: " + e.getMessage());
        }
        return List.of();
    }

    private void submitAction(String values, String endpoint, String aaURL) {
        try {
            String response = client.post()
                    .uri(URI.create(aaURL + MGMT_RESOURCE + endpoint))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(values)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                    .block();
            logger.log(Level.FINE, () -> response);

        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("Failed to submit %s to %s on %s", values, endpoint, aaURL), e);
        }
    }


    long configureAA(Map<ArchiveAction, List<ArchivePVOptions>> archivePVS, String aaURL)
            throws JsonProcessingException {
        logger.log(Level.INFO, () -> String.format("Configure PVs %s in %s", archivePVS.toString(), aaURL));
        long count = 0;
        // Don't request to archive an empty list.
        if (archivePVS.isEmpty()) {
            return count;
        }
        if (!archivePVS.get(ArchiveAction.ARCHIVE).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be archived "
                            + archivePVS.get(ArchiveAction.ARCHIVE).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.ARCHIVE)),
                    ArchiveAction.ARCHIVE.getEndpoint(),
                    aaURL);
            count += archivePVS.get(ArchiveAction.ARCHIVE).size();
        }
        if (!archivePVS.get(ArchiveAction.PAUSE).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be paused "
                            + archivePVS.get(ArchiveAction.PAUSE).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.PAUSE).stream()
                            .map(ArchivePVOptions::getPv)
                            .collect(Collectors.toList())),
                    ArchiveAction.PAUSE.getEndpoint(),
                    aaURL);
            count += archivePVS.get(ArchiveAction.PAUSE).size();
        }
        if (!archivePVS.get(ArchiveAction.RESUME).isEmpty()) {
            logger.log(
                    Level.INFO,
                    () -> "Submitting to be resumed "
                            + archivePVS.get(ArchiveAction.RESUME).size() + " pvs");
            submitAction(
                    objectMapper.writeValueAsString(archivePVS.get(ArchiveAction.RESUME).stream()
                            .map(ArchivePVOptions::getPv)
                            .collect(Collectors.toList())),
                    ArchiveAction.RESUME.getEndpoint(),
                    aaURL);
            count += archivePVS.get(ArchiveAction.RESUME).size();
        }
        return count;
    }

    List<String> getAAPolicies(String aaURL) {
        if (StringUtils.isEmpty(aaURL)) {
            return List.of();
        }
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
            logger.log(Level.WARNING, "Could not get AA policies list from: " + aaURL, e);
            return List.of();
        }
    }

    String getVersion(String archiverURL) {
        try {

            String response = client.get()
                    .uri(URI.create(archiverURL + ARCHIVER_VERSIONS_RESOURCE))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                    .block();
            Map<String, String> versionMap = objectMapper.readValue(response, Map.class);
            String[] mgmtVersion = versionMap.get("mgmt_version").split("Archiver Appliance Version ");
            if (mgmtVersion.length > 1) {
                return mgmtVersion[1];
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not get version from: " + archiverURL, e);
            return "";
        }
        return "";
    }
}
