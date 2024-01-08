package org.phoebus.channelfinder.epics;

import org.epics.pva.data.PVABoolArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATable;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.phoebus.channelfinder.epics.ChannelFinderEpicsService.COLUMN_CHANNEL_NAME;
import static org.phoebus.channelfinder.epics.ChannelFinderEpicsService.COLUMN_OWNER;

public class NTXmlUtil {
    /**
     * A helper method to convert the result of the channelfinder v4 service
     * to a list of {@link Channel}
     *
     * @param result - NTTable returned by the channelfinder service
     * @return list of channels
     */
    public static synchronized List<Channel> parse(PVAStructure result) {
        PVATable table = PVATable.fromStructure(result);

        List<String> names = Arrays.asList(table.getLabels().get());
        List<Channel> channels = new ArrayList<>();

        if (names.contains(COLUMN_CHANNEL_NAME)) {
            PVAStringArray array = table.getColumn(COLUMN_CHANNEL_NAME);
            Arrays.stream(array.get()).forEach(c -> channels.add(new Channel(c)));
        }

        if (names.contains(COLUMN_OWNER)) {
            PVAStringArray array = table.getColumn(COLUMN_OWNER);
            List<Optional<String>> owners =
                    Arrays.stream(array.get()).map(Optional::ofNullable).collect(Collectors.toList());
            for (int i = 0; i < channels.size(); i++) {
                if (owners.get(i).isPresent())
                    channels.get(i).setOwner(owners.get(i).get());
            }
        }

        for (String name : names) {
            if (!name.equals(COLUMN_CHANNEL_NAME) && !name.equals(COLUMN_OWNER)) {
                PVAData pvaData = table.getColumn(name);

                if (pvaData.getClass().equals(PVABoolArray.class)) {
                    boolean[] array = ((PVABoolArray) pvaData).get();
                    for (int i = 0; i < array.length; i++) {
                        if (array[i]) {
                            channels.get(i).getTags().add(new Tag(name));
                        }
                    }
                } else if (pvaData.getClass().equals(PVAStringArray.class)) {
                    String[] array = ((PVAStringArray) pvaData).get();
                    for (int i = 0; i < array.length; i++) {
                        if (array[i] != null) {
                            channels.get(i).getProperties().add(new Property(name, null, array[i]));
                        }
                    }
                }
            }
        }
        return channels;
    }
}
