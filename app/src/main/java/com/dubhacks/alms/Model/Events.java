package com.dubhacks.alms.Model;

import java.util.List;

public class Events {

    public String name, locationName, address, url, startTime, date, description;
    public List<Double> l;
    public List<String> categories;

    public Events() {

    }

    public Events (String name, String locationName, String address,
                   List<Double> l, String url, String startTime, String date,
                   List<String> categories, String description) {
        this.name = name;
        this.locationName = locationName;
        this.address = address;
        this.l =l;
        this.url = url;
        this.startTime = startTime;
        this.date = date;
        this.categories = categories;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Double> getL() {
        return l;
    }

    public void setL(List<Double> l) {
        this.l = l;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
