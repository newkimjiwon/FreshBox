// File: app/src/main/java/com/example/freshbox/data/CategoryDao.kt
package com.example.freshbox.data // 이 파일이 속한 패키지를 정의합니다.

import androidx.lifecycle.LiveData // LiveData를 사용하여 데이터 변경을 UI에 자동으로 반영할 수 있도록 import 합니다.
import androidx.room.Dao // 이 인터페이스가 Data Access Object임을 Room에 알리는 어노테이션입니다.
import androidx.room.Delete // 데이터 삭제 작업을 위한 어노테이션입니다.
import androidx.room.Insert // 데이터 삽입 작업을 위한 어노테이션입니다.
import androidx.room.OnConflictStrategy // 데이터 삽입 시 충돌(예: 기본 키 중복)이 발생했을 때 처리 방법을 정의합니다.
import androidx.room.Query // 직접 SQL 쿼리를 작성하여 데이터베이스 작업을 수행하기 위한 어노테이션입니다.
import androidx.room.Update // 데이터 업데이트 작업을 위한 어노테이션입니다.

// @Dao 어노테이션은 이 인터페이스가 Room 데이터베이스의 DAO임을 나타냅니다.
// DAO는 데이터베이스에 접근하는 메서드들을 추상화하여 제공합니다.
@Dao
interface CategoryDao {

    // 새로운 Category 객체를 'categories' 테이블에 삽입합니다.
    // onConflict = OnConflictStrategy.IGNORE: 만약 삽입하려는 Category와 동일한 기본 키(또는 unique 인덱스 충돌)를 가진
    //                                       데이터가 이미 테이블에 있다면, 새로운 데이터 삽입을 무시합니다.
    // suspend fun: 이 함수는 코루틴 내에서 비동기적으로 실행되어야 함을 나타냅니다 (DB 작업은 UI 스레드에서 직접 수행하면 안 됨).
    // 반환 타입 Long: 성공적으로 삽입된 행의 ID (rowId)를 반환합니다. 충돌로 무시된 경우 특정 값(예: -1)을 반환할 수 있습니다. (선택 사항)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    // 여러 Category 객체들을 'categories' 테이블에 한 번에 삽입합니다.
    // 마찬가지로 OnConflictStrategy.IGNORE 전략을 사용합니다.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    // 기존 Category 객체의 정보를 'categories' 테이블에서 업데이트합니다.
    // 전달된 Category 객체의 기본 키(id)를 기준으로 해당 행을 찾아 업데이트합니다.
    @Update
    suspend fun updateCategory(category: Category)

    // 특정 Category 객체를 'categories' 테이블에서 삭제합니다.
    // 전달된 Category 객체의 기본 키(id)를 기준으로 해당 행을 찾아 삭제합니다.
    @Delete
    suspend fun deleteCategory(category: Category)

    // 'categories' 테이블의 모든 카테고리 정보를 이름(name) 오름차순으로 정렬하여 가져옵니다.
    // 반환 타입 LiveData<List<Category>>: 데이터베이스의 categories 테이블 내용이 변경될 때마다
    //                                    자동으로 새로운 List<Category>를 발행하여 UI가 관찰하고 업데이트할 수 있도록 합니다.
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    // 특정 ID를 가진 Category 정보를 'categories' 테이블에서 가져옵니다.
    // 반환 타입 LiveData<Category?>: 해당 ID의 카테고리가 있으면 Category 객체를, 없으면 null을 포함한 LiveData를 발행합니다.
    // @param id 조회할 카테고리의 ID.
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): LiveData<Category?>

    // 특정 이름을 가진 Category 정보를 'categories' 테이블에서 가져옵니다 (LIMIT 1을 사용하여 하나만).
    // suspend fun: 코루틴 내에서 비동기적으로 실행됩니다.
    // 반환 타입 Category?: 해당 이름의 카테고리가 있으면 Category 객체를, 없으면 null을 반환합니다.
    // 이 메서드는 주로 새 카테고리 추가 시 이름 중복을 방지하는 데 사용될 수 있습니다.
    // @param name 조회할 카테고리의 이름.
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
}