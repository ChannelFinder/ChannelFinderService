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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Tag object that can be represented as XML/JSON in payload data.
 *
 * @author Ralph Lange {@literal <ralph.lange@gmx.de>}
 */
@XmlRootElement(name="tag")
@XmlType (propOrder={"name","owner","channels"})
public class XmlTag {
    private String name = null;
    private String owner = null;
    private List<XmlChannel> channels = new ArrayList<XmlChannel>();

    /**
     * Creates a new instance of XmlTag.
     *
     */
    public XmlTag() {
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name name of new tag
     */
    public XmlTag(String name) {
        this.name = name;
    }

    /**
     * Creates a new instance of XmlTag.
     *
     * @param name name of new tag
     * @param owner owner of new tag
     */
    public XmlTag(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    /**
     * Getter for tag name.
     *
     * @return tag name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for tag name.
     *
     * @param name tag name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for tag owner.
     *
     * @return tag owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for tag owner.
     *
     * @param owner tag owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for tag's XmlChannels.
     *
     * @return XmlChannels object
     */
    public List<XmlChannel> getChannels() {
        return channels;
    }

    /**
     * Setter for tag's XmlChannels.
     *
     * @param channels XmlChannels object
     */
    public void setChannels(List<XmlChannel> channels) {
        this.channels = channels;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @return string representation for log
     */
    public String toLog() {
        if (this.channels == null) {
            return this.getName() + "(" + this.getOwner() + ")";
        } else {
            return this.getName() + "(" + this.getOwner() + ")" + (this.channels);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channels == null) ? 0 : channels.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
        XmlTag other = (XmlTag) obj;
        if (channels == null) {
            if (other.channels != null)
                return false;
        } else if (!channels.equals(other.channels))
            return false;
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
        return true;
    }

    /**
     * A filter to be used with the jackson mapper to ignore the embedded
     * xmlchannels in the tag object
     * 
     * @author Kunal Shroff
     *
     */
    abstract class OnlyXmlTag {
        @JsonIgnore
        private List<XmlChannel> channels;
    }

}
