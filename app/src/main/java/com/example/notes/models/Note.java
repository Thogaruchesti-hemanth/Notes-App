package com.example.notes.models;

public class Note {

    private int id;
    private String title;
    private String message;
    private String createdAt;
    private String backgroundColor;

    public Note(int id, String title, String message, String createdAt, String backgroundColor) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.backgroundColor = backgroundColor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
