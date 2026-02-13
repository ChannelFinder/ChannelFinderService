package org.phoebus.channelfinder.service.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @ValueSource(strings = {"other-archiver", "test-archiver"})
  void testGetStatuses(String archiverAlias) throws JsonProcessingException, InterruptedException {
    String archiverUrl = mockWebServer.url("/").toString();
    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "Being archived"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<Map<String, String>> result = archiverService.getStatuses(pvs, archiverUrl, archiverAlias);

    assertEquals(1, result.size());
    assertEquals("pv1", result.getFirst().get("pv"));
    assertEquals("Being archived", result.getFirst().get("status"));

    RecordedRequest request = mockWebServer.takeRequest();
    if ("test-archiver".equals(archiverAlias)) {
      assertEquals("POST", request.getMethod());
      assertEquals("//mgmt/bpl/getPVStatus", request.getPath());
      assertEquals("[\"pv1\"]", request.getBody().readUtf8());
    } else {
      assertEquals("GET", request.getMethod());
      assertTrue(request.getPath().startsWith("/mgmt/bpl/getPVStatus?pv=pv1"));
    }
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
  void testSubmitBasicAction() throws JsonProcessingException, InterruptedException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "ok"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.getFirst());

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("//mgmt/bpl", request.getPath());
    assertEquals("[\"pv1\"]", request.getBody().readUtf8());
  }

  @Test
  void testSubmitBasicActionStatusNotOk() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "failed"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, archiverUrl);
    assertTrue(successfulPvs.isEmpty());
  }

  @Test
  void testSubmitBasicActionPartialFailure() throws JsonProcessingException {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1", "pv2");
    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "ok"), Map.of("pv", "pv2", "status", "failed"));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.getFirst());
  }

  @Test
  void testSubmitBasicActionInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("pv1");
    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    assertThrows(
        ArchiverServiceException.class,
        () -> archiverService.submitBasicAction(pvs, ArchiveAction.NONE, archiverUrl));
  }

  @ParameterizedTest
  @EnumSource(ArchiveAction.class)
  void testConfigureAA(ArchiveAction action) throws JsonProcessingException, InterruptedException {
    String archiverUrl = mockWebServer.url("/").toString();
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");

    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS = new EnumMap<>(ArchiveAction.class);
    archivePVS.put(ArchiveAction.ARCHIVE, List.of());
    archivePVS.put(ArchiveAction.PAUSE, List.of());
    archivePVS.put(ArchiveAction.RESUME, List.of());
    archivePVS.put(action, List.of(options));

    String status = action == ArchiveAction.ARCHIVE ? "Archive request submitted" : "ok";
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", status));
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    long count = archiverService.configureAA(archivePVS, archiverUrl);

    assertEquals(action != ArchiveAction.NONE ? 1 : 0, count);

    if (action == ArchiveAction.NONE) {
      assertEquals(0, mockWebServer.getRequestCount());
      return;
    }

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("//mgmt/bpl" + action.getEndpoint(), request.getPath());

    if (action == ArchiveAction.ARCHIVE) {
      // For archive, we send the list of ArchivePVOptions objects
      String expectedBody = objectMapper.writeValueAsString(List.of(options));
      assertEquals(expectedBody, request.getBody().readUtf8());
    } else {
      // For pause/resume, we send the list of PV names
      assertEquals("[\"pv1\"]", request.getBody().readUtf8());
    }
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
  void testGetAAPolicies() throws JsonProcessingException, InterruptedException {
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

    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("GET", request.getMethod());
    assertEquals("//mgmt/bpl/getPolicyList", request.getPath());
  }

  @Test
  void testGetAAPoliciesInvalidResponse() {
    String archiverUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
        new MockResponse().setBody("invalid-json").addHeader("Content-Type", "application/json"));

    List<String> result = archiverService.getAAPolicies(archiverUrl);

    assertTrue(result.isEmpty());
  }

  @Test
  void testSubmitActionWithRealResponseResume() {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("PV1", "PV2");
    String responseBody =
        "[{\"pvName\":\"PV1\",\"engine_desc\":\"Successfully resumed the archiving of PV PV1\",\"engine_pvName\":\"PV1\",\"engine_status\":\"ok\",\"status\":\"ok\"},{\"pvName\":\"PV2\",\"engine_desc\":\"Successfully resumed the archiving of PV PV2\",\"engine_pvName\":\"PV2\",\"engine_status\":\"ok\",\"status\":\"ok\"}]";

    mockWebServer.enqueue(
        new MockResponse().setBody(responseBody).addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.RESUME, archiverUrl);
    assertEquals(2, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
    assertTrue(successfulPvs.contains("PV2"));
  }

  @Test
  void testSubmitActionWithRealResponsePause() {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("PV1");
    String responseBody =
        "[{\"pvName\":\"PV1\",\"engine_desc\":\"Successfully paused the archiving of PV PV1\",\"engine_pvName\":\"PV1\",\"engine_status\":\"ok\",\"etl_status\":\"ok\",\"etl_desc\":\"Successfully removed PV PV1 from the cluster\",\"etl_pvName\":\"PV1\",\"status\":\"ok\"}]";

    mockWebServer.enqueue(
        new MockResponse().setBody(responseBody).addHeader("Content-Type", "application/json"));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.PAUSE, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
  }

  @Test
  void testSubmitActionWithRealResponseArchive() {
    String archiverUrl = mockWebServer.url("/").toString();
    List<String> pvs = List.of("PV1");
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("PV1");
    List<ArchivePVOptions> payload = List.of(options);
    String responseBody = "[{ \"pvName\": \"PV1\", \"status\": \"Archive request submitted\" }]";

    mockWebServer.enqueue(
        new MockResponse().setBody(responseBody).addHeader("Content-Type", "application/json"));

    List<String> successfulPvs = archiverService.submitArchiveAction(pvs, payload, archiverUrl);
    assertEquals(1, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
  }
}
