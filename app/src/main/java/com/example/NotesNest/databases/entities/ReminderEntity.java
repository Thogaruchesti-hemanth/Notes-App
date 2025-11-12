package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class ReminderEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String type;        // reminder, task, birthday
    private String title;       // reminder & task
    private String name;        // birthday
    private String message;     // reminder & task
    private long notification;  // time in millis
    private boolean repeated;   // yearly/based on user
    private int gradientStartColor; // store start color as int
    private int gradientEndColor;   // store end color as int

    // Add these missing fields
    private String repeatType;  // "Daily", "Weekly", "Monthly", "Yearly", "None"
    private String notifyType;  // "On that day", "Day before", etc.

    // ------------------- CONSTRUCTORS -------------------

    public ReminderEntity() {
        // Default constructor
    }

    // Optional: Constructor for easy creation
    public ReminderEntity(String type, String title, String message, long notification,
                          boolean repeated, String backgroundColor, String repeatType, String notifyType) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.notification = notification;
        this.repeated = repeated;
        this.repeatType = repeatType;
        this.notifyType = notifyType;
    }

    // ------------------- GETTERS SETTERS -------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getNotification() {
        return notification;
    }

    public void setNotification(long notification) {
        this.notification = notification;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
    }

    // ------------------- HELPER METHODS -------------------

    @Override
    public String toString() {
        return "ReminderEntity{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", notification=" + notification +
                ", repeated=" + repeated +
                ", repeatType='" + repeatType + '\'' +
                ", notifyType='" + notifyType + '\'' +
                '}';
    }

    // getters & setters
    public int getGradientStartColor() { return gradientStartColor; }
    public void setGradientStartColor(int gradientStartColor) { this.gradientStartColor = gradientStartColor; }

    public int getGradientEndColor() { return gradientEndColor; }
    public void setGradientEndColor(int gradientEndColor) { this.gradientEndColor = gradientEndColor; }

    public boolean isBirthday() {
        return "birthday".equals(type);
    }

    public boolean isTask() {
        return "task".equals(type);
    }

    public boolean isReminder() {
        return "reminder".equals(type);
    }
}