package gov.bnl.channelfinder;
/**
 * #%L
 * ChannelFinder Directory Service
 * %%
 * Copyright (C) 2010 - 2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * %%
 * Copyright (C) 2010 - 2012 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Channel object that can be represented as XML/JSON in payload data.
 *
 * @author Ralph Lange {@literal <ralph.lange@gmx.de>}
 */

@XmlRootElement(name="channel")
@XmlType (propOrder={"name","owner","properties","tags"})
public class XmlChannel {
    private String name;
    private String owner;
    private List<XmlProperty> properties = new ArrayList<XmlProperty>();
    private List<XmlTag> tags = new ArrayList<XmlTag>();
  
    /** Creates a new instance of XmlChannel */
    public XmlChannel() {
    }

    /**
     * Creates a new instance of XmlChannel.
     *
     * @param name - channel name
     */
    public XmlChannel(String name) {
        this.name = name;
    }

    /**
     * Creates a new instance of XmlChannel.
     *
     * @param name - channel name
     * @param owner - owner name
     */
    public XmlChannel(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    /**
     * 
     * @param name - channel name
     * @param owner - channel owner
     * @param properties - list of channel properties
     * @param tags - list of channel tags
     */
    public XmlChannel(String name, String owner, List<XmlProperty> properties, List<XmlTag> tags) {
        this.name = name;
        this.owner = owner;
        this.properties = properties;
        this.tags = tags;
    }

    /**
     * Getter for channel name.
     *
     * @return name - channel name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for channel name.
     *
     * @param name - channel name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for channel owner.
     *
     * @return owner - channel owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for channel owner.
     *
     * @param owner - channel owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<XmlProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<XmlProperty> properties) {
        this.properties = properties;
    }

    public List<XmlTag> getTags() {
        return tags;
    }

    public void setTags(List<XmlTag> tags) {
        this.tags = tags;
    }

    public void addTag(XmlTag tag) {
        this.tags.add(tag);
    }

    public void addTags(List<XmlTag> tags) {
        this.tags.addAll(tags);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlChannel to create the string representation for
     * @return string representation
     */
    public static String toLog(XmlChannel data) {
        return data.getName() + "(" + data.getOwner() + "):["
                + (data.properties)
                + (data.tags)
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XmlChannel other = (XmlChannel) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }
}
