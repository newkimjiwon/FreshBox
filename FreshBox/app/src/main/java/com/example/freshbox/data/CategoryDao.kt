// File: app/src/main/java/com/example/freshbox/data/CategoryDao.kt
package com.example.freshbox.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // 이미 있는 이름의 카테고리면 무시
    suspend fun insertCategory(category: Category): Long // 삽입된 행의 ID 반환 (선택 사항)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>) // 여러 카테고리 한 번에 삽입

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): LiveData<Category?> // ID로 특정 카테고리 조회

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category? // 이름으로 특정 카테고리 조회 (중복 방지용)
}