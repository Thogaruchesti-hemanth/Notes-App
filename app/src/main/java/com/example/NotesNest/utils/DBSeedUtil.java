package com.example.NotesNest.utils;

import android.content.Context;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class DBSeedUtil {

    private static Context context;

    public static void seedDefaultCategories(Context context) {
        DBSeedUtil.context = context;
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        if (pref.isCategorySeedDone()) return;   // ✅ Already seeded → skip

        AppDatabase db = AppDatabase.getInstance(context);

        Executors.newSingleThreadExecutor().execute(() -> {

            List<CategoryEntity> defaultCategories = Arrays.asList(
                    new CategoryEntity("All", null, null),
                    new CategoryEntity("Work", "#FFB300", "ic_work"),
                    new CategoryEntity("Professional", "#42A5F5", "ic_professional"),
                    new CategoryEntity("Ideas", "#66BB6A", "ic_ideas")
            );

            db.categoryDao().insertAll(defaultCategories);

            pref.setCategorySeedDone(true);  // ✅ Save flag
        });
    }
}
