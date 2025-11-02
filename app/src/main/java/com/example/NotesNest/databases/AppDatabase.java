package com.example.NotesNest.databases;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.example.NotesNest.databases.daos.CategoryDao;
import com.example.NotesNest.databases.daos.NoteDao;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteFTSEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;


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
                            ).addCallback(roomCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract NoteDao noteDao();    // 👇 This callback runs ONLY the first time database is created
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            Executors.newSingleThreadExecutor().execute(() -> {
                List<CategoryEntity> defaultCategories = Arrays.asList(
                        new CategoryEntity("Work", "#FFB300", "ic_work"),
                        new CategoryEntity("Professional", "#42A5F5", "ic_professional"),
                        new CategoryEntity("Ideas", "#66BB6A", "ic_ideas")
                );

                getInstance(AppDatabaseHolder.context)
                        .categoryDao()
                        .insertAll(defaultCategories);
            });
        }
    };

    public abstract CategoryDao categoryDao();


}