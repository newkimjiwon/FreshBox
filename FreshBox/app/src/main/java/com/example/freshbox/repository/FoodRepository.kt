// File: app/src/main/java/com/example/freshbox/repository/FoodRepository.kt
package com.example.freshbox.repository

import androidx.lifecycle.LiveData // LiveData 사용을 위해 import
import com.example.freshbox.data.Category // Category 데이터 Entity import
import com.example.freshbox.data.CategoryDao // Category 데이터 접근 객체(DAO) import
import com.example.freshbox.data.FoodDao // FoodItem 데이터 접근 객체(DAO) import
import com.example.freshbox.data.FoodItem // FoodItem 데이터 Entity import
import java.util.Calendar // 날짜 계산을 위해 Calendar 클래스 import

/**
 * 데이터 소스(FoodDao, CategoryDao)와 ViewModel 사이의 중개자 역할을 하는 Repository 클래스입니다.
 * 데이터 관련 로직을 캡슐화하고, ViewModel에 필요한 데이터를 제공하거나 데이터 조작을 위임받아 처리합니다.
 *
 * @property foodDao FoodItem Entity에 접근하기 위한 DAO.
 * @property categoryDao Category Entity에 접근하기 위한 DAO.
 */
class FoodRepository(private val foodDao: FoodDao, private val categoryDao: CategoryDao) {

    // --- CalendarFragment를 위한 메서드 ---
    /**
     * 특정 날짜 범위(타임스탬프 기준)에 유통기한이 만료되는 식품 목록을 비동기적으로 가져옵니다.
     * @param startTimestamp 시작 타임스탬프 (해당 날짜의 시작 시간).
     * @param endTimestamp 종료 타임스탬프 (해당 날짜의 종료 시간).
     * @return 해당 범위 내에 만료되는 FoodItem 리스트.
     */
    suspend fun getFoodItemsExpiringBetween(startTimestamp: Long, endTimestamp: Long): List<FoodItem> {
        return foodDao.getFoodItemsExpiringBetween(startTimestamp, endTimestamp)
    }

    // --- 기존 FoodItem 관련 메서드들 ---
    /**
     * 모든 FoodItem을 유통기한 오름차순으로 정렬하여 LiveData 형태로 가져옵니다.
     * LiveData를 사용하므로, 데이터 변경 시 자동으로 UI가 업데이트될 수 있습니다.
     */
    fun getAllFoodItemsSortedByExpiry(): LiveData<List<FoodItem>> = foodDao.getAllFoodItemsSortedByExpiry()

    /**
     * 현재 날짜를 기준으로 아직 유통기한이 지나지 않은 "활성" FoodItem 목록을 LiveData 형태로 가져옵니다.
     * (소비기한 임박 식품 + 아직 기한이 많이 남은 식품 모두 포함)
     */
    fun getActiveFoodItems(): LiveData<List<FoodItem>> {
        // 오늘 날짜의 시작 시간(00:00:00.000)을 계산합니다.
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return foodDao.getActiveFoodItems(today) // FoodDao를 통해 활성 식품 목록 조회
    }

    /**
     * 현재 날짜를 기준으로 유통기한이 이미 지난 "만료" FoodItem 목록을 LiveData 형태로 가져옵니다.
     */
    fun getExpiredFoodItems(): LiveData<List<FoodItem>> {
        // 오늘 날짜의 시작 시간(00:00:00.000)을 계산합니다.
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return foodDao.getExpiredFoodItems(today) // FoodDao를 통해 만료 식품 목록 조회
    }

    /**
     * 특정 ID를 가진 FoodItem을 LiveData 형태로 가져옵니다.
     * @param id 조회할 FoodItem의 ID.
     * @return 해당 ID의 FoodItem을 담은 LiveData (없으면 null 포함 LiveData).
     */
    fun getFoodItemById(id: Int): LiveData<FoodItem?> = foodDao.getFoodItemById(id)

    /**
     * 새로운 FoodItem을 데이터베이스에 비동기적으로 삽입합니다.
     * @param foodItem 삽입할 FoodItem 객체.
     */
    suspend fun insertFoodItem(foodItem: FoodItem) {
        foodDao.insertFoodItem(foodItem)
    }

    /**
     * 기존 FoodItem의 정보를 데이터베이스에서 비동기적으로 업데이트합니다.
     * @param foodItem 업데이트할 FoodItem 객체 (ID 포함).
     */
    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodDao.updateFoodItem(foodItem)
    }

    /**
     * 특정 FoodItem을 데이터베이스에서 비동기적으로 삭제합니다.
     * @param foodItem 삭제할 FoodItem 객체.
     */
    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodDao.deleteFoodItem(foodItem)
    }

    // --- Category 관련 메서드들 ---
    /**
     * 모든 Category를 이름 오름차순으로 정렬하여 LiveData 형태로 가져옵니다.
     */
    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategories()

    /**
     * 새로운 Category를 데이터베이스에 비동기적으로 삽입합니다.
     * @param category 삽입할 Category 객체.
     * @return 삽입된 행의 ID (성공 시 0보다 큰 값, 실패 또는 무시된 경우 다를 수 있음 - OnConflictStrategy에 따라).
     */
    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    /**
     * 여러 Category 객체들을 데이터베이스에 비동기적으로 한 번에 삽입합니다.
     * @param categories 삽입할 Category 리스트.
     */
    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
    }

    /**
     * 기존 Category의 정보를 데이터베이스에서 비동기적으로 업데이트합니다.
     * @param category 업데이트할 Category 객체 (ID 포함).
     */
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    /**
     * 특정 Category를 데이터베이스에서 비동기적으로 삭제합니다.
     * @param category 삭제할 Category 객체.
     */
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    /**
     * 특정 이름을 가진 Category를 데이터베이스에서 비동기적으로 조회합니다 (중복 방지용).
     * @param name 조회할 카테고리 이름.
     * @return 해당 이름의 Category 객체 (없으면 null).
     */
    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun deleteCategoryAndUncategorizeItems(category: Category) {
        // 1. 해당 카테고리 ID를 사용하는 모든 FoodItem의 categoryId를 NULL로 설정합니다.
        foodDao.clearCategoryIdForItems(category.id)
        // 2. FoodItem들의 업데이트가 완료된 후, Category를 삭제합니다.
        categoryDao.deleteCategory(category)
    }

    /**
     * 특정 ID를 가진 Category를 LiveData 형태로 가져옵니다.
     * @param id 조회할 Category의 ID.
     */
    fun getCategoryById(id: Long): LiveData<Category?> {
        return categoryDao.getCategoryById(id)
    }

    // --- FoodItem 검색 관련 추가된 메서드들 (FoodDao에 정의된 쿼리 기반) ---
    /**
     * 특정 카테고리 ID에 해당하는 FoodItem 목록을 LiveData 형태로 가져옵니다.
     * @param categoryId 조회할 카테고리의 ID.
     */
    fun getFoodItemsByCategoryId(categoryId: Long): LiveData<List<FoodItem>> {
        return foodDao.getFoodItemsByCategoryId(categoryId)
    }

    /**
     * 특정 태그를 포함하는 FoodItem 목록을 LiveData 형태로 가져옵니다.
     * (FoodDao에서는 tags 문자열 컬럼에 대해 LIKE 검색 수행)
     * @param tag 검색할 태그 문자열.
     */
    fun getFoodItemsWithTag(tag: String): LiveData<List<FoodItem>> {
        return foodDao.getFoodItemsWithTag(tag)
    }

    /**
     * 카테고리가 지정되지 않은(categoryId가 null인) FoodItem 목록을 LiveData 형태로 가져옵니다.
     */
    fun getUncategorizedFoodItems(): LiveData<List<FoodItem>> {
        return foodDao.getUncategorizedFoodItems()
    }

    // --- WorkManager (유통기한 알림) 관련 메서드 ---
    /**
     * 오늘 유통기한이 만료되는 식품 목록을 비동기적으로 가져옵니다.
     * ExpiryCheckWorker에서 사용됩니다.
     * @param startOfDayMillis 오늘 날짜의 시작 시간 타임스탬프.
     * @param endOfDayMillis 오늘 날짜의 종료 시간 타임스탬프.
     * @return 오늘 만료되는 FoodItem 리스트.
     */
    suspend fun getItemsExpiringToday(startOfDayMillis: Long, endOfDayMillis: Long): List<FoodItem> {
        return foodDao.getItemsExpiringToday(startOfDayMillis, endOfDayMillis)
    }
}