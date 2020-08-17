package com.karagathon.vesselreporting.model;

import java.time.LocalDate;

public class Report {

    private Long id;
    private String name;
    private String location;
    private String description;
    private LocalDate submissionDate;

    public Report(String name, String location, String description, LocalDate submissionDate) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.submissionDate = submissionDate;
    }

    public Report() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDate submissionDate) {
        this.submissionDate = submissionDate;
    }
}
