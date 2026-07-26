package com.example.NotesNest.databases.repositories;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.CategoryDao;
import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final ExecutorService executorService;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    // -------------------- READ --------------------

    public LiveData<List<CategoryEntity>> getAllCategories(String userId) {
        return categoryDao.getAllCategories(userId);
    }

    public LiveData<CategoryEntity> getCategoryById(String categoryId, String userId) {
        return categoryDao.getCategoryById(categoryId, userId);
    }

    // -------------------- WRITE --------------------

    public void insert(CategoryEntity category) {
        executorService.execute(() -> categoryDao.insert(category));
    }

    public void update(CategoryEntity category) {
        executorService.execute(() -> categoryDao.update(category));
    }

    public void delete(CategoryEntity category) {
        executorService.execute(() -> categoryDao.delete(category));
    }

    public void deleteByName(String categoryName, String userId) {
        executorService.execute(() -> categoryDao.deleteByName(categoryName, userId));
    }

    public void getCategoryName(String categoryId, String userId, CategoryNameCallback callback) {
        executorService.execute(() -> {
            String name = categoryDao.getCategoryName(categoryId, userId);
            if (name == null) name = "Uncategorized";

            String finalName = name;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalName));
        });
    }

    public interface CategoryNameCallback {
        void onResult(String categoryName);
    }

}
