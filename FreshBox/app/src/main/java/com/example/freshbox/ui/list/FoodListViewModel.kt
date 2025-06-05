// File: app/src/main/java/com/example/freshbox/ui/list/FoodListViewModel.kt
package com.example.freshbox.ui.list

import android.app.Application
import android.util.Log // 디버깅을 위한 Log 클래스 import
import androidx.lifecycle.* // ViewModel, LiveData, AndroidViewModel, MediatorLiveData, switchMap 등 사용
import com.example.freshbox.data.AppDatabase // Room 데이터베이스 클래스
import com.example.freshbox.data.FoodItem // FoodItem 데이터 Entity
import com.example.freshbox.data.Category // Category 데이터 Entity
import com.example.freshbox.repository.FoodRepository // 데이터 로직을 처리하는 Repository
import kotlinx.coroutines.launch // 코루틴을 사용한 비동기 작업을 위해 import
import java.time.LocalDate // CalendarFragment에서 날짜 처리를 위해 사용
import java.time.ZoneId // 시간대 처리를 위해 사용
// import java.time.temporal.ChronoUnit // 현재 코드에서 직접 사용되지 않으므로 주석 처리

// 식품 목록 필터링 타입을 정의하는 Enum 클래스
enum class FoodFilterType { ALL, ACTIVE, EXPIRED, EXPIRING_SOON }

// AndroidViewModel을 상속받아 Application Context를 사용할 수 있도록 함
class FoodListViewModel(application: Application) : AndroidViewModel(application) {
    // 데이터베이스 인스턴스 및 DAO, Repository 초기화
    private val foodDao = AppDatabase.getDatabase(application).foodDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    private val repository = FoodRepository(foodDao, categoryDao) // FoodDao와 CategoryDao를 모두 사용

    // --- 원본 데이터 소스 LiveData ---
    // Repository로부터 가져오는 기본적인 LiveData 목록들. 이들은 Room에 의해 자동으로 업데이트됨.
    // 이 LiveData들은 주로 내부 필터링 로직의 기본 데이터로 사용됨.
    private val allFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getAllFoodItemsSortedByExpiry()
    private val activeFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getActiveFoodItems() // 현재 날짜 기준, 만료되지 않은 아이템
    private val expiredFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getExpiredFoodItems() // 현재 날짜 기준, 만료된 아이템

    // --- UI(Fragment/Activity)에 노출할 LiveData ---
    // 모든 카테고리 목록 (예: 카테고리 필터 UI 채우기용)
    val allCategories: LiveData<List<Category>> = repository.getAllCategories()

    // --- 필터링 조건 LiveData ---
    // 현재 선택된 카테고리 ID를 저장하는 MutableLiveData (private)
    // 외부에서는 setCategoryFilter()를 통해 값을 변경하고, public LiveData를 통해 관찰함.
    // 초기값 null은 "전체 카테고리"를 의미.
    private val _selectedCategoryId = MutableLiveData<Long?>(null)
    val selectedCategoryId: LiveData<Long?> = _selectedCategoryId // Fragment에서 관찰할 수 있도록 public으로 노출

    // 현재 입력된 검색 키워드를 저장하는 MutableLiveData (private)
    private val _searchedKeyword = MutableLiveData<String?>(null)
    val searchedKeyword: LiveData<String?> = _searchedKeyword // Fragment에서 관찰할 수 있도록 public으로 노출


    // --- HomeFragment용 LiveData ---
    // MediatorLiveData는 여러 LiveData 소스를 관찰하고, 이들 중 하나라도 변경되면 새로운 값을 발행할 수 있음.
    // apply { value = emptyList() }는 LiveData가 초기 구독 시 null 대신 빈 리스트를 갖도록 하여 NPE 방지 및 UI 초기 상태 관리에 도움.
    val homeExpiringSoonItems = MediatorLiveData<List<FoodItem>>().apply { value = emptyList() } // 소비기한 임박 식품 (필터링됨)
    val homeExpiredItems = MediatorLiveData<List<FoodItem>>().apply { value = emptyList() }     // 소비기한 만료 식품 (필터링됨)

