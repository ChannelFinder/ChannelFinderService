package org.phoebus.channelfinder.entity;

import java.util.ArrayList;
import java.util.List;

public class Scroll {
    private String id;
    private List<Channel> channels = new ArrayList<>();

    @Override
    public String toString() {
        return "Scroll{" +
                "id='" + id + '\'' +
                ", channels=" + channels +
                '}';
    }

    /**
     * Creates a new instance of Scroll.
     *
     */
    public Scroll() {
    }
    
    /**
     * Creates a new instance of Scroll.
     *
     * @param id - scroll name
     * @param channels - list of channels
     */
    public Scroll(String id, List<Channel> channels) {
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

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }
}