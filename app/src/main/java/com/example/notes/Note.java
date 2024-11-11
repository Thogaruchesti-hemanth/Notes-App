package com.example.notes;

public class Note {

    int id;
    String title;
    String context;

    public Note(int id,String title, String context) {
        this.id =id;
        this.title = title;
        this.context = context;
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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