    // --- AllFoodsActivity용 LiveData ---
    val filteredFoodItemsForAllActivity = MediatorLiveData<List<FoodItem>>().apply { value = emptyList() } // 모든 필터 적용된 목록
    private val _filterTypeForAllActivity = MutableLiveData<FoodFilterType>(FoodFilterType.ALL) // AllFoodsActivity의 현재 필터 타입

    // --- CalendarFragment용 LiveData ---
    private val _itemsForCalendarSelectedDate = MutableLiveData<List<FoodItem>>() // 선택된 날짜의 식품 목록 (내부용)
    val itemsForCalendarSelectedDate: LiveData<List<FoodItem>> = _itemsForCalendarSelectedDate // 외부 노출용

    // CalendarFragment가 전체 아이템 목록을 관찰하여 달력에 날짜별 이벤트(구매/만료)를 표시하기 위함
    val allFoodItemsForCalendar: LiveData<List<FoodItem>> = allFoodItemsFromRepo


    // ViewModel이 생성될 때(초기화 시) 실행되는 블록
    init {
        Log.d("FoodListViewModel", "ViewModel initialized. Initial categoryId: ${_selectedCategoryId.value}, keyword: ${_searchedKeyword.value}")

        // HomeFragment에서 사용될 homeExpiringSoonItems와 homeExpiredItems MediatorLiveData에 소스들을 추가합니다.
        // 이 소스들 중 어느 하나라도 값이 변경되면, 지정된 람다 함수(여기서는 updateHomeLists())가 호출됩니다.
        val homeTriggerSources = listOf<LiveData<*>>(
            activeFoodItemsFromRepo,  // 원본 활성 식품 목록 (소비기한 임박의 기반)
            expiredFoodItemsFromRepo, // 원본 만료 식품 목록
            _selectedCategoryId,      // 카테고리 필터 조건 변경 시
            _searchedKeyword,       // 검색어 필터 조건 변경 시
            allCategories             // 전체 카테고리 목록 변경 시 (categoriesMap 업데이트 및 카테고리명 검색에 영향)
        )
        homeTriggerSources.forEach { source -> // 각 소스에 대해
            homeExpiringSoonItems.addSource(source) { // homeExpiringSoonItems가 이 소스를 관찰하도록 추가
                Log.d("ViewModel_Trigger", "homeExpiringSoonItems triggered by source change: ${source}")
                updateHomeLists() // 소스 변경 시 homeExpiringSoonItems 값 업데이트
            }
            homeExpiredItems.addSource(source) { // homeExpiredItems가 이 소스를 관찰하도록 추가
                Log.d("ViewModel_Trigger", "homeExpiredItems triggered by source change: ${source}")
                updateHomeLists() // 소스 변경 시 homeExpiredItems 값 업데이트
            }
        }
        Log.d("FoodListViewModel", "Sources added for HomeFragment LiveData.")


        // AllFoodsActivity에서 사용될 filteredFoodItemsForAllActivity MediatorLiveData에 소스들을 추가합니다.
        val allActivityTriggerSources = listOf<LiveData<*>>(
            _filterTypeForAllActivity, // AllFoodsActivity의 필터 타입 변경 시
            _selectedCategoryId,       // 공통 카테고리 필터 변경 시
            _searchedKeyword,        // 공통 검색어 필터 변경 시
            allFoodItemsFromRepo,      // 모든 식품 목록 (FilterType.ALL 일 때의 기반)
            activeFoodItemsFromRepo,   // 활성 식품 목록 (FilterType.ACTIVE/EXPIRING_SOON 일 때의 기반)
            expiredFoodItemsFromRepo,  // 만료 식품 목록 (FilterType.EXPIRED 일 때의 기반)
            allCategories
        )
        allActivityTriggerSources.forEach { source ->
            filteredFoodItemsForAllActivity.addSource(source) {
                Log.d("ViewModel_Trigger", "filteredFoodItemsForAllActivity triggered by source change: ${source}")
                updateFilteredDataForAllActivity() // 소스 변경 시 filteredFoodItemsForAllActivity 값 업데이트
            }
        }
        Log.d("FoodListViewModel", "Sources added for AllFoodsActivity LiveData.")

        // ViewModel 생성 시 초기 필터 상태를 설정하여 LiveData들이 초기값을 가지도록 합니다.
        // 이 호출들은 각 MutableLiveData의 값을 변경하고, 결과적으로 연결된 MediatorLiveData의 업데이트 함수를 트리거합니다.
        // setSearchKeyword(null) // 초기 검색어 없음
        // setCategoryFilter(null)  // 초기 카테고리 필터 없음 ("전체")
        // setFilterTypeForAllActivity(FoodFilterType.ALL) // AllFoodsActivity의 기본 필터 타입
        // -> init 블록에서 LiveData의 초기값을 이미 null 또는 ALL로 설정했으므로,
        //    여기서 set 메서드를 다시 호출하면 중복되거나 의도치 않은 초기 로딩이 발생할 수 있습니다.
        //    MediatorLiveData는 소스 LiveData가 첫 값을 발행할 때 또는 값이 변경될 때 반응합니다.
        //    만약 명시적인 초기 데이터 로딩이 필요하다면, init 마지막에 updateHomeLists(), updateFilteredDataForAllActivity()를 호출할 수 있습니다.
    }

