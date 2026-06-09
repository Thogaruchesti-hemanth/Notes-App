package com.example.NotesNest.databases.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.CategoryDao;
import com.example.NotesNest.databases.entities.CategoryEntity;

import com.example.NotesNest.utils.AppExecutors;

import java.util.List;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final AppExecutors executors;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        executors = AppExecutors.getInstance();
    }

    // -------------------- READ --------------------

    public LiveData<List<CategoryEntity>> getAllCategories(String userId) {
        return categoryDao.getAllCategories(userId);
    }

    public LiveData<CategoryEntity> getCategoryById(int categoryId, String userId) {
        return categoryDao.getCategoryById(categoryId, userId);
    }

    public LiveData<CategoryEntity> getCategoryByName(String categoryName, String userId) {
        return categoryDao.getCategoryByName(categoryName, userId);
    }

    // -------------------- WRITE --------------------

    public void insert(CategoryEntity category) {
        executors.diskIO().execute(() -> categoryDao.insert(category));
    }

    public void insertAll(List<CategoryEntity> categories) {
        executors.diskIO().execute(() -> categoryDao.insertAll(categories));
    }

    public void update(CategoryEntity category) {
        executors.diskIO().execute(() -> categoryDao.update(category));
    }

    public void delete(CategoryEntity category) {
        executors.diskIO().execute(() -> categoryDao.delete(category));
    }

    public void deleteById(int categoryId, String userId) {
        executors.diskIO().execute(() -> categoryDao.deleteCategoryById(categoryId, userId));
    }

    public void deleteByName(String categoryName, String userId) {
        executors.diskIO().execute(() -> categoryDao.deleteByName(categoryName, userId));
    }

    public void deleteAll(String userId) {
        executors.diskIO().execute(() -> categoryDao.deleteAll(userId));
    }

    // -------------------- UTILITIES --------------------

    public boolean isCategoryExists(String categoryName, String userId) {
        return categoryDao.countCategoryByName(categoryName, userId) > 0;
    }

    public void getCategoryName(int categoryId, String userId, CategoryNameCallback callback) {
        executors.diskIO().execute(() -> {
            String name = categoryDao.getCategoryName(categoryId, userId);
            if (name == null) name = "Uncategorized";

            String finalName = name;
            executors.mainThread().execute(() -> {
                callback.onResult(finalName);
            });
        });
    }

    public interface CategoryNameCallback {
        void onResult(String categoryName);
    }

}
