// File: app/src/main/java/com/example/freshbox/ui/addedit/AddEditViewModel.kt
package com.example.freshbox.ui.addedit

import android.app.Application
import androidx.lifecycle.*
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem         // data.FoodItem 사용
import com.example.freshbox.data.Category        // Category Entity import
// CategoryDao는 FoodRepository 생성 시 AppDatabase에서 가져오므로 직접 import는 필수 아님
import com.example.freshbox.repository.FoodRepository
import com.example.freshbox.util.SingleLiveEvent
import kotlinx.coroutines.launch
import java.util.Date

class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository

    private val _foodItemId = MutableLiveData<Int?>()

    val foodItem: LiveData<FoodItem?> = _foodItemId.switchMap { id ->
        if (id == null || id == -1) { // -1 또는 다른 식별 불가능한 ID를 새 아이템 모드로 간주
            MutableLiveData<FoodItem?>().apply { value = null }
        } else {
            repository.getFoodItemById(id)
        }
    }

    private val _saveEvent = SingleLiveEvent<Unit>()
    val saveEvent: LiveData<Unit> = _saveEvent

    // 카테고리 목록을 UI에 제공하기 위한 LiveData
    val categories: LiveData<List<Category>>

    // 새 카테고리 추가 성공/실패 또는 중복 알림을 위한 이벤트 (선택 사항)
    private val _categoryAddedEvent = SingleLiveEvent<Pair<Boolean, String>>() // Pair<성공여부, 메시지>
    val categoryAddedEvent: LiveData<Pair<Boolean, String>> = _categoryAddedEvent

    // FoodListViewModel.kt 의 init 블록 또는 repository 초기화 부분 예상 수정안
    init {
        val database = AppDatabase.getDatabase(application)
        val foodDao = database.foodDao()
        val categoryDao = database.categoryDao() // CategoryDao 인스턴스 가져오기
        repository = FoodRepository(foodDao, categoryDao) // FoodRepository 생성자에 categoryDao 전달

        categories = repository.getAllCategories() // 카테고리 목록 LiveData 초기화
    }

    fun loadFoodItem(id: Int) {
        _foodItemId.value = id
    }

    fun setNewItemMode() {
        _foodItemId.value = -1 // 새 아이템 모드를 나타내는 값 (예: -1 또는 null)
    }

    fun saveOrUpdateFoodItem(
        name: String,
        expiryDate: Date,
        quantity: String,
        categoryId: Long?,
        tags: List<String>,
        purchaseDate: Date?,
        storage: String?, // FoodItem Entity에서는 storageLocation
        memo: String?,
        isFrozen: Boolean,    // <<< isFrozen 파라미터
        imagePath: String?    // <<< imagePath 파라미터
    ) {
        val currentFoodItem = foodItem.value

        val itemToSave: com.example.freshbox.data.FoodItem = if (currentFoodItem != null && currentFoodItem.id != 0) { // 타입 명시
            currentFoodItem.copy(
                name = name,
                expiryDate = expiryDate,
                quantity = quantity,
                categoryId = categoryId,
                tags = tags,
                purchaseDate = purchaseDate,
                storageLocation = storage, // 필드 이름 확인: FoodItem Entity에서는 storageLocation
                memo = memo,
                isFrozen = isFrozen,
                imagePath = imagePath    // imagePath 할당
            )
        } else {
            com.example.freshbox.data.FoodItem( // 타입 명시
                name = name,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                quantity = quantity,
                categoryId = categoryId,
                tags = tags,
                storageLocation = storage, // 필드 이름 확인
                memo = memo,
                isFrozen = isFrozen,
                imagePath = imagePath    // imagePath 할당
            )
        }

        viewModelScope.launch {
            if (itemToSave.id != 0) {
                repository.updateFoodItem(itemToSave)
            } else {
                repository.insertFoodItem(itemToSave)
            }
            _saveEvent.call()
        }
    }

    // 새 카테고리 추가 로직
    fun addNewCategory(categoryName: String) {
        viewModelScope.launch {
            if (categoryName.isBlank()) {
                _categoryAddedEvent.value = Pair(false, "카테고리 이름은 비워둘 수 없습니다.")
                return@launch
            }
            val existingCategory = repository.getCategoryByName(categoryName.trim())
            if (existingCategory == null) {
                val newCategory = Category(name = categoryName.trim(), isCustom = true)
                val newRowId = repository.insertCategory(newCategory) // insertCategory가 Long ID를 반환한다고 가정
                if (newRowId > 0L) {
                    _categoryAddedEvent.value = Pair(true, "'${newCategory.name}' 카테고리가 추가되었습니다.")
                } else {
                    _categoryAddedEvent.value = Pair(false, "카테고리 추가에 실패했습니다.")
                }
            } else {
                _categoryAddedEvent.value = Pair(false, "이미 존재하는 카테고리 이름입니다.")
            }
        }
    }
}