    // 여러 LiveData에서 공통으로 사용될 수 있는 필터링 로직 함수
    // sourceList에 대해 categoryId와 keyword로 필터링하여 결과를 반환합니다.
    private fun applySharedFilters(
        sourceList: List<FoodItem>,
        categoryId: Long?,
        keyword: String?,
        categoriesMap: Map<Long, String> // 카테고리 이름으로 검색하기 위해 필요
    ): List<FoodItem> {
        var filteredList = sourceList // 원본 리스트로 시작
        Log.d("FoodListViewModel_Filter", "applySharedFilters - Input size: ${sourceList.size}, categoryId: $categoryId, keyword: '$keyword'")

        // 1. 카테고리 ID로 필터링 (categoryId가 null이 아닐 경우)
        if (categoryId != null) {
            filteredList = filteredList.filter { it.categoryId == categoryId }
        }
        Log.d("FoodListViewModel_Filter", "applySharedFilters - After Category filter. Result size: ${filteredList.size}")

        // 2. 키워드로 필터링 (keyword가 비어있지 않을 경우)
        // 식품 이름, 태그, 카테고리 이름(변환된)에 키워드가 포함되어 있는지 확인합니다.
        if (!keyword.isNullOrBlank()) {
            filteredList = filteredList.filter { item ->
                val nameMatch = item.name.lowercase().contains(keyword)
                val tagMatch = item.tags.any { tag -> tag.lowercase().contains(keyword) }
                val categoryNameMatch = item.categoryId?.let { catId ->
                    categoriesMap[catId]?.contains(keyword) // 카테고리 ID로 이름을 찾아 검색어와 비교
                } ?: false
                nameMatch || tagMatch || categoryNameMatch // 셋 중 하나라도 일치하면 포함
            }
        }
        Log.d("FoodListViewModel_Filter", "applySharedFilters - After Keyword filter. Result size: ${filteredList.size}")
        return filteredList // 최종 필터링된 리스트 반환
    }

