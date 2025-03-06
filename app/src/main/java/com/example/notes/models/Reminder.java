package com.example.notes.models;

public class Reminder {
    private int id;
    private String title;
    private String message;
    private String reminderTime;
    private String backgroundColor;
    private int remindBefore;

    public Reminder(int id, String title, String message, String reminderTime, String backgroundColor, int remindBefore) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.reminderTime = reminderTime;
        this.backgroundColor = backgroundColor;
        this.remindBefore = remindBefore;
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

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getRemindBefore() {
        return remindBefore;
    }

    public void setRemindBefore(int remindBefore) {
        this.remindBefore = remindBefore;
    }
}