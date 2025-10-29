package com.example.NotesNest.databases.daos;

import androidx.room.*;

import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    // 🔹 Insert a new category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CategoryEntity category);

    // 🔹 Update an existing category
    @Update
    void update(CategoryEntity category);

    // 🔹 Delete a category
    @Delete
    void delete(CategoryEntity category);

    // 🔹 Get all categories, sorted alphabetically
    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> getAllCategories();

    // 🔹 Get category by ID
    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    CategoryEntity getCategoryById(int categoryId);

    // 🔹 Delete category by ID
    @Query("DELETE FROM categories WHERE id = :categoryId")
    void deleteCategoryById(int categoryId);

    // 🔹 Check if category exists by name (optional utility)
    @Query("SELECT COUNT(*) FROM categories WHERE name = :categoryName")
    int countCategoryByName(String categoryName);
}