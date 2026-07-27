package com.example.NotesNest.utils;

import android.content.Context;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DBSeedUtil {

    private DBSeedUtil() {
        // Prevent instantiation
    }

    public static void seedDefaultCategories(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;

        Context appContext = context.getApplicationContext();
        AppPreferences pref = AppPreferences.getInstance();
        if (pref.isCategorySeedDoneForUser(userId)) return;

        AppDatabase db = AppDatabase.getInstance(appContext);

        // Use CompletableFuture for a one-off background task
        // This avoids manual ExecutorService management and related warnings
        CompletableFuture.runAsync(() -> {
            List<CategoryEntity> defaultCategories = Arrays.asList(
                    new CategoryEntity("All", 1, userId),
                    new CategoryEntity("Work", 2, userId),
                    new CategoryEntity("Professional", 3, userId),
                    new CategoryEntity("Ideas", 4, userId)
            );

            db.categoryDao().insertAll(defaultCategories);
            pref.setCategorySeedDoneForUser(userId, true);
        });
    }
}
