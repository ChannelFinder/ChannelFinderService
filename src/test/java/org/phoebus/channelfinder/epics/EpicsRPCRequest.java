package org.phoebus.channelfinder.epics;

import org.epics.nt.NTURI;
import org.epics.nt.NTURIBuilder;
import org.epics.pvaccess.client.rpc.RPCClientImpl;
import org.epics.pvdata.pv.PVStructure;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple example client to the channelfinder epics rpc service
 * @author Kunal Shroff
 *
 */
public class EpicsRPCRequest {
    private static final Logger logger = Logger.getLogger(EpicsRPCRequest.class.getName());
    public static void main(String[] args) {

        RPCClientImpl client = new RPCClientImpl(ChannelFinderEpicsService.SERVICE_DESC);

        NTURIBuilder uriBuilder = NTURI.createBuilder();
        uriBuilder.addQueryString("_name");
        NTURI uri = uriBuilder.create();
        uri.getPVStructure().getStringField("scheme").put("pva");
        uri.getPVStructure().getStringField("path").put(ChannelFinderEpicsService.SERVICE_DESC);
        uri.getQuery().getStringField("_name").put("*");
        try {
            PVStructure result = client.request(uri.getPVStructure(), 3.0);
//            List<Channel> channels = NTXmlUtil.parse(result);
//            channels.forEach(c -> System.out.println(c.toLog()));
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
