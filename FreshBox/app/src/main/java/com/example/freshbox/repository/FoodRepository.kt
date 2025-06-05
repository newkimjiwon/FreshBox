// File: app/src/main/java/com/example/freshbox/repository/FoodRepository.kt
package com.example.freshbox.repository

import androidx.lifecycle.LiveData
import com.example.freshbox.data.Category // Category Entity import
import com.example.freshbox.data.CategoryDao // CategoryDao import
import com.example.freshbox.data.FoodDao
import com.example.freshbox.data.FoodItem
import java.util.Calendar

// 생성자에 CategoryDao 추가
class FoodRepository(private val foodDao: FoodDao, private val categoryDao: CategoryDao) {

    // 특정 날짜 범위에 만료되는 식품 목록 가져오기 (캘린더용)
    suspend fun getFoodItemsExpiringBetween(startTimestamp: Long, endTimestamp: Long): List<FoodItem> {
        return foodDao.getFoodItemsExpiringBetween(startTimestamp, endTimestamp)
    }

    // --- 기존 FoodItem 관련 메서드들 ---
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

    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category): Long { // 반환 타입 Long으로 명시 (선택사항)
        return categoryDao.insertCategory(category)
    }

    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    fun getCategoryById(id: Long): LiveData<Category?> {
        return categoryDao.getCategoryById(id)
    }

    // --- FoodItem 검색 관련 새 메서드들 추가 (FoodDao에 정의된 쿼리 기반) ---
    fun getFoodItemsByCategoryId(categoryId: Long): LiveData<List<FoodItem>> {
        return foodDao.getFoodItemsByCategoryId(categoryId)
    }

    fun getFoodItemsWithTag(tag: String): LiveData<List<FoodItem>> {
        return foodDao.getFoodItemsWithTag(tag)
    }

    fun getUncategorizedFoodItems(): LiveData<List<FoodItem>> {
        return foodDao.getUncategorizedFoodItems()
    }

    // 오늘 유통기한이 만료되는 식품 목록 가져오기
    suspend fun getItemsExpiringToday(startOfDayMillis: Long, endOfDayMillis: Long): List<FoodItem> {
        return foodDao.getItemsExpiringToday(startOfDayMillis, endOfDayMillis)
    }
}