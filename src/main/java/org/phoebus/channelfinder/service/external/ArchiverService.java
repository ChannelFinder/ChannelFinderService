package org.phoebus.channelfinder.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.phoebus.channelfinder.exceptions.ArchiverServiceException;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ArchiverService {
  private static final Logger logger = Logger.getLogger(ArchiverService.class.getName());
  private static final int STATUS_BATCH_SIZE =
      100; // Limit comes from tomcat server maxHttpHeaderSize which by default is a header of size
  // 8k

  private final RestClient client;

  private static final String MGMT_RESOURCE = "/mgmt/bpl";
  private static final String POLICY_RESOURCE = MGMT_RESOURCE + "/getPolicyList";
  private static final String PV_STATUS_RESOURCE = MGMT_RESOURCE + "/getPVStatus";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final MediaType CONTENT_TYPE = MediaType.APPLICATION_JSON;

  private enum StatusResponseKey {
    PV("pv"),
    STATUS("status"),
    PV_NAME("pvName");
    private final String key;

    StatusResponseKey(String key) {
      this.key = key;
    }

    String key() {
      return this.key;
    }
  }

  @Value("${aa.post_support:}")
  private List<String> postSupportArchivers;

  @Autowired
  public ArchiverService(
      RestClient.Builder builder, @Value("${aa.timeout_seconds:15}") int timeoutSeconds) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setReadTimeout(timeoutSeconds * 1000);
    this.client = builder.requestFactory(factory).build();
  }

  ArchiverService(RestClient.Builder builder) {
    this.client = builder.build();
  }

  private Stream<List<String>> partitionSet(Set<String> pvSet, int pageSize) {
    List<String> list = new ArrayList<>(pvSet);
    return IntStream.range(0, (list.size() + pageSize - 1) / pageSize)
        .mapToObj(i -> list.subList(i * pageSize, Math.min(pageSize * (i + 1), list.size())));
  }

  public List<Map<String, String>> getStatuses(
      Map<String, ArchivePVOptions> archivePVS, String archiverURL, String archiverAlias) {
    Set<String> pvs = archivePVS.keySet();
    boolean postSupportOverride = postSupportArchivers.contains(archiverAlias);
    logger.log(Level.INFO, "Archiver Alias: {0}", archiverAlias);
    logger.log(Level.INFO, "Post Support Override Archivers: {0}", postSupportArchivers);

    if (postSupportOverride) {
      logger.log(Level.INFO, "Post Support");
      return getStatusesFromPvListBody(archiverURL, pvs.stream().toList());
    } else {
      logger.log(Level.INFO, "Query Support");
      Stream<List<String>> stream = partitionSet(pvs, STATUS_BATCH_SIZE);

      return stream
          .map(pvList -> getStatusesFromPvListQuery(archiverURL, pvList))
          .flatMap(List::stream)
          .toList();
    }
  }

  private List<Map<String, String>> getStatusesFromPvListQuery(
      String archiverURL, List<String> pvs) {
    String uriString = archiverURL + PV_STATUS_RESOURCE;
    URI pvStatusURI =
        UriComponentsBuilder.fromUri(URI.create(uriString))
            .queryParam(StatusResponseKey.PV.key(), String.join(",", pvs))
            .build()
            .toUri();

    try {
      List<Map<String, String>> result =
          client.get().uri(pvStatusURI).retrieve().body(new ParameterizedTypeReference<>() {});
      return result != null ? result : List.of();
    } catch (Exception e) {
      logger.log(
          Level.WARNING,
          String.format(
              "There was an error getting a response with URI: %s. Error: %s",
              uriString, e.getMessage()));
      return List.of();
    }
  }

  private List<Map<String, String>> getStatusesFromPvListBody(
      String archiverURL, List<String> pvs) {
    String uriString = archiverURL + PV_STATUS_RESOURCE;
    try {
      List<Map<String, String>> result =
          client
              .post()
              .uri(URI.create(uriString))
              .contentType(CONTENT_TYPE)
              .body(pvs)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      return result != null ? result : List.of();
    } catch (Exception e) {
      logger.log(
          Level.WARNING,
          String.format(
              "There was an error getting a response with URI: %s. Error: %s",
              uriString, e.getMessage()));
      return List.of();
    }
  }

  private List<Map<String, String>> sendRequest(Object payload, String uriString) {
    try {
      String values = objectMapper.writeValueAsString(payload);
      List<Map<String, String>> response =
          client
              .post()
              .uri(URI.create(uriString))
              .contentType(CONTENT_TYPE)
              .body(values)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (response == null) {
        throw new ArchiverServiceException("No response from " + uriString);
      }
      return response;
    } catch (Exception e) {
      throw new ArchiverServiceException(
          String.format("Failed to submit %s to %s", payload, uriString), e);
    }
  }

  List<String> submitArchiveAction(List<String> pvs, List<ArchivePVOptions> payload, String aaURL) {
    String endpoint = ArchiveAction.ARCHIVE.getEndpoint();
    String uriString = aaURL + MGMT_RESOURCE + endpoint;
    List<Map<String, String>> response = sendRequest(payload, uriString);
    return validateSubmitActionResponse(pvs, ArchiveAction.ARCHIVE, response);
  }

  List<String> submitBasicAction(List<String> pvs, ArchiveAction action, String aaURL) {
    String endpoint = action.getEndpoint();
    String uriString = aaURL + MGMT_RESOURCE + endpoint;
    List<Map<String, String>> response = sendRequest(pvs, uriString);
    return validateSubmitActionResponse(pvs, action, response);
  }

  private static List<String> validateSubmitActionResponse(
      List<String> pvs, ArchiveAction action, List<Map<String, String>> response) {
    List<String> successfulPvs = new ArrayList<>();
    List<String> failedPvs = new ArrayList<>();
    for (int i = 0; i < response.size(); i++) {
      Map<String, String> result = response.get(i);
      String pv = result.get(StatusResponseKey.PV_NAME.key());
      if (pv == null) {
        pv = result.get(StatusResponseKey.PV.key());
      }
      if (pv == null) {
        pv = (i < pvs.size()) ? pvs.get(i) : "UNKNOWN_PV";
      }
      String status = result.get(StatusResponseKey.STATUS.key());
      if (!action.getSuccessfulStatus().equalsIgnoreCase(status)) {
        failedPvs.add(pv);
      } else {
        logger.log(Level.FINE, "Successfully submitted " + action + " for PV " + pv);
        successfulPvs.add(pv);
      }
    }
    if (!failedPvs.isEmpty()) {
      logger.log(Level.WARNING, "Failed to submit " + action + " for PVs: " + failedPvs);
    }
    return successfulPvs;
  }

  public long configureAA(Map<ArchiveAction, List<ArchivePVOptions>> archivePVS, String aaURL) {
    logger.log(
        Level.INFO, () -> String.format("Configure PVs %s in %s", archivePVS.toString(), aaURL));
    long count = 0;
    // Don't request to archive an empty list.
    if (archivePVS.isEmpty()) {
      return count;
    }

    count += processAction(ArchiveAction.ARCHIVE, archivePVS.get(ArchiveAction.ARCHIVE), aaURL);
    count += processAction(ArchiveAction.PAUSE, archivePVS.get(ArchiveAction.PAUSE), aaURL);
    count += processAction(ArchiveAction.RESUME, archivePVS.get(ArchiveAction.RESUME), aaURL);

    return count;
  }

  private long processAction(ArchiveAction action, List<ArchivePVOptions> options, String aaURL) {
    if (options.isEmpty()) {
      return 0;
    }
    logger.log(
        Level.INFO,
        () -> "Submitting to be " + action.name().toLowerCase() + "d " + options.size() + " pvs");
    List<String> pvs = options.stream().map(ArchivePVOptions::getPv).collect(Collectors.toList());
    try {
      List<String> successfulPvs;
      if (action == ArchiveAction.ARCHIVE) {
        successfulPvs = submitArchiveAction(pvs, options, aaURL);
      } else {
        successfulPvs = submitBasicAction(pvs, action, aaURL);
      }
      return successfulPvs.size();
    } catch (ArchiverServiceException e) {
      logger.log(Level.WARNING, "Failed to submit " + action.name().toLowerCase() + " request", e);
      return 0;
    }
  }

  public List<String> getAAPolicies(String aaURL) {
    if (StringUtils.isEmpty(aaURL)) {
      return List.of();
    }
    try {
      String uriString = aaURL + POLICY_RESOURCE;
      Map<String, String> policyMap =
          client
              .get()
              .uri(URI.create(uriString))
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (policyMap == null) {
        return List.of();
      }
      return new ArrayList<>(policyMap.keySet());
    } catch (Exception e) {
      // problem collecting policies from AA, so warn and return empty list
      logger.log(Level.WARNING, "Could not get AA policies list from: " + aaURL, e);
      return List.of();
    }
  }
}
