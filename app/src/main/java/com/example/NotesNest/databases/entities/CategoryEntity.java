package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;  // Work, Ideas, Personal…
    public int order;

    public CategoryEntity(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public CategoryEntity() {
    }
}
