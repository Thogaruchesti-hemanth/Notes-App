package com.example.NotesNest.databases;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
                NoteFTSEntity.class,
                CategoryEntity.class,
                ReminderEntity.class
        },
        version = 3,
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
                            .addMigrations(MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add the new columns for gradient colors
            database.execSQL("ALTER TABLE reminders ADD COLUMN gradientStartColor INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE reminders ADD COLUMN gradientEndColor INTEGER NOT NULL DEFAULT 0");
        }
    };


    public abstract NoteDao noteDao();

    public abstract CategoryDao categoryDao();

    public abstract ReminderDao reminderDao();
}
