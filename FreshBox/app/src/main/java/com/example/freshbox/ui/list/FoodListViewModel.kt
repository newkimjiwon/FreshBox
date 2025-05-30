// File: app/src/main/java/com/example/freshbox/ui/list/FoodListViewModel.kt
package com.example.freshbox.ui.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem
import com.example.freshbox.data.Category
import com.example.freshbox.repository.FoodRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
// import java.time.temporal.ChronoUnit // ChronoUnit은 현재 코드에서 직접 사용되지 않음

enum class FoodFilterType { ALL, ACTIVE, EXPIRED, EXPIRING_SOON }

class FoodListViewModel(application: Application) : AndroidViewModel(application) {
    private val foodDao = AppDatabase.getDatabase(application).foodDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    private val repository = FoodRepository(foodDao, categoryDao)

    // --- 원본 데이터 소스 LiveData ---
    private val allFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getAllFoodItemsSortedByExpiry()
    private val activeFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getActiveFoodItems()
    private val expiredFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getExpiredFoodItems()

    // --- UI에 노출할 LiveData ---
    val allCategories: LiveData<List<Category>> = repository.getAllCategories()

    // --- 필터링 조건 LiveData ---
    private val _selectedCategoryId = MutableLiveData<Long?>()
    private val _searchedKeyword = MutableLiveData<String?>()

    // --- HomeFragment용 LiveData ---
    val homeExpiringSoonItems = MediatorLiveData<List<FoodItem>>()
    val homeExpiredItems = MediatorLiveData<List<FoodItem>>()

    // --- AllFoodsActivity용 LiveData ---
    val filteredFoodItemsForAllActivity = MediatorLiveData<List<FoodItem>>()
    private val _filterTypeForAllActivity = MutableLiveData<FoodFilterType>(FoodFilterType.ALL)

    // --- CalendarFragment용 LiveData ---
    private val _itemsForCalendarSelectedDate = MutableLiveData<List<FoodItem>>()
    val itemsForCalendarSelectedDate: LiveData<List<FoodItem>> = _itemsForCalendarSelectedDate

    // CalendarFragment가 전체 아이템 목록을 관찰할 수 있도록 공개 (날짜 마킹용)
    val allFoodItemsForCalendar: LiveData<List<FoodItem>> = allFoodItemsFromRepo


    init {
        Log.d("FoodListViewModel", "ViewModel initialized")

        // HomeFragment용 LiveData 소스 설정
        // allCategories는 categoriesMap 생성에 사용되므로, 키워드 필터링에서 카테고리 이름 검색 시 필요
        val homeSources = listOf<LiveData<*>>(
            activeFoodItemsFromRepo,
            expiredFoodItemsFromRepo, // homeExpiredItems도 active가 아닌 expired에서 가져와야 할 수 있음
            _selectedCategoryId,
            _searchedKeyword,
            allCategories
        )

        homeSources.forEach { source ->
            homeExpiringSoonItems.addSource(source) { updateHomeLists() }
            homeExpiredItems.addSource(source) { updateHomeLists() }
        }
        // 로그 간소화: 각 MediatorLiveData에 대해 한 번만 로그를 남기거나, updateHomeLists 시작 시 로그
        Log.d("ViewModel_Source", "Sources added for homeExpiringSoonItems and homeExpiredItems")


        // AllFoodsActivity용 LiveData 소스 설정
        val allActivitySources = listOf<LiveData<*>>(
            _filterTypeForAllActivity,
            _selectedCategoryId,
            _searchedKeyword,
            allFoodItemsFromRepo,
            activeFoodItemsFromRepo,
            expiredFoodItemsFromRepo,
            allCategories
        )
        allActivitySources.forEach { source ->
            filteredFoodItemsForAllActivity.addSource(source) { updateFilteredDataForAllActivity() }
        }
        Log.d("ViewModel_Source", "Sources added for filteredFoodItemsForAllActivity")


        // 초기 필터 값 설정
        setSearchKeyword(null)
        setCategoryFilter(null)
        setFilterTypeForAllActivity(FoodFilterType.ALL) // AllFoodsActivity 기본 필터
    }

