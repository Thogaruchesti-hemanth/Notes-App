package com.example.NotesNest.databases.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.repositories.CategoryRepository;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository categoryRepository;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = new CategoryRepository(application);
    }

    public LiveData<List<CategoryEntity>> getAllCategories(String userId) {
        return categoryRepository.getAllCategories(userId);
    }

    public LiveData<CategoryEntity> getCategoryById(int categoryId, String userId) {
        return categoryRepository.getCategoryById(categoryId, userId);
    }

    public void insertCategory(CategoryEntity category) {
        categoryRepository.insert(category);
    }

    public void deleteCategory(CategoryEntity category) {
        categoryRepository.delete(category);
    }

    public void deleteCategoryByName(String categoryName, String userId) {
        categoryRepository.deleteByName(categoryName, userId);
    }

    public void updateCategory(CategoryEntity category) {
        categoryRepository.update(category);
    }

    public void getCategoryName(int categoryId, String userId, CategoryRepository.CategoryNameCallback callback) {
        categoryRepository.getCategoryName(categoryId, userId, callback);
    }

}
