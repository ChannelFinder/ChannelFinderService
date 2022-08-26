package org.phoebus.channelfinder;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="scroll")
@XmlType (propOrder={"id","channels"})
public class XmlScroll {
    private String id;
    private List<XmlChannel> channels = new ArrayList<>();
    
    /**
     * Creates a new instance of XmlScroll.
     *
     */
    public XmlScroll() {
    }
    
    /**
     * Creates a new instance of XmlScroll.
     *
     * @param id - scroll name
     * @param channels - list of channels
     */
    public XmlScroll(String id, List<XmlChannel> channels) {
        super();
        this.id = id;
        this.channels = channels;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<XmlChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<XmlChannel> channels) {
        this.channels = channels;
    }
}