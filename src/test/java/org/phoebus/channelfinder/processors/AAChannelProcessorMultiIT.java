package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.processors.aa.AAChannelProcessor;
import org.phoebus.channelfinder.processors.aa.ArchiveAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.activeProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.inactiveProperty;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(value = "classpath:application_test.properties")
class AAChannelProcessorMultiIT {

    @Autowired
    AAChannelProcessor aaChannelProcessor;

    MockWebServer mockArchiverAppliance;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockArchiverAppliance = new MockWebServer();
        mockArchiverAppliance.start(17665);

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void teardown() throws IOException {
        mockArchiverAppliance.shutdown();
    }

    @Test
    void testProcessMulti()
        throws JsonProcessingException, InterruptedException {
        List<Channel> channels = List.of(
            new Channel("PVArchivedActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVPausedActive", "owner", List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVNoneActive0", "owner", List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVArchivedInactive", "owner", List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVPausedInactive", "owner", List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVNoneInactive", "owner", List.of(archiveProperty, inactiveProperty), List.of()),
            new Channel("PVArchivedNotag", "owner", List.of(), List.of()),
            new Channel("PVNoneActive1", "owner", List.of(archiveProperty, activeProperty), List.of()),
            new Channel("PVNoneActive2", "owner", List.of(archiveProperty, activeProperty), List.of())
        );

        Map<String, String> namesToStatuses = Map.of(
            "PVArchivedActive", "Being archived",
            "PVPausedActive", "Paused",
            "PVNoneActive0", "Not being archived",
            "PVArchivedInactive", "Being archived",
            "PVPausedInactive", "Paused",
            "PVNoneInactive", "Not being archived",
            "PVArchivedNotag", "Being archived",
            "PVNoneActive1", "Not being archived",
            "PVNoneActive2", "Not being archived"
        );
        Map<ArchiveAction, List<String>> actionsToNames = Map.of(
            ArchiveAction.RESUME, List.of("PVPausedActive"),
            ArchiveAction.PAUSE, List.of("PVArchivedInactive", "PVArchivedNotag"),
            ArchiveAction.ARCHIVE, List.of("PVNoneActive0", "PVNoneActive1", "PVNoneActive2")
        );
        Map<ArchiveAction, List<String>> actionsToSubmissions = Map.of(
            ArchiveAction.ARCHIVE, List.of("PVNoneActive2", "PVNoneActive1", "PVNoneActive0"),
            ArchiveAction.RESUME, List.of("PVPausedActive"),
            ArchiveAction.PAUSE, List.of("PVArchivedNotag", "PVArchivedInactive")
        );
        // Request to version
        Map<String, String> versions = Map.of("mgmt_version", "Archiver Appliance Version 1.1.0");
        mockArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(versions))
            .addHeader("Content-Type", "application/json"));

        // Request to policies
        Map<String, String> policyList = Map.of("policy", "description");
        mockArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(policyList))
            .addHeader("Content-Type", "application/json"));

        // Request to archiver status
        List<Map<String, String>> archivePVStatuses =
            namesToStatuses.entrySet().stream().map(entry -> Map.of("pvName", entry.getKey(), "status", entry.getValue())).toList();
        mockArchiverAppliance.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(archivePVStatuses))
            .addHeader("Content-Type", "application/json"));

        // Requests to archiver
        actionsToNames.forEach((key, value) -> {
            List<Map<String, String>> archiverResponse =
                value.stream().map(channel -> Map.of("pvName", channel, "status", key + " request submitted")).toList();
            try {
                mockArchiverAppliance.enqueue(new MockResponse()
                    .setBody(objectMapper.writeValueAsString(archiverResponse))
                    .addHeader("Content-Type", "application/json"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        long count = aaChannelProcessor.process(channels);
        assertEquals(count, 6);

        AtomicInteger expectedRequests = new AtomicInteger(1);
        RecordedRequest requestVersion = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestVersion != null;
        assertEquals("/mgmt/bpl/getVersions", requestVersion.getPath());

        expectedRequests.addAndGet(1);
        RecordedRequest requestPolicy = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestPolicy != null;
        assertEquals("/mgmt/bpl/getPolicyList", requestPolicy.getPath());

        expectedRequests.addAndGet(1);
        RecordedRequest requestStatus = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
        assert requestStatus != null;
        assert requestStatus.getRequestUrl() != null;
        assertEquals("/mgmt/bpl/getPVStatus", requestStatus.getRequestUrl().encodedPath());

        while (mockArchiverAppliance.getRequestCount() > 0) {
            RecordedRequest requestAction = null;
            try {
                requestAction = mockArchiverAppliance.takeRequest(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (requestAction == null) {
                break;
            }
            expectedRequests.addAndGet(1);
            assert requestAction.getPath() != null;
            assertTrue(requestAction.getPath().startsWith("/mgmt/bpl"));
            ArchiveAction key = actionFromEndpoint(requestAction.getPath().substring("/mgmt/bpl".length()));
            String body = requestAction.getBody().readUtf8();
            actionsToSubmissions.get(key).forEach(pv ->
                assertTrue(body.contains(pv))
            );
        }

        assertEquals(mockArchiverAppliance.getRequestCount(), expectedRequests.get());
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
