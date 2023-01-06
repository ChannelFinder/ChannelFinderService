package org.phoebus.channelfinder.processors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A post processor which uses the channel property *archive* to add pv's to the archiver appliance
 * The archive parameters are specified in the property value, they consist of 2 parts
 * the sampling method which can be scan or monitor
 * the sampling rate defined in seconds
 *
 * e.g. archive=monitor@1.0
 */
@Configuration
public class AAChannelProcessor implements ChannelProcessor{

    private static final Logger log = Logger.getLogger(AAChannelProcessor.class.getName());

    @Value("${aa.enabled:true}")
    private boolean aaEnabled;

    @Value("${aa.url:http://localhost:10065}")
    private String aaURL;

    private static final String mgmtResource = "/mgmt/bpl/archivePV";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private WebClient client = WebClient.create();

    private static final String archivePropertyName = "archive";

    @Override
    public boolean enabled() {
        return aaEnabled;
    }

    @Override
    public String processorName() {
        return "Process " + archivePropertyName + " properties on channels";
    }

    @Override
    public void process(List<XmlChannel> channels) throws JsonProcessingException {
        List<ArchivePV> archivePVS = new ArrayList<>();
        channels.stream()
                .forEach(channel -> {
                    Optional<XmlProperty> archiveProperty = channel.getProperties().stream()
                            .filter(xmlProperty -> archivePropertyName.equalsIgnoreCase(xmlProperty.getName()))
                            .findFirst();
                    if(archiveProperty.isPresent()) {
                        ArchivePV newArchiverPV = new ArchivePV();
                        newArchiverPV.setPv(channel.getName());
                        newArchiverPV.setSamplingParameters(archiveProperty.get().getValue());
                        archivePVS.add(newArchiverPV);
                    }
                });
        configureAA(archivePVS);
    }


    private void configureAA(List<ArchivePV> archivePVS) throws JsonProcessingException {
        String response = client.post()
                .uri(URI.create(aaURL + mgmtResource))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(archivePVS))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info(response);
    }

    @JsonInclude(Include.NON_NULL)
    public static class ArchivePV {
        private String pv;
        private String samplingmethod;
        private String samplingperiod;

        public String getPv() {
            return pv;
        }

        public void setPv(String pv) {
            this.pv = pv;
        }

        /**
         * process the archive property value string to configure the sampling method and rate
         * @param parameters string expected in the form monitor@1.0
         */
        public void setSamplingParameters(String parameters) {
            if(parameters.equalsIgnoreCase("default")){
                return;
            }
            String[] p = parameters.split("@");
            if(p.length == 2) {
                switch (p[0].toUpperCase()) {
                    case "MONITOR":
                        setSamplingmethod("MONITOR");
                        break;
                    case "SCAN":
                        setSamplingmethod("SCAN");
                        break;
                    default:
                        // invalid sampling method
                }
                // catch number format errors
                setSamplingperiod(p[1]);
            }

        }

        public String getSamplingmethod() {
            return samplingmethod;
        }

        public void setSamplingmethod(String samplingmethod) {
            this.samplingmethod = samplingmethod;
        }

        public String getSamplingperiod() {
            return samplingperiod;
        }

        public void setSamplingperiod(String samplingperiod) {
            this.samplingperiod = samplingperiod;
        }
    }
}
