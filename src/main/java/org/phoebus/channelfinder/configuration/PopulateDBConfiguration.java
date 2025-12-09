package org.phoebus.channelfinder.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * An class for creating the example database.
 *
 * <p>The createDB method can be invoked with a specified list of cells For each cell, there are
 * 1500 channels The channel names follow the following convention {system}:{cell}-{device}-{signal}
 *
 * <p>systems: SR(1000) and BR(500) cell: C001 device: Magnets, Powersupply, BPMs,... signal: St or
 * RB
 *
 * <p>e.g. BR:C001-MG:1{QDP:D}Fld-SP
 *
 * <p>Additionally, each channel has a list of tags and properties
 *
 * <p>Properties include: location, cell, element, device, family, unit, type, z_pos_r, mount, and
 * some generic properties properties group[0-9] with values [0, 1, 2, 5, 10, 20, 50, 100, 200, 500]
 * which matches the # of channels with those values
 *
 * <p>Tags include: group[0-9]_[count] with count values [0, 1, 2, 5, 10, 20, 50, 100, 200, 500]
 * which matches the # of channels with those values
 *
 * @author Kunal Shroff
 */
@Configuration
public class PopulateDBConfiguration {

  private static final Logger logger = Logger.getLogger(PopulateDBConfiguration.class.getName());
  public static final String DEVICE_POWER_SUPPLY = "power supply";
  public static final String DEVICE_MAGNET = "magnet";
  public static final String UNIT_TEMP = "temperature";
  public static final String SIGTYPE_STATUS = "status";
  public static final String ELEMENT_VACUUM = "vacuum";
  public static final String UNIT_FIELD = "field";
  public static final String SIGTYPE_READBACK = "readback";
  public static final String SIGTYPE_SETPOINT = "setpoint";
  public static final String UNIT_CURRENT = "current";
  public static final String UNIT_POWER = "power";
  public static final String SIGTYPE_SWITCH = "switch";
  public static final String ELEMENT_TEMPERATURE_SENSOR = "temperature sensor";
  public static final String DEVICE_SENSOR = "sensor";
  public static final String PROPERTY_NAME_MOUNT = "mount";
  public static final String T_1_RB = "}T:1-RB";
  public static final String T_2_RB = "}T:2-RB";
  public static final String ON_ST = "}On-St";
  public static final String OK_ST = "}OK-St";
  public static final String GROUP = "group";

  static int maxProp = 40; // must be >=20
  static int maxTag = 60; // must be >=11

  // The number of cells can be increases to 100
  // Each cell would consist of a 1000 SR channels and 500 BO channels
  private static int numberOfCells = 2;

  static String cowner = "testc";
  static String powner = "testp";
  static String towner = "testt";

  public static final List<Integer> valBucket = List.of(0, 1, 2, 5, 10, 20, 50, 100, 200, 500);
  public static final List<Integer> valBucketSize =
      List.of(
          1000 - valBucket.stream().mapToInt(Integer::intValue).sum(),
          1,
          2,
          5,
          10,
          20,
          50,
          100,
          200,
          500);

  // A static list of props, tags, channels handled by this class which must be cleaned up on
  // closure.
  static Set<Property> propertySet = new HashSet<>();
  Set<Tag> tagSet = new HashSet<>();
  Set<String> channelList = new HashSet<>();

  @Autowired ElasticConfig esService;

  @Autowired
  @Qualifier("indexClient")
  ElasticsearchClient client;

  public static final ObjectMapper mapper = new ObjectMapper();

  static int index;

  static List<Integer> tokens1000 = new ArrayList<>();
  static List<Integer> tokens500 = new ArrayList<>();

  // Create a list of properties
  static {
    for (int i = 10; i < 70; i++) {
      propertySet.add(new Property("prop" + String.format("%03d", i), powner));
    }

    index = 0;
    valBucketSize.forEach(
        count -> {
          for (int i = 0; i < count; i++) {
            tokens1000.add(valBucket.get(index));
          }
          index++;
        });

    index = 0;
    valBucketSize.forEach(
        count -> {
          for (int i = 0; i < count; i++) {
            tokens500.add(valBucket.get(index));
          }
          index++;
        });
  }

