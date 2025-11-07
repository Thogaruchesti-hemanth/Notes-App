package com.example.NotesNest.models;

public class Task {
    public String name;
    public long startTime;
    public long endTime;

    public Task(String name, long startTime, long endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
