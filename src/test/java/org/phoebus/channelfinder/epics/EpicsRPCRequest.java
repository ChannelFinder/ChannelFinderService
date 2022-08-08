package org.phoebus.channelfinder.epics;

import java.util.List;

import org.epics.nt.NTURI;
import org.epics.nt.NTURIBuilder;
import org.epics.pvaccess.client.rpc.RPCClientImpl;
import org.epics.pvdata.pv.PVStructure;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.epics.ChannelFinderEpicsService;
import org.phoebus.channelfinder.epics.NTXmlUtil;

/**
 * A simple example client to the channelfinder epics rpc service
 * @author Kunal Shroff
 *
 */
public class EpicsRPCRequest {

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
//            List<XmlChannel> channels = NTXmlUtil.parse(result);
//            channels.forEach(c -> System.out.println(c.toLog()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
