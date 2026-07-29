package com.example.NotesNest.models;

public class Task {
    private String title;
    private String type;
    private final long startTime;
    private String id;

    private final int startColor;
    private final int endColor;
    private boolean isDone; // 👈 new field

    public Task(String title, String type, long startTime, String id, int startColor, int endColor) {
        this.title = title;
        this.type = type;
        this.startTime = startTime;

        this.id = id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // constructor, getters, setters
    public int getStartColor() {
        return startColor;
    }

    public int getEndColor() {
        return endColor;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }
}