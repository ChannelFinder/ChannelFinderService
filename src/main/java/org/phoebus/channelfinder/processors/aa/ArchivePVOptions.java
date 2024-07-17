package org.phoebus.channelfinder.processors.aa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivePVOptions {
    private static final Logger logger = Logger.getLogger(ArchivePVOptions.class.getName());

    private String pv;
    private String samplingMethod;
    private String samplingPeriod;
    private String policy;
    @JsonIgnore
    private String pvStatus;

    @Override
    public String toString() {
        return "ArchivePV{" + "pv='"
                + pv + '\'' + ", samplingMethod='"
                + samplingMethod + '\'' + ", samplingPeriod='"
                + samplingPeriod + '\'' + ", policy='"
                + policy + '\'' + ", pvStatus='"
                + pvStatus + '\'' + '}';
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    /**
     * process the archive property value string to configure the sampling method and rate
     *
     * @param parameters string expected in the form monitor@1.0
     */
    public void setSamplingParameters(String parameters, List<String> policyList) {
        if (parameters.equalsIgnoreCase("default")) {
            return;
        }
        if (policyList.contains(parameters)) {
            setPolicy(parameters);
            return;
        }
        String[] p = parameters.split("@");
        if (p.length == 2) {
            switch (p[0].toUpperCase()) {
                case "MONITOR":
                    setSamplingMethod("MONITOR");
                    break;
                case "SCAN":
                    setSamplingMethod("SCAN");
                    break;
                default:
                    // invalid sampling method
                    logger.log(Level.WARNING, "Invalid sampling method " + p[0]);
                    return;
            }
            // ignore anything after first space
            String sp = p[1].split("\\s")[0];
            // catch number format errors
            try {
                Float.parseFloat(sp);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid sampling period" + sp);
                setSamplingMethod(null);
                return;
            }
            setSamplingPeriod(sp);
        }
    }

    public String getSamplingMethod() {
        return samplingMethod;
    }

    public void setSamplingMethod(String samplingMethod) {
        this.samplingMethod = samplingMethod;
    }

    public String getSamplingPeriod() {
        return samplingPeriod;
    }

    public void setSamplingPeriod(String samplingPeriod) {
        this.samplingPeriod = samplingPeriod;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPvStatus() {
        return pvStatus;
    }

    public void setPvStatus(String pvStatus) {
        this.pvStatus = pvStatus;
    }
}
