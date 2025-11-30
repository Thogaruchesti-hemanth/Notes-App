package com.example.NotesNest.models;

public class OnBoardItem {

    String title;
    String description;
    int imageResourceId;

    public OnBoardItem(String title, String description, int imageResourceId) {
        this.title = title;
        this.description = description;
        this.imageResourceId = imageResourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}