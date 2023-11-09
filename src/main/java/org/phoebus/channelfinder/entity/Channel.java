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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel object that can be represented as XML/JSON in payload data.
 *
 * @author Ralph Lange {@literal <ralph.lange@gmx.de>}
 */

public class Channel {
    private String name;
    private String owner;
    private List<Property> properties = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    /** Creates a new instance of Channel */
    public Channel() {
    }

    /**
     * Creates a new instance of Channel.
     *
     * @param name - channel name
     */
    public Channel(String name) {
        this.name = name;
    }

    /**
     * Creates a new instance of Channel.
     *
     * @param name - channel name
     * @param owner - owner name
     */
    public Channel(String name, String owner) {
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
    public Channel(String name, String owner, List<Property> properties, List<Tag> tags) {
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

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    /**
     * Add the given tag to the list of tags associated with this channel
     * If the tag already exists then it is replaced with <code>tag</code>
     * @param tag the tag to be added to the channel
     */
    public void addTag(Tag tag) {
        // If the tag already exists, then filter it out
        this.tags = this.tags.stream().filter(t ->
            !t.getName().equals(tag.getName())
        ).collect(Collectors.toList());
        // add the updated version of the tag
        this.tags.add(tag);
    }

    /**
     * Remove the given tag to the list of tags associated with this channel
     * @param tag the tag to be removed from channel
     */
    public void removeTag(Tag tag) {
        this.tags = this.tags.stream().filter(t ->
            !t.getName().equals(tag.getName())
        ).collect(Collectors.toList());
    }

    /**
     * Add the given list of tags to the list of tags associated with this channel
     * @param tags the tags to be added to the channel
     */
    public void addTags(List<Tag> tags) {
        tags.forEach(this::addTag);
    }

    /**
     * Add the given property to the list of properties associated with this channel
     * If the property already exists then it is replaced with the provided one
     * @param property the property to be added to the channel
     */
    public void addProperty(Property property) {
        // If the property already exists, then filter it out
        this.properties = this.properties.stream().filter(p ->
            !p.getName().equals(property.getName())
        ).collect(Collectors.toList());
        // add the updated version of the property
        this.properties.add(property);
    }

    /**
     * Remove the given property to the list of properties associated with this channel
     * @param property the property to be removed from the channel
     */
    public void removeProperty(Property property) {
        this.properties = this.properties.stream().filter(p ->
            !p.getName().equals(property.getName())
        ).collect(Collectors.toList());
    }

    /**
     * Add the given list of properties to the properties associated with this channel
     * @param properties the properties to be added to the channel
     */
    public void addProperties(List<Property> properties) {
        properties.forEach(this::addProperty);
    }
    /**
     * Creates a compact string representation for the log.
     *
     * @return string representation
     */
    public String toLog() {
        return this.getName() + "(" + this.getOwner() + "):["
                + (this.properties)
                + (this.tags)
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
    public String toString() {
        return "Channel{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", properties=" + properties +
                ", tags=" + tags +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Channel other = (Channel) obj;
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
