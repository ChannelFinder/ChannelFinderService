package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.processors.aa.AAChannelProcessor;
import org.phoebus.channelfinder.processors.aa.ArchiveAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.activeProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.inactiveProperty;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(value = "classpath:application_test_multi.properties")
class AAChannelProcessorMultiArchiverIT {

    public static final String BEING_ARCHIVED = "Being archived";
    public static final String PAUSED = "Paused";
    public static final String NOT_BEING_ARCHIVED = "Not being archived";
    public static final String OWNER = "owner";
    @Autowired
    AAChannelProcessor aaChannelProcessor;

    MockWebServer mockQueryArchiverAppliance;
    MockWebServer mockPostArchiverAppliance;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockQueryArchiverAppliance = new MockWebServer();
        mockQueryArchiverAppliance.start(17664);
        mockPostArchiverAppliance = new MockWebServer();
        mockPostArchiverAppliance.start(17665);

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void teardown() throws IOException {
        mockQueryArchiverAppliance.shutdown();
        mockPostArchiverAppliance.shutdown();
    }

    static Stream<Arguments> provideArguments() {
        List<Channel> channels = List.of(
            new Channel("PVArchivedActive", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVPausedActive", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVNoneActive0", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVArchivedInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVPausedInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVNoneInactive", OWNER, List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVArchivedNotag", OWNER, List.of(), List.of()),
            new Channel("PVNoneActive1", OWNER, List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVNoneActive2", OWNER, List.of(archiveProperty, activeProperty), List.of())
        );

        Map<String, String> namesToStatuses = Map.of(
            "PVArchivedActive", BEING_ARCHIVED,
            "PVPausedActive", PAUSED,
            "PVNoneActive0", NOT_BEING_ARCHIVED,
            "PVArchivedInactive", BEING_ARCHIVED,
            "PVPausedInactive", PAUSED,
            "PVNoneInactive", NOT_BEING_ARCHIVED,
            "PVArchivedNotag", BEING_ARCHIVED,
            "PVNoneActive1", NOT_BEING_ARCHIVED,
            "PVNoneActive2", NOT_BEING_ARCHIVED
        );
        Map<ArchiveAction, List<String>> actionsToNames = Map.of(
            ArchiveAction.RESUME, List.of("PVPausedActive"),
            ArchiveAction.PAUSE, List.of("PVArchivedInactive", "PVArchivedNotag"),
            ArchiveAction.ARCHIVE, List.of("PVNoneActive0", "PVNoneActive1", "PVNoneActive2")
        );
        int expectedProcessedChannels = 12;

        List<String> massPVNames = IntStream.range(1, 100).mapToObj(i -> "PV" + i).toList();
        return Stream.of(
            Arguments.of(channels, namesToStatuses, actionsToNames, expectedProcessedChannels),
            Arguments.of(
                massPVNames.stream().map(s -> new Channel(s, OWNER, List.of(archiveProperty, activeProperty), List.of())).toList(),
                massPVNames.stream().collect(Collectors.toMap(String::toString, e -> NOT_BEING_ARCHIVED)),
                Map.of(ArchiveAction.ARCHIVE, massPVNames),
                massPVNames.size()
            ));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void testProcessMultiArchivers(List<Channel> channels,
                          Map<String, String> namesToStatuses,
                          Map<ArchiveAction, List<String>> actionsToNames,
                          int expectedProcessedChannels)
        throws JsonProcessingException, InterruptedException {

        // Request to version
        Map<String, String> queryVersions = Map.of("mgmt_version", "Archiver Appliance Version 1.1.0 Query Support");
        mockQueryArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(queryVersions))
            .addHeader("Content-Type", "application/json"));

        // Request to policies
        Map<String, String> policyList = Map.of("policy", "description");
        mockQueryArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(policyList))
            .addHeader("Content-Type", "application/json"));

        // Request to archiver status
        List<Map<String, String>> archivePVStatuses =
            namesToStatuses.entrySet().stream().map(entry -> Map.of("pvName", entry.getKey(), "status", entry.getValue())).toList();
        mockQueryArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(archivePVStatuses))
            .addHeader("Content-Type", "application/json"));

        // Requests to archiver
        actionsToNames.forEach((key, value) -> {
            List<Map<String, String>> archiverResponse =
                value.stream().map(channel -> Map.of("pvName", channel, "status", key + " request submitted")).toList();
            try {
                mockQueryArchiverAppliance.enqueue(new MockResponse()
                    .setBody(objectMapper.writeValueAsString(archiverResponse))
                    .addHeader("Content-Type", "application/json"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });


        // Request to query version
        Map<String, String> postVersions = Map.of("mgmt_version", "Archiver Appliance Version 1.1.0 Post Support");
        mockPostArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(postVersions))
            .addHeader("Content-Type", "application/json"));

        // Request to policies
        mockPostArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(policyList))
            .addHeader("Content-Type", "application/json"));

        // Request to archiver status
        mockPostArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(archivePVStatuses))
            .addHeader("Content-Type", "application/json"));

        // Requests to archiver
        actionsToNames.forEach((key, value) -> {
            List<Map<String, String>> archiverResponse =
                value.stream().map(channel -> Map.of("pvName", channel, "status", key + " request submitted")).toList();
            try {
                mockPostArchiverAppliance.enqueue(new MockResponse()
                    .setBody(objectMapper.writeValueAsString(archiverResponse))
                    .addHeader("Content-Type", "application/json"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        aaChannelProcessor.process(channels);

        AtomicInteger expectedQueryRequests = new AtomicInteger(1);
        RecordedRequest requestQueryVersion = mockQueryArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestQueryVersion != null;
        assertEquals("/mgmt/bpl/getVersions", requestQueryVersion.getPath());

        expectedQueryRequests.addAndGet(1);
        RecordedRequest requestQueryPolicy = mockQueryArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestQueryPolicy != null;
        assertEquals("/mgmt/bpl/getPolicyList", requestQueryPolicy.getPath());

        expectedQueryRequests.addAndGet(1);
        RecordedRequest requestQueryStatus = mockQueryArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestQueryStatus != null;
        assert requestQueryStatus.getRequestUrl() != null;
        assertEquals("/mgmt/bpl/getPVStatus", requestQueryStatus.getRequestUrl().encodedPath());
        
        AtomicInteger expectedPostRequests = new AtomicInteger(1);
        RecordedRequest requestPostVersion = mockPostArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestPostVersion != null;
        assertEquals("/mgmt/bpl/getVersions", requestPostVersion.getPath());

        expectedPostRequests.addAndGet(1);
        RecordedRequest requestPostPolicy = mockPostArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestPostPolicy != null;
        assertEquals("/mgmt/bpl/getPolicyList", requestPostPolicy.getPath());

        expectedPostRequests.addAndGet(1);
        RecordedRequest requestPostStatus = mockPostArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestPostStatus != null;
        assert requestPostStatus.getRequestUrl() != null;
        assertEquals("/mgmt/bpl/getPVStatus", requestPostStatus.getRequestUrl().encodedPath());

        while (mockQueryArchiverAppliance.getRequestCount() > 0) {
            RecordedRequest requestAction = null;
            try {
                requestAction = mockQueryArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (requestAction == null) {
                break;
            }
            expectedQueryRequests.addAndGet(1);
            assert requestAction.getPath() != null;
            assertTrue(requestAction.getPath().startsWith("/mgmt/bpl"));
            ArchiveAction key = actionFromEndpoint(requestAction.getPath().substring("/mgmt/bpl".length()));
            String body = requestAction.getBody().readUtf8();
            actionsToNames.get(key).forEach(pv ->
                assertTrue(body.contains(pv))
            );
        }

        while (mockPostArchiverAppliance.getRequestCount() > 0) {
            RecordedRequest requestAction = null;
            try {
                requestAction = mockPostArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (requestAction == null) {
                break;
            }
            expectedPostRequests.addAndGet(1);
            assert requestAction.getPath() != null;
            assertTrue(requestAction.getPath().startsWith("/mgmt/bpl"));
            ArchiveAction key = actionFromEndpoint(requestAction.getPath().substring("/mgmt/bpl".length()));
            String body = requestAction.getBody().readUtf8();
            actionsToNames.get(key).forEach(pv ->
                assertTrue(body.contains(pv))
            );
        }

        assertEquals(mockPostArchiverAppliance.getRequestCount(), expectedPostRequests.get());
        assertEquals(mockQueryArchiverAppliance.getRequestCount(), expectedQueryRequests.get());
    }


    public ArchiveAction actionFromEndpoint(final String endpoint) {
        for (ArchiveAction action : ArchiveAction.values()) {
            if (action.getEndpoint().equals(endpoint)) {
                return action;
            }
        }
        return null;
    }
}
