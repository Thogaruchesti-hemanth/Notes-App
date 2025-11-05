package com.example.NotesNest.databases;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.NotesNest.databases.daos.CategoryDao;
import com.example.NotesNest.databases.daos.NoteDao;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteFTSEntity;

@Database(
        entities = {
                NoteEntity.class,
                NoteFTSEntity.class,
                CategoryEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "notesDatabase.db"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract NoteDao noteDao();

    public abstract CategoryDao categoryDao();
}
