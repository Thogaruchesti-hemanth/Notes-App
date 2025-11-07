package com.example.NotesNest.models;

import java.time.LocalDate;

public class CalendarItem {
    public String date;
    public String day;
    public LocalDate localDate;

    public CalendarItem(String date, String day, LocalDate localDate) {
        this.date = date;
        this.day = day;
        this.localDate = localDate;
    }
}
