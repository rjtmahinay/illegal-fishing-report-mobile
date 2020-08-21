package com.karagathon.vesselreporting.model;

public class Report {

    private String id;
    private String name;
    private String location;
    private String description;
    private String formattedDate;

    public Report(String id, String name, String location, String description, String formattedDate) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.formattedDate = formattedDate;
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


    public String getFormattedDate() {
        return formattedDate;
    }

}
