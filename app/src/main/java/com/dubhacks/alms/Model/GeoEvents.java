package com.dubhacks.alms.Model;

import java.util.List;

public class GeoEvents {
    public List<Double> l;
    public String g;

    public GeoEvents() {

    }

    public GeoEvents(List<Double> l, String g) {
        this.l = l;
        this.g = g;
    }

    public List<Double> getL() {
        return l;
    }

    public void setL(List<Double> l) {
        this.l = l;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }
}
