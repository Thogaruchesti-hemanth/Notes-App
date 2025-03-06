package com.example.notes.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notesDatabase.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "datetime TEXT, " +
                "background_color TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS important (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "dateTime TEXT, " +
                "background_color TEXT);");


        db.execSQL("CREATE TABLE IF NOT EXISTS reminder (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "reminder_time TEXT, " +
                "background_color TEXT, " +
                "remind_before INTEGER);");


        db.execSQL("CREATE TABLE IF NOT EXISTS todo ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task TEXT NOT NULL, " +
                "description TEXT, " +
                "due_date TEXT, " +
                "priority INTEGER CHECK(priority IN (1, 2, 3)), " +
                "is_completed INTEGER DEFAULT 0 CHECK(is_completed IN (0, 1)), " +
                "progress INTEGER DEFAULT 0 CHECK(progress BETWEEN 0 AND 100)," +
                "background_color TEXT" +
                ");");


        db.execSQL("CREATE TABLE IF NOT EXISTS wishes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "wish TEXT NOT NULL, " +
                "description TEXT DEFAULT '', " +
                "date TEXT," +
                "is_fulfilled INTEGER NOT NULL DEFAULT 0, " +
                "background_color TEXT," +
                "priority INTEGER CHECK(priority BETWEEN 1 AND 5) DEFAULT 3);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }
}
