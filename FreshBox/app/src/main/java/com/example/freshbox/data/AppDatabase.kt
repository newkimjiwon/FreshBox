// File: app/src/main/java/com/example/freshbox/data/AppDatabase.kt
package com.example.freshbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Category Entity와 StringListConverter를 import합니다.
// StringListConverter가 FoodItem.kt 내에 있다면 별도 import는 필요 없을 수 있으나,
// AppDatabase 클래스에서 명시적으로 인식하려면 import하거나 Database 어노테이션에 포함해야 합니다.
// 여기서는 StringListConverter가 FoodItem.kt와 같은 패키지에 있다고 가정합니다.

@Database(entities = [FoodItem::class, Category::class], version = 2, exportSchema = false) // Category Entity 추가, 버전 2로 변경
@TypeConverters(Converters::class, StringListConverter::class) // StringListConverter 등록
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun categoryDao(): CategoryDao // CategoryDao 접근 메서드 추가

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 데이터베이스 버전 1에서 2로의 마이그레이션 정의
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. categories 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `isCustom` INTEGER NOT NULL DEFAULT 1
                    )
                """)
                // name 컬럼에 UNIQUE 인덱스 추가 (선택 사항이지만 권장)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")

                // 2. food_items 테이블 변경 (barcode 삭제, categoryId 및 tags 추가)
                // SQLite는 ALTER TABLE DROP COLUMN을 직접 지원하지 않는 경우가 많으므로,
                // 새 테이블을 만들고 데이터를 복사한 후 기존 테이블을 삭제하고 새 테이블 이름을 변경하는 방식을 사용합니다.

                // 2-1. 새 스키마로 임시 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `purchaseDate` INTEGER, 
                        `expiryDate` INTEGER NOT NULL, 
                        `quantity` TEXT NOT NULL DEFAULT '1', 
                        `categoryId` INTEGER, 
                        `storageLocation` TEXT, 
                        `memo` TEXT, 
                        `isFrozen` INTEGER NOT NULL DEFAULT 0,
                        `tags` TEXT NOT NULL DEFAULT '' 
                    )
                """)
                // `purchaseDate`, `expiryDate`는 Long(타임스탬프)으로 저장되므로 INTEGER 타입
                // `categoryId`는 Category 테이블의 id를 참조 (외래 키 제약 조건은 나중에 추가 고려)
                // `tags`는 TEXT 타입, 기본값으로 빈 문자열

                // 2-2. 기존 food_items 테이블에서 food_items_new 테이블로 데이터 복사
                // 기존 'category' (String) 컬럼은 새 'categoryId' (Long)로 직접 매핑하기 어려우므로,
                // 여기서는 categoryId를 NULL (또는 특정 기본값 ID)로 설정하고, tags는 빈 문자열로 초기화합니다.
                // barcode 컬럼은 새 테이블에 없으므로 복사되지 않아 자연스럽게 삭제됩니다.
                db.execSQL("""
                    INSERT INTO `food_items_new` (id, name, purchaseDate, expiryDate, quantity, storageLocation, memo, isFrozen, categoryId, tags)
                    SELECT id, name, purchaseDate, expiryDate, quantity, storageLocation, memo, isFrozen, NULL, '' 
                    FROM `food_items`
                """)

                // 2-3. 기존 food_items 테이블 삭제
                db.execSQL("DROP TABLE `food_items`")

                // 2-4. food_items_new 테이블 이름을 food_items로 변경
                db.execSQL("ALTER TABLE `food_items_new` RENAME TO `food_items`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refrigerator_app_db" // 데이터베이스 파일명은 유지
                )
                    .addMigrations(MIGRATION_1_2) // 마이그레이션 전략 추가
                    // .addCallback(AppDatabaseCallback(CoroutineScope(Dispatchers.IO))) // (선택사항) 초기 데이터 삽입용 콜백
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // (선택사항) 데이터베이스 생성 시 기본 카테고리 추가를 위한 콜백
        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialCategories(database.categoryDao())
                    }
                }
            }

            suspend fun populateInitialCategories(categoryDao: CategoryDao) {
                // 여기에 기본 카테고리들을 추가합니다.
                val defaultCategories = listOf(
                    Category(name = "과일", isCustom = false),
                    Category(name = "채소", isCustom = false),
                    Category(name = "육류", isCustom = false),
                    Category(name = "유제품", isCustom = false),
                    Category(name = "음료", isCustom = false),
                    Category(name = "기타", isCustom = false)
                )
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }
}