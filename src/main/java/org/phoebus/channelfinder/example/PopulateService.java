package org.phoebus.channelfinder.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import org.phoebus.channelfinder.ElasticConfig;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.phoebus.channelfinder.XmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An class for creating the example database.
 * <p>
 * The createDB method can be invoked with a specified list of cells
 * For each cell, there are 1500 channels
 * The channel names follow the following convention
 * {system}:{cell}-{device}-{signal}
 * <p>
 * systems: SR(1000) and BR(500)
 * cell: C001
 * device: Magnets, Powersupply, BPMs,...
 * signal: St or RB
 * <p>
 * e.g. BR:C001-MG:1{QDP:D}Fld-SP
 * <p>
 * Additionally, each channel has a list of tags and properties
 * <p>
 * Properties include:
 * location, cell, element, device, family, unit, type, z_pos_r, mount, and some generic properties
 * properties group[0-9] with values [0, 1, 2, 5, 10, 20, 50, 100, 200, 500] which matches the # of channels with those values
 * <p>
 * Tags include:
 * group[0-9]_[count] with count values [0, 1, 2, 5, 10, 20, 50, 100, 200, 500] which matches the # of channels with those values
 *
 * @author Kunal Shroff
 */
@Configuration
public class PopulateService {

    private static final Logger logger = Logger.getLogger(PopulateService.class.getName());

    static int max_prop = 40; // must be >=20
    static int max_tag = 60; // must be >=11

    // The number of cells can be increases to 100
    // Each cell would consist of a 1000 SR channels and 500 BO channels
    private static int numberOfCells = 2;

    static String cowner = "testc";
    static String powner = "testp";
    static String towner = "testt";

    static List<Integer> val_bucket = Arrays.asList(0, 1, 2, 5, 10, 20, 50, 100, 200, 500);

    // A static list of props, tags, channels handled by this class which must be cleaned up on closure.
    static Set<XmlProperty> prop_list = new HashSet<>();
    Set<XmlTag> tag_list = new HashSet<>();
    Set<String> channel_list = new HashSet<>();

    @Autowired
    ElasticConfig esService;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Value("${elasticsearch.channel.index:channelfinder}")
    private String ES_CHANNEL_INDEX;
    @Value("${elasticsearch.channel.type:cf_channel}")
    private String ES_CHANNEL_TYPE;
    @Value("${elasticsearch.tag.index:cf_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:cf_tag}")
    private String ES_TAG_TYPE;
    @Value("${elasticsearch.property.index:cf_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:cf_property}")
    private String ES_PROPERTY_TYPE;


    static ObjectMapper mapper = new ObjectMapper();


    static int index = 0;

    static List<Integer> tokens_1000 = new ArrayList<>();
    static List<Integer> tokens_500 = new ArrayList<>();

    // Create a list of properties
    static {
        for (int i = 10; i < 70; i++) {
            prop_list.add(new XmlProperty("prop" + String.format("%03d", i), powner));
        }

        index = 0;
        Arrays.asList(112, 1, 2, 5, 10, 20, 50, 100, 200, 500).stream().forEachOrdered(count -> {
            for (int i = 0; i < count; i++) {
                tokens_1000.add(val_bucket.get(index));
            }
            index++;
        });

        index = 0;
        Arrays.asList(112, 1, 2, 5, 10, 20, 50, 100, 200).stream().forEachOrdered(count -> {
            for (int i = 0; i < count; i++) {
                tokens_500.add(val_bucket.get(index));
            }
            index++;
        });
    }

