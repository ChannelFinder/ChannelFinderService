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

/**
 * Property object that can be represented as JSON in payload data.
 *
 * @author Ralph Lange {@literal <ralph.lange@gmx.de>}
 */
@XmlRootElement(name="property")
@XmlType (propOrder={"name","owner","value","channels"})
public class XmlProperty {
    private String name = null;
    private String owner = null;
    private String value = null;
    private List<XmlChannel> channels = new ArrayList<XmlChannel>();

    /**
     * Creates a new instance of XmlProperty.
     *
     */
    public XmlProperty() {
    }

    /**
     * Creates a new instance of XmlProperty.
     *
     * @param name property name
     * @param owner property owner
     */
    public XmlProperty(String name, String owner) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Creates a new instance of XmlProperty.
     *
     * @param name property name
     * @param owner property owner
     * @param value property value
     */
    public XmlProperty(String name, String owner, String value) {
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
    public List<XmlChannel> getChannels() {
        return channels;
    }

    /**
     * set the channels associated with this property
     * 
     * @param channels - list of channels
     */
    public void setChannels(List<XmlChannel> channels) {
        this.channels = channels;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data the XmlProperty to log
     * @return string representation for log
     */
    public static String toLog(XmlProperty data) {
         if (data.channels == null) {
            return data.getName() + "(" + data.getOwner() + ")";
        } else {
            return data.getName() + "(" + data.getOwner() + ")"
                    + (data.channels);
        }
    }
}
