package com.example.NotesNest.databases.daos;

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

    // 🔹 Get all categories sorted alphabetically
    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> getAllCategories();

    // 🔹 Get category by ID
    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    CategoryEntity getCategoryById(int categoryId);

    // 🔹 Check if category exists by name (useful for avoiding duplicates)
    @Query("SELECT COUNT(*) FROM categories WHERE name = :categoryName")
    int countCategoryByName(String categoryName);

    // 🔹 Delete all categories (optional utility)
    @Query("DELETE FROM categories")
    void deleteAll();

    // 🔹 Get category by name
    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    CategoryEntity getCategoryByName(String categoryName);

    // 🔹 Delete by name (used in your fragment)
    @Query("DELETE FROM categories WHERE name = :categoryName")
    void deleteByName(String categoryName);
}