    private fun updateHomeLists() {
        val currentKeyword = _searchedKeyword.value?.trim()?.lowercase()
        val currentCategoryId = _selectedCategoryId.value
        // allCategories.value가 null일 수 있으므로 안전 호출 및 기본값 제공
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        Log.d("FoodListViewModel", "updateHomeLists called. CategoryId: $currentCategoryId, Keyword: $currentKeyword")

        // 소비기한 임박 목록 필터링
        val activeList = activeFoodItemsFromRepo.value ?: emptyList()
        var filteredActive = activeList.filter { it.isExpiringSoon() } // 먼저 isExpiringSoon으로 필터링

        if (currentCategoryId != null) {
            filteredActive = filteredActive.filter { it.categoryId == currentCategoryId }
        }
        if (!currentKeyword.isNullOrBlank()) {
            filteredActive = filteredActive.filter { item ->
                item.name.lowercase().contains(currentKeyword) ||
                        item.tags.any { tag -> tag.lowercase().contains(currentKeyword) } ||
                        (item.categoryId?.let { catId -> categoriesMap[catId]?.contains(currentKeyword) } ?: false)
            }
        }
        homeExpiringSoonItems.value = filteredActive
        Log.d("FoodListViewModel", "homeExpiringSoonItems LIVEDATA updated with ${filteredActive.size} items.")

        // 소비기한 만료 목록 필터링
        val expiredList = expiredFoodItemsFromRepo.value ?: emptyList()
        var filteredExpired = expiredList // 이미 만료된 아이템 목록을 가져옴

        if (currentCategoryId != null) {
            filteredExpired = filteredExpired.filter { it.categoryId == currentCategoryId }
        }
        if (!currentKeyword.isNullOrBlank()) {
            filteredExpired = filteredExpired.filter { item ->
                item.name.lowercase().contains(currentKeyword) ||
                        item.tags.any { tag -> tag.lowercase().contains(currentKeyword) } ||
                        (item.categoryId?.let { catId -> categoriesMap[catId]?.contains(currentKeyword) } ?: false)
            }
        }
        homeExpiredItems.value = filteredExpired
        Log.d("FoodListViewModel", "homeExpiredItems LIVEDATA updated with ${filteredExpired.size} items.")
    }

    private fun updateFilteredDataForAllActivity() {
        val currentFilterType = _filterTypeForAllActivity.value
        val currentCategoryId = _selectedCategoryId.value
        val currentKeyword = _searchedKeyword.value?.trim()?.lowercase()
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        Log.d("FoodListViewModel", "updateFilteredDataForAllActivity called. CategoryId: $currentCategoryId, Keyword: $currentKeyword, FilterType: $currentFilterType")

        val sourceList = when (currentFilterType) {
            FoodFilterType.ALL -> allFoodItemsFromRepo.value
            FoodFilterType.ACTIVE -> activeFoodItemsFromRepo.value
            FoodFilterType.EXPIRED -> expiredFoodItemsFromRepo.value
            FoodFilterType.EXPIRING_SOON -> activeFoodItemsFromRepo.value?.filter { it.isExpiringSoon() }
            null -> allFoodItemsFromRepo.value
        } ?: emptyList()

        var resultList = sourceList
        if (currentCategoryId != null) {
            resultList = resultList.filter { it.categoryId == currentCategoryId }
        }
        if (!currentKeyword.isNullOrBlank()) {
            resultList = resultList.filter { item ->
                item.name.lowercase().contains(currentKeyword) ||
                        item.tags.any { tag -> tag.lowercase().contains(currentKeyword) } ||
                        (item.categoryId?.let { catId -> categoriesMap[catId]?.contains(currentKeyword) } ?: false)
            }
        }
        filteredFoodItemsForAllActivity.value = resultList
        Log.d("FoodListViewModel", "filteredFoodItemsForAllActivity LIVEDATA updated with ${resultList.size} items.")
    }


    fun setFilterTypeForAllActivity(filterType: FoodFilterType) {
        if (_filterTypeForAllActivity.value != filterType) {
            Log.d("FoodListViewModel", "setFilterTypeForAllActivity: $filterType")
            _filterTypeForAllActivity.value = filterType
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        if (_selectedCategoryId.value != categoryId) {
            Log.d("FoodListViewModel", "setCategoryFilter: $categoryId")
            _selectedCategoryId.value = categoryId
        }
    }

    fun setSearchKeyword(keyword: String?) {
        val newKeyword = keyword?.trim()?.ifEmpty { null }
        if (_searchedKeyword.value != newKeyword) {
            Log.d("FoodListViewModel", "_searchedKeyword LiveData updated to: $newKeyword by setSearchKeyword")
            _searchedKeyword.value = newKeyword
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) = viewModelScope.launch {
        repository.deleteFoodItem(foodItem)
    }

    fun loadItemsExpiringOnDate(selectedDate: LocalDate) {
        viewModelScope.launch {
            Log.d("FoodListViewModel", "loadItemsExpiringOnDate called for: $selectedDate")
            val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault())
            // 해당 날짜의 시작 (00:00:00) 부터 다음 날짜의 시작 (00:00:00) 바로 전까지로 범위 설정
            val endOfDay = startOfDay.plusDays(1).minusNanos(1)


            val startTimestamp = startOfDay.toInstant().toEpochMilli()
            val endTimestamp = endOfDay.toInstant().toEpochMilli() // 해당 날짜의 가장 마지막 순간

            Log.d("FoodListViewModel", "Querying between $startTimestamp and $endTimestamp")
            val items = repository.getFoodItemsExpiringBetween(startTimestamp, endTimestamp)
            _itemsForCalendarSelectedDate.value = items
            Log.d("FoodListViewModel", "Items loaded for $selectedDate: ${items.size}")
        }
    }
}