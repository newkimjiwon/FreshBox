package com.example.freshbox.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem
import com.example.freshbox.repository.FoodRepository
import kotlinx.coroutines.launch

enum class FoodFilterType { ALL, ACTIVE, EXPIRED, EXPIRING_SOON }

class FoodListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FoodRepository
    private val _filterType = MutableLiveData<FoodFilterType>(FoodFilterType.ACTIVE)

    private val allFoodItemsFromRepo: LiveData<List<FoodItem>>
    private val activeFoodItemsFromRepo: LiveData<List<FoodItem>>
    private val expiredFoodItemsFromRepo: LiveData<List<FoodItem>>

    val filteredFoodItems = MediatorLiveData<List<FoodItem>>()

    init {
        val foodDao = AppDatabase.getDatabase(application).foodDao()
        repository = FoodRepository(foodDao)

        allFoodItemsFromRepo = repository.getAllFoodItemsSortedByExpiry()
        activeFoodItemsFromRepo = repository.getActiveFoodItems()
        expiredFoodItemsFromRepo = repository.getExpiredFoodItems()

        filteredFoodItems.addSource(_filterType) { filter ->
            updateFilteredData(filter, activeFoodItemsFromRepo.value)
        }
        filteredFoodItems.addSource(activeFoodItemsFromRepo) { activeItems ->
            updateFilteredData(_filterType.value, activeItems)
        }
        filteredFoodItems.addSource(allFoodItemsFromRepo) { allItems ->
            if (_filterType.value == FoodFilterType.ALL) {
                filteredFoodItems.value = allItems ?: emptyList()
            }
        }
        filteredFoodItems.addSource(expiredFoodItemsFromRepo) { expiredItems ->
            if (_filterType.value == FoodFilterType.EXPIRED) {
                filteredFoodItems.value = expiredItems ?: emptyList()
            }
        }
        updateFilteredData(_filterType.value, activeFoodItemsFromRepo.value)
    }

    private fun updateFilteredData(filterType: FoodFilterType?, currentActiveItems: List<FoodItem>?) {
        when (filterType) {
            FoodFilterType.ALL -> {
                if (filteredFoodItems.value != allFoodItemsFromRepo.value) {
                    filteredFoodItems.value = allFoodItemsFromRepo.value ?: emptyList()
                }
            }
            FoodFilterType.ACTIVE -> {
                if (filteredFoodItems.value != activeFoodItemsFromRepo.value) {
                    filteredFoodItems.value = activeFoodItemsFromRepo.value ?: emptyList()
                }
            }
            FoodFilterType.EXPIRED -> {
                if (filteredFoodItems.value != expiredFoodItemsFromRepo.value) {
                    filteredFoodItems.value = expiredFoodItemsFromRepo.value ?: emptyList()
                }
            }
            FoodFilterType.EXPIRING_SOON -> {
                filteredFoodItems.value = currentActiveItems?.filter { it.isExpiringSoon() } ?: emptyList()
            }
            null -> {
                filteredFoodItems.value = currentActiveItems ?: emptyList()
            }
        }
    }

    fun setFilter(filterType: FoodFilterType) {
        if (_filterType.value != filterType) {
            _filterType.value = filterType
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) = viewModelScope.launch {
        repository.deleteFoodItem(foodItem)
    }
}