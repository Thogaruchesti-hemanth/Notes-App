package com.example.NotesNest.utils;

import android.content.Context;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class DBSeedUtil {

    private static Context context;

    public static void seedDefaultCategories(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;
        
        DBSeedUtil.context = context;
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        if (pref.isCategorySeedDoneForUser(userId)) return;   // ✅ Already seeded for this user → skip

        AppDatabase db = AppDatabase.getInstance(context);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<CategoryEntity> defaultCategories = Arrays.asList(
                    new CategoryEntity("All", 1, userId),
                    new CategoryEntity("Work", 2, userId),
                    new CategoryEntity("Professional", 3, userId),
                    new CategoryEntity("Ideas", 4, userId)
            );

            db.categoryDao().insertAll(defaultCategories);
            pref.setCategorySeedDoneForUser(userId, true);  // ✅ Save flag per user
        });
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        DBSeedUtil.context = context;
    }
}
