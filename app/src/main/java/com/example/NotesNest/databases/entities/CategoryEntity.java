package com.example.NotesNest.databases.entities;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String color;
    public String icon;
}
