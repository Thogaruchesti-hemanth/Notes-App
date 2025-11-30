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
                    new CategoryEntity("All"),
                    new CategoryEntity("Work"),
                    new CategoryEntity("Professional"),
                    new CategoryEntity("Ideas")
            );

            db.categoryDao().insertAll(defaultCategories);

            pref.setCategorySeedDone(true);  // ✅ Save flag
        });
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        DBSeedUtil.context = context;
    }
}
