package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.processors.aa.AAChannelProcessor;
import org.phoebus.channelfinder.processors.aa.ArchiveAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.activeProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.archiveProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.inactiveProperty;
import static org.phoebus.channelfinder.processors.AAChannelProcessorIT.paramableAAChannelProcessorTest;
import static org.phoebus.channelfinder.processors.AAChannelProcessorMultiIT.NOT_BEING_ARCHIVED;
import static org.phoebus.channelfinder.processors.AAChannelProcessorMultiIT.OWNER;

@WebMvcTest(AAChannelProcessor.class)
@TestPropertySource(locations = "classpath:application_test.properties")
class AAChannelProcessorExtraFieldsIT {
    protected static Property extraFieldsProperty = new Property("archive_extra_fields", "owner", "a b c");
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

    static Stream<Arguments> provideArguments() {
        List<Channel> channels = List.of(
            new Channel("PVNoneActive", OWNER, List.of(archiveProperty, activeProperty, extraFieldsProperty), List.of())
        );

        Map<String, String> namesToStatuses = Map.of(
            "PVNoneActive", NOT_BEING_ARCHIVED,
            "PVNoneActive.a", NOT_BEING_ARCHIVED,
            "PVNoneActive.b", NOT_BEING_ARCHIVED,
            "PVNoneActive.c", NOT_BEING_ARCHIVED
        );
        Map<ArchiveAction, List<String>> actionsToNames = Map.of(
            ArchiveAction.ARCHIVE, List.of("PVNoneActive", "PVNoneActive.a", "PVNoneActive.b", "PVNoneActive.c")
        );
        int expectedProcessedChannels = 4;

        return Stream.of(
            Arguments.of(channels, namesToStatuses, actionsToNames, expectedProcessedChannels));

    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void testProcessNotArchivedActive(
        List<Channel> channels,
        Map<String, String> namesToStatuses,
        Map<ArchiveAction, List<String>> actionsToNames,
        int expectedProcessedChannels)
        throws JsonProcessingException, InterruptedException {
        AAChannelProcessorMultiIT.paramableMultiAAChannelProcessorTest(
            mockArchiverAppliance,
            objectMapper,
            aaChannelProcessor,
            channels,
            namesToStatuses,
            actionsToNames,
            expectedProcessedChannels
        );
    }
}
