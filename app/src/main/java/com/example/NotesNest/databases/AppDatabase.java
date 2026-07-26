package com.example.NotesNest.databases;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.NotesNest.databases.daos.CategoryDao;
import com.example.NotesNest.databases.daos.NoteDao;
import com.example.NotesNest.databases.daos.ReminderDao;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteFTSEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;

@Database(
        entities = {
                NoteEntity.class,
                CategoryEntity.class,
                NoteFTSEntity.class,
                ReminderEntity.class
        },
        version = 5
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // ------------------- Singleton -------------------
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "notesnest.db"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ------------------- DAOs -------------------
    public abstract NoteDao noteDao();

    public abstract CategoryDao categoryDao();

    public abstract ReminderDao reminderDao();

}
