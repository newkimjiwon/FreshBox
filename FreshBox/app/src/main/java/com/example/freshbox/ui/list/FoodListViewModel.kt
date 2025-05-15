package com.example.freshbox.ui.list

import android.app.Application
import androidx.lifecycle.*
import com.example.freshbox.data.AppDatabase
import com.example.freshbox.data.FoodItem
import com.example.freshbox.repository.FoodRepository
import kotlinx.coroutines.launch

enum class FoodFilterType { ALL, ACTIVE, EXPIRED, EXPIRING_SOON }

class FoodListViewModel(application: Application) : AndroidViewModel(application) {
    private val foodDao = AppDatabase.getDatabase(application).foodDao()
    private val repository = FoodRepository(foodDao)

    private val _filterType = MutableLiveData<FoodFilterType>(FoodFilterType.ACTIVE)

    private val allFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getAllFoodItemsSortedByExpiry()
    private val activeFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getActiveFoodItems()
    private val expiredFoodItemsFromRepo: LiveData<List<FoodItem>> = repository.getExpiredFoodItems()

    val filteredFoodItems = MediatorLiveData<List<FoodItem>>()
    val expiredFoodItems: LiveData<List<FoodItem>> = repository.getExpiredFoodItems()
    val expiringSoonFoodItems = MediatorLiveData<List<FoodItem>>()

    init {
        // expiringSoonFoodItems 연결
        expiringSoonFoodItems.addSource(activeFoodItemsFromRepo) { list ->
            expiringSoonFoodItems.value = list.filter { it.isExpiringSoon() }
        }

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
