// File: app/src/main/java/com/example/freshbox/ui/addedit/AddEditViewModel.kt
package com.example.freshbox.ui.addedit

import android.app.Application
import androidx.lifecycle.* // ViewModel, LiveData, AndroidViewModel, switchMap 등 Android Lifecycle 관련 클래스 import
import com.example.freshbox.data.AppDatabase // Room 데이터베이스 클래스
import com.example.freshbox.data.FoodItem         // FoodItem 데이터 Entity
import com.example.freshbox.data.Category        // Category 데이터 Entity
// CategoryDao는 FoodRepository 생성 시 AppDatabase에서 가져오므로 직접 import는 필수는 아님 (AppDatabase를 통해 접근)
import com.example.freshbox.repository.FoodRepository // 데이터 로직을 처리하는 Repository
import com.example.freshbox.util.SingleLiveEvent // 일회성 이벤트를 위한 LiveData 확장 클래스
import kotlinx.coroutines.launch // 코루틴을 사용한 비동기 작업을 위해 import
import java.util.Date // 날짜 데이터를 다루기 위해 import

// AndroidViewModel을 상속받아 Application Context를 사용할 수 있도록 함
class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    // FoodRepository 인스턴스. 데이터 관련 로직은 이 Repository를 통해 처리됨.
    private val repository: FoodRepository

    // 현재 수정 중이거나 새로 추가할 FoodItem의 ID를 저장하는 MutableLiveData.
    // private으로 선언하여 ViewModel 외부에서는 직접 값을 변경할 수 없도록 함.
    // Int? 타입으로, ID가 없거나(새 아이템) 아직 로드되지 않았을 경우 null일 수 있음.
    private val _foodItemId = MutableLiveData<Int?>()

    // UI(Fragment)에 노출될 FoodItem LiveData.
    // _foodItemId가 변경될 때마다 repository.getFoodItemById(id)를 호출하여
    // 해당 ID의 FoodItem을 가져와 발행함 (switchMap 사용).
    // ID가 null이거나 -1이면 새 아이템 모드로 간주하여 null을 발행.
    val foodItem: LiveData<FoodItem?> = _foodItemId.switchMap { id ->
        if (id == null || id == -1) { // 새 아이템 모드 또는 ID가 유효하지 않은 경우
            MutableLiveData<FoodItem?>().apply { value = null } // null을 가진 새 LiveData 발행
        } else {
            repository.getFoodItemById(id) // ID가 유효하면 Repository에서 해당 FoodItem을 가져옴 (LiveData 반환)
        }
    }

    // 저장 완료 이벤트를 UI(Fragment)에 알리기 위한 SingleLiveEvent.
    // private으로 선언하여 ViewModel 외부에서는 직접 이벤트를 발생시킬 수 없도록 함.
    private val _saveEvent = SingleLiveEvent<Unit>()
    // 외부(Fragment)에서는 이 LiveData를 관찰하여 저장 완료 시 특정 동작(예: BottomSheet 닫기)을 수행.
    val saveEvent: LiveData<Unit> = _saveEvent

    // 카테고리 목록을 UI(Fragment)에 제공하기 위한 LiveData.
    // Fragment의 카테고리 선택 UI(예: 드롭다운)를 채우는 데 사용됨.
    val categories: LiveData<List<Category>>

    // 새 카테고리 추가 시 성공/실패 또는 중복 여부를 UI에 알리기 위한 SingleLiveEvent (선택 사항).
    // Pair<Boolean, String>은 (성공여부, 메시지)를 나타냄.
    private val _categoryAddedEvent = SingleLiveEvent<Pair<Boolean, String>>()
    val categoryAddedEvent: LiveData<Pair<Boolean, String>> = _categoryAddedEvent

    // ViewModel이 생성될 때(초기화 시) 실행되는 블록
    init {
        // Application Context를 사용하여 데이터베이스 인스턴스를 가져옴
        val database = AppDatabase.getDatabase(application)
        // 데이터베이스 인스턴스로부터 FoodDao와 CategoryDao를 가져옴
        val foodDao = database.foodDao()
        val categoryDao = database.categoryDao()
        // FoodDao와 CategoryDao를 사용하여 FoodRepository 인스턴스 생성
        repository = FoodRepository(foodDao, categoryDao)

        // Repository를 통해 전체 카테고리 목록을 가져와 categories LiveData 초기화
        categories = repository.getAllCategories()
    }

    // 수정할 FoodItem의 ID를 설정하고, 이에 따라 foodItem LiveData가 업데이트되도록 하는 함수
    // AddFoodBottomSheetFragment에서 수정 모드로 진입 시 호출됨
    fun loadFoodItem(id: Int) {
        _foodItemId.value = id // _foodItemId 값을 변경하여 foodItem LiveData의 switchMap 트리거
    }

    // 새 아이템 추가 모드로 설정하는 함수
    // AddFoodBottomSheetFragment에서 새 아이템 추가 시 호출됨
    fun setNewItemMode() {
        _foodItemId.value = -1 // ID를 -1 (또는 null)로 설정하여 foodItem LiveData가 null을 발행하도록 함
    }

    // UI(Fragment)로부터 받은 식품 정보를 Room 데이터베이스에 저장하거나 업데이트하는 함수
    fun saveOrUpdateFoodItem(
        name: String,          // 식품 이름
        expiryDate: Date,      // 소비기한
        quantity: String,      // 수량
        categoryId: Long?,     // 선택된 카테고리 ID (Nullable)
        tags: List<String>,    // 태그 목록
        purchaseDate: Date?,   // 구매일 (Nullable)
        storage: String?,      // 보관 위치 (ViewModel 파라미터명, Entity는 storageLocation)
        memo: String?,         // 메모 (Nullable)
        isFrozen: Boolean,    // 냉동 여부
        imagePath: String?    // 이미지 파일 경로 (Nullable)
    ) {
        // 현재 foodItem LiveData의 값을 가져옴 (수정 모드일 경우 기존 아이템 정보)
        val currentFoodItem = foodItem.value

        // 저장할 FoodItem 객체 생성
        val itemToSave: com.example.freshbox.data.FoodItem = // 타입을 명시적으로 data.FoodItem으로
            if (currentFoodItem != null && currentFoodItem.id != 0) { // 수정 모드: currentFoodItem이 있고 ID가 0이 아니면
                // 기존 FoodItem 객체를 복사(copy)하고 변경된 값들로 업데이트
                currentFoodItem.copy(
                    name = name,
                    expiryDate = expiryDate,
                    quantity = quantity,
                    categoryId = categoryId,
                    tags = tags,
                    purchaseDate = purchaseDate,
                    storageLocation = storage, // Entity의 필드명(storageLocation)에 맞게 할당
                    memo = memo,
                    isFrozen = isFrozen,
                    imagePath = imagePath
                )
            } else { // 새 아이템 추가 모드
                // 새로운 FoodItem 객체 생성 (ID는 Room이 자동 생성하므로 0 또는 기본값)
                com.example.freshbox.data.FoodItem( // 타입을 명시적으로 data.FoodItem으로
                    name = name,
                    purchaseDate = purchaseDate,
                    expiryDate = expiryDate,
                    quantity = quantity,
                    categoryId = categoryId,
                    tags = tags,
                    storageLocation = storage, // Entity의 필드명(storageLocation)에 맞게 할당
                    memo = memo,
                    isFrozen = isFrozen,
                    imagePath = imagePath
                )
            }

        // viewModelScope.launch를 사용하여 코루틴 내에서 비동기적으로 데이터베이스 작업 수행
        // 이렇게 하면 UI 스레드가 중단되는 것을 방지할 수 있음
        viewModelScope.launch {
            if (itemToSave.id != 0) { // ID가 0이 아니면 (즉, 기존 아이템이면)
                repository.updateFoodItem(itemToSave) // Repository를 통해 업데이트
            } else { // ID가 0이면 (즉, 새 아이템이면)
                repository.insertFoodItem(itemToSave) // Repository를 통해 삽입
            }
            _saveEvent.call() // 저장 완료 이벤트를 UI(Fragment)에 알림
        }
    }

    // 새 카테고리를 추가하는 로직 (UI에서 호출)
    fun addNewCategory(categoryName: String) {
        viewModelScope.launch { // 코루틴으로 비동기 실행
            // 카테고리 이름이 비어있는지 확인
            if (categoryName.isBlank()) {
                _categoryAddedEvent.value = Pair(false, "카테고리 이름은 비워둘 수 없습니다.")
                return@launch // 함수 종료
            }
            // 입력된 이름(앞뒤 공백 제거)으로 이미 카테고리가 존재하는지 Repository를 통해 확인 (suspend 함수 호출)
            val existingCategory = repository.getCategoryByName(categoryName.trim())
            if (existingCategory == null) { // 존재하지 않는 카테고리 이름이면
                val newCategory = Category(name = categoryName.trim(), isCustom = true) // 새 Category 객체 생성
                val newRowId = repository.insertCategory(newCategory) // Repository를 통해 DB에 삽입 (삽입된 row ID 반환 가정)
                if (newRowId > 0L) { // 삽입 성공 시 (row ID가 0보다 크면)
                    _categoryAddedEvent.value = Pair(true, "'${newCategory.name}' 카테고리가 추가되었습니다.")
                } else { // 삽입 실패 시
                    _categoryAddedEvent.value = Pair(false, "카테고리 추가에 실패했습니다.")
                }
            } else { // 이미 존재하는 카테고리 이름이면
                _categoryAddedEvent.value = Pair(false, "이미 존재하는 카테고리 이름입니다.")
            }
        }
    }
}