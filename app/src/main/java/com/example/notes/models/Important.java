package com.example.notes.models;

public class Important {

    private int id;
    private String title;
    private String message;
    private String dateTime;
    private String backgroundColor;

    public Important(int id, String title, String message, String dateTime, String backgroundColor) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.dateTime = dateTime;
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

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
