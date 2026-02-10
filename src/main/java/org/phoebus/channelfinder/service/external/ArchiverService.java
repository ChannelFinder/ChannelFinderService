package org.phoebus.channelfinder.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.commons.lang3.StringUtils;
import org.phoebus.channelfinder.exceptions.ArchiverServiceException;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ArchiverService {
  private static final Logger logger = Logger.getLogger(ArchiverService.class.getName());
  private static final int STATUS_BATCH_SIZE =
      100; // Limit comes from tomcat server maxHttpHeaderSize which by default is a header of size
  // 8k

  private final WebClient client = webClient();

  private static final String MGMT_RESOURCE = "/mgmt/bpl";
  private static final String POLICY_RESOURCE = MGMT_RESOURCE + "/getPolicyList";
  private static final String PV_STATUS_RESOURCE = MGMT_RESOURCE + "/getPVStatus";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final String MGMT_VERSION_KEY = "mgmt_version";
  private static final MediaType CONTENT_TYPE = MediaType.APPLICATION_JSON;

  private enum StatusResponseKey {
    PV("pv"),
    STATUS("status");
    private final String key;

    StatusResponseKey(String key) {
      this.key = key;
    }

    String key() {
      return this.key;
    }
  }

  @Value("${aa.timeout_seconds:15}")
  private int timeoutSeconds;

  @Value("${aa.post_support:}")
  private List<String> postSupportArchivers;

  private static WebClient webClient() {
    final int size = (int) DataSize.ofMegabytes(16).toBytes();
    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();
    return WebClient.builder().exchangeStrategies(strategies).build();
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

    return client
        .get()
        .uri(pvStatusURI)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<Map<String, String>>() {})
        .timeout(Duration.of(timeoutSeconds, ChronoUnit.SECONDS))
        .onErrorResume(e -> showError(uriString, e).thenMany(Flux.empty()))
        .collectList()
        .block();
  }

  private List<Map<String, String>> getStatusesFromPvListBody(
      String archiverURL, List<String> pvs) {
    String uriString = archiverURL + PV_STATUS_RESOURCE;
    return client
        .post()
        .uri(URI.create(uriString))
        .contentType(CONTENT_TYPE)
        .bodyValue(pvs)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<Map<String, String>>() {})
        .timeout(Duration.of(timeoutSeconds, ChronoUnit.SECONDS))
        .onErrorResume(e -> showError(uriString, e).thenMany(Flux.empty()))
        .collectList()
        .block();
  }

  List<String> submitAction(List<String> pvs, Object payload, ArchiveAction action, String aaURL) {
    String uriString = aaURL + MGMT_RESOURCE + action.getEndpoint();
    List<Map<String, String>> response;
    try {
      String values = objectMapper.writeValueAsString(payload);
      response =
          client
              .post()
              .uri(URI.create(uriString))
              .contentType(CONTENT_TYPE)
              .bodyValue(values)
              .retrieve()
              .bodyToFlux(new ParameterizedTypeReference<Map<String, String>>() {})
              .timeout(Duration.of(timeoutSeconds, ChronoUnit.SECONDS))
              .collectList()
              .block();
    } catch (Exception e) {
      throw new ArchiverServiceException(
          String.format("Failed to submit %s to %s on %s", payload, action, aaURL), e);
    }

    if (response == null) {
      throw new ArchiverServiceException("No response from " + uriString);
    }

    return validateSubmitActionResponse(pvs, action, response);
  }

  private static List<String> validateSubmitActionResponse(
      List<String> pvs, ArchiveAction action, List<Map<String, String>> response) {
    List<String> successfulPvs = new ArrayList<>();
    List<String> failedPvs = new ArrayList<>();
    for (int i = 0; i < response.size(); i++) {
      Map<String, String> result = response.get(i);
      String pv = (i < pvs.size()) ? pvs.get(i) : "UNKNOWN_PV";
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
    if (!archivePVS.get(ArchiveAction.ARCHIVE).isEmpty()) {
      List<ArchivePVOptions> archiveOptions = archivePVS.get(ArchiveAction.ARCHIVE);
      logger.log(Level.INFO, () -> "Submitting to be archived " + archiveOptions.size() + " pvs");
      List<String> pvs =
          archiveOptions.stream().map(ArchivePVOptions::getPv).collect(Collectors.toList());
      try {
        List<String> successfulPvs =
            submitAction(pvs, archiveOptions, ArchiveAction.ARCHIVE, aaURL);
        count += successfulPvs.size();
      } catch (ArchiverServiceException e) {
        logger.log(Level.WARNING, "Failed to submit archive request", e);
      }
    }
    if (!archivePVS.get(ArchiveAction.PAUSE).isEmpty()) {
      List<ArchivePVOptions> pauseOptions = archivePVS.get(ArchiveAction.PAUSE);
      logger.log(Level.INFO, () -> "Submitting to be paused " + pauseOptions.size() + " pvs");
      List<String> pvs =
          pauseOptions.stream().map(ArchivePVOptions::getPv).collect(Collectors.toList());
      try {
        List<String> successfulPvs = submitAction(pvs, pvs, ArchiveAction.PAUSE, aaURL);
        count += successfulPvs.size();
      } catch (ArchiverServiceException e) {
        logger.log(Level.WARNING, "Failed to submit pause request", e);
      }
    }
    if (!archivePVS.get(ArchiveAction.RESUME).isEmpty()) {
      List<ArchivePVOptions> resumeOptions = archivePVS.get(ArchiveAction.RESUME);
      logger.log(Level.INFO, () -> "Submitting to be resumed " + resumeOptions.size() + " pvs");
      List<String> pvs =
          resumeOptions.stream().map(ArchivePVOptions::getPv).collect(Collectors.toList());
      try {
        List<String> successfulPvs = submitAction(pvs, pvs, ArchiveAction.RESUME, aaURL);
        count += successfulPvs.size();
      } catch (ArchiverServiceException e) {
        logger.log(Level.WARNING, "Failed to submit resume request", e);
      }
    }
    return count;
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
              .bodyToFlux(new ParameterizedTypeReference<Map<String, String>>() {})
              .timeout(Duration.of(10, ChronoUnit.SECONDS))
              .onErrorResume(e -> showError(uriString, e).thenMany(Flux.empty()))
              .next()
              .block();
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

  private Mono<String> showError(String uriString, Throwable error) {
    logger.log(
        Level.WARNING,
        String.format(
            "There was an error getting a response with URI: %s. Error: %s",
            uriString, error.getMessage()));
    return Mono.empty();
  }
}
