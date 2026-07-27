package com.example.NotesNest.databases.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String name;  // Work, Ideas, Personal…
    public int order;
    public String userId;

    public CategoryEntity(String name, int order, String userId) {
        this();
        this.name = name;
        this.order = order;
        this.userId = userId;
    }

    public CategoryEntity() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryEntity that = (CategoryEntity) o;
        return order == that.order &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, order, userId);
    }
}
