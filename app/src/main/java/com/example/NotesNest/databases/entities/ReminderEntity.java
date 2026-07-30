package com.example.NotesNest.databases.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(
        tableName = "reminders",
        indices = {
                @Index("userId"),
                @Index("notificationTime")
        }
)
public class ReminderEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String userId;                 // 👈 support multiple users
    public String type;                // reminder | task | birthday

    // Common fields
    public String title;               // reminder + task
    public String message;             // reminder + task

    // Birthday specific
    public String name;                // birthday person name

    // Notification
    public long notificationTime;      // millis
    public boolean isRepeated;
    public String repeatType;          // Daily, Weekly, Monthly, Yearly, None
    public String notifyType;          // On day, Day before, etc.

    // UI Colors
    public int gradientStartColor;
    public int gradientEndColor;
    // Offline-first sync fields
    public boolean isDeleted;          // soft-deleted
    public boolean isDone;             // 👈 new field for completion state

    public ReminderEntity() {
        this.id = UUID.randomUUID().toString();
    }
}
