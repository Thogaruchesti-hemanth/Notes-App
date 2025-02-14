package com.example.notes.models;

public class Reminder {

    public static final String TABLE_NAME = "reminders";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_REMINDER_DATE = "reminder_date";

    private int id;
    private String message;
    private String reminderDate;

    public Reminder(int id, String message, String reminderDate) {
        this.id = id;
        this.message = message;
        this.reminderDate = reminderDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(String reminderDate) {
        this.reminderDate = reminderDate;
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MESSAGE + " TEXT, " +
                    COLUMN_REMINDER_DATE + " TEXT)";
}