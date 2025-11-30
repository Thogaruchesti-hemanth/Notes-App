package com.example.NotesNest.databases.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databases.repositories.ReminderRepository;

import java.util.List;

public class ReminderViewModel extends AndroidViewModel {

    private final ReminderRepository reminderRepository;

    // ADD THESE TWO
    private final MutableLiveData<Long> insertResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        reminderRepository = new ReminderRepository(application);
    }

    // -------------------- READ --------------------
    // (unchanged)
    public LiveData<List<ReminderEntity>> getAllReminders(String userId) {
        return reminderRepository.getAllReminders(userId);
    }

    // -------------------- WRITE --------------------

    // Normal insert (no result)
    public void insertReminder(ReminderEntity reminder) {
        reminderRepository.insert(reminder);
    }

    // Normal update
    public void updateReminder(ReminderEntity reminder) {
        reminderRepository.update(reminder);
    }

    public void deleteReminder(ReminderEntity reminder) {
        reminderRepository.delete(reminder);
    }

    // -------------------- GETTERS FOR LIVEDATA --------------------

    public LiveData<Long> getInsertResult() {
        return insertResult;
    }

    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    // In ReminderViewModel class - add this method:

    public LiveData<ReminderEntity> getReminderById(int id, String userId) {
        return reminderRepository.getReminderById(id, userId);
    }
}
