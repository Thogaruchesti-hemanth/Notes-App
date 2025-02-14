package com.example.notes.models;

public class Wish {

    // Table name
    public static final String TABLE_NAME = "wishes";

    // Column names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_WISH = "wish";
    public static final String COLUMN_DATE = "date";

    // Variables for the model
    private int id;
    private String wish;
    private String date;

    // Constructor
    public Wish(int id, String wish, String date) {
        this.id = id;
        this.wish = wish;
        this.date = date;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWish() {
        return wish;
    }

    public void setWish(String wish) {
        this.wish = wish;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // SQL to create the table
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_WISH + " TEXT, " +
                    COLUMN_DATE + " TEXT)";
}
