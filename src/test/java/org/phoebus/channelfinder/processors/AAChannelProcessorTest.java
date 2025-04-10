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

import org.phoebus.channelfinder.processors.aa.AAChannelProcessor;
import org.phoebus.channelfinder.processors.aa.ArchivePVOptions;

class AAChannelProcessorTest {

    public static final String SIM_TESTING_POLICY = "sim://testingPolicy";
    public static final String POLICY_FAST = "Fast";
    public static final String POLICY_SLOW_CONTROLLED = "SlowControlled";
    public static final String SAMPLING_METHOD_SCAN = "SCAN";
    public static final String POLICY_FAST_CONTROLLED = "FastControlled";
    public static final String POLICY_SLOW = "Slow";
    public static final String SAMPLING_METHOD_MONITOR = "MONITOR";

    @Test
    void archivePropertyParsePass() {
        ArchivePVOptions ar = new ArchivePVOptions();
        List<String> testPolicyList = Arrays.asList(POLICY_FAST, POLICY_FAST_CONTROLLED, POLICY_SLOW, POLICY_SLOW_CONTROLLED);
        ar.setPv("sim://testing");
        ar.setSamplingParameters("monitor@1.0", testPolicyList);

        Assertions.assertEquals(ar.getPv(), "sim://testing");
        Assertions.assertEquals(ar.getSamplingMethod(), SAMPLING_METHOD_MONITOR);
        Assertions.assertEquals(ar.getSamplingPeriod(), "1.0");

    }

    @ParameterizedTest
    @MethodSource("provideArchivePropertyArguments")
    void testArchiveProperty(String parameters, List<String> policyList,
                             String expectedSamplingMethod, String expectedSamplingPeriod) {
        ArchivePVOptions ar = new ArchivePVOptions();
        ar.setSamplingParameters(parameters, policyList);
        Assertions.assertEquals(expectedSamplingMethod, ar.getSamplingMethod());
        Assertions.assertEquals(expectedSamplingPeriod, ar.getSamplingPeriod());
    }

    private static Stream<Arguments> provideArchivePropertyArguments() {
        List<String> testPolicyList = Arrays.asList(POLICY_FAST, POLICY_FAST_CONTROLLED, POLICY_SLOW, POLICY_SLOW_CONTROLLED);
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
            Arguments.of("MONITOR@0.01 ignore me", testPolicyList, SAMPLING_METHOD_MONITOR, "0.01"),
            Arguments.of("MONITOR@1 ignore me", testPolicyList, SAMPLING_METHOD_MONITOR, "1"),
            Arguments.of("MONITOR@0.01 ignore me", testPolicyList, SAMPLING_METHOD_MONITOR, "0.01"),
            Arguments.of("ScAn@10.01000", testPolicyList, SAMPLING_METHOD_SCAN, "10.01000"),
            Arguments.of("MONITOR@0.01", testPolicyList, SAMPLING_METHOD_MONITOR, "0.01"),
            Arguments.of("MONITOR@1", testPolicyList, SAMPLING_METHOD_MONITOR, "1"),
            Arguments.of("scan@.01", testPolicyList, SAMPLING_METHOD_SCAN, ".01"),
            Arguments.of("scan@1.01", testPolicyList, SAMPLING_METHOD_SCAN, "1.01")
        );
    }

    @Test
    void defaultArchiveTag() {
        ArchivePVOptions ar = new ArchivePVOptions();
        List<String> testPolicyList = Arrays.asList(POLICY_FAST, POLICY_FAST_CONTROLLED, POLICY_SLOW, POLICY_SLOW_CONTROLLED);
        ar.setPv("sim://testing");
        ar.setSamplingParameters("default", testPolicyList);

        Assertions.assertEquals(ar.getPv(), "sim://testing");
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertNull(ar.getPolicy());
    }

    @Test
    void archivePolicyParsing() {
        ArchivePVOptions ar = new ArchivePVOptions();
        ar.setPv(SIM_TESTING_POLICY);
        ar.setPolicy(POLICY_FAST);

        Assertions.assertEquals(ar.getPv(), SIM_TESTING_POLICY);
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertEquals(ar.getPolicy(), POLICY_FAST);

        ar.setPolicy(POLICY_SLOW_CONTROLLED);
        Assertions.assertNull(ar.getSamplingMethod());
        Assertions.assertNull(ar.getSamplingPeriod());
        Assertions.assertEquals(ar.getPolicy(), POLICY_SLOW_CONTROLLED);

        ar.setSamplingParameters("scan@60", new ArrayList<>());
        Assertions.assertEquals(ar.getSamplingMethod(), SAMPLING_METHOD_SCAN);
        Assertions.assertEquals(ar.getSamplingPeriod(), "60");
        Assertions.assertEquals(ar.getPolicy(), POLICY_SLOW_CONTROLLED);
    }
    @Test
    void archiveExtraFieldParsing() {
        Assertions.assertEquals(List.of("a", "b", "c"), AAChannelProcessor.parseExtraFieldsProperty("a b c"));
    }

    @Test
    void archivePVJson() throws JsonProcessingException {
        ArchivePVOptions ar1 = new ArchivePVOptions();
        ar1.setPv("sim://testing1");
        ar1.setSamplingParameters("monitor@1.0", new ArrayList<>());

        ArchivePVOptions ar2 = new ArchivePVOptions();
        ar2.setPv("sim://testing2");
        ar2.setSamplingParameters("scan@0.2", new ArrayList<>());

        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(List.of(ar1, ar2));

        String expectedString = "[{\"pv\":\"sim://testing1\",\"samplingmethod\":\"MONITOR\",\"samplingperiod\":\"1.0\"},{\"pv\":\"sim://testing2\",\"samplingmethod\":\"SCAN\",\"samplingperiod\":\"0.2\"}]";
        Assertions.assertEquals(str, expectedString);

        // Only a pv name
        ArchivePVOptions ar3 = new ArchivePVOptions();
        ar3.setPv("sim://testing3");
        str = objectMapper.writeValueAsString(List.of(ar3));

        expectedString = "[{\"pv\":\"sim://testing3\"}]";
        Assertions.assertEquals(str, expectedString);

        // Test policies
        List<String> testPolicyList = Arrays.asList(POLICY_FAST, POLICY_FAST_CONTROLLED, POLICY_SLOW, POLICY_SLOW_CONTROLLED);

        // Valid policy
        ArchivePVOptions ar4 = new ArchivePVOptions();
        ar4.setPv("sim://testing4");
        ar4.setSamplingParameters(POLICY_FAST, testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar4));

        expectedString = "[{\"pv\":\"sim://testing4\",\"policy\":\"Fast\"}]";
        Assertions.assertEquals(str, expectedString);

        // Invalid policy
        ArchivePVOptions ar5 = new ArchivePVOptions();
        ar5.setPv("sim://testing5");
        ar5.setSamplingParameters("Fastest", testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar5));

        expectedString = "[{\"pv\":\"sim://testing5\"}]";
        Assertions.assertEquals(str, expectedString);
    }

}