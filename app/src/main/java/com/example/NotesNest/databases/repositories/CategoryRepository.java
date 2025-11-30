package com.example.NotesNest.databases.repositories;

import android.app.Application;

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

    public LiveData<List<CategoryEntity>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<CategoryEntity> getCategoryById(int categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }

    public LiveData<CategoryEntity> getCategoryByName(String categoryName) {
        return categoryDao.getCategoryByName(categoryName);
    }

    // -------------------- WRITE --------------------

    public void insert(CategoryEntity category) {
        executorService.execute(() -> categoryDao.insert(category));
    }

    public void insertAll(List<CategoryEntity> categories) {
        executorService.execute(() -> categoryDao.insertAll(categories));
    }

    public void update(CategoryEntity category) {
        executorService.execute(() -> categoryDao.update(category));
    }

    public void delete(CategoryEntity category) {
        executorService.execute(() -> categoryDao.delete(category));
    }

    public void deleteById(int categoryId) {
        executorService.execute(() -> categoryDao.deleteCategoryById(categoryId));
    }

    public void deleteByName(String categoryName) {
        executorService.execute(() -> categoryDao.deleteByName(categoryName));
    }

    public void deleteAll() {
        executorService.execute(categoryDao::deleteAll);
    }

    // -------------------- UTILITIES --------------------

    public boolean isCategoryExists(String categoryName) {
        return categoryDao.countCategoryByName(categoryName) > 0;
    }
}
