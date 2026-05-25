package com.coit20258.drs.model;

import java.time.LocalDateTime;

public class DisasterReport {

    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";

    public static final String STATUS_REPORTED = "REPORTED";
    public static final String STATUS_UNDER_ASSESSMENT = "UNDER_ASSESSMENT";
    public static final String STATUS_RESPONDING = "RESPONDING";
    public static final String STATUS_RESOLVED = "RESOLVED";

    private int id;
    private String disasterType;
    private String location;
    private String severityLevel;
    private String description;
    private String status;
    private User reportedBy;
    private LocalDateTime reportedAt;

    public DisasterReport() {
    }

    public DisasterReport(
            String disasterType,
            String location,
            String severityLevel,
            String description,
            User reportedBy) {
        this.disasterType = disasterType;
        this.location = location;
        this.severityLevel = severityLevel;
        this.description = description;
        this.status = STATUS_REPORTED;
        this.reportedBy = reportedBy;
        this.reportedAt = LocalDateTime.now();
    }

    public DisasterReport(
            int id,
            String disasterType,
            String location,
            String severityLevel,
            String description,
            String status,
            User reportedBy,
            LocalDateTime reportedAt) {
        this.id = id;
        this.disasterType = disasterType;
        this.location = location;
        this.severityLevel = severityLevel;
        this.description = description;
        this.status = status;
        this.reportedBy = reportedBy;
        this.reportedAt = reportedAt;
    }

    public int getId() {
        return this.id;
    }

    public String getDisasterType() {
        return this.disasterType;
    }

    public String getLocation() {
        return this.location;
    }

    public String getSeverityLevel() {
        return this.severityLevel;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public User getReportedBy() {
        return this.reportedBy;
    }

    public LocalDateTime getReportedAt() {
        return this.reportedAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    @Override
    public String toString() {
        return String.format(
                "[%d] %s at %s | %s | %s",
                this.id,
                this.disasterType,
                this.location,
                this.severityLevel,
                this.status);
    }
}
