package com.example.notes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.ReminderScheduler;
import com.example.notes.models.Reminder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReminderDatabaseHandler {

    private final SQLiteDatabase db;
    private final Context context;

    public ReminderDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
        this.context = context;
    }

    public long insertReminder(String title, String message, String reminderTime, String backgroundColor, int remindBefore) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("reminder_time", reminderTime);
        values.put("background_color", backgroundColor);
        values.put("remind_before", remindBefore);

        long id = db.insert("reminder", null, values);

        if (id != -1) {
            scheduleReminder(id, message, reminderTime, remindBefore);
        }

        return id;
    }

    public Reminder getReminderById(int id) {
        Reminder reminder = null;
        Cursor cursor = null;

        try {
            cursor = db.query("reminder", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int reminderId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String reminderTime = cursor.getString(cursor.getColumnIndexOrThrow("reminder_time"));
                String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color"));
                int remindBefore = cursor.getInt(cursor.getColumnIndexOrThrow("remind_before"));

                reminder = new Reminder(reminderId, title, message, reminderTime, backgroundColor, remindBefore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reminder;
    }

    public int updateReminder(int id, String title, String message, String reminderTime, String backgroundColor, int remindBefore) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("reminder_time", reminderTime);
        values.put("background_color", backgroundColor);
        values.put("remind_before", remindBefore);

        int rowsUpdated = db.update("reminder", values, "id = ?", new String[]{String.valueOf(id)});

        if (rowsUpdated > 0) {
            scheduleReminder(id, message, reminderTime, remindBefore);
        }

        return rowsUpdated;
    }

    public int deleteReminder(int id) {
        int rowsDeleted = db.delete("reminder", "id = ?", new String[]{String.valueOf(id)});

        if (rowsDeleted > 0) {
            ReminderScheduler.cancelReminder(context, id);
        }

        return rowsDeleted;
    }

    public ArrayList<Reminder> getAll() {
        ArrayList<Reminder> reminderList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("reminder", null, null, null, null, null, "reminder_time ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int reminderId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String reminderTime = cursor.getString(cursor.getColumnIndexOrThrow("reminder_time"));
                    String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color"));
                    int remindBefore = cursor.getInt(cursor.getColumnIndexOrThrow("remind_before"));

                    reminderList.add(new Reminder(reminderId, title, message, reminderTime, backgroundColor, remindBefore));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reminderList;
    }

    private void scheduleReminder(long id, String message, String reminderTime, int remindBefore) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            Date reminderDate = inputFormat.parse(reminderTime);

            if (reminderDate != null) {
                long reminderTimeMillis = reminderDate.getTime();
                ReminderScheduler.scheduleReminder(context, id, message, reminderTimeMillis, remindBefore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
