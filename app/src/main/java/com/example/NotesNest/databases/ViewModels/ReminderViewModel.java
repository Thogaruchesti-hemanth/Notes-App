package com.example.NotesNest.databases.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databases.repositories.ReminderRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderViewModel extends AndroidViewModel {

    private final ReminderRepository reminderRepository;
    private final ExecutorService executorService;

    private final MutableLiveData<Long> insertResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        reminderRepository = new ReminderRepository(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    // -------------------- READ --------------------

    /**
     * Get all reminders for a specific user
     */
    public LiveData<List<ReminderEntity>> getAllReminders(String userId) {
        return reminderRepository.getAllReminders(userId);
    }

    /**
     * Get a specific reminder by ID and user ID
     */
    public LiveData<ReminderEntity> getReminderById(String id, String userId) {
        return reminderRepository.getReminderById(id, userId);
    }

    // -------------------- WRITE (with results) --------------------

    /**
     * Insert a new reminder and return the inserted ID via LiveData
     */
    public void insertReminder(ReminderEntity reminder) {
        executorService.execute(() -> {
            try {
                long rowId = reminderRepository.insertAndGetId(reminder);
                insertResult.postValue(rowId);
            } catch (Exception e) {
                insertResult.postValue(-1L);
            }
        });
    }

    /**
     * Update an existing reminder and return success status via LiveData
     */
    public void updateReminder(ReminderEntity reminder) {
        executorService.execute(() -> {
            try {
                int rowsAffected = reminderRepository.updateAndGetCount(reminder);
                updateResult.postValue(rowsAffected > 0);
            } catch (Exception e) {
                updateResult.postValue(false);
            }
        });
    }

    /**
     * Delete a reminder and return success status via LiveData
     */
    public void deleteReminder(ReminderEntity reminder) {
        executorService.execute(() -> {
            try {
                reminderRepository.delete(reminder);
                deleteResult.postValue(true);
            } catch (Exception e) {
                deleteResult.postValue(false);
            }
        });
    }

    // -------------------- GETTERS FOR LIVEDATA --------------------

    /**
     * Get the result of the last insert operation
     *
     * @return LiveData containing the inserted row ID (or -1 if failed)
     */
    public LiveData<Long> getInsertResult() {
        return insertResult;
    }

    /**
     * Get the result of the last update operation
     *
     * @return LiveData containing true if successful, false otherwise
     */
    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}