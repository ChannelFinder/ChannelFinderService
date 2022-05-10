package org.phoebus.channelfinder.epics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.epics.nt.NTTable;
import org.epics.pvdata.pv.BooleanArrayData;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StringArrayData;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.channelfinder.XmlProperty;
import org.phoebus.channelfinder.XmlTag;

public class NTXmlUtil {

    /**
     * A helper method to convert the the result of the channelfinder v4 service
     * to the a list of {@link XmlChannel}
     * 
     * @param result - NTTable returned by the channelfinder service
     * @return list of channels
     * @throws Exception failed to convert to NTTable
     */
    public static synchronized List<XmlChannel> parse(PVStructure result) throws Exception {
        if (NTTable.isCompatible(result)) {
            NTTable table = NTTable.wrap(result);
            List<String> names = Arrays.asList(table.getColumnNames());
            List<XmlChannel> channels = new ArrayList<>();
            
            if(names.contains("channelName")){
                PVStringArray array = (PVStringArray) table.getColumn("channelName");
                StringArrayData data = new StringArrayData();
                int len = array.get(0, array.getLength(), data);
                Arrays.asList(data.data).forEach(name -> channels.add(new XmlChannel(name)));
            }
            
            if(names.contains("owner")){
                PVStringArray array = (PVStringArray) table.getColumn("owner");
                StringArrayData data = new StringArrayData();
                int len = array.get(0, array.getLength(), data);
                List<Optional<String>> owners = Arrays.asList(data.data).stream().map(Optional::ofNullable)
                        .collect(Collectors.toList());
                for (int i = 0; i < channels.size(); i++) {
                    if (owners.get(i).isPresent())
                        channels.get(i).setOwner(owners.get(i).get());
                }
            }
            
            for (String name : names) {
                if(!name.equals("channelName") && !name.equals("owner")){
                    ScalarType type = table.getColumn(name).getScalarArray().getElementType();
                    if (type.equals(ScalarType.pvBoolean)){
                        PVBooleanArray array = (PVBooleanArray) table.getColumn(name);
                        BooleanArrayData data = new BooleanArrayData();
                        array.get(0, array.getLength(), data);
                        List<Boolean> tag = new ArrayList<>(array.getLength());
                        for (int i = 0; i < array.getLength(); i++) {
                            tag.add(data.data[i]);
                            if(data.data[i]){
                                channels.get(i).getTags().add(new XmlTag(name));
                            }
                        }
                    }
                    else if (type.equals(ScalarType.pvString)) {
                        PVStringArray array = (PVStringArray) table.getColumn(name);
                        StringArrayData data = new StringArrayData();
                        int len = array.get(0, array.getLength(), data);
                        List<Optional<String>> list = Arrays.asList(data.data).stream().map(Optional::ofNullable)
                                .collect(Collectors.toList());
                        for (int i = 0; i < channels.size(); i++) {
                            if (list.get(i).isPresent())
                                channels.get(i).getProperties().add(new XmlProperty(name, null, list.get(i).get()));
                        }
                    }
                }
            }
            return channels;
        } else {
            throw new Exception(result.toString() +" is not compatible with NTTable");
        }
    }
}
