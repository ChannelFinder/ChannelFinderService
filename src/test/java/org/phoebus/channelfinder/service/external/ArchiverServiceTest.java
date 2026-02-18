package org.phoebus.channelfinder.service.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class ArchiverServiceTest {

  private static final String ARCHIVER_URL = "http://localhost:17665";
  private MockRestServiceServer mockServer;
  private ArchiverService archiverService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();

    archiverService = new ArchiverService(builder);
    objectMapper = new ObjectMapper();

    ReflectionTestUtils.setField(archiverService, "postSupportArchivers", List.of("test-archiver"));
  }

  @AfterEach
  void tearDown() {
    mockServer.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {"other-archiver", "test-archiver"})
  void testGetStatuses(String archiverAlias) throws JsonProcessingException {

    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "Being archived"));

    if ("test-archiver".equals(archiverAlias)) {
      mockServer
          .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl/getPVStatus"))
          .andExpect(method(HttpMethod.POST))
          .andExpect(content().json("[\"pv1\"]"))
          .andRespond(
              withSuccess(
                  objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));
    } else {
      mockServer
          .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl/getPVStatus?pv=pv1"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(
              withSuccess(
                  objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));
    }

    List<Map<String, String>> result =
        archiverService.getStatuses(pvs, ARCHIVER_URL, archiverAlias);

    assertEquals(1, result.size());
    assertEquals("pv1", result.getFirst().get("pv"));
    assertEquals("Being archived", result.getFirst().get("status"));
  }

  @Test
  void testGetStatusesInvalidResponse() {

    Map<String, ArchivePVOptions> pvs = Map.of("pv1", new ArchivePVOptions());

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl/getPVStatus?pv=pv1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("invalid-json", MediaType.APPLICATION_JSON));

    List<Map<String, String>> result =
        archiverService.getStatuses(pvs, ARCHIVER_URL, "other-archiver");

    assertTrue(result.isEmpty());
  }

  @Test
  void testSubmitBasicAction() throws JsonProcessingException {

    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "ok"));

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("[\"pv1\"]"))
        .andRespond(
            withSuccess(
                objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, ARCHIVER_URL);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.getFirst());
  }

  @Test
  void testSubmitBasicActionStatusNotOk() throws JsonProcessingException {

    List<String> pvs = List.of("pv1");
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "failed"));

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, ARCHIVER_URL);
    assertTrue(successfulPvs.isEmpty());
  }

  @Test
  void testSubmitBasicActionPartialFailure() throws JsonProcessingException {

    List<String> pvs = List.of("pv1", "pv2");
    List<Map<String, String>> expectedResponse =
        List.of(Map.of("pv", "pv1", "status", "ok"), Map.of("pv", "pv2", "status", "failed"));

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.NONE, ARCHIVER_URL);
    assertEquals(1, successfulPvs.size());
    assertEquals("pv1", successfulPvs.getFirst());
  }

  @Test
  void testSubmitBasicActionInvalidResponse() {

    List<String> pvs = List.of("pv1");

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("invalid-json", MediaType.APPLICATION_JSON));

    assertThrows(
        ArchiverServiceException.class,
        () -> archiverService.submitBasicAction(pvs, ArchiveAction.NONE, ARCHIVER_URL));
  }

  @ParameterizedTest
  @EnumSource(ArchiveAction.class)
  void testConfigureAA(ArchiveAction action) throws JsonProcessingException {

    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");

    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS = new EnumMap<>(ArchiveAction.class);
    archivePVS.put(ArchiveAction.ARCHIVE, List.of());
    archivePVS.put(ArchiveAction.PAUSE, List.of());
    archivePVS.put(ArchiveAction.RESUME, List.of());
    archivePVS.put(action, List.of(options));

    String status = action == ArchiveAction.ARCHIVE ? "Archive request submitted" : "ok";
    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", status));

    if (action != ArchiveAction.NONE) {
      String endpoint = action.getEndpoint();
      String expectedBody;
      if (action == ArchiveAction.ARCHIVE) {
        expectedBody = objectMapper.writeValueAsString(List.of(options));
      } else {
        expectedBody = "[\"pv1\"]";
      }

      mockServer
          .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + endpoint))
          .andExpect(method(HttpMethod.POST))
          .andExpect(content().json(expectedBody))
          .andRespond(
              withSuccess(
                  objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));
    }

    long count = archiverService.configureAA(archivePVS, ARCHIVER_URL);

    assertEquals(action != ArchiveAction.NONE ? 1 : 0, count);
  }

  @Test
  void testConfigureAAStatusNotOk() throws JsonProcessingException {

    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(options),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of());

    List<Map<String, String>> expectedResponse = List.of(Map.of("pv", "pv1", "status", "failed"));

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + ArchiveAction.ARCHIVE.getEndpoint()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

    long count = archiverService.configureAA(archivePVS, ARCHIVER_URL);

    assertEquals(0, count);
  }

  @Test
  void testConfigureAAInvalidResponse() {

    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("pv1");
    Map<ArchiveAction, List<ArchivePVOptions>> archivePVS =
        Map.of(
            ArchiveAction.ARCHIVE, List.of(options),
            ArchiveAction.PAUSE, List.of(),
            ArchiveAction.RESUME, List.of());

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + ArchiveAction.ARCHIVE.getEndpoint()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("invalid-json", MediaType.APPLICATION_JSON));

    long count = archiverService.configureAA(archivePVS, ARCHIVER_URL);

    assertEquals(0, count);
  }

  @Test
  void testGetAAPolicies() throws JsonProcessingException {

    Map<String, String> policies = Map.of("policy1", "desc1", "policy2", "desc2");

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl/getPolicyList"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(objectMapper.writeValueAsString(policies), MediaType.APPLICATION_JSON));

    List<String> result = archiverService.getAAPolicies(ARCHIVER_URL);

    assertEquals(2, result.size());
    assertTrue(result.contains("policy1"));
    assertTrue(result.contains("policy2"));
  }

  @Test
  void testGetAAPoliciesInvalidResponse() {

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl/getPolicyList"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("invalid-json", MediaType.APPLICATION_JSON));

    List<String> result = archiverService.getAAPolicies(ARCHIVER_URL);

    assertTrue(result.isEmpty());
  }

  @Test
  void testSubmitActionWithRealResponseResume() {

    List<String> pvs = List.of("PV1", "PV2");
    String responseBody =
        "[{\"pvName\":\"PV1\",\"engine_desc\":\"Successfully resumed the archiving of PV PV1\",\"engine_pvName\":\"PV1\",\"engine_status\":\"ok\",\"status\":\"ok\"},{\"pvName\":\"PV2\",\"engine_desc\":\"Successfully resumed the archiving of PV PV2\",\"engine_pvName\":\"PV2\",\"engine_status\":\"ok\",\"status\":\"ok\"}]";

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + ArchiveAction.RESUME.getEndpoint()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("[\"PV1\",\"PV2\"]"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.RESUME, ARCHIVER_URL);
    assertEquals(2, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
    assertTrue(successfulPvs.contains("PV2"));
  }

  @Test
  void testSubmitActionWithRealResponsePause() {

    List<String> pvs = List.of("PV1");
    String responseBody =
        "[{\"pvName\":\"PV1\",\"engine_desc\":\"Successfully paused the archiving of PV PV1\",\"engine_pvName\":\"PV1\",\"engine_status\":\"ok\",\"etl_status\":\"ok\",\"etl_desc\":\"Successfully removed PV PV1 from the cluster\",\"etl_pvName\":\"PV1\",\"status\":\"ok\"}]";

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + ArchiveAction.PAUSE.getEndpoint()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("[\"PV1\"]"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    List<String> successfulPvs =
        archiverService.submitBasicAction(pvs, ArchiveAction.PAUSE, ARCHIVER_URL);
    assertEquals(1, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
  }

  @Test
  void testSubmitActionWithRealResponseArchive() throws JsonProcessingException {

    List<String> pvs = List.of("PV1");
    ArchivePVOptions options = new ArchivePVOptions();
    options.setPv("PV1");
    List<ArchivePVOptions> payload = List.of(options);
    String responseBody = "[{ \"pvName\": \"PV1\", \"status\": \"Archive request submitted\" }]";

    mockServer
        .expect(requestTo(ARCHIVER_URL + "/mgmt/bpl" + ArchiveAction.ARCHIVE.getEndpoint()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json(objectMapper.writeValueAsString(payload)))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    List<String> successfulPvs = archiverService.submitArchiveAction(pvs, payload, ARCHIVER_URL);
    assertEquals(1, successfulPvs.size());
    assertTrue(successfulPvs.contains("PV1"));
  }
}
