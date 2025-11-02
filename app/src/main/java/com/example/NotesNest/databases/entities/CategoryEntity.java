package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;   // e.g. All, Work, Ideas
    public String color;  // optional UI accent
    public String icon;   // optional

    public CategoryEntity(String name, String color, String icon) {
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    // ✅ Room needs this default constructor
    public CategoryEntity() {
    }
}