    public synchronized void cleanupDB() {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (String channelName : channel_list) {
            br.operations(op -> op
                    .delete(idx -> idx
                            .index(ES_CHANNEL_INDEX)
                            .id(channelName)
                    )
            );
        }
        for (XmlTag tag : tag_list) {
            br.operations(op -> op
                    .delete(idx -> idx
                            .index(ES_TAG_INDEX)
                            .id(tag.getName())
                    )
            );
        }
        for (XmlProperty property : prop_list) {
            br.operations(op -> op
                    .delete(idx -> idx
                            .index(ES_PROPERTY_INDEX)
                            .id(property.getName())
                    )
            );
        }
        try {
            BulkResponse result = client.bulk(br.refresh(Refresh.True).build());
            if (result.errors()) {
                logger.log(Level.SEVERE, "Bulk had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        logger.log(Level.SEVERE, () -> item.error().reason());
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public synchronized void createDB(int cells) {
        numberOfCells = cells;
        createDB();
    }

    public Set<String> getChannelList() {
        return channel_list;
    }

    public synchronized void createDB() {
        long start = System.currentTimeMillis();
        Collection<Boolean> finalResult = new ArrayList<>();
        start = System.currentTimeMillis();
        for (int i = 1; i <= numberOfCells; i++) {
            String cell = String.format("%03d", i);
            try {
                finalResult.add(insertSRCell(cell));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = 1; i <= numberOfCells; i++) {
            String cell = String.format("%03d", i);
            try {
                finalResult.add(insertBOCell(cell));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final long time = System.currentTimeMillis() - start;
        logger.log(Level.INFO, () -> finalResult.size() + " channels created sequentially in time: " + time);
        prop_list.forEach(p ->
            logger.log(Level.INFO, p.toLog())
        );
        tag_list.forEach(t ->
            logger.log(Level.INFO, t.toLog())
        );

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlProperty property : prop_list) {
            br.operations(op -> op.index(index -> index.index(ES_PROPERTY_INDEX).id(property.getName()).document(property)));
        }
        for (XmlTag tag : tag_list) {
            br.operations(op -> op.index(index -> index.index(ES_TAG_INDEX).id(tag.getName()).document(tag)));
        }
        br.refresh(Refresh.True);

        try {
            BulkResponse results = client.bulk(br.build());
            if (results.errors()) {
                logger.log(Level.SEVERE, "Bulk had errors");
                for (BulkResponseItem item : results.items()) {
                    if (item.error() != null) {
                        logger.log(Level.SEVERE, () -> item.error().reason());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       logger.log(Level.INFO, "completed populating");
    }


    private boolean insertSRCell(String cell) throws Exception {
        String loc = "storage ring";
        String pre = "SR:C";
        // Tokens
        Map<Integer, List<Integer>> tokens = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            tokens.put(i, tokens_1000.stream().map(Integer::new).collect(Collectors.toList()));
        }

        AtomicInteger channelCounter = new AtomicInteger(0);

        Collection<XmlChannel> result = new ArrayList<>(1000);
        result.addAll(insert_big_magnets(tokens, channelCounter, 2, pre, "DP", loc, cell, "dipole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 5, pre, "QDP:D", loc, cell, "defocusing quadrupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 5, pre, "QDP:F", loc, cell, "focusing quadrupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "QDP:S", loc, cell, "skew quadrupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "STP", loc, cell, "sextupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 5, pre, "HC:S", loc, cell, "horizontal slow corrector"));
        result.addAll(insert_air_magnets(tokens, channelCounter, 5, pre, "HC:F", loc, cell, "horizontal fast corrector"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 5, pre, "VC:S", loc, cell, "vertical slow corrector"));
        result.addAll(insert_air_magnets(tokens, channelCounter, 4, pre, "VC:F", loc, cell, "vertical fast corrector"));

        result.addAll(insert_valves(tokens, channelCounter, 5, pre, "GV", loc, cell, "vacuum"));

        result.addAll(insert_gauges(tokens, channelCounter, 5, pre, "VGC", loc, cell, "vacuum"));
        result.addAll(insert_gauges(tokens, channelCounter, 5, pre, "TCG", loc, cell, "vacuum"));

        result.addAll(insert_pumps(tokens, channelCounter, 2, pre, "IPC", loc, cell, "vacuum"));
        result.addAll(insert_pumps(tokens, channelCounter, 2, pre, "TMP", loc, cell, "vacuum"));

        result.addAll(insert_temps(tokens, channelCounter, 40, pre, "TC", loc, cell, "temperature sensor"));

        result.addAll(insert_bpms(tokens, channelCounter, 4, pre, "BSA", loc, cell, "small aperture BPM"));
        result.addAll(insert_bpms(tokens, channelCounter, 4, pre, "BHS", loc, cell, "high stability BPM"));
        result.addAll(insert_bpms(tokens, channelCounter, 4, pre, "BLA", loc, cell, "large aperture BPM"));

        long start = System.currentTimeMillis();
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlChannel channel : result) {
            br.operations(op -> op.index(IndexOperation.of(i -> i.index(ES_CHANNEL_INDEX).id(channel.getName()).document(channel))));
        }
        String prepare = "|Prepare: " + (System.currentTimeMillis() - start) + "|";
        start = System.currentTimeMillis();
        br.refresh(Refresh.True);

        BulkResponse srResult = client.bulk(br.build());
        String execute = "|Execute: " + (System.currentTimeMillis() - start) + "|";
        logger.log(Level.INFO, "Insterted BO cell " + cell + " " + prepare + " " + execute);
        if (srResult.errors()) {
            logger.log(Level.SEVERE, "Bulk had errors");
            for (BulkResponseItem item : srResult.items()) {
                if (item.error() != null) {
                    logger.log(Level.SEVERE, () -> item.error().reason());
                }
            }
        } else {
            return true;
        }
        return false;
    }

    private boolean insertBOCell(String cell) throws Exception {
        String loc = "booster";
        String pre = "BR:C";

        // Tokens
        Map<Integer, List<Integer>> tokens = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            tokens.put(i, tokens_500.stream().map(Integer::new).collect(Collectors.toList()));
        }

        AtomicInteger channelCounter = new AtomicInteger(0);

        Collection<XmlChannel> result = new ArrayList<>(500);

        result.addAll(insert_big_magnets(tokens, channelCounter, 2, pre, "DP", loc, cell, "dipole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "QDP:D", loc, cell, "defocusing quadrupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "QDP:F", loc, cell, "focusing quadrupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 2, pre, "STP", loc, cell, "sextupole"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "HC", loc, cell, "horizontal corrector"));
        result.addAll(insert_big_magnets(tokens, channelCounter, 4, pre, "VC", loc, cell, "vertical corrector"));

        result.addAll(insert_valves(tokens, channelCounter, 4, pre, "GV", loc, cell, "vacuum"));

        result.addAll(insert_gauges(tokens, channelCounter, 4, pre, "VGC", loc, cell, "vacuum"));
        result.addAll(insert_gauges(tokens, channelCounter, 2, pre, "TCG", loc, cell, "vacuum"));

        result.addAll(insert_pumps(tokens, channelCounter, 2, pre, "IPC", loc, cell, "vacuum"));
        result.addAll(insert_pumps(tokens, channelCounter, 2, pre, "TMP", loc, cell, "vacuum"));

        result.addAll(insert_temps(tokens, channelCounter, 10, pre, "TC", loc, cell, "temperature sensor"));

        result.addAll(insert_bpms(tokens, channelCounter, 2, pre, "BLA", loc, cell, "beam position monitor"));

        long start = System.currentTimeMillis();
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (XmlChannel channel : result) {
            br.operations(op -> op.index(IndexOperation.of(i -> i.index(ES_CHANNEL_INDEX).id(channel.getName()).document(channel))));
        }
        String prepare = "|Prepare: " + (System.currentTimeMillis() - start) + "|";
        start = System.currentTimeMillis();
        br.refresh(Refresh.True);

        BulkResponse boosterResult = client.bulk(br.build());
        String execute = "|Execute: " + (System.currentTimeMillis() - start) + "|";
        logger.log(Level.INFO, "Insterted BO cell " + cell + " " + prepare + " " + execute);
        if (boosterResult.errors()) {
            logger.log(Level.SEVERE, "Bulk had errors");
            for (BulkResponseItem item : boosterResult.items()) {
                if (item.error() != null) {
                    logger.log(Level.SEVERE, () -> item.error().reason());
                }
            }
        } else {
            return true;
        }
        return false;
    }

    private Collection<XmlChannel> insert_big_magnets(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix,
                                                      String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}I-RB", loc, cell, element, "power supply", "current", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}I-SP", loc, cell, element, "power supply", "current", "setpoint"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}On-Sw", loc, cell, element, "power supply", "power", "switch"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Rst-Cmd", loc, cell, element, "power supply", "reset", "command"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}On-St", loc, cell, element, "power supply", "power", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Acc-St", loc, cell, element, "power supply", "access", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}OK-St", loc, cell, element, "power supply", "sum error", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}T-St", loc, cell, element, "power supply", "temperature", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}F-St", loc, cell, element, "power supply", "water flow", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Gnd-St", loc, cell, element, "power supply", "ground", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Ctl-St", loc, cell, element, "power supply", "control", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Val-St", loc, cell, element, "power supply", "value", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}Fld-RB", loc, cell, element, "magnet", "field", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}Fld-SP", loc, cell, element, "magnet", "field", "setpoint"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}T:1-RB", loc, cell, element, "magnet", "temperature", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}T:2-RB", loc, cell, element, "magnet", "temperature", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}F-RB", loc, cell, element, "magnet", "water flow", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}F:in-St", loc, cell, element, "magnet", "water flow in", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}F:out-St", loc, cell, element, "magnet", "water flow out", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}F:dif-St", loc, cell, element, "magnet", "water flow diff", "status"));
        return channels;
    }

