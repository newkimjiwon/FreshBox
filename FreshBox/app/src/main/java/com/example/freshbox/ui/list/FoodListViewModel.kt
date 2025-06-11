// File: app/src/main/java/com/example/freshbox/ui/list/FoodListViewModel.kt
package com.example.freshbox.ui.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.map // LiveData 변환(map)을 사용하기 위해 import를 추가합니다.
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem
import com.example.freshbox.data.Category
import com.example.freshbox.repository.FoodRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class FoodFilterType { ALL, ACTIVE, EXPIRED, EXPIRING_SOON }

class FoodListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository

    // --- 1. 프로퍼티(변수) 선언부 ---
    private val allFoodItemsFromRepo: LiveData<List<FoodItem>>

    // allCategories와 customCategories를 여기서는 타입만 선언합니다.
    val allCategories: LiveData<List<Category>>
    val customCategories: LiveData<List<Category>>

    // --- 2. 필터링 조건 LiveData ---
    private val _selectedCategoryId = MutableLiveData<Long?>(null)
    val selectedCategoryId: LiveData<Long?> = _selectedCategoryId
    private val _searchedKeyword = MutableLiveData<String?>(null)
    val searchedKeyword: LiveData<String?> = _searchedKeyword
    private val _filterTypeForAllActivity = MutableLiveData<FoodFilterType>(FoodFilterType.ALL)

    // --- 3. UI에 노출할 최종 LiveData ---
    val homeExpiringSoonItems = MediatorLiveData<List<FoodItem>>()
    val homeExpiredItems = MediatorLiveData<List<FoodItem>>()
    val filteredFoodItemsForAllActivity = MediatorLiveData<List<FoodItem>>()
    private val _itemsForCalendarSelectedDate = MutableLiveData<List<FoodItem>>()
    val itemsForCalendarSelectedDate: LiveData<List<FoodItem>> = _itemsForCalendarSelectedDate
    val allFoodItemsForCalendar: LiveData<List<FoodItem>>

    /**
     * 클래스 생성 시 실행되는 초기화 블록
     */
    init {
        val database = AppDatabase.getDatabase(application)
        repository = FoodRepository(database.foodDao(), database.categoryDao())

        // --- 4. 프로퍼티(변수) 초기화(값 할당) 부 ---
        allFoodItemsFromRepo = repository.getAllFoodItemsSortedByExpiry()

        // 4-1. allCategories를 먼저 초기화합니다.
        allCategories = repository.getAllCategories()

        // 4-2. 초기화된 allCategories를 사용하여 customCategories를 초기화합니다.
        //      이 순서가 매우 중요합니다.
        customCategories = allCategories.map { list ->
            list.filter { it.isCustom }
        }

        allFoodItemsForCalendar = allFoodItemsFromRepo

        // --- Observer 설정 ---
        val homeUpdateTrigger = Observer<Any?> { updateHomeLists() }
        homeExpiringSoonItems.addSource(allFoodItemsFromRepo, homeUpdateTrigger)
        homeExpiringSoonItems.addSource(_selectedCategoryId, homeUpdateTrigger)
        homeExpiringSoonItems.addSource(_searchedKeyword, homeUpdateTrigger)
        homeExpiredItems.addSource(allFoodItemsFromRepo, homeUpdateTrigger)
        homeExpiredItems.addSource(_selectedCategoryId, homeUpdateTrigger)
        homeExpiredItems.addSource(_searchedKeyword, homeUpdateTrigger)

        val allFoodsUpdateTrigger = Observer<Any?> { updateAllFoodsList() }
        filteredFoodItemsForAllActivity.addSource(allFoodItemsFromRepo, allFoodsUpdateTrigger)
        filteredFoodItemsForAllActivity.addSource(_selectedCategoryId, allFoodsUpdateTrigger)
        filteredFoodItemsForAllActivity.addSource(_searchedKeyword, allFoodsUpdateTrigger)
        filteredFoodItemsForAllActivity.addSource(_filterTypeForAllActivity, allFoodsUpdateTrigger)

        updateHomeLists()
        updateAllFoodsList()
    }

    private fun updateHomeLists() {
        val allItems = allFoodItemsFromRepo.value ?: return
        val categoryId = _selectedCategoryId.value
        val keyword = _searchedKeyword.value?.trim()?.lowercase()
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        val baseFilteredList = allItems.filter { item ->
            val categoryMatch = (categoryId == null || item.categoryId == categoryId)
            val keywordMatch = keyword.isNullOrBlank() ||
                    item.name.lowercase().contains(keyword) ||
                    item.tags.any { tag -> tag.lowercase().contains(keyword) } ||
                    item.categoryId?.let { categoriesMap[it]?.contains(keyword) } == true
            categoryMatch && keywordMatch
        }

        homeExpiredItems.value = baseFilteredList.filter { it.isExpired() }.take(10)
        homeExpiringSoonItems.value = baseFilteredList.filter { !it.isExpired() && it.isExpiringSoon() }.take(10)
    }

    private fun updateAllFoodsList() {
        val allItems = allFoodItemsFromRepo.value ?: return
        val categoryId = _selectedCategoryId.value
        val keyword = _searchedKeyword.value?.trim()?.lowercase()
        val filterType = _filterTypeForAllActivity.value ?: FoodFilterType.ALL
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        val typeFilteredList = when (filterType) {
            FoodFilterType.ALL -> allItems
            FoodFilterType.ACTIVE -> allItems.filter { !it.isExpired() }
            FoodFilterType.EXPIRED -> allItems.filter { it.isExpired() }
            FoodFilterType.EXPIRING_SOON -> allItems.filter { !it.isExpired() && it.isExpiringSoon() }
        }

        val finalList = typeFilteredList.filter { item ->
            val categoryMatch = (categoryId == null || item.categoryId == categoryId)
            val keywordMatch = keyword.isNullOrBlank() ||
                    item.name.lowercase().contains(keyword) ||
                    item.tags.any { tag -> tag.lowercase().contains(keyword) } ||
                    item.categoryId?.let { categoriesMap[it]?.contains(keyword) } == true
            categoryMatch && keywordMatch
        }
        filteredFoodItemsForAllActivity.value = finalList
    }

    // --- Public methods for UI ---
    /**
     * 특정 카테고리를 안전하게 삭제하는 함수입니다. (UI에서 호출)
     * Repository를 통해 카테고리 삭제 및 관련 아이템들의 상태 업데이트를 요청합니다.
     * @param category 삭제할 Category 객체.
     */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategoryAndUncategorizeItems(category)
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        if (_selectedCategoryId.value != categoryId) {
            _selectedCategoryId.value = categoryId
        }
    }

    fun setSearchKeyword(keyword: String?) {
        val newKeyword = keyword?.trim()?.ifEmpty { null }
        if (_searchedKeyword.value != newKeyword) {
            _searchedKeyword.value = newKeyword
        }
    }

    fun setFilterTypeForAllActivity(filterType: FoodFilterType) {
        if (_filterTypeForAllActivity.value != filterType) {
            _filterTypeForAllActivity.value = filterType
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) = viewModelScope.launch {
        repository.deleteFoodItem(foodItem)
    }

    fun loadItemsExpiringOnDate(selectedDate: LocalDate) {
        viewModelScope.launch {
            val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault())
            val endOfDay = startOfDay.plusDays(1).minusNanos(1)
            val startTimestamp = startOfDay.toInstant().toEpochMilli()
            val endTimestamp = endOfDay.toInstant().toEpochMilli()
            _itemsForCalendarSelectedDate.value = repository.getFoodItemsExpiringBetween(startTimestamp, endTimestamp)
        }
    }
}