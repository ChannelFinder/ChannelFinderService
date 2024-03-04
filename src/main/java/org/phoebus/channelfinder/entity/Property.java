package org.phoebus.channelfinder.entity;
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Property object that can be represented as JSON in payload data.
 *
 * @author Ralph Lange {@literal <ralph.lange@gmx.de>}
 */
public class Property {
    private String name;
    private String owner;

    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", value='" + value + '\'' +
                ", channels=" + channels +
                '}';
    }

    private String value;
    private List<Channel> channels = new ArrayList<>();

    /**
     * Creates a new instance of Property.
     *
     */
    public Property() {
    }

    /**
     * Creates a new instance of Property.
     *
     * @param name property name
     * @param owner property owner
     */
    public Property(String name, String owner) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Creates a new instance of Property.
     *
     * @param name property name
     * @param owner property owner
     * @param value property value
     */
    public Property(String name, String owner, String value) {
        this.value = value;
        this.owner = owner;
        this.name = name;
    }

    /**
     * Getter for property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property name.
     *
     * @param name property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for property value.
     *
     * @return property value
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for property value.
     *
     * @param value property value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Getter for property owner.
     *
     * @return property owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for property owner.
     *
     * @param owner property owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Get the list of channels associated with this property
     * @return {@link List} of channels
     */
    public List<Channel> getChannels() {
        return channels;
    }

    /**
     * set the channels associated with this property
     * 
     * @param channels - list of channels
     */
    public void setChannels(List<Channel> channels) {
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
            return this.getName() + "(" + this.getOwner() + ")"
                    + (this.channels);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channels == null) ? 0 : channels.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        Property other = (Property) obj;
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
        if (value == null) {
            return other.value == null;
        } else return value.equals(other.value);
    }

    /**
     * A filter to be used with the jackson mapper to ignore the embedded
     * xmlchannels in the property object
     * 
     * @author Kunal Shroff
     *
     */
    public abstract static class OnlyProperty {
        @JsonIgnore
        private List<Channel> channels;
    }

    /**
     * A filter to be used with the jackson mapper to ignore the embedded
     * xmlchannels and value in the property object
     * 
     * @author Kunal Shroff
     *
     */
    public abstract static class OnlyNameOwnerProperty {
        @JsonIgnore
        private String value;
        @JsonIgnore
        private List<Channel> channels;
    }
}
