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

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?
}