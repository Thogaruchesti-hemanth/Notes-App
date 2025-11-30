package com.example.NotesNest.databases.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.NotesNest.databases.entities.ReminderEntity;

import java.util.List;

@Dao
public interface ReminderDao {

    // ------------------- Basic CRUD -------------------
    @Insert
    long insertReminder(ReminderEntity reminder);

    @Update
    int updateReminder(ReminderEntity reminder);

    @Delete
    void deleteReminder(ReminderEntity reminder);

    @Query("DELETE FROM reminders WHERE id = :id AND userId = :userId")
    void deleteById(int id, int userId);

    @Query("SELECT * FROM reminders WHERE id = :id AND userId = :userId AND isDeleted = 0")
    ReminderEntity getById(int id, String userId);

    // ------------------- Get All Reminders -------------------
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY notificationTime ASC")
    List<ReminderEntity> getAllReminders(int userId);

    @Query("SELECT * FROM reminders WHERE userId = :userId")
    LiveData<List<ReminderEntity>> getAllRemindersLive(String userId);

    // ------------------- Date Range with LiveData -------------------
    @Query("SELECT * FROM reminders WHERE userId = :userId AND notificationTime BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY notificationTime ASC")
    LiveData<List<ReminderEntity>> getRemindersByDateRangeLive(int userId, long startDate, long endDate);

    // ------------------- Get Single Reminder with LiveData -------------------
    @Query("SELECT * FROM reminders WHERE id = :id AND userId = :userId AND isDeleted = 0")
    LiveData<ReminderEntity> getReminderByIdLive(int id, String userId);

}