    private Collection<XmlChannel> insert_air_magnets(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix, String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}I-RB", loc, cell, element, "power supply", "current", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}I-SP", loc, cell, element, "power supply", "current", "setpoint"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}On-Sw", loc, cell, element, "power supply", "power", "switch"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Rst-Cmd", loc, cell, element, "power supply", "reset", "command"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}On-St", loc, cell, element, "power supply", "power", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}Acc-St", loc, cell, element, "power supply", "access", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PS:", "{" + dev + "}OK-St", loc, cell, element, "power supply", "sum error", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}Fld-RB", loc, cell, element, "magnet", "field", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}Fld-SP", loc, cell, element, "magnet", "field", "setpoint"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "MG:", "{" + dev + "}T-RB", loc, cell, element, "magnet", "temperature", "readback"));
        return channels;
    }


    private Collection<XmlChannel> insert_valves(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix, String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}Opn-Sw", loc, cell, element, "valve", "position", "switch"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}Opn-St", loc, cell, element, "valve", "position", "status"));
        return channels;
    }

    private Collection<XmlChannel> insert_gauges(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix, String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}P-RB", loc, cell, element, "gauge", "pressure", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}OK-St", loc, cell, element, "gauge", "error", "status"));
        return channels;
    }


    private Collection<XmlChannel> insert_pumps(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix,
                                                String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}I-RB", loc, cell, element, "pump", "current", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}P-RB", loc, cell, element, "pump", "pressure", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}On-Sw", loc, cell, element, "pump", "power", "switch"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}OK-St", loc, cell, element, "pump", "error", "status"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "VA:", "{" + dev + "}On-St", loc, cell, element, "pump", "power", "status"));
        return channels;
    }

    private Collection<XmlChannel> insert_temps(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix,
                                                String dev, String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PU:T", "{" + dev + "}T:1-RB", loc, cell, element, "sensor", "temperature 1", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PU:T", "{" + dev + "}T:2-RB", loc, cell, element, "sensor", "temperature 2", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PU:T", "{" + dev + "}T:3-RB", loc, cell, element, "sensor", "temperature 3", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PU:T", "{" + dev + "}T:4-RB", loc, cell, element, "sensor", "temperature 4", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "PU:T", "{" + dev + "}On-St", loc, cell, element, "sensor", "power", "status"));
        return channels;
    }

    private Collection<XmlChannel> insert_bpms(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCell, int count, String prefix, String dev,
                                               String loc, String cell, String element) {
        List<XmlChannel> channels = new ArrayList<>();
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "BI:", "{" + dev + "}Pos:X-RB", loc, cell, element, "bpm", "x position", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "BI:", "{" + dev + "}Pos:Y-RB", loc, cell, element, "bpm", "y position", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "BI:", "{" + dev + "}Sig:X-RB", loc, cell, element, "bpm", "x sigma", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "BI:", "{" + dev + "}Sig:Y-RB", loc, cell, element, "bpm", "y sigma", "readback"));
        channels.addAll(insert_bunch(tokens, channelInCell, count, prefix, "BI:", "{" + dev + "}On-St", loc, cell, element, "bpm", "power", "status"));
        return channels;
    }

    private Collection<XmlChannel> insert_bunch(Map<Integer, List<Integer>> tokens, AtomicInteger channelInCellCounter, int count, String prefix,
                                                String midfix, String postfix, String location, String cell, String element, String device, String unit,
                                                String sigtype) {
        int cw = count > 9 ? 2 : 1;
        List<XmlChannel> result = new ArrayList<>(count);
        for (int i = 1; i < count + 1; i++) {
            XmlChannel channel;
            if (count == 1) {
                channel = new XmlChannel(prefix + cell + "-" + midfix + postfix, cowner);
            } else {
                channel = new XmlChannel(prefix + cell + "-" + midfix + String.format("%0" + cw + "d", i) + postfix,
                        cowner);
            }
            int channelInCell = channelInCellCounter.getAndIncrement();

            channel.getProperties().add(new XmlProperty("location", powner, location));
            channel.getProperties().add(new XmlProperty("cell", powner, cell));
            channel.getProperties().add(new XmlProperty("element", powner, element));
            channel.getProperties().add(new XmlProperty("device", powner, device));
            if (count != 1) {
                channel.getProperties().add(new XmlProperty("family", powner, String.format("%0" + cw + "d", i)));
            }
            channel.getProperties().add(new XmlProperty("unit", powner, unit));
            channel.getProperties().add(new XmlProperty("type", powner, sigtype));

            String pos_c = String.format("%03d", Math.round((10.0 / (count + 1)) * i));
            channel.getProperties().add(new XmlProperty("z_pos_r", powner, pos_c));

            if (postfix.endsWith("}T:1-RB")) {
                channel.getProperties().add(new XmlProperty("mount", powner, "outside"));
            } else if (postfix.endsWith("}T:2-RB")) {
                channel.getProperties().add(new XmlProperty("mount", powner, "inside"));
            } else if (postfix.endsWith("}T:3-RB")) {
                channel.getProperties().add(new XmlProperty("mount", powner, "top"));
            } else if (postfix.endsWith("}T:4-RB")) {
                channel.getProperties().add(new XmlProperty("mount", powner, "bottom"));
            } else {
                channel.getProperties().add(new XmlProperty("mount", powner, "center"));
            }

            for (Entry<Integer, List<Integer>> entry : tokens.entrySet()) {
                // pop val from the tokens
                Integer val = entry.getValue().remove(ThreadLocalRandom.current().nextInt(entry.getValue().size()));
                channel.getProperties().add(new XmlProperty("group" + String.valueOf(entry.getKey()), powner, String.valueOf(val)));
                channel.getTags().add(new XmlTag("group" + String.valueOf(entry.getKey()) + "_" + val, towner));
            }

            if (channelInCell % 2 == 1) {
                channel.getProperties().add(new XmlProperty("group6", powner, "500"));
                channel.getTags().add(new XmlTag("group6_500", towner));
            } else if (channelInCell <= 2 * 200) {
                channel.getProperties().add(new XmlProperty("group6", powner, "200"));
                channel.getTags().add(new XmlTag("group6_200", towner));
            } else if (channelInCell <= 2 * (200 + 100)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "100"));
                channel.getTags().add(new XmlTag("group6_100", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "50"));
                channel.getTags().add(new XmlTag("group6_50", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "20"));
                channel.getTags().add(new XmlTag("group6_20", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "10"));
                channel.getTags().add(new XmlTag("group6_10", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "5"));
                channel.getTags().add(new XmlTag("group6_5", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "2"));
                channel.getTags().add(new XmlTag("group6_2", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
                channel.getProperties().add(new XmlProperty("group6", powner, "1"));
                channel.getTags().add(new XmlTag("group6_1", towner));
            } else {
                channel.getProperties().add(new XmlProperty("group6", powner, "0"));
                channel.getTags().add(new XmlTag("group6_0", towner));
            }

            if (channelInCell % 2 == 0) {
                channel.getProperties().add(new XmlProperty("group7", powner, "500"));
                channel.getTags().add(new XmlTag("group7_500", towner));
            } else if (channelInCell <= 2 * 200) {
                channel.getProperties().add(new XmlProperty("group7", powner, "200"));
                channel.getTags().add(new XmlTag("group7_200", towner));
            } else if (channelInCell <= 2 * (200 + 100)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "100"));
                channel.getTags().add(new XmlTag("group7_100", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "50"));
                channel.getTags().add(new XmlTag("group7_50", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "20"));
                channel.getTags().add(new XmlTag("group7_20", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "10"));
                channel.getTags().add(new XmlTag("group7_10", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "5"));
                channel.getTags().add(new XmlTag("group7_5", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "2"));
                channel.getTags().add(new XmlTag("group7_2", towner));
            } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
                channel.getProperties().add(new XmlProperty("group7", powner, "1"));
                channel.getTags().add(new XmlTag("group7_1", towner));
            } else {
                channel.getProperties().add(new XmlProperty("group7", powner, "0"));
                channel.getTags().add(new XmlTag("group7_0", towner));
            }
            if (channelInCell <= 500) {
                channel.getProperties().add(new XmlProperty("group8", powner, "500"));
                channel.getTags().add(new XmlTag("group8_500", towner));
            } else if (channelInCell <= 500 + 200) {
                channel.getProperties().add(new XmlProperty("group8", powner, "200"));
                channel.getTags().add(new XmlTag("group8_200", towner));
            } else if (channelInCell <= 500 + (200 + 100)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "100"));
                channel.getTags().add(new XmlTag("group8_100", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "50"));
                channel.getTags().add(new XmlTag("group8_50", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50 + 20)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "20"));
                channel.getTags().add(new XmlTag("group8_20", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50 + 20 + 10)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "10"));
                channel.getTags().add(new XmlTag("group8_10", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50 + 20 + 10 + 5)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "5"));
                channel.getTags().add(new XmlTag("group8_5", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "2"));
                channel.getTags().add(new XmlTag("group8_2", towner));
            } else if (channelInCell <= 500 + (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
                channel.getProperties().add(new XmlProperty("group8", powner, "1"));
                channel.getTags().add(new XmlTag("group8_1", towner));
            } else {
                channel.getProperties().add(new XmlProperty("group8", powner, "0"));
                channel.getTags().add(new XmlTag("group8_0", towner));
            }

            if (channelInCell > 500) {
                channel.getProperties().add(new XmlProperty("group9", powner, "500"));
                channel.getTags().add(new XmlTag("group9_500", towner));
            } else if (channelInCell > 500 - 200) {
                channel.getProperties().add(new XmlProperty("group9", powner, "200"));
                channel.getTags().add(new XmlTag("group9_200", towner));
            } else if (channelInCell > 500 - 200 - 100) {
                channel.getProperties().add(new XmlProperty("group9", powner, "100"));
                channel.getTags().add(new XmlTag("group9_100", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50) {
                channel.getProperties().add(new XmlProperty("group9", powner, "50"));
                channel.getTags().add(new XmlTag("group9_50", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50 - 20) {
                channel.getProperties().add(new XmlProperty("group9", powner, "20"));
                channel.getTags().add(new XmlTag("group9_20", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50 - 20 - 10) {
                channel.getProperties().add(new XmlProperty("group9", powner, "10"));
                channel.getTags().add(new XmlTag("group9_10", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50 - 20 - 10 - 5) {
                channel.getProperties().add(new XmlProperty("group9", powner, "5"));
                channel.getTags().add(new XmlTag("group9_5", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50 - 20 - 10 - 5 - 2) {
                channel.getProperties().add(new XmlProperty("group9", powner, "2"));
                channel.getTags().add(new XmlTag("group9_2", towner));
            } else if (channelInCell > 500 - 200 - 100 - 50 - 20 - 10 - 5 - 2 - 1) {
                channel.getProperties().add(new XmlProperty("group9", powner, "1"));
                channel.getTags().add(new XmlTag("group9_1", towner));
            } else {
                channel.getProperties().add(new XmlProperty("group9", powner, "0"));
                channel.getTags().add(new XmlTag("group9_0", towner));
            }

            for (int j = 20; j < max_prop; j++) {
                channel.getProperties().add(new XmlProperty("prop" + String.format("%02d", j), powner,
                        channelInCell + "-" + String.format("%02d", j)));
            }
            for (int k = 11; k < max_tag; k++) {
                channel.getTags().add(new XmlTag("tag" + String.format("%02d", k), towner));
            }
            Integer cellCount = Integer.valueOf(cell);
            if (cellCount % 9 == 0) {
                channel.getTags().add(new XmlTag("tagnine", towner));
            } else if (cellCount % 8 == 0) {
                channel.getTags().add(new XmlTag("tageight", towner));
            } else if (cellCount % 7 == 0) {
                channel.getTags().add(new XmlTag("tagseven", towner));
            } else if (cellCount % 6 == 0) {
                channel.getTags().add(new XmlTag("tagsix", towner));
            } else if (cellCount % 5 == 0) {
                channel.getTags().add(new XmlTag("tagfive", towner));
            } else if (cellCount % 4 == 0) {
                channel.getTags().add(new XmlTag("tagfour", towner));
            } else if (cellCount % 3 == 0) {
                channel.getTags().add(new XmlTag("tagthree", towner));
            } else if (cellCount % 2 == 0) {
                channel.getTags().add(new XmlTag("tagtwo", towner));
            } else {
                channel.getTags().add(new XmlTag("tagone", towner));
            }

            channel_list.add(channel.getName());
            prop_list.addAll(channel.getProperties().stream().map(p ->
                new XmlProperty(p.getName(), p.getOwner())
            ).collect(Collectors.toSet()));
            tag_list.addAll(channel.getTags());
            result.add(channel);
        }
        return result;
    }

}
