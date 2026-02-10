package org.phoebus.channelfinder.service.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.exceptions.ArchiverServiceException;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchiveAction;
import org.phoebus.channelfinder.service.model.archiver.aa.ArchivePVOptions;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArchiverServiceTest {

  private MockWebServer mockWebServer;
  private ArchiverService archiverService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    archiverService = new ArchiverService();
    objectMapper = new ObjectMapper();
    ReflectionTestUtils.setField(archiverService, "timeoutSeconds", 5);
    ReflectionTestUtils.setField(archiverService, "postSupportArchivers", List.of("test-archiver"));
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void testGetStatusesQuery() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "Being archived"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<Map<String, String>> result =
        archiverService.getStatuses(pvs, archiverUrl, "other-archiver");

    assertEquals(1, result.size());
    assertEquals("pv1", result.get(0).get("pv"));
    assertEquals("Being archived", result.get(0).get("status"));
  }

  @Test
  void testGetStatusesPost() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "Being archived"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<Map<String, String>> result =
        archiverService.getStatuses(pvs, archiverUrl, "test-archiver");

    assertEquals(1, result.size());
    assertEquals("pv1", result.get(0).get("pv"));
    assertEquals("Being archived", result.get(0).get("status"));
  }

  @Test
  void testGetStatusesInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();
    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    List<Map<String, String>> result =
        archiverService.getStatuses(pvs, archiverUrl, "other-archiver");

    assertTrue(result.isEmpty());
  }

  @Test
  void testSubmitAction() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "ok"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitAction(pvs, pvs, ArchiveAction.NONE, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.get(0));
  }

  @Test
  void testSubmitActionStatusNotOk() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "failed"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitAction(pvs, pvs, ArchiveAction.NONE, archiverUrl);
    assertTrue(successfulPvs.isEmpty());
  }

  @Test
  void testSubmitActionPartialFailure() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1", "pv2");
    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "ok"), Map.of("pv", "pv2", "status", "failed"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitAction(pvs, pvs, ArchiveAction.NONE, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.get(0));
  }

  @Test
  void testSubmitActionInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    assertThrows(
        ArchiverServiceException.class,
        () -> archiverService.submitAction(pvs, pvs, ArchiveAction.NONE, archiverUrl));
  }

  @Test
  void testConfigureAAArchive() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(options),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of());

    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "Archive request submitted"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(1, count);
  }

  @Test
  void testConfigureAAPause() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(),
            ArchiveAction.PAUSE, List.of(options),
            ArchiveAction.RESUME, List.of());

    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "ok"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(1, count);
  }

  @Test
  void testConfigureAAResume() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of(options));

    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "ok"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(1, count);
  }

  @Test
  void testConfigureAAStatusNotOk() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(options),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of());

    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "failed"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(0, count);
  }

  @Test
  void testConfigureAAInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(options),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of());

    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(0, count);
  }

  @Test
  void testGetAAPolicies() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    Map<String, String> policies = Map.of("policy1", "desc1", "policy2", "desc2");

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(policies))
            .addHeader("Content-Type", "application/json"));

    List<String> result = archiverService.getAAPolicies(archiverUrl);

    assertEquals(2, result.size());
    assertTrue(result.contains("policy1"));
    assertTrue(result.contains("policy2"));
  }

  @Test
  void testGetAAPoliciesInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    List<String> result = archiverService.getAAPolicies(archiverUrl);

    assertTrue(result.isEmpty());
  }
}
