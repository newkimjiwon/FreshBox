package com.example.freshbox.ui.addedit

import android.app.Application
import androidx.lifecycle.*
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem
import com.example.freshbox.repository.FoodRepository
import com.example.freshbox.util.SingleLiveEvent
import kotlinx.coroutines.launch
import java.util.Date

class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository

    private val _foodItemId = MutableLiveData<Int?>()

    val foodItem: LiveData<FoodItem?> = MediatorLiveData<FoodItem?>().apply {
        addSource(_foodItemId) { id ->
            if (id == null || id == -1) {
                value = null
            } else {
                viewModelScope.launch {
                    val source = repository.getFoodItemById(id)
                    addSource(source) { item ->
                        value = item
                    }
                }
            }
        }
    }

    private val _saveEvent = SingleLiveEvent<Unit>()
    val saveEvent: LiveData<Unit> = _saveEvent

    private val _barcodeResultEvent = SingleLiveEvent<FoodItem>()
    val barcodeResultEvent: LiveData<FoodItem> = _barcodeResultEvent

    private var currentBarcode: String? = null

    init {
        val foodDao = AppDatabase.getDatabase(application).foodDao()
        repository = FoodRepository(foodDao)
    }

    fun loadFoodItem(id: Int) {
        _foodItemId.value = id
    }

    fun setNewItemMode() {
        _foodItemId.value = -1
        currentBarcode = null
    }

    fun processBarcode(barcode: String) {
        currentBarcode = barcode
        viewModelScope.launch {
            val existingItem = repository.getFoodItemByBarcode(barcode)
            _barcodeResultEvent.value = existingItem ?: FoodItem(
                id = 0,
                name = "",
                purchaseDate = null, // 추가
                expiryDate = Date(),
                barcode = barcode
            )
        }
    }

    fun saveOrUpdateFoodItem(
        name: String,
        expiryDate: Date,
        quantity: String,
        category: String?,
        purchaseDate: Date?,
        storage: String?,
        memo: String?
    ) {
        val currentItem = foodItem.value

        val itemToSave: FoodItem = if (currentItem != null && currentItem.id != 0) {
            currentItem.copy(
                name = name,
                expiryDate = expiryDate,
                quantity = quantity,
                category = category,
                purchaseDate = purchaseDate,
                storageLocation = storage,
                memo = memo,
                barcode = currentBarcode ?: currentItem.barcode
            )
        } else {
            FoodItem(
                id = 0,
                name = name,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                quantity = quantity,
                category = category,
                storageLocation = storage,
                memo = memo,
                barcode = currentBarcode
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
}
