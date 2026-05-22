package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;  // Work, Ideas, Personal…
    public int order;
    public String userId;

    public CategoryEntity(String name, int order, String userId) {
        this.name = name;
        this.order = order;
        this.userId = userId;
    }

    public CategoryEntity() {
    }
}
