package com.example.notes.models;

public class ToDo {
    private final int id;
    private String task;
    private String description;
    private String dueDate;
    private int priority;
    private int isCompleted;
    private int progress;
    private String backgroundColor;

    public ToDo(int id, String task, String description, String dueDate, int priority, int isCompleted, int progress, String backgroundColor) {
        this.id = id;
        this.task = task;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.isCompleted = isCompleted;
        this.progress = progress;
        this.backgroundColor = backgroundColor;
    }

    public int getId() {
        return id;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(int isCompleted) {
        this.isCompleted = isCompleted;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
