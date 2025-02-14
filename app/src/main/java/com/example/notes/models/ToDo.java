package com.example.notes.models;

public class ToDo {

    // Table name
    public static final String TABLE_NAME = "todos";

    // Column names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TASK = "task";
    public static final String COLUMN_DUE_DATE = "due_date";

    // Variables for the model
    private int id;
    private String task;
    private String dueDate;

    // Constructor
    public ToDo(int id, String task, String dueDate) {
        this.id = id;
        this.task = task;
        this.dueDate = dueDate;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    // SQL to create the table
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TASK + " TEXT, " +
                    COLUMN_DUE_DATE + " TEXT)";
}
