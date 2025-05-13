package com.example.freshbox.repository

import androidx.lifecycle.LiveData
import com.example.freshbox.data.FoodDao
import com.example.freshbox.data.FoodItem
import java.util.Calendar

class FoodRepository(private val foodDao: FoodDao) {

    fun getAllFoodItemsSortedByExpiry(): LiveData<List<FoodItem>> = foodDao.getAllFoodItemsSortedByExpiry()

    fun getActiveFoodItems(): LiveData<List<FoodItem>> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return foodDao.getActiveFoodItems(today)
    }

    fun getExpiredFoodItems(): LiveData<List<FoodItem>> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return foodDao.getExpiredFoodItems(today)
    }


    fun getFoodItemById(id: Int): LiveData<FoodItem?> = foodDao.getFoodItemById(id)

    suspend fun insertFoodItem(foodItem: FoodItem) {
        foodDao.insertFoodItem(foodItem)
    }

    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodDao.updateFoodItem(foodItem)
    }

    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodDao.deleteFoodItem(foodItem)
    }

    suspend fun getFoodItemByBarcode(barcode: String): FoodItem? {
        return foodDao.getFoodItemByBarcode(barcode)
    }
}