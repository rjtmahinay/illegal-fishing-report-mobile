package com.karagathon.vesselreporting.model;

import java.util.Date;

public class Report {

    private String id;
    private String name;
    private String location;
    private String description;
    private String email;
    private Date date;

    public Report(String id, String name, String location, String description, String email, Date date) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.email = email;
        this.date = date;
    }

    public Report() {

    }

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }


    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

    public Date getDate() {
        return date;
    }

}
