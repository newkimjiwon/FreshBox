package com.example.freshbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FoodItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Date <-> Long 변환기 등록
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refrigerator_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}