    // HomeFragment에 표시될 소비기한 임박 및 만료 목록을 업데이트하는 함수
    // _selectedCategoryId 또는 _searchedKeyword 등이 변경될 때 호출됩니다.
    private fun updateHomeLists() {
        val currentKeyword = _searchedKeyword.value?.trim()?.lowercase() // 현재 검색어 (소문자 변환)
        val currentCategoryId = _selectedCategoryId.value // 현재 선택된 카테고리 ID
        // 카테고리 ID-이름 Map (allCategories LiveData가 null일 수 있으므로 안전 호출 및 기본값 제공)
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        Log.d("FoodListViewModel", "updateHomeLists called. CategoryId: $currentCategoryId, Keyword: '$currentKeyword'")

        // 소비기한 임박 목록 필터링
        val activeListSource = activeFoodItemsFromRepo.value ?: emptyList() // 원본 활성 식품 목록
        val expiringSoonBase = activeListSource.filter { it.isExpiringSoon() } // 1차: 소비기한 임박 식품만 필터링
        Log.d("FoodListViewModel", "Base expiringSoonList (isExpiringSoon only): ${expiringSoonBase.size} items")
        // 2차: 카테고리 및 키워드로 추가 필터링하여 homeExpiringSoonItems LiveData 값 업데이트
        homeExpiringSoonItems.value = applySharedFilters(expiringSoonBase, currentCategoryId, currentKeyword, categoriesMap)
        Log.d("FoodListViewModel", "homeExpiringSoonItems LIVEDATA updated with ${homeExpiringSoonItems.value?.size ?: 0} items.")

        // 소비기한 만료 목록 필터링
        val expiredListSource = expiredFoodItemsFromRepo.value ?: emptyList() // 원본 만료 식품 목록
        Log.d("FoodListViewModel", "Base expiredList: ${expiredListSource.size} items")
        // 카테고리 및 키워드로 필터링하여 homeExpiredItems LiveData 값 업데이트
        homeExpiredItems.value = applySharedFilters(expiredListSource, currentCategoryId, currentKeyword, categoriesMap)
        Log.d("FoodListViewModel", "homeExpiredItems LIVEDATA updated with ${homeExpiredItems.value?.size ?: 0} items.")
    }

    // AllFoodsActivity에 표시될 전체 필터링된 목록을 업데이트하는 함수
    private fun updateFilteredDataForAllActivity() {
        val currentFilterType = _filterTypeForAllActivity.value // 현재 필터 타입 (ALL, ACTIVE 등)
        val currentCategoryId = _selectedCategoryId.value // 현재 선택된 카테고리 ID
        val currentKeyword = _searchedKeyword.value?.trim()?.lowercase() // 현재 검색어
        val categoriesMap = allCategories.value?.associateBy({ it.id }, { it.name.lowercase() }) ?: emptyMap()

        Log.d("FoodListViewModel", "updateFilteredDataForAllActivity. CategoryId: $currentCategoryId, Keyword: '$currentKeyword', FilterType: $currentFilterType")

        // 1. 필터 타입에 따라 기본 소스 목록 결정
        val sourceListBasedOnType = when (currentFilterType) {
            FoodFilterType.ALL -> allFoodItemsFromRepo.value
            FoodFilterType.ACTIVE -> activeFoodItemsFromRepo.value
            FoodFilterType.EXPIRED -> expiredFoodItemsFromRepo.value
            FoodFilterType.EXPIRING_SOON -> activeFoodItemsFromRepo.value?.filter { it.isExpiringSoon() }
            null -> allFoodItemsFromRepo.value // 기본값은 전체 목록
        } ?: emptyList()

        // 2. 카테고리 및 키워드로 추가 필터링하여 filteredFoodItemsForAllActivity LiveData 값 업데이트
        filteredFoodItemsForAllActivity.value = applySharedFilters(sourceListBasedOnType, currentCategoryId, currentKeyword, categoriesMap)
        Log.d("FoodListViewModel", "filteredFoodItemsForAllActivity LIVEDATA updated with ${filteredFoodItemsForAllActivity.value?.size ?: 0} items.")
    }

    // --- Public methods for UI (Fragment/Activity) to call to set filters ---

    // AllFoodsActivity에서 필터 타입을 설정하는 함수
    fun setFilterTypeForAllActivity(filterType: FoodFilterType) {
        // 현재 필터 타입과 다를 경우에만 업데이트 (불필요한 LiveData 업데이트 방지)
        if (_filterTypeForAllActivity.value != filterType) {
            Log.d("FoodListViewModel", "setFilterTypeForAllActivity: $filterType")
            _filterTypeForAllActivity.value = filterType
        } else {
            // 이미 같은 필터 타입이라도, 다른 필터(카테고리, 검색어)가 변경되었을 수 있으므로
            // filteredFoodItemsForAllActivity를 강제로 다시 계산하도록 호출 (선택적)
            updateFilteredDataForAllActivity()
        }
    }

