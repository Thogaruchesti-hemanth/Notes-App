package com.example.NotesNest.databases.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    // 🔹 Insert a single category (ignore duplicates by name)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    // 🔹 Insert multiple categories (useful for pre-populating)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<CategoryEntity> categories);

    // 🔹 Update an existing category
    @Update
    void update(CategoryEntity category);

    // 🔹 Delete a category
    @Delete
    void delete(CategoryEntity category);

    // 🔹 Delete category by ID
    @Query("DELETE FROM categories WHERE id = :categoryId")
    void deleteCategoryById(int categoryId);

    // 🔹 Get all categories as LiveData (reactive, offline-friendly)
    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    // 🔹 Get category by ID
    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    LiveData<CategoryEntity> getCategoryById(int categoryId);

    // 🔹 Check if category exists by name
    @Query("SELECT COUNT(*) FROM categories WHERE name = :categoryName")
    int countCategoryByName(String categoryName);

    // 🔹 Get category by name
    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    LiveData<CategoryEntity> getCategoryByName(String categoryName);

    // 🔹 Delete by name
    @Query("DELETE FROM categories WHERE name = :categoryName")
    void deleteByName(String categoryName);

    // 🔹 Delete all categories
    @Query("DELETE FROM categories")
    void deleteAll();

    @Query("SELECT name FROM categories WHERE id = :categoryId LIMIT 1")
    String getCategoryName(int categoryId);
}
