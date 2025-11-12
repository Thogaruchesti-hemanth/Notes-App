package com.example.NotesNest.models;

public class Task {
    private String title;
    private String type;
    private long startTime;
    private long endTime;
    private long id;
    private String description; // Add this field

    private final int startColor; // e.g., 0xFFE91E63
    private final int endColor;   // e.g., 0xFFFFC107

    public Task(String title, String type, long startTime, long endTime, long id, String description, int startColor, int endColor) {
        this.title = title;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = id;
        this.description = description;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Add getter and setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // constructor, getters, setters
    public int getStartColor() {
        return startColor;
    }

    public int getEndColor() {
        return endColor;
    }
}