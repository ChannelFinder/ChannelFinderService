package org.phoebus.channelfinder.epics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.epics.pva.data.PVABoolArray;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.MustBeArrayException;
import org.epics.pva.data.nt.NotValueException;
import org.epics.pva.data.nt.PVATable;
import org.epics.pva.data.nt.PVAURI;
import org.epics.pva.server.RPCService;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.ChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/**
 * A pva RPC service for channelfinder
 * <p>
 * Request:
 * <p>
 * The client requests a query as a NTURI pvStructure.
 * <p>
 * Result:
 * <p>
 * The service returns the result as an NTTable pvStructure.
 * 
 * @author Kunal Shroff
 *
 */
@Service
@ComponentScan(basePackages="org.phoebus.channelfinder")
public class ChannelFinderEpicsService {

    private static final Logger logger = Logger.getLogger(ChannelFinderEpicsService.class.getName());

    public static final String SERVICE_DESC = "cfService:query";

    public static final String COLUMN_CHANNEL_NAME = "channelName";
    public static final String COLUMN_OWNER = "owner";

    @Autowired
    ChannelRepository repository;

    PVAServer server;
    ServerPV serverPV;

    @PostConstruct
    public void init() throws Exception {

        logger.log(Level.INFO, "Launching the epics rpc channelfinder service: " + SERVICE_DESC);

        server = new PVAServer();

        logger.log(Level.INFO, SERVICE_DESC + " initializing...");
        ChannelFinderServiceImpl service = new ChannelFinderServiceImpl(repository);
        serverPV = server.createPV(SERVICE_DESC, service);
        logger.log(Level.INFO, SERVICE_DESC + " is operational.");

        
    }

    @PreDestroy
    public void onDestroy() {
        logger.log(Level.INFO, "Shutting down service " + SERVICE_DESC);
        logger.info("Shutting down service " + SERVICE_DESC);
        serverPV.close();
        server.close();
        logger.info(SERVICE_DESC + " Shutdown complete.");
    }

    private static class ChannelFinderServiceImpl implements RPCService {


        private final ChannelRepository repository;

        public ChannelFinderServiceImpl(ChannelRepository repository) {
            this.repository = repository;
            logger.log(Level.INFO, "start");
        }

        @Override
        public PVAStructure call(PVAStructure args) throws Exception {
            logger.log(Level.FINE, args::toString);
            HandlerQuery query = new HandlerQuery(args, repository);
            return query.run();
        }

        private static class HandlerQuery  {

            private final PVAStructure args;
            private final ChannelRepository channelRepository;

            public HandlerQuery(PVAStructure args, ChannelRepository channelRepository) {
                this.args = args;
                this.channelRepository = channelRepository;
            }

            public PVAStructure run() throws MustBeArrayException {

                MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
                PVAURI uri = PVAURI.fromStructure(args);
                Map<String, String> query;
                try {
                    query = uri.getQuery();
                } catch (NotValueException e) {
                    logger.log(Level.WARNING, () -> "Query " + uri + " not valid." + e.getMessage());
                    throw new UnsupportedOperationException("The requested operation is not supported.");
                }
                for (String parameter : query.keySet()) {
                    String value = query.get(parameter);
                    if (value != null && !value.isEmpty()) {
                        switch (parameter) {
                        case "_name":
                            searchParameters.put("~name", List.of(value));
                            break;
                        case "_tag":
                            searchParameters.put("~tag", List.of(value));
                            break;
                        case "_size":
                            searchParameters.put("~size", List.of(value));
                            break;
                        case "_from":
                            searchParameters.put("~from", List.of(value));
                            break;
                        default:
                            searchParameters.put(parameter, List.of(value));
                            break;
                        }
                    }
                }

                List<Channel> result = channelRepository.search(searchParameters).channels();

                final Map<String, List<String>> channelTable = new HashMap<>();
                final Map<String, List<String>> channelPropertyTable = new HashMap<>();
                final Map<String, boolean[]> channelTagTable = new HashMap<>();
                channelTable.put(COLUMN_CHANNEL_NAME, Arrays.asList(new String[result.size()]));
                channelTable.put(COLUMN_OWNER, Arrays.asList(new String[result.size()]));

                AtomicInteger counter = new AtomicInteger(0);

                result.forEach(ch -> {

                    int index = counter.getAndIncrement();

                    channelTable.get(COLUMN_CHANNEL_NAME).set(index, ch.getName());
                    channelTable.get(COLUMN_OWNER).set(index, ch.getOwner());

                    ch.getTags().forEach(t -> {
                        if (!channelTagTable.containsKey(t.getName())) {
                            channelTagTable.put(t.getName(), new boolean[result.size()]);
                        }
                        channelTagTable.get(t.getName())[index] = true;
                    });

                    ch.getProperties().forEach(prop -> {
                        if (!channelPropertyTable.containsKey(prop.getName())) {
                            channelPropertyTable.put(prop.getName(), Arrays.asList(new String[result.size()]));
                        }
                        channelPropertyTable.get(prop.getName()).set(index, prop.getValue());
                    });
                });
                PVATable.PVATableBuilder ntTableBuilder = PVATable.PVATableBuilder.aPVATable().name(SERVICE_DESC);

                channelTable.keySet().forEach(name ->
                    ntTableBuilder.addColumn(new PVAStringArray(name, channelTable.get(name).toArray(String[]::new)))
                );
                channelPropertyTable.keySet().forEach(name ->
                        ntTableBuilder.addColumn(new PVAStringArray(name, channelPropertyTable.get(name).toArray(String[]::new)))
                );
                channelTagTable.keySet().forEach(name ->
                        ntTableBuilder.addColumn(new PVABoolArray(name, channelTagTable.get(name)))
                );

                logger.log(Level.FINE, ntTableBuilder::toString);
                return ntTableBuilder.build();
            }
        }

    }
}
