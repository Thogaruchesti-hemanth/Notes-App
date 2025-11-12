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
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Drop the reminders table
            database.execSQL("DROP TABLE IF EXISTS reminders");

            // Recreate the reminders table with the new schema
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`type` TEXT, " +
                            "`title` TEXT, " +
                            "`name` TEXT, " +
                            "`message` TEXT, " +
                            "`notification` INTEGER NOT NULL, " +
                            "`repeated` INTEGER NOT NULL, " +
                            "`repeatType` TEXT, " +
                            "`notifyType` TEXT, " +
                            "`gradientStartColor` INTEGER NOT NULL DEFAULT 0, " +
                            "`gradientEndColor` INTEGER NOT NULL DEFAULT 0" +
                            ")"
            );
        }
    };
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
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract NoteDao noteDao();

    public abstract CategoryDao categoryDao();

    public abstract ReminderDao reminderDao();
}
