package org.phoebus.channelfinder.epics;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.epics.nt.NTTable;
import org.epics.nt.NTTableBuilder;
import org.epics.nt.NTURI;
import org.epics.pvaccess.PVAException;
import org.epics.pvaccess.server.rpc.RPCResponseCallback;
import org.epics.pvaccess.server.rpc.RPCServer;
import org.epics.pvaccess.server.rpc.RPCServiceAsync;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.phoebus.channelfinder.ChannelRepository;
import org.phoebus.channelfinder.XmlChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A pva RPC service for channelfinder
 * 
 * Request:
 * 
 * The client requests a query as a NTURI pvStructure.
 * 
 * Result:
 * 
 * The service returns the result as an NTTable pvStructure.
 * 
 * @author Kunal Shroff
 *
 */
@Service
@ComponentScan(basePackages="org.phoebus.channelfinder")
public class ChannelFinderEpicsService {

    private static final Logger logger = Logger.getLogger(ChannelFinderEpicsService.class.getName());

    private final ExecutorService pool = Executors.newScheduledThreadPool(1);
    
    public static final String SERVICE_DESC = "cfService:query";

    @Autowired
    ChannelRepository repository;

    RPCServer server;

    private ChannelFinderServiceImpl service;

    @PostConstruct
    public void init() {

        logger.log(Level.INFO, "Launching the epics rpc channelfinder service: " + SERVICE_DESC);
        server = new RPCServer();

        logger.log(Level.INFO, SERVICE_DESC + " initializing...");
        service = new ChannelFinderServiceImpl(repository);
        server.registerService(SERVICE_DESC, service);
        server.printInfo();
        logger.log(Level.INFO, SERVICE_DESC + " is operational.");

        pool.submit(() -> {
            try {
                server.run(0);
            } catch (PVAException e) {
                logger.log(Level.SEVERE, "Failed to start the epics rpc channelfinder service", e);
            }
        });
        
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        logger.log(Level.INFO, "Shutting down service " + SERVICE_DESC);
        try {
            service.shutdown();
            server.destroy();
            logger.log(Level.INFO, SERVICE_DESC + " Shutdown complete.");
        } catch (PVAException e) {
            logger.log(Level.SEVERE, "Failed to close service : " + SERVICE_DESC, e);
        }
    }

    private static class ChannelFinderServiceImpl implements RPCServiceAsync {


        private ChannelRepository repository;

        public ChannelFinderServiceImpl(ChannelRepository repository) {
            this.repository = repository;
            logger.log(Level.INFO, "start");
        }

        private final ExecutorService pool = Executors.newScheduledThreadPool(50);

        @Override
        public void request(PVStructure args, RPCResponseCallback call) {
            logger.log(Level.FINE, () -> args.toString());
            HandlerQuery query = new HandlerQuery(args, call, repository);
            query.run();
        }

        private static class HandlerQuery implements Runnable {

            private final RPCResponseCallback callback;
            private final PVStructure args;
            private final ChannelRepository channelRepository;

            public HandlerQuery(PVStructure args, RPCResponseCallback callback, ChannelRepository channelRepository) {
                this.callback = callback;
                this.args = args;
                this.channelRepository = channelRepository;
            }

            @Override
            public void run() {

                final Set<String> filteredColumns = Collections.emptySet();

                MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
                NTURI uri = NTURI.wrap(args);
                String[] query = uri.getQueryNames();
                for (String parameter : query) {
                    String value = uri.getQueryField(PVString.class, parameter).get();
                    if (value != null && !value.isEmpty()) {
                        switch (parameter) {
                        case "_name":
                            searchParameters.put("~name", Arrays.asList(value));
                            break;
                        case "_tag":
                            searchParameters.put("~tag", Arrays.asList(value));
                            break;
                        case "_size":
                            searchParameters.put("~size", Arrays.asList(value));
                            break;
                        case "_from":
                            searchParameters.put("~from", Arrays.asList(value));
                            break;
                        default:
                            searchParameters.put(parameter, Arrays.asList(value));
                            break;
                        }
                    }
                }

                List<XmlChannel> result = channelRepository.search(searchParameters);

                final Map<String, List<String>> channelTable = new HashMap<>();
                final Map<String, List<String>> channelPropertyTable = new HashMap<>();
                final Map<String, boolean[]> channelTagTable = new HashMap<>();
                channelTable.put("channelName", Arrays.asList(new String[result.size()]));
                channelTable.put("owner", Arrays.asList(new String[result.size()]));

                AtomicInteger counter = new AtomicInteger(0);

                result.forEach(ch -> {

                    int index = counter.getAndIncrement();

                    channelTable.get("channelName").set(index, ch.getName());
                    channelTable.get("owner").set(index, ch.getOwner());

                    if (!filteredColumns.contains("ALL")) {
                        ch.getTags().stream().filter(tag ->
                            filteredColumns.isEmpty() || filteredColumns.contains(tag.getName())
                        ).forEach(t -> {
                            if (!channelTagTable.containsKey(t.getName())) {
                                channelTagTable.put(t.getName(), new boolean[result.size()]);
                            }
                            channelTagTable.get(t.getName())[index] = true;
                        });

                        ch.getProperties().stream().filter(prop ->
                            filteredColumns.isEmpty() || filteredColumns.contains(prop.getName())
                        ).forEach(prop -> {
                            if (!channelPropertyTable.containsKey(prop.getName())) {
                                channelPropertyTable.put(prop.getName(), Arrays.asList(new String[result.size()]));
                            }
                            channelPropertyTable.get(prop.getName()).set(index, prop.getValue());
                        });
                    }
                });
                NTTableBuilder ntTableBuilder = NTTable.createBuilder();
                channelTable.keySet().forEach(name ->
                    ntTableBuilder.addColumn(name, ScalarType.pvString)
                );
                channelPropertyTable.keySet().forEach(name ->
                    ntTableBuilder.addColumn(name, ScalarType.pvString)
                );
                channelTagTable.keySet().forEach(name ->
                    ntTableBuilder.addColumn(name, ScalarType.pvBoolean)
                );
                NTTable ntTable = ntTableBuilder.create();

                channelTable.entrySet().stream().forEach(col ->
                    ntTable.getColumn(PVStringArray.class, col.getKey()).put(0, col.getValue().size(),
                            col.getValue().stream().toArray(String[]::new), 0)
                );

                channelPropertyTable.entrySet().stream().forEach(col ->
                    ntTable.getColumn(PVStringArray.class, col.getKey()).put(0, col.getValue().size(),
                            col.getValue().stream().toArray(String[]::new), 0)
                );

                channelTagTable.entrySet().stream().forEach(col ->
                    ntTable.getColumn(PVBooleanArray.class, col.getKey()).put(0, col.getValue().length,
                            col.getValue(), 0)
                );

                logger.log(Level.FINE, () -> ntTable.toString());
                this.callback.requestDone(StatusFactory.getStatusCreate().getStatusOK(), ntTable.getPVStructure());
            }
        }
    
        public void shutdown() {
            logger.log(Level.INFO, "shutting down service.");
            pool.shutdown();
            // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            logger.log(Level.INFO, "completed shut down.");
        }
    }
}
