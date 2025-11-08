package com.example.NotesNest.databases.repository;

import android.content.Context;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.ReminderDao;
import com.example.NotesNest.databases.entities.ReminderEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {

    private final ReminderDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ReminderRepository(Context context) {
        dao = AppDatabase.getInstance(context).reminderDao();
    }

    public void insert(ReminderEntity entity) {
        executor.execute(() -> dao.insertReminder(entity));
    }

    public void update(ReminderEntity entity) {
        executor.execute(() -> dao.updateReminder(entity));
    }

    public void delete(ReminderEntity entity) {
        executor.execute(() -> dao.deleteReminder(entity));
    }

    public List<ReminderEntity> getAll() {
        return dao.getAllReminders();
    }

    public List<ReminderEntity> getByType(String type) {
        return dao.getRemindersByType(type);
    }
}
