// File: app/src/main/java/com/example/freshbox/data/Category.kt
package com.example.freshbox.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Category.kt
@Entity(tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]) // <<< 인덱스 정의 추가
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(collate = ColumnInfo.NOCASE) // 대소문자 구분 없는 UNIQUE를 위해 NOCASE 사용 가능
    val name: String,
    val isCustom: Boolean = true
)