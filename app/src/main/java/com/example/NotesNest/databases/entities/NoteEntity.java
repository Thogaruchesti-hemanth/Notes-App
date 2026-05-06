package com.example.NotesNest.databases.entities;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "notes",
        foreignKeys = @ForeignKey(
                entity = CategoryEntity.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index("userId"),
                @Index("categoryId")
        }
)
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;              // 👈 separate notes for multiple users
    public Integer categoryId;      // nullable

    public String title;
    public String content;          // better name for message
    public String colorHex;         // background color

    public long createdAt;          // System.currentTimeMillis()
    public long updatedAt;          // for offline sync conflict resolution
    public boolean isSynced;        // offline-first flag
    public boolean isDeleted;       // soft deletion flag
    public boolean isPinned;        // pinned note flag

    public NoteEntity() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteEntity that = (NoteEntity) o;
        return id == that.id &&
                createdAt == that.createdAt &&
                updatedAt == that.updatedAt &&
                isSynced == that.isSynced &&
                isDeleted == that.isDeleted &&
                isPinned == that.isPinned &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(categoryId, that.categoryId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(content, that.content) &&
                Objects.equals(colorHex, that.colorHex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, categoryId, title, content, colorHex, createdAt, updatedAt, isSynced, isDeleted, isPinned);
    }
}
