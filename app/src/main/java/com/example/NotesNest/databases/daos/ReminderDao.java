package com.example.NotesNest.databases.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.NotesNest.databases.entities.ReminderEntity;

import java.util.List;

@Dao
public interface ReminderDao {

    // Basic CRUD operations
    @Insert
    long insertReminder(ReminderEntity reminder);

    @Update
    void updateReminder(ReminderEntity reminder);

    @Delete
    void deleteReminder(ReminderEntity reminder);

    // Get single reminder
    @Query("SELECT * FROM reminders WHERE id = :id")
    ReminderEntity getById(int id);

    // Delete by ID
    @Query("DELETE FROM reminders WHERE id = :id")
    void deleteById(int id);

    // GET ALL REMINDERS - Different sorting options
    @Query("SELECT * FROM reminders ORDER BY notification ASC")
    List<ReminderEntity> getAllReminders();

    @Query("SELECT * FROM reminders ORDER BY title ASC")
    List<ReminderEntity> getAllRemindersOrderByTitle();

    @Query("SELECT * FROM reminders ORDER BY type ASC, notification ASC")
    List<ReminderEntity> getAllRemindersOrderByType();

    // GET BY TYPE - Different variations
    @Query("SELECT * FROM reminders WHERE type = :type ORDER BY notification ASC")
    List<ReminderEntity> getRemindersByType(String type);

    @Query("SELECT * FROM reminders WHERE type = :type ORDER BY title ASC")
    List<ReminderEntity> getRemindersByTypeOrderByTitle(String type);

    @Query("SELECT * FROM reminders WHERE type IN (:types) ORDER BY notification ASC")
    List<ReminderEntity> getRemindersByTypes(List<String> types);

    // Additional useful methods
    @Query("SELECT * FROM reminders WHERE repeated = 1 ORDER BY notification ASC")
    List<ReminderEntity> getRepeatedReminders();

    @Query("SELECT COUNT(*) FROM reminders WHERE type = :type")
    int getCountByType(String type);

    @Query("SELECT COUNT(*) FROM reminders")
    int getTotalCount();

    @Query("SELECT * FROM reminders WHERE notification BETWEEN :startDate AND :endDate ORDER BY notification ASC")
    List<ReminderEntity> getRemindersByDateRange(long startDate, long endDate);
}