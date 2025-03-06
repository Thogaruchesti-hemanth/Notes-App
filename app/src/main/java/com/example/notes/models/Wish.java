package com.example.notes.models;

public class Wish {

    private int id;
    private String wish;
    private String description;
    private String date;
    private boolean isFulfilled;
    private int priority;
    private String backgroundColor;

    public Wish(int id, String wish, String description, String date, boolean isFulfilled, int priority, String backgroundColor) {
        this.id = id;
        this.wish = wish;
        this.description = description;
        this.date = date;
        this.isFulfilled = isFulfilled;
        this.priority = priority;
        this.backgroundColor = backgroundColor;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isFulfilled() {
        return isFulfilled;
    }

    public void setFulfilled(boolean fulfilled) {
        isFulfilled = fulfilled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