  public synchronized void cleanupDB() {
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (String channelName : channelList) {
      br.operations(
          op -> op.delete(idx -> idx.index(esService.getES_CHANNEL_INDEX()).id(channelName)));
    }
    for (Tag tag : tagSet) {
      br.operations(
          op -> op.delete(idx -> idx.index(esService.getES_TAG_INDEX()).id(tag.getName())));
    }
    for (Property property : propertySet) {
      br.operations(
          op ->
              op.delete(idx -> idx.index(esService.getES_PROPERTY_INDEX()).id(property.getName())));
    }
    try {
      BulkResponse result = client.bulk(br.refresh(Refresh.True).build());
      if (result.errors()) {
        logger.log(Level.SEVERE, "CleanupDb Bulk had errors");
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
    return channelList;
  }

  public synchronized void createDB() {
    int freq = 25;
    Collection<Channel> channels = new ArrayList<>();
    createSRChannels(channels, freq);
    createBOChannels(channels, freq);
    bulkInsertAllChannels(channels);
    propertySet.forEach(p -> logger.log(Level.INFO, p.toLog()));
    tagSet.forEach(t -> logger.log(Level.INFO, t.toLog()));

    BulkRequest.Builder br = new BulkRequest.Builder();
    for (Property property : propertySet) {
      br.operations(
          op ->
              op.index(
                  bIndex ->
                      bIndex
                          .index(esService.getES_PROPERTY_INDEX())
                          .id(property.getName())
                          .document(property)));
    }
    for (Tag tag : tagSet) {
      br.operations(
          op ->
              op.index(
                  bIndex ->
                      bIndex.index(esService.getES_TAG_INDEX()).id(tag.getName()).document(tag)));
    }
    br.refresh(Refresh.True);

    checkBulkResponse(br);
    logger.log(Level.INFO, "completed populating");
  }

  private void checkBulkResponse(BulkRequest.Builder br) {
    try {
      BulkResponse results = client.bulk(br.build());
      if (results.errors()) {
        logger.log(Level.SEVERE, "CreateDB Bulk had errors");
        for (BulkResponseItem item : results.items()) {
          if (item.error() != null) {
            logger.log(Level.SEVERE, () -> item.error().reason());
          }
        }
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "CreateDB Bulk operation failed.", e);
    }
  }

  private void bulkInsertAllChannels(Collection<Channel> channels) {
    try {
      logger.info("Bulk inserting channels");

      bulkInsertChannels(channels);
      channels.clear();

    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  private void createBOChannels(Collection<Channel> channels, int freq) {
    logger.info(() -> "Creating BO channels");

    for (int i = 1; i <= numberOfCells; i++) {

      String cell = String.format("%03d", i);
      channels.addAll(insertBOCell(cell));

      if (i % freq == 0) {
        bulkInsertAllChannels(channels);
      }
    }
  }

  private void createSRChannels(Collection<Channel> channels, int freq) {
    logger.info("Creating SR channels");

    for (int i = 1; i <= numberOfCells; i++) {
      String cell = String.format("%03d", i);
      channels.addAll(insertSRCell(cell));
      if (i % freq == 0) {
        bulkInsertAllChannels(channels);
      }
    }
  }

  public void createTagsAndProperties(URL tagResource, URL propertyResource) {
    // Setup the default tags
    String tagsURL;
    tagsURL = tagResource.toExternalForm();
    try (InputStream input = new URL(tagsURL).openStream()) {
      List<Tag> jsonTag = mapper.readValue(input, new TypeReference<List<Tag>>() {});

      jsonTag.forEach(
          tag -> {
            try {
              if (!client
                  .exists(e -> e.index(esService.getES_TAG_INDEX()).id(tag.getName()))
                  .value()) {
                IndexRequest<Tag> indexRequest =
                    IndexRequest.of(
                        i ->
                            i.index(esService.getES_TAG_INDEX())
                                .id(tag.getName())
                                .document(tag)
                                .refresh(Refresh.True));
                client.index(indexRequest);
              }
            } catch (IOException e) {
              logger.log(Level.WARNING, "Failed to initialize tag : " + tag.getName(), e);
            }
          });
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Failed to initialize tag ", ex);
    }

    // Setup the default properties
    String propertiesURL;
    propertiesURL = propertyResource.toExternalForm();
    try (InputStream input = new URL(propertiesURL).openStream()) {
      List<Property> jsonTag = mapper.readValue(input, new TypeReference<>() {});

      jsonTag.forEach(
          property -> {
            try {
              if (!client
                  .exists(e -> e.index(esService.getES_PROPERTY_INDEX()).id(property.getName()))
                  .value()) {
                IndexRequest<Property> indexRequest =
                    IndexRequest.of(
                        i ->
                            i.index(esService.getES_PROPERTY_INDEX())
                                .id(property.getName())
                                .document(property)
                                .refresh(Refresh.True));
                client.index(indexRequest);
              }
            } catch (IOException e) {
              logger.log(Level.WARNING, "Failed to initialize property : " + property.getName(), e);
            }
          });
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Failed to initialize property ", ex);
    }
  }

  private Collection<Channel> insertSRCell(String cell) {
    String loc = "storage ring";
    String pre = "SR:C";
    // Tokens
    Map<Integer, List<Integer>> tokens = new HashMap<>();
    for (int i = 0; i < 6; i++) {
      tokens.put(i, new ArrayList<>(tokens1000));
    }

    AtomicInteger channelCounter = new AtomicInteger(0);

    Collection<Channel> result = new ArrayList<>(1000);
    result.addAll(insertBigMagnets(tokens, channelCounter, 2, pre, "DP", loc, cell, "dipole"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 5, pre, "QDP:D", loc, cell, "defocusing quadrupole"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 5, pre, "QDP:F", loc, cell, "focusing quadrupole"));
    result.addAll(
        insertBigMagnets(tokens, channelCounter, 4, pre, "QDP:S", loc, cell, "skew quadrupole"));
    result.addAll(insertBigMagnets(tokens, channelCounter, 4, pre, "STP", loc, cell, "sextupole"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 5, pre, "HC:S", loc, cell, "horizontal slow corrector"));
    result.addAll(
        insertAirMagnets(
            tokens, channelCounter, 5, pre, "HC:F", loc, cell, "horizontal fast corrector"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 5, pre, "VC:S", loc, cell, "vertical slow corrector"));
    result.addAll(
        insertAirMagnets(
            tokens, channelCounter, 4, pre, "VC:F", loc, cell, "vertical fast corrector"));

    result.addAll(insertValves(tokens, channelCounter, 5, pre, loc, cell));

    result.addAll(insertGauges(tokens, channelCounter, 5, pre, "VGC", loc, cell));
    result.addAll(insertGauges(tokens, channelCounter, 5, pre, "TCG", loc, cell));

    result.addAll(insertPumps(tokens, channelCounter, pre, "IPC", loc, cell));
    result.addAll(insertPumps(tokens, channelCounter, pre, "TMP", loc, cell));

    result.addAll(insertTemps(tokens, channelCounter, 40, pre, loc, cell));

    result.addAll(
        insertBpms(tokens, channelCounter, 4, pre, "BSA", loc, cell, "small aperture BPM"));
    result.addAll(
        insertBpms(tokens, channelCounter, 4, pre, "BHS", loc, cell, "high stability BPM"));
    result.addAll(
        insertBpms(tokens, channelCounter, 4, pre, "BLA", loc, cell, "large aperture BPM"));

    return result;
  }

  private void bulkInsertChannels(Collection<Channel> result) throws IOException {
    long start = System.currentTimeMillis();
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (Channel channel : result) {
      br.operations(
          op ->
              op.index(
                  IndexOperation.of(
                      i ->
                          i.index(esService.getES_CHANNEL_INDEX())
                              .id(channel.getName())
                              .document(channel))));
    }
    String prepare = "|Prepare: " + (System.currentTimeMillis() - start) + "|";
    start = System.currentTimeMillis();
    br.refresh(Refresh.True);

    BulkResponse srResult = client.bulk(br.build());
    String execute = "|Execute: " + (System.currentTimeMillis() - start) + "|";
    logger.log(Level.INFO, () -> "Inserted cell " + prepare + " " + execute);
    if (srResult.errors()) {
      logger.log(Level.SEVERE, "Bulk insert had errors");
      for (BulkResponseItem item : srResult.items()) {
        if (item.error() != null) {
          logger.log(Level.SEVERE, () -> item.error().reason());
        }
      }
    }
  }

  private Collection<Channel> insertBOCell(String cell) {
    String loc = "booster";
    String pre = "BR:C";

    // Tokens
    Map<Integer, List<Integer>> tokens = new HashMap<>();
    for (int i = 0; i < 6; i++) {
      tokens.put(i, new ArrayList<>(tokens500));
    }

    AtomicInteger channelCounter = new AtomicInteger(0);

    Collection<Channel> result = new ArrayList<>(500);

    result.addAll(insertBigMagnets(tokens, channelCounter, 2, pre, "DP", loc, cell, "dipole"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 4, pre, "QDP:D", loc, cell, "defocusing quadrupole"));
    result.addAll(
        insertBigMagnets(
            tokens, channelCounter, 4, pre, "QDP:F", loc, cell, "focusing quadrupole"));
    result.addAll(insertBigMagnets(tokens, channelCounter, 2, pre, "STP", loc, cell, "sextupole"));
    result.addAll(
        insertBigMagnets(tokens, channelCounter, 4, pre, "HC", loc, cell, "horizontal corrector"));
    result.addAll(
        insertBigMagnets(tokens, channelCounter, 4, pre, "VC", loc, cell, "vertical corrector"));

    result.addAll(insertValves(tokens, channelCounter, 4, pre, loc, cell));

    result.addAll(insertGauges(tokens, channelCounter, 4, pre, "VGC", loc, cell));
    result.addAll(insertGauges(tokens, channelCounter, 2, pre, "TCG", loc, cell));

    result.addAll(insertPumps(tokens, channelCounter, pre, "IPC", loc, cell));
    result.addAll(insertPumps(tokens, channelCounter, pre, "TMP", loc, cell));

    result.addAll(insertTemps(tokens, channelCounter, 10, pre, loc, cell));

    result.addAll(
        insertBpms(tokens, channelCounter, 2, pre, "BLA", loc, cell, "beam position monitor"));
    return result;
  }

  private Collection<Channel> insertBigMagnets(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String dev,
      String loc,
      String cell,
      String element) {
    ArrayList<Channel> channels = new ArrayList<>();
    insertPowerSupplyChannels(
        channels, tokens, channelInCell, count, prefix, dev, loc, cell, element);
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}T-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            UNIT_TEMP,
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}F-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "water flow",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}Gnd-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "ground",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}Ctl-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "control",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}Val-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "value",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}Fld-RB",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_FIELD,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}Fld-SP",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_FIELD,
            SIGTYPE_SETPOINT));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + T_1_RB,
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_TEMP,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + T_2_RB,
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_TEMP,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}F-RB",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            "water flow",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}F:in-St",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            "water flow in",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}F:out-St",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            "water flow out",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}F:dif-St",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            "water flow diff",
            SIGTYPE_STATUS));
    return channels;
  }

  private void insertPowerSupplyChannels(
      ArrayList<Channel> channels,
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String dev,
      String loc,
      String cell,
      String element) {
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}I-RB",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            UNIT_CURRENT,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}I-SP",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            UNIT_CURRENT,
            SIGTYPE_SETPOINT));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}On-Sw",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            UNIT_POWER,
            SIGTYPE_SWITCH));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}Rst-Cmd",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "reset",
            "command"));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + ON_ST,
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            UNIT_POWER,
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + "}Acc-St",
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "access",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PS:",
            "{" + dev + OK_ST,
            loc,
            cell,
            element,
            DEVICE_POWER_SUPPLY,
            "sum error",
            SIGTYPE_STATUS));
  }

  private Collection<Channel> insertAirMagnets(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String dev,
      String loc,
      String cell,
      String element) {
    ArrayList<Channel> channels = new ArrayList<>();
    insertPowerSupplyChannels(
        channels, tokens, channelInCell, count, prefix, dev, loc, cell, element);
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}Fld-RB",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_FIELD,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}Fld-SP",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_FIELD,
            SIGTYPE_SETPOINT));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "MG:",
            "{" + dev + "}T-RB",
            loc,
            cell,
            element,
            DEVICE_MAGNET,
            UNIT_TEMP,
            SIGTYPE_READBACK));
    return channels;
  }

  private Collection<Channel> insertValves(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String loc,
      String cell) {
    ArrayList<Channel> channels = new ArrayList<>();
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "VA:",
            "{" + "GV" + "}Opn-Sw",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "valve",
            "position",
            SIGTYPE_SWITCH));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "VA:",
            "{" + "GV" + "}Opn-St",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "valve",
            "position",
            SIGTYPE_STATUS));
    return channels;
  }

  private Collection<Channel> insertGauges(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String dev,
      String loc,
      String cell) {
    List<Channel> channels = new ArrayList<>();
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "VA:",
            "{" + dev + "}P-RB",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "gauge",
            "pressure",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "VA:",
            "{" + dev + OK_ST,
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "gauge",
            "error",
            SIGTYPE_STATUS));
    return channels;
  }

  private Collection<Channel> insertPumps(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      String prefix,
      String dev,
      String loc,
      String cell) {
    List<Channel> channels = new ArrayList<>();
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            2,
            prefix,
            "VA:",
            "{" + dev + "}I-RB",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "pump",
            UNIT_CURRENT,
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            2,
            prefix,
            "VA:",
            "{" + dev + "}P-RB",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "pump",
            "pressure",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            2,
            prefix,
            "VA:",
            "{" + dev + "}On-Sw",
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "pump",
            UNIT_POWER,
            SIGTYPE_SWITCH));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            2,
            prefix,
            "VA:",
            "{" + dev + OK_ST,
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "pump",
            "error",
            SIGTYPE_STATUS));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            2,
            prefix,
            "VA:",
            "{" + dev + ON_ST,
            loc,
            cell,
            PopulateDBConfiguration.ELEMENT_VACUUM,
            "pump",
            UNIT_POWER,
            SIGTYPE_STATUS));
    return channels;
  }

  private Collection<Channel> insertTemps(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String loc,
      String cell) {
    List<Channel> channels = new ArrayList<>();
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PU:T",
            "{" + "TC" + T_1_RB,
            loc,
            cell,
            ELEMENT_TEMPERATURE_SENSOR,
            DEVICE_SENSOR,
            "temperature 1",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PU:T",
            "{" + "TC" + T_2_RB,
            loc,
            cell,
            ELEMENT_TEMPERATURE_SENSOR,
            DEVICE_SENSOR,
            "temperature 2",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PU:T",
            "{" + "TC" + "}T:3-RB",
            loc,
            cell,
            ELEMENT_TEMPERATURE_SENSOR,
            DEVICE_SENSOR,
            "temperature 3",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PU:T",
            "{" + "TC" + "}T:4-RB",
            loc,
            cell,
            ELEMENT_TEMPERATURE_SENSOR,
            DEVICE_SENSOR,
            "temperature 4",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "PU:T",
            "{" + "TC" + ON_ST,
            loc,
            cell,
            ELEMENT_TEMPERATURE_SENSOR,
            DEVICE_SENSOR,
            UNIT_POWER,
            SIGTYPE_STATUS));
    return channels;
  }

  private Collection<Channel> insertBpms(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCell,
      int count,
      String prefix,
      String dev,
      String loc,
      String cell,
      String element) {
    List<Channel> channels = new ArrayList<>();
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "BI:",
            "{" + dev + "}Pos:X-RB",
            loc,
            cell,
            element,
            "bpm",
            "x position",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "BI:",
            "{" + dev + "}Pos:Y-RB",
            loc,
            cell,
            element,
            "bpm",
            "y position",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "BI:",
            "{" + dev + "}Sig:X-RB",
            loc,
            cell,
            element,
            "bpm",
            "x sigma",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "BI:",
            "{" + dev + "}Sig:Y-RB",
            loc,
            cell,
            element,
            "bpm",
            "y sigma",
            SIGTYPE_READBACK));
    channels.addAll(
        insertBunch(
            tokens,
            channelInCell,
            count,
            prefix,
            "BI:",
            "{" + dev + ON_ST,
            loc,
            cell,
            element,
            "bpm",
            UNIT_POWER,
            SIGTYPE_STATUS));
    return channels;
  }

  private Collection<Channel> insertBunch(
      Map<Integer, List<Integer>> tokens,
      AtomicInteger channelInCellCounter,
      int count,
      String prefix,
      String midfix,
      String postfix,
      String location,
      String cell,
      String element,
      String device,
      String unit,
      String sigtype) {
    int cw = count > 9 ? 2 : 1;
    List<Channel> result = new ArrayList<>(count);
    for (int i = 1; i < count + 1; i++) {
      Channel channel;
      if (count == 1) {
        channel = new Channel(prefix + cell + "-" + midfix + postfix, cowner);
      } else {
        channel =
            new Channel(
                prefix + cell + "-" + midfix + String.format("%0" + cw + "d", i) + postfix, cowner);
      }
      int channelInCell = channelInCellCounter.getAndIncrement();

      channel.getProperties().add(new Property("location", powner, location));
      channel.getProperties().add(new Property("cell", powner, cell));
      channel.getProperties().add(new Property("element", powner, element));
      channel.getProperties().add(new Property("device", powner, device));
      if (count != 1) {
        channel
            .getProperties()
            .add(new Property("family", powner, String.format("%0" + cw + "d", i)));
      }
      channel.getProperties().add(new Property("unit", powner, unit));
      channel.getProperties().add(new Property("type", powner, sigtype));

      String posC = String.format("%03d", Math.round((10.0 / (count + 1)) * i));
      channel.getProperties().add(new Property("z_pos_r", powner, posC));

      if (postfix.endsWith(T_1_RB)) {
        channel.getProperties().add(new Property(PROPERTY_NAME_MOUNT, powner, "outside"));
      } else if (postfix.endsWith(T_2_RB)) {
        channel.getProperties().add(new Property(PROPERTY_NAME_MOUNT, powner, "inside"));
      } else if (postfix.endsWith("}T:3-RB")) {
        channel.getProperties().add(new Property(PROPERTY_NAME_MOUNT, powner, "top"));
      } else if (postfix.endsWith("}T:4-RB")) {
        channel.getProperties().add(new Property(PROPERTY_NAME_MOUNT, powner, "bottom"));
      } else {
        channel.getProperties().add(new Property(PROPERTY_NAME_MOUNT, powner, "center"));
      }

      for (Entry<Integer, List<Integer>> entry : tokens.entrySet()) {
        // pop val from the tokens
        int randIndex = ThreadLocalRandom.current().nextInt(entry.getValue().size());
        Integer val = entry.getValue().remove(randIndex);
        channel
            .getProperties()
            .add(new Property("group" + entry.getKey(), powner, String.valueOf(val)));
        channel.getTags().add(new Tag("group" + entry.getKey() + "_" + val, towner));
      }

      String group = GROUP + 6;
      if (channelInCell % 2 == 1) {
        channel.getProperties().add(new Property(group, powner, "500"));
        channel.getTags().add(new Tag(group + "_500", towner));
      } else if (channelInCell < 2 * 200) {
        channel.getProperties().add(new Property(group, powner, "200"));
        channel.getTags().add(new Tag(group + "_200", towner));
      } else if (channelInCell < 2 * (200 + 100)) {
        channel.getProperties().add(new Property(group, powner, "100"));
        channel.getTags().add(new Tag(group + "_100", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50)) {
        channel.getProperties().add(new Property(group, powner, "50"));
        channel.getTags().add(new Tag(group + "_50", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50 + 20)) {
        channel.getProperties().add(new Property(group, powner, "20"));
        channel.getTags().add(new Tag(group + "_20", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50 + 20 + 10)) {
        channel.getProperties().add(new Property(group, powner, "10"));
        channel.getTags().add(new Tag(group + "_10", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50 + 20 + 10 + 5)) {
        channel.getProperties().add(new Property(group, powner, "5"));
        channel.getTags().add(new Tag(group + "_5", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
        channel.getProperties().add(new Property(group, powner, "2"));
        channel.getTags().add(new Tag(group + "_2", towner));
      } else if (channelInCell < 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
        channel.getProperties().add(new Property(group, powner, "1"));
        channel.getTags().add(new Tag(group + "_1", towner));
      } else {
        channel.getProperties().add(new Property(group, powner, "0"));
        channel.getTags().add(new Tag(group + "_0", towner));
      }

      group = GROUP + 7;
      if (channelInCell % 2 == 0) {
        channel.getProperties().add(new Property(group, powner, String.valueOf(500)));
        channel.getTags().add(new Tag(group + "_500", towner));
      } else if (channelInCell <= 2 * 200) {
        channel.getProperties().add(new Property(group, powner, "200"));
        channel.getTags().add(new Tag(group + "_200", towner));
      } else if (channelInCell <= 2 * (200 + 100)) {
        channel.getProperties().add(new Property(group, powner, "100"));
        channel.getTags().add(new Tag(group + "_100", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50)) {
        channel.getProperties().add(new Property(group, powner, "50"));
        channel.getTags().add(new Tag(group + "_50", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50 + 20)) {
        channel.getProperties().add(new Property(group, powner, "20"));
        channel.getTags().add(new Tag(group + "_20", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10)) {
        channel.getProperties().add(new Property(group, powner, "10"));
        channel.getTags().add(new Tag(group + "_10", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5)) {
        channel.getProperties().add(new Property(group, powner, "5"));
        channel.getTags().add(new Tag(group + "_5", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
        channel.getProperties().add(new Property(group, powner, "2"));
        channel.getTags().add(new Tag(group + "_2", towner));
      } else if (channelInCell <= 2 * (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
        channel.getProperties().add(new Property(group, powner, "1"));
        channel.getTags().add(new Tag(group + "_1", towner));
      } else {
        channel.getProperties().add(new Property(group, powner, "0"));
        channel.getTags().add(new Tag(group + "_0", towner));
      }

      group = GROUP + 8;
      if (channelInCell < 500) {
        channel.getProperties().add(new Property(group, powner, String.valueOf(500)));
        channel.getTags().add(new Tag(group + "_500", towner));
      } else if (channelInCell < 500 + 200) {
        channel.getProperties().add(new Property(group, powner, "200"));
        channel.getTags().add(new Tag(group + "_200", towner));
      } else if (channelInCell < 500 + (200 + 100)) {
        channel.getProperties().add(new Property(group, powner, "100"));
        channel.getTags().add(new Tag(group + "_100", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50)) {
        channel.getProperties().add(new Property(group, powner, "50"));
        channel.getTags().add(new Tag(group + "_50", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50 + 20)) {
        channel.getProperties().add(new Property(group, powner, "20"));
        channel.getTags().add(new Tag(group + "_20", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50 + 20 + 10)) {
        channel.getProperties().add(new Property(group, powner, "10"));
        channel.getTags().add(new Tag(group + "_10", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50 + 20 + 10 + 5)) {
        channel.getProperties().add(new Property(group, powner, "5"));
        channel.getTags().add(new Tag(group + "_5", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50 + 20 + 10 + 5 + 2)) {
        channel.getProperties().add(new Property(group, powner, "2"));
        channel.getTags().add(new Tag(group + "_2", towner));
      } else if (channelInCell < 500 + (200 + 100 + 50 + 20 + 10 + 5 + 2 + 1)) {
        channel.getProperties().add(new Property(group, powner, "1"));
        channel.getTags().add(new Tag(group + "_1", towner));
      } else {
        channel.getProperties().add(new Property(group, powner, "0"));
        channel.getTags().add(new Tag(group + "_0", towner));
      }

      group = GROUP + 9;
      if (channelInCell >= 500) {
        channel.getProperties().add(new Property(group, powner, String.valueOf(500)));
        channel.getTags().add(new Tag(group + "_500", towner));
      } else if (channelInCell >= 500 - 200) {
        channel.getProperties().add(new Property(group, powner, "200"));
        channel.getTags().add(new Tag(group + "_200", towner));
      } else if (channelInCell >= 500 - 200 - 100) {
        channel.getProperties().add(new Property(group, powner, "100"));
        channel.getTags().add(new Tag(group + "_100", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50) {
        channel.getProperties().add(new Property(group, powner, "50"));
        channel.getTags().add(new Tag(group + "_50", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50 - 20) {
        channel.getProperties().add(new Property(group, powner, "20"));
        channel.getTags().add(new Tag(group + "_20", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50 - 20 - 10) {
        channel.getProperties().add(new Property(group, powner, "10"));
        channel.getTags().add(new Tag(group + "_10", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50 - 20 - 10 - 5) {
        channel.getProperties().add(new Property(group, powner, "5"));
        channel.getTags().add(new Tag(group + "_5", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50 - 20 - 10 - 5 - 2) {
        channel.getProperties().add(new Property(group, powner, "2"));
        channel.getTags().add(new Tag(group + "_2", towner));
      } else if (channelInCell >= 500 - 200 - 100 - 50 - 20 - 10 - 5 - 2 - 1) {
        channel.getProperties().add(new Property(group, powner, "1"));
        channel.getTags().add(new Tag(group + "_1", towner));
      } else {
        channel.getProperties().add(new Property(group, powner, "0"));
        channel.getTags().add(new Tag(group + "_0", towner));
      }

      for (int j = 20; j < maxProp; j++) {
        channel
            .getProperties()
            .add(
                new Property(
                    "prop" + String.format("%02d", j),
                    powner,
                    channelInCell + "-" + String.format("%02d", j)));
      }
      for (int k = 11; k < maxTag; k++) {
        channel.getTags().add(new Tag("tag" + String.format("%02d", k), towner));
      }
      int cellCount = Integer.parseInt(cell);
      if (cellCount % 9 == 0) {
        channel.getTags().add(new Tag("tagnine", towner));
      } else if (cellCount % 8 == 0) {
        channel.getTags().add(new Tag("tageight", towner));
      } else if (cellCount % 7 == 0) {
        channel.getTags().add(new Tag("tagseven", towner));
      } else if (cellCount % 6 == 0) {
        channel.getTags().add(new Tag("tagsix", towner));
      } else if (cellCount % 5 == 0) {
        channel.getTags().add(new Tag("tagfive", towner));
      } else if (cellCount % 4 == 0) {
        channel.getTags().add(new Tag("tagfour", towner));
      } else if (cellCount % 3 == 0) {
        channel.getTags().add(new Tag("tagthree", towner));
      } else if (cellCount % 2 == 0) {
        channel.getTags().add(new Tag("tagtwo", towner));
      } else {
        channel.getTags().add(new Tag("tagone", towner));
      }

      channelList.add(channel.getName());
      propertySet.addAll(
          channel.getProperties().stream()
              .map(p -> new Property(p.getName(), p.getOwner()))
              .collect(Collectors.toSet()));
      tagSet.addAll(channel.getTags());
      result.add(channel);
    }
    return result;
  }
}
