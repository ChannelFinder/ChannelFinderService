package org.phoebus.channelfinder.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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

    private static Logger logger = Logger.getLogger(PopulateService.class.getCanonicalName());

    // The number of cells can be increases to 100
    // Each cell would consist of a 1000 SR channels and 500 BO channels
    private static int numberOfCells = 2;

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

    // Create a list of properties
    static {
        for (int i = 10; i < 70; i++) {
            prop_list.add(new XmlProperty("prop" + String.format("%03d", i), PopulateServiceUtil.powner));
        }
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
        // create SR cell channels with properties, tags
        Collection<XmlChannel> result = PopulateServiceUtil.insertSRCellChannels(cell);

        // collect channels, properties, tags
        for (XmlChannel channel : result) {
            channel_list.add(channel.getName());
            prop_list.addAll(channel.getProperties().stream().map(p -> new XmlProperty(p.getName(), p.getOwner())).collect(Collectors.toSet()));
            tag_list.addAll(channel.getTags());
        }

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
        logger.log(Level.INFO, () -> "Inserted SR cell " + cell + " " + prepare + " " + execute);
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
        // create BO cell channels with properties, tags
        Collection<XmlChannel> result = PopulateServiceUtil.insertBOCellChannels(cell);

        // collect channels, properties, tags
        for (XmlChannel channel : result) {
            channel_list.add(channel.getName());
            prop_list.addAll(channel.getProperties().stream().map(p -> new XmlProperty(p.getName(), p.getOwner())).collect(Collectors.toSet()));
            tag_list.addAll(channel.getTags());
        }

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
        logger.log(Level.INFO, () -> "Inserted BO cell " + cell + " " + prepare + " " + execute);
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

}
