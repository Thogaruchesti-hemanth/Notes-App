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
                "message TEXT, " +
                "date TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS reminder (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "message TEXT, " +
                "reminder_time TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS todo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task TEXT, " +
                "due_date TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS wishes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "wish TEXT, " +
                "date TEXT);");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }
}
