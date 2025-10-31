package com.example.NotesNest.databases.entities;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String message;
    public String date;
    public String time;
    public String background_color;
    public Integer category_id;
}
