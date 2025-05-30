// File: app/src/main/java/com/example/freshbox/data/FoodDao.kt
package com.example.freshbox.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem)

    @Update
    suspend fun updateFoodItem(foodItem: FoodItem)

    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItem)

    @Query("SELECT * FROM food_items WHERE id = :id")
    fun getFoodItemById(id: Int): LiveData<FoodItem?>

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllFoodItemsSortedByExpiry(): LiveData<List<FoodItem>>

    // 소비기한이 지나지 않은 식품만 가져오기 (소비기한 임박 + 아직 남은 식품)
    @Query("SELECT * FROM food_items WHERE expiryDate >= :todayTimestamp ORDER BY expiryDate ASC")
    fun getActiveFoodItems(todayTimestamp: Long): LiveData<List<FoodItem>>

    // 소비기한이 지난 식품만 가져오기
    @Query("SELECT * FROM food_items WHERE expiryDate < :todayTimestamp ORDER BY expiryDate DESC")
    fun getExpiredFoodItems(todayTimestamp: Long): LiveData<List<FoodItem>>

    // --- 사용자 정의 카테고리 및 태그 관련 쿼리 ---
    // (이전에 추가했던 내용들이 있다면 여기에 포함되어야 합니다)

    // 특정 카테고리 ID에 해당하는 식품 목록 가져오기
    @Query("SELECT * FROM food_items WHERE categoryId = :categoryId ORDER BY expiryDate ASC")
    fun getFoodItemsByCategoryId(categoryId: Long): LiveData<List<FoodItem>>

    // 특정 태그를 포함하는 식품 목록 가져오기
    @Query("SELECT * FROM food_items WHERE tags LIKE '%' || :tag || '%' ORDER BY expiryDate ASC")
    fun getFoodItemsWithTag(tag: String): LiveData<List<FoodItem>>

    // 카테고리가 지정되지 않은 식품만 가져오는 쿼리
    @Query("SELECT * FROM food_items WHERE categoryId IS NULL ORDER BY expiryDate ASC")
    fun getUncategorizedFoodItems(): LiveData<List<FoodItem>>

    // --- CalendarFragment를 위한 새 쿼리 추가 ---
    // 특정 날짜 범위에 만료되는 식품 목록 가져오기
    @Query("SELECT * FROM food_items WHERE expiryDate >= :startTimestamp AND expiryDate <= :endTimestamp ORDER BY expiryDate ASC")
    suspend fun getFoodItemsExpiringBetween(startTimestamp: Long, endTimestamp: Long): List<FoodItem>
}