    // 공용 카테고리 필터를 설정하는 함수 (HomeFragment, AllFoodsActivity 등에서 호출 가능)
    fun setCategoryFilter(categoryId: Long?) {
        // 현재 선택된 카테고리 ID와 다를 경우에만 업데이트
        if (_selectedCategoryId.value != categoryId) {
            Log.d("FoodListViewModel", "setCategoryFilter: $categoryId. Old value: ${_selectedCategoryId.value}")
            _selectedCategoryId.value = categoryId // private _selectedCategoryId 업데이트 -> 연결된 MediatorLiveData들 트리거
        } else if (categoryId == null && _selectedCategoryId.value == null) {
            // 만약 "전체"(null)가 이미 선택된 상태에서 다시 "전체"를 선택한 경우,
            // 다른 필터(예: 검색어)가 적용된 상태를 해제하고 싶을 수 있으므로,
            // LiveData 값을 강제로 다시 설정하여 Observer를 트리거할 수 있음 (또는 update 함수 직접 호출)
            Log.d("FoodListViewModel", "setCategoryFilter: re-setting to null (all categories) to potentially refresh lists with other filters cleared.")
            _selectedCategoryId.value = null // 값을 다시 설정하여 Observer 호출 유도 (주의: LiveData는 값이 실제로 변경되어야 알림)
            // 이 경우, 검색어도 함께 초기화하는 등의 정책을 고려할 수 있음.
        }
    }

    // 공용 검색어를 설정하는 함수
    fun setSearchKeyword(keyword: String?) {
        val newKeyword = keyword?.trim()?.ifEmpty { null } // 앞뒤 공백 제거 및 빈 문자열이면 null 처리
        // 현재 검색어와 다를 경우에만 업데이트
        if (_searchedKeyword.value != newKeyword) {
            Log.d("FoodListViewModel", "_searchedKeyword LiveData updated to: '$newKeyword' by setSearchKeyword")
            _searchedKeyword.value = newKeyword // private _searchedKeyword 업데이트 -> 연결된 MediatorLiveData들 트리거
        } else if (newKeyword == null && _searchedKeyword.value != null) {
            // 검색어가 있다가 없어졌을 때 (사용자가 검색창을 비웠을 때) 명시적으로 null로 설정하여 업데이트 트리거
            Log.d("FoodListViewModel", "_searchedKeyword explicitly set to null as newKeyword is null and old was not.")
            _searchedKeyword.value = null
        }
    }

    // 식품 아이템을 삭제하는 함수 (코루틴 내에서 비동기 실행)
    fun deleteFoodItem(foodItem: FoodItem) = viewModelScope.launch {
        repository.deleteFoodItem(foodItem)
        // Room의 LiveData는 데이터 변경을 자동으로 감지하므로, 삭제 후 목록은 자동으로 업데이트됨.
    }

    // CalendarFragment에서 특정 날짜에 만료되는 식품 목록을 로드하는 함수
    fun loadItemsExpiringOnDate(selectedDate: LocalDate) {
        viewModelScope.launch { // 코루틴 스코프에서 실행
            Log.d("FoodListViewModel", "loadItemsExpiringOnDate called for: $selectedDate")
            // 선택된 LocalDate를 해당 날짜의 시작과 끝 타임스탬프(Long)로 변환
            val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()) // 시스템 기본 시간대로 설정
            val endOfDay = startOfDay.plusDays(1).minusNanos(1) // 해당 날짜의 가장 마지막 순간 (23:59:59.999...)

            val startTimestamp = startOfDay.toInstant().toEpochMilli()
            val endTimestamp = endOfDay.toInstant().toEpochMilli()

            Log.d("FoodListViewModel", "Querying for calendar items between $startTimestamp and $endTimestamp")
            // Repository를 통해 해당 기간의 만료 식품 목록을 가져옴 (suspend 함수 호출)
            val items = repository.getFoodItemsExpiringBetween(startTimestamp, endTimestamp)
            // 결과를 _itemsForCalendarSelectedDate LiveData에 할당하여 UI에 알림
            _itemsForCalendarSelectedDate.value = items
            Log.d("FoodListViewModel", "Items loaded for $selectedDate (calendar): ${items.size}")
        }
    }
}