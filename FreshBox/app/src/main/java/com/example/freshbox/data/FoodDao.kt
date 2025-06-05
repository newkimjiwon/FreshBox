// File: app/src/main/java/com/example/freshbox/data/FoodDao.kt
package com.example.freshbox.data // 이 파일이 속한 패키지를 정의합니다.

import androidx.lifecycle.LiveData // LiveData를 사용하여 데이터 변경을 UI에 자동으로 반영할 수 있도록 import 합니다.
import androidx.room.* // Room 라이브러리의 주요 어노테이션들(Dao, Insert, Query 등)을 사용하기 위해 import 합니다.
import java.util.Date // FoodItem Entity에서 Date 타입을 사용하므로 import (여기서는 직접 사용 X)

// @Dao 어노테이션은 이 인터페이스가 Room 데이터베이스의 DAO(Data Access Object)임을 나타냅니다.
// DAO는 데이터베이스에 접근하는 메서드들을 추상화하여 제공합니다.
@Dao
interface FoodDao {

    // 새로운 FoodItem 객체를 'food_items' 테이블에 삽입합니다.
    // onConflict = OnConflictStrategy.REPLACE: 만약 삽입하려는 FoodItem과 동일한 기본 키(id)를 가진
    //                                       데이터가 이미 테이블에 있다면, 기존 데이터를 새 데이터로 교체합니다.
    // suspend fun: 이 함수는 코루틴 내에서 비동기적으로 실행되어야 함을 나타냅니다 (DB 작업은 UI 스레드에서 직접 수행 X).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem)

    // 기존 FoodItem 객체의 정보를 'food_items' 테이블에서 업데이트합니다.
    // 전달된 FoodItem 객체의 기본 키(id)를 기준으로 해당 행을 찾아 업데이트합니다.
    @Update
    suspend fun updateFoodItem(foodItem: FoodItem)

    // 특정 FoodItem 객체를 'food_items' 테이블에서 삭제합니다.
    // 전달된 FoodItem 객체의 기본 키(id)를 기준으로 해당 행을 찾아 삭제합니다.
    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItem)

    // 특정 ID를 가진 FoodItem 정보를 'food_items' 테이블에서 가져옵니다.
    // 반환 타입 LiveData<FoodItem?>: 해당 ID의 식품이 있으면 FoodItem 객체를, 없으면 null을 포함한 LiveData를 발행합니다.
    //                              데이터베이스의 해당 아이템이 변경되면 LiveData가 자동으로 새 값을 발행합니다.
    // @param id 조회할 FoodItem의 ID.
    @Query("SELECT * FROM food_items WHERE id = :id")
    fun getFoodItemById(id: Int): LiveData<FoodItem?>

    // 'food_items' 테이블의 모든 식품 정보를 소비기한(expiryDate) 오름차순으로 정렬하여 가져옵니다.
    // 반환 타입 LiveData<List<FoodItem>>: 데이터베이스의 food_items 테이블 내용이 변경될 때마다
    //                                    자동으로 새로운 List<FoodItem>을 발행합니다.
    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllFoodItemsSortedByExpiry(): LiveData<List<FoodItem>>

    // 소비기한이 지나지 않은 식품만 가져오기 (소비기한 임박 + 아직 남은 식품)
    // 현재 날짜의 시작 타임스탬프보다 소비기한이 크거나 같은 식품들을 조회합니다.
    // @param todayTimestamp 오늘 날짜의 시작 시간(자정)을 나타내는 Long 타입 타임스탬프.
    @Query("SELECT * FROM food_items WHERE expiryDate >= :todayTimestamp ORDER BY expiryDate ASC")
    fun getActiveFoodItems(todayTimestamp: Long): LiveData<List<FoodItem>>

    // 소비기한이 지난 식품만 가져오기
    // 현재 날짜의 시작 타임스탬프보다 소비기한이 작은 식품들을 조회합니다. 소비기한이 늦은 순(최근에 만료된 순)으로 정렬합니다.
    // @param todayTimestamp 오늘 날짜의 시작 시간(자정)을 나타내는 Long 타입 타임스탬프.
    @Query("SELECT * FROM food_items WHERE expiryDate < :todayTimestamp ORDER BY expiryDate DESC")
    fun getExpiredFoodItems(todayTimestamp: Long): LiveData<List<FoodItem>>

    // --- 사용자 정의 카테고리 및 태그 관련 쿼리 ---
    // (이전에 이 부분에 다른 쿼리들이 추가되었을 수 있습니다.)

    // 특정 카테고리 ID에 해당하는 식품 목록 가져오기
    // FoodItem Entity에 categoryId 필드가 있어야 하며, :categoryId 파라미터로 전달된 값과 일치하는 아이템들을 조회합니다.
    // @param categoryId 조회할 카테고리의 ID.
    @Query("SELECT * FROM food_items WHERE categoryId = :categoryId ORDER BY expiryDate ASC")
    fun getFoodItemsByCategoryId(categoryId: Long): LiveData<List<FoodItem>>

    // 특정 태그를 포함하는 식품 목록 가져오기
    // FoodItem Entity의 tags 필드(List<String>이지만 TypeConverter를 통해 TEXT로 저장됨)에
    // :tag 파라미터로 전달된 문자열이 포함되어 있는지 LIKE 검색으로 조회합니다.
    // @param tag 검색할 태그 문자열.
    @Query("SELECT * FROM food_items WHERE tags LIKE '%' || :tag || '%' ORDER BY expiryDate ASC")
    fun getFoodItemsWithTag(tag: String): LiveData<List<FoodItem>>

    // 카테고리가 지정되지 않은(categoryId가 null인) 식품만 가져오는 쿼리
    @Query("SELECT * FROM food_items WHERE categoryId IS NULL ORDER BY expiryDate ASC")
    fun getUncategorizedFoodItems(): LiveData<List<FoodItem>>

    // --- CalendarFragment를 위한 새 쿼리 추가 ---
    // 특정 날짜 범위에 만료되는 식품 목록 가져오기
    // 소비기한(expiryDate)이 :startTimestamp 와 :endTimestamp 사이에 있는 식품들을 조회합니다.
    // suspend fun: 코루틴 내에서 비동기적으로 실행됩니다. (CalendarFragment에서 날짜 클릭 시 사용)
    // @param startTimestamp 조회 시작 시간 타임스탬프.
    // @param endTimestamp 조회 종료 시간 타임스탬프.
    @Query("SELECT * FROM food_items WHERE expiryDate >= :startTimestamp AND expiryDate <= :endTimestamp ORDER BY expiryDate ASC")
    suspend fun getFoodItemsExpiringBetween(startTimestamp: Long, endTimestamp: Long): List<FoodItem>

    // --- WorkManager (유통기한 알림)를 위한 쿼리 ---
    // 오늘 유통기한이 만료되는 식품 목록 가져오기 (특정 시간 범위 내)
    // ExpiryCheckWorker에서 사용됩니다.
    // @param startOfDayMillis 오늘 날짜의 시작 시간 타임스탬프.
    // @param endOfDayMillis 오늘 날짜의 종료 시간 타임스탬프.
    @Query("SELECT * FROM food_items WHERE expiryDate >= :startOfDayMillis AND expiryDate <= :endOfDayMillis")
    suspend fun getItemsExpiringToday(startOfDayMillis: Long, endOfDayMillis: Long): List<FoodItem>
}