package com.example.NotesNest.databases.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.ReminderDao;
import com.example.NotesNest.databases.entities.ReminderEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {

    private final ReminderDao reminderDao;
    private final ExecutorService executorService;

    public ReminderRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        reminderDao = db.reminderDao();
        executorService = Executors.newFixedThreadPool(4);
    }

// -------------------- READ --------------------
// ... existing read methods remain unchanged ...

// -------------------- WRITE --------------------

    public void insert(ReminderEntity reminder) {
        executorService.execute(() -> reminderDao.insertReminder(reminder));
    }

    public void update(ReminderEntity reminder) {
        executorService.execute(() -> reminderDao.updateReminder(reminder));
    }

    public void delete(ReminderEntity reminder) {
        executorService.execute(() -> reminderDao.deleteReminder(reminder));
    }

    // -------------------- WRITE WITH CALLBACK --------------------

    public void insert(ReminderEntity reminder, OnInsertCallback callback) {
        executorService.execute(() -> {
            long id = reminderDao.insertReminder(reminder);
            if (callback != null) callback.onInsert(id);
        });
    }

    public void update(ReminderEntity reminder, OnUpdateCallback callback) {
        executorService.execute(() -> {
            int rows = reminderDao.updateReminder(reminder);
            if (callback != null) callback.onUpdate(rows > 0);
        });
    }

    public LiveData<List<ReminderEntity>> getAllReminders(String userId) {
        return reminderDao.getAllRemindersLive(userId);
    }

    public void getAllReminders(String userID, Callback callback) {
        executorService.execute(() -> {
            List<ReminderEntity> list = reminderDao.getAllReminders(userID);
            if (callback != null) callback.onResult(list);
        });
    }

    // In ReminderRepository class - add this method:

    public LiveData<ReminderEntity> getReminderById(int id, String userId) {
        return reminderDao.getReminderByIdLive(id, userId);
    }

    public ReminderEntity getReminderById(String userId, int id){
        return reminderDao.getById(id,userId);
    }

    /**
     * Insert a reminder and return the inserted row ID
     * This is a blocking operation and should be called from a background thread
     */
    public long insertAndGetId(ReminderEntity reminder) {
        return reminderDao.insertReminder(reminder);
    }

    /**
     * Update a reminder and return the number of rows affected
     * This is a blocking operation and should be called from a background thread
     */
    public int updateAndGetCount(ReminderEntity reminder) {
        return reminderDao.updateReminder(reminder);
    }

// -------------------- CALLBACK INTERFACES --------------------

    public interface OnInsertCallback {
        void onInsert(long id);
    }

    public interface OnUpdateCallback {
        void onUpdate(boolean success);
    }

    public interface Callback {
        void onResult(List<ReminderEntity> reminders);
    }
}
