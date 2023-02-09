package org.phoebus.channelfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.phoebus.channelfinder.processors.AAChannelProcessor.ArchivePV;

public class AAChannelProcessorTest {

    @Test
    public void archivePropertyParsing() {
        ArchivePV ar = new ArchivePV();
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");
        ar.setPv("sim://testing");
        ar.setSamplingParameters("monitor@1.0", testPolicyList);

        assertEquals(ar.getPv(), "sim://testing");
        assertEquals(ar.getSamplingmethod(), "MONITOR");
        assertEquals(ar.getSamplingperiod(), "1.0");

        ArchivePV ar2 = new ArchivePV();
        ar2.setSamplingParameters("scan@.01", testPolicyList);
        assertEquals(ar2.getSamplingmethod(), "SCAN");
        assertEquals(ar2.getSamplingperiod(), ".01");

        ArchivePV ar3 = new ArchivePV();
        ar3.setSamplingParameters("MONITOR@1", testPolicyList);
        assertEquals(ar3.getSamplingmethod(), "MONITOR");
        assertEquals(ar3.getSamplingperiod(), "1");

        ArchivePV ar4 = new ArchivePV();
        ar4.setSamplingParameters("MONITOR@0.01", testPolicyList);
        assertEquals(ar4.getSamplingmethod(), "MONITOR");
        assertEquals(ar4.getSamplingperiod(), "0.01");

        ArchivePV ar5 = new ArchivePV();
        ar5.setSamplingParameters("ScAn@10.01000", testPolicyList);
        assertEquals(ar5.getSamplingmethod(), "SCAN");
        assertEquals(ar5.getSamplingperiod(), "10.01000");

        ArchivePV ar6 = new ArchivePV();
        ar6.setSamplingParameters("MONITOR@1 ignore me", testPolicyList);
        assertEquals(ar6.getSamplingmethod(), "MONITOR");
        assertEquals(ar6.getSamplingperiod(), "1");

        ArchivePV ar7 = new ArchivePV();
        ar7.setSamplingParameters("MONITOR@0.01 ignore me", testPolicyList);
        assertEquals(ar7.getSamplingmethod(), "MONITOR");
        assertEquals(ar7.getSamplingperiod(), "0.01");

        // test failures

        ArchivePV ar8 = new ArchivePV();
        ar8.setSamplingParameters("MONITOR@NOTNUMBER", testPolicyList);
        assertEquals(ar8.getSamplingmethod(), null);
        assertEquals(ar8.getSamplingperiod(), null);

        ArchivePV ar9 = new ArchivePV();
        ar9.setSamplingParameters("MMMONITOR@10.0", testPolicyList);
        assertEquals(ar9.getSamplingmethod(), null);
        assertEquals(ar9.getSamplingperiod(), null);

        ArchivePV ar10 = new ArchivePV();
        ar10.setSamplingParameters("INVALID", testPolicyList);
        assertEquals(ar10.getSamplingmethod(), null);
        assertEquals(ar10.getSamplingperiod(), null);

        ArchivePV ar11 = new ArchivePV();
        ar11.setSamplingParameters("oops@", testPolicyList);
        assertEquals(ar11.getSamplingmethod(), null);
        assertEquals(ar11.getSamplingperiod(), null);

        ArchivePV ar12 = new ArchivePV();
        ar12.setSamplingParameters("@1.0", testPolicyList);
        assertEquals(ar12.getSamplingmethod(), null);
        assertEquals(ar12.getSamplingperiod(), null);

        ArchivePV ar13 = new ArchivePV();
        ar13.setSamplingParameters("@blah", testPolicyList);
        assertEquals(ar13.getSamplingmethod(), null);
        assertEquals(ar13.getSamplingperiod(), null);

        ArchivePV ar14 = new ArchivePV();
        ar14.setSamplingParameters("INVALID@1", testPolicyList);
        assertEquals(ar14.getSamplingmethod(), null);
        assertEquals(ar14.getSamplingperiod(), null);

        ArchivePV ar15 = new ArchivePV();
        ar15.setSamplingParameters("SCAN @1", testPolicyList);
        assertEquals(ar15.getSamplingmethod(), null);
        assertEquals(ar15.getSamplingperiod(), null);

        ArchivePV ar16 = new ArchivePV();
        ar16.setSamplingParameters("SCAN@ 1.0", testPolicyList);
        assertEquals(ar16.getSamplingmethod(), null);
        assertEquals(ar16.getSamplingperiod(), null);

        ArchivePV ar17 = new ArchivePV();
        ar17.setSamplingParameters("SCAN@1invalid", testPolicyList);
        assertEquals(ar17.getSamplingmethod(), null);
        assertEquals(ar17.getSamplingperiod(), null);
    }

    @Test
    public void defaultArchiveTag() {
        ArchivePV ar = new ArchivePV();
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");
        ar.setPv("sim://testing");
        ar.setSamplingParameters("default", testPolicyList);

        assertEquals(ar.getPv(), "sim://testing");
        assertEquals(ar.getSamplingmethod(), null);
        assertEquals(ar.getSamplingperiod(), null);
        assertEquals(ar.getPolicy(), null);
    }

    @Test
    public void archivePolicyParsing() {
        ArchivePV ar = new ArchivePV();
        ar.setPv("sim://testingPolicy");
        ar.setPolicy("Fast");

        assertEquals(ar.getPv(), "sim://testingPolicy");
        assertEquals(ar.getSamplingmethod(), null);
        assertEquals(ar.getSamplingperiod(), null);
        assertEquals(ar.getPolicy(), "Fast");

        ar.setPolicy("SlowControlled");
        assertEquals(ar.getSamplingmethod(), null);
        assertEquals(ar.getSamplingperiod(), null);
        assertEquals(ar.getPolicy(), "SlowControlled");

        ar.setSamplingParameters("scan@60", new ArrayList<>());
        assertEquals(ar.getSamplingmethod(), "SCAN");
        assertEquals(ar.getSamplingperiod(), "60");
        assertEquals(ar.getPolicy(), "SlowControlled");
    }

    @Test
    public void json() throws JsonProcessingException {
        ArchivePV ar1 = new ArchivePV();
        ar1.setPv("sim://testing1");
        ar1.setSamplingParameters("monitor@1.0", new ArrayList<>());

        ArchivePV ar2 = new ArchivePV();
        ar2.setPv("sim://testing2");
        ar2.setSamplingParameters("scan@0.2", new ArrayList<>());

        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(List.of(ar1, ar2));

        String expectedString = "[{\"pv\":\"sim://testing1\",\"samplingmethod\":\"MONITOR\",\"samplingperiod\":\"1.0\"},{\"pv\":\"sim://testing2\",\"samplingmethod\":\"SCAN\",\"samplingperiod\":\"0.2\"}]";
        assertEquals(str, expectedString);

        // Only a pv name
        ArchivePV ar3 = new ArchivePV();
        ar3.setPv("sim://testing3");
        str = objectMapper.writeValueAsString(List.of(ar3));

        expectedString = "[{\"pv\":\"sim://testing3\"}]";
        assertEquals(str, expectedString);

        // Test policies
        List<String> testPolicyList = Arrays.asList("Fast", "FastControlled", "Slow", "SlowControlled");

        // Valid policy
        ArchivePV ar4 = new ArchivePV();
        ar4.setPv("sim://testing4");
        ar4.setSamplingParameters("Fast", testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar4));

        expectedString = "[{\"pv\":\"sim://testing4\",\"policy\":\"Fast\"}]";
        assertEquals(str, expectedString);

        // Invalid policy
        ArchivePV ar5 = new ArchivePV();
        ar5.setPv("sim://testing5");
        ar5.setSamplingParameters("Fastest", testPolicyList);
        str = objectMapper.writeValueAsString(List.of(ar5));

        expectedString = "[{\"pv\":\"sim://testing5\"}]";
        assertEquals(str, expectedString);
    }

}