package com.example.notes.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Reminder;

import java.util.ArrayList;

public class ReminderDatabaseHandler {

    private SQLiteDatabase db;

    public ReminderDatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertReminder(String title, String description, String dueDate) {
        ContentValues values = new ContentValues();
        values.put("message", title);
        values.put("reminder_time", dueDate);
        return db.insert("reminder", null, values);
    }

    public Reminder getReminderById(int id) {
        Reminder reminder = null;
        Cursor cursor = null;

        try {
            cursor = db.query("reminder", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int reminderIdColumnIndex = cursor.getColumnIndex("id");
                int titleColumnIndex = cursor.getColumnIndex("title");
                int descriptionColumnIndex = cursor.getColumnIndex("description");
                int dueDateColumnIndex = cursor.getColumnIndex("due_date");

                if (reminderIdColumnIndex >= 0 && titleColumnIndex >= 0 &&
                        descriptionColumnIndex >= 0 && dueDateColumnIndex >= 0) {

                    int reminderId = cursor.getInt(reminderIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
                    String description = cursor.getString(descriptionColumnIndex);
                    String dueDate = cursor.getString(dueDateColumnIndex);

                    reminder = new Reminder(reminderId, description, dueDate);
                } else {
                }
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


    public int updateReminder(int id, String title, String description, String dueDate) {
        ContentValues values = new ContentValues();
        values.put("message", title);
        values.put("date", dueDate);
        return db.update("reminder", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteReminder(int id) {
        return db.delete("reminder", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Reminder> getAll() {
        ArrayList<Reminder> reminderList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("reminder", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int reminderIdColumnIndex = cursor.getColumnIndex("id");
                    int titleColumnIndex = cursor.getColumnIndex("message");

                    int dueDateColumnIndex = cursor.getColumnIndex("reminder_time");

                    if (reminderIdColumnIndex >= 0 && titleColumnIndex >= 0 && dueDateColumnIndex >= 0) {

                        int reminderId = cursor.getInt(reminderIdColumnIndex);
                        String title = cursor.getString(titleColumnIndex);
                        String dueDate = cursor.getString(dueDateColumnIndex);

                        reminderList.add(new Reminder(reminderId, title, dueDate));
                    }
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
}

