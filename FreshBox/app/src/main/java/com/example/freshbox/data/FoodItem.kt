// File: app/src/main/java/com/example/freshbox/data/FoodItem.kt
package com.example.freshbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.Calendar
import java.util.Date
import androidx.room.TypeConverters

@Entity(tableName = "food_items")
@TypeConverters(Converters::class, StringListConverter::class)
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    var purchaseDate: Date?,
    var expiryDate: Date,
    var quantity: String = "1",
    var categoryId: Long?,
    var storageLocation: String? = null,
    var memo: String? = null,
    var isFrozen: Boolean = false,
    var tags: List<String> = emptyList(),
    var imagePath: String? = null // <<< 이 필드가 있는지 확인, 없다면 추가!
) {
    // ... (isExpiringSoon, isExpired 메서드) ...
    fun isExpiringSoon(daysThreshold: Int = 3): Boolean {
        val today = Calendar.getInstance()
        val expiry = Calendar.getInstance().apply { time = this@FoodItem.expiryDate }
        expiry.add(Calendar.DAY_OF_YEAR, -daysThreshold)
        return !this.isExpired() && today.time.after(expiry.time)
    }

    fun isExpired(): Boolean {
        val todayCal = Calendar.getInstance()
        val expiryCal = Calendar.getInstance().apply {
            this.time = this@FoodItem.expiryDate
            this.set(Calendar.HOUR_OF_DAY, 0); this.set(Calendar.MINUTE, 0); this.set(Calendar.SECOND, 0); this.set(Calendar.MILLISECOND, 0)
        }
        val currentCal = Calendar.getInstance().apply {
            this.set(Calendar.HOUR_OF_DAY, 0); this.set(Calendar.MINUTE, 0); this.set(Calendar.SECOND, 0); this.set(Calendar.MILLISECOND, 0)
        }
        return currentCal.timeInMillis > expiryCal.timeInMillis
    }
}

// 기존 Converters 클래스는 그대로 유지 (Date <-> Long)
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

// 태그 리스트를 위한 TypeConverter (새로 추가)
class StringListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}