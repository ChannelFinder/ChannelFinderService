package org.phoebus.channelfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.phoebus.channelfinder.processors.AAChannelProcessor.ArchivePV;

public class AAChannelProcessorTest {

    @Test
    public void archivePropertyParsing() {
        ArchivePV ar = new ArchivePV();
        ar.setPv("sim://testing");
        ar.setSamplingParameters("monitor@1.0");

        assertEquals(ar.getPv(), "sim://testing");
        assertEquals(ar.getSamplingmethod(), "MONITOR");
        assertEquals(ar.getSamplingperiod(), Float.valueOf("1.0"));

        ar.setSamplingParameters("scan@.01");
        assertEquals(ar.getSamplingmethod(), "SCAN");
        assertEquals(ar.getSamplingperiod(), Float.valueOf(".01"));
    }

    @Test
    public void json() throws JsonProcessingException {
        ArchivePV ar1 = new ArchivePV();
        ar1.setPv("sim://testing1");
        ar1.setSamplingParameters("monitor@1.0");

        ArchivePV ar2 = new ArchivePV();
        ar2.setPv("sim://testing2");
        ar2.setSamplingParameters("scan@0.2");

        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(List.of(ar1, ar2));

        String expectedString = "[{\"samplingmethod\":\"MONITOR\",\"samplingperiod\":1.0,\"pvname\":\"sim://testing1\"},{\"samplingmethod\":\"SCAN\",\"samplingperiod\":0.2,\"pvname\":\"sim://testing2\"}]";
        assertEquals(str, expectedString);

        // Only a pv name
        ArchivePV ar3 = new ArchivePV();
        ar3.setPv("sim://testing3");
        str = objectMapper.writeValueAsString(List.of(ar3));

        expectedString = "[{\"pvname\":\"sim://testing3\"}]";
        assertEquals(str, expectedString);

    }

}