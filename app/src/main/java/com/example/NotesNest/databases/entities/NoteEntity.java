package com.example.NotesNest.databases.entities;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
}
