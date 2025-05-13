package com.example.freshbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar // java.util.Calendar import 확인
import java.util.Date
import androidx.room.TypeConverter // <--- 이 import가 있는지 확인하세요!

@Entity(tableName = "food_items")
data class FoodItem(
    // ... 다른 필드들은 동일 ...
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    var purchaseDate: Date?,
    var expiryDate: Date,
    var quantity: String = "1",
    var category: String? = null,
    var storageLocation: String? = null,
    var memo: String? = null,
    var barcode: String? = null,
    var isFrozen: Boolean = false
) {
    // isExpiringSoon 메서드 (필요시 this 추가)
    fun isExpiringSoon(daysThreshold: Int = 3): Boolean {
        val today = Calendar.getInstance()
        val expiry = Calendar.getInstance().apply { time = this@FoodItem.expiryDate } // 명확하게 FoodItem의 expiryDate 참조
        expiry.add(Calendar.DAY_OF_YEAR, -daysThreshold)
        return !this.isExpired() && today.time.after(expiry.time) // this. 추가
    }


    // isExpired 메서드 수정
    fun isExpired(): Boolean {
        val todayCal = Calendar.getInstance()

        // 소비기한 날짜의 자정
        val expiryCal = Calendar.getInstance().apply {
            this.time = this@FoodItem.expiryDate // 명확하게 FoodItem의 expiryDate 참조
            this.set(Calendar.HOUR_OF_DAY, 0)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
        }

        // 오늘 날짜의 자정
        val currentCal = Calendar.getInstance().apply {
            this.set(Calendar.HOUR_OF_DAY, 0)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
        }
        // 오늘 자정이 소비기한 자정보다 이후인지 확인
        return currentCal.timeInMillis > expiryCal.timeInMillis
    }
}

// Room이 Date 타입을 저장할 수 있도록 TypeConverter 추가
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