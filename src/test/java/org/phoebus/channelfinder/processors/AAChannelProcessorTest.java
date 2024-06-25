package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.phoebus.channelfinder.processors.aa.ArchivePV;

class AAChannelProcessorTest {

    @Test
    void archivePropertyParsePass() {
        ArchivePV ar = new ArchivePV();
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");
        ar.setPv("sim://testing");
        ar.setSamplingParameters("monitor@1.0", testPolicyList);

        Assertions.assertEquals(ar.getPv(), "sim://testing");
        Assertions.assertEquals(ar.getSamplingMethod(), "MONITOR");
        Assertions.assertEquals(ar.getSamplingPeriod(), "1.0");

    }

    @ParameterizedTest
    @MethodSource("provideArchivePropertyArguments")
    void testArchiveProperty(String parameters, List<String> policyList,
                             String expectedSamplingMethod, String expectedSamplingPeriod) {
        ArchivePV ar = new ArchivePV();
        ar.setSamplingParameters(parameters, policyList);
        Assertions.assertEquals(expectedSamplingMethod, ar.getSamplingMethod());
        Assertions.assertEquals(expectedSamplingPeriod, ar.getSamplingPeriod());
    }

    private static Stream<Arguments> provideArchivePropertyArguments() {
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");
        return Stream.of(
            // Failure
            Arguments.of("@blah", testPolicyList, null, null),
            Arguments.of("MONITOR@NOTNUMBER", testPolicyList, null, null),
            Arguments.of("SCAN@1invalid", testPolicyList, null, null),
            Arguments.of("SCAN@ 1.0", testPolicyList, null, null),
            Arguments.of("SCAN @1", testPolicyList, null, null),
            Arguments.of("INVALID@1", testPolicyList, null, null),
            Arguments.of("@1.0", testPolicyList, null, null),
            Arguments.of("oops@", testPolicyList, null, null),
            Arguments.of("INVALID", testPolicyList, null, null),
            Arguments.of("MMMONITOR@10.0", testPolicyList, null, null),
            // Success
            Arguments.of("MONITOR@0.01 ignore me", testPolicyList, "MONITOR", "0.01"),
            Arguments.of("MONITOR@1 ignore me", testPolicyList, "MONITOR", "1"),
            Arguments.of("MONITOR@0.01 ignore me", testPolicyList, "MONITOR", "0.01"),
            Arguments.of("ScAn@10.01000", testPolicyList, "SCAN", "10.01000"),
            Arguments.of("MONITOR@0.01", testPolicyList, "MONITOR", "0.01"),
            Arguments.of("MONITOR@1", testPolicyList, "MONITOR", "1"),
            Arguments.of("scan@.01", testPolicyList, "SCAN", ".01"),
            Arguments.of("scan@1.01", testPolicyList, "SCAN", "1.01")
        );
    }

    @Test
    void defaultArchiveTag() {
        ArchivePV ar = new ArchivePV();
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");
        ar.setPv("sim://testing");
        ar.setSamplingParameters("default", testPolicyList);

        Assertions.assertEquals(ar.getPv(), "sim://testing");
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertNull(ar.getPolicy());
    }

    @Test
    void archivePolicyParsing() {
        ArchivePV ar = new ArchivePV();
        ar.setPv("sim://testingPolicy");
        ar.setPolicy("Fast");

        Assertions.assertEquals(ar.getPv(), "sim://testingPolicy");
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertEquals(ar.getPolicy(), "Fast");

        ar.setPolicy("SlowControlled");
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertEquals(ar.getPolicy(), "SlowControlled");

        ar.setSamplingParameters("scan@60", new ArrayList<>());
        Assertions.assertEquals(ar.getSamplingMethod(), "SCAN");
        Assertions.assertEquals(ar.getSamplingPeriod(), "60");
        Assertions.assertEquals(ar.getPolicy(), "SlowControlled");
    }

    @Test
    void archivePVJson() throws JsonProcessingException {
        ArchivePV ar1 = new ArchivePV();
        ar1.setPv("sim://testing1");
        ar1.setSamplingParameters("monitor@1.0", new ArrayList<>());

        ArchivePV ar2 = new ArchivePV();
        ar2.setPv("sim://testing2");
        ar2.setSamplingParameters("scan@0.2", new ArrayList<>());

        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(List.of(ar1, ar2));

        String expectedString = "[{\"pv\":\"sim://testing1\",\"samplingMethod\":\"MONITOR\",\"samplingPeriod\":\"1.0\"},{\"pv\":\"sim://testing2\",\"samplingMethod\":\"SCAN\",\"samplingPeriod\":\"0.2\"}]";
        Assertions.assertEquals(str, expectedString);

        // Only a pv name
        ArchivePV ar3 = new ArchivePV();
        ar3.setPv("sim://testing3");
        str = objectMapper.writeValueAsString(List.of(ar3));

        expectedString = "[{\"pv\":\"sim://testing3\"}]";
        Assertions.assertEquals(str, expectedString);

        // Test policies
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");

        // Valid policy
        ArchivePV ar4 = new ArchivePV();
        ar4.setPv("sim://testing4");
        ar4.setSamplingParameters("Fast", testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar4));

        expectedString = "[{\"pv\":\"sim://testing4\",\"policy\":\"Fast\"}]";
        Assertions.assertEquals(str, expectedString);

        // Invalid policy
        ArchivePV ar5 = new ArchivePV();
        ar5.setPv("sim://testing5");
        ar5.setSamplingParameters("Fastest", testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar5));

        expectedString = "[{\"pv\":\"sim://testing5\"}]";
        Assertions.assertEquals(str, expectedString);
    }

}