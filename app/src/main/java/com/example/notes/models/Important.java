package com.example.notes.models;

public class Important {

    public static final String TABLE_NAME = "important";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_DATE = "date";

    private int id;
    private String message;
    private String date;

    public Important(int id, String message, String date) {
        this.id = id;
        this.message = message;
        this.date = date;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MESSAGE + " TEXT, " +
                    COLUMN_DATE + " TEXT)";
}
