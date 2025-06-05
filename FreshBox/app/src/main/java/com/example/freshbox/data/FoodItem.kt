// File: app/src/main/java/com/example/freshbox/data/FoodItem.kt
package com.example.freshbox.data // 이 파일이 속한 패키지를 정의합니다.

import androidx.room.Entity // Room Entity임을 나타내는 어노테이션
import androidx.room.PrimaryKey // 기본 키(Primary Key)를 지정하는 어노테이션
import androidx.room.TypeConverter // Room이 사용자 정의 타입을 데이터베이스에 저장 가능한 형태로 변환하는 방법을 알려주는 어노테이션
import java.util.Calendar // 날짜 계산(isExpiringSoon, isExpired)에 사용
import java.util.Date // 구매일(purchaseDate) 및 소비기한(expiryDate)의 타입으로 사용
import androidx.room.TypeConverters // 여러 TypeConverter 클래스를 Entity나 Database에 한 번에 등록하기 위한 어노테이션

// "@Entity" 어노테이션은 이 클래스가 Room 데이터베이스의 테이블과 매핑됨을 나타냅니다.
// "tableName" 속성은 데이터베이스 내의 실제 테이블 이름을 지정합니다.
@Entity(tableName = "food_items")
// "@TypeConverters" 어노테이션은 이 Entity가 사용할 TypeConverter 클래스들을 지정합니다.
// Converters는 Date <-> Long 변환을, StringListConverter는 List<String> <-> String 변환을 담당합니다.
@TypeConverters(Converters::class, StringListConverter::class)
// 식품 아이템 데이터를 나타내는 data class 입니다.
// data class는 equals(), hashCode(), toString(), copy() 등의 메서드를 자동으로 생성해줍니다.
data class FoodItem(
    // "@PrimaryKey" 어노테이션은 이 필드가 테이블의 기본 키임을 나타냅니다.
    // "autoGenerate = true"는 Room이 자동으로 ID 값을 생성하도록 합니다 (예: SQLite의 AUTOINCREMENT).
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 식품 아이템의 고유 ID (기본값 0, Room에 의해 자동 생성)

    var name: String, // 식품의 이름
    var purchaseDate: Date?, // 구매 날짜 (Nullable: 구매일을 모를 수도 있음)
    var expiryDate: Date, // 소비기한 날짜 (NonNull: 소비기한은 필수 정보로 가정)
    var quantity: String = "1", // 수량 (기본값 "1", 예: "1개", "200g" 등 문자열로 유연하게 관리)
    var categoryId: Long?, // 이 식품이 속한 카테고리의 ID (Category Entity 참조, Nullable: 카테고리 미지정 가능)
    var storageLocation: String? = null, // 보관 위치 (예: "냉장실", "냉동실", Nullable)
    var memo: String? = null, // 추가 메모 (Nullable)
    var isFrozen: Boolean = false, // 냉동 보관 여부 (기본값 false)
    var tags: List<String> = emptyList(), // 태그 목록 (기본값 빈 리스트)
    var imagePath: String? = null // 식품 이미지의 파일 경로 (Nullable)
) {
    // 소비기한이 임박했는지 확인하는 헬퍼 메서드
    // @param daysThreshold 임박 기준으로 간주할 남은 일수 (기본값 3일)
    // @return 소비기한이 지나지 않았고, daysThreshold 이내로 다가오면 true 반환
    fun isExpiringSoon(daysThreshold: Int = 3): Boolean {
        val today = Calendar.getInstance() // 현재 날짜와 시간
        val expiry = Calendar.getInstance().apply { time = this@FoodItem.expiryDate } // 이 FoodItem의 소비기한
        // 소비기한으로부터 daysThreshold만큼 이전 날짜 계산
        expiry.add(Calendar.DAY_OF_YEAR, -daysThreshold)
        // 소비기한이 아직 지나지 않았고(&&), 현재 날짜가 (소비기한 - daysThreshold) 날짜보다 이후이면 true (즉, 임박)
        return !this.isExpired() && today.time.after(expiry.time)
    }

    // 소비기한이 지났는지 확인하는 헬퍼 메서드
    // @return 소비기한 날짜의 자정 < 현재 날짜의 자정 이면 true 반환 (즉, 소비기한 당일 자정이 지나면 만료로 간주)
    fun isExpired(): Boolean {
        val todayCal = Calendar.getInstance() // 현재 날짜 Calendar 객체
        // 이 FoodItem의 소비기한 날짜를 기준으로 시간을 자정(00:00:00.000)으로 설정한 Calendar 객체
        val expiryCal = Calendar.getInstance().apply {
            this.time = this@FoodItem.expiryDate // FoodItem의 expiryDate 사용
            this.set(Calendar.HOUR_OF_DAY, 0); this.set(Calendar.MINUTE, 0); this.set(Calendar.SECOND, 0); this.set(Calendar.MILLISECOND, 0)
        }
        // 현재 날짜를 기준으로 시간을 자정으로 설정한 Calendar 객체
        val currentCal = Calendar.getInstance().apply {
            this.set(Calendar.HOUR_OF_DAY, 0); this.set(Calendar.MINUTE, 0); this.set(Calendar.SECOND, 0); this.set(Calendar.MILLISECOND, 0)
        }
        // 현재 날짜의 자정 타임스탬프가 소비기한 날짜의 자정 타임스탬프보다 크면 만료된 것으로 간주
        return currentCal.timeInMillis > expiryCal.timeInMillis
    }
}

// Room 데이터베이스가 Date 타입을 직접 저장할 수 없으므로, Long (타임스탬프) 타입으로 변환하기 위한 TypeConverter 클래스
class Converters {
    // Long 타입의 타임스탬프를 Date 객체로 변환하는 메서드
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        // value가 null이면 null을 반환하고, 아니면 Long 값을 사용하여 Date 객체 생성
        return value?.let { Date(it) }
    }

    // Date 객체를 Long 타입의 타임스탬프로 변환하는 메서드
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        // date가 null이면 null을 반환하고, 아니면 Date 객체의 time (밀리초 타임스탬프) 반환
        return date?.time
    }
}

// Room 데이터베이스가 List<String> 타입을 직접 저장할 수 없으므로, 단일 String으로 변환하기 위한 TypeConverter 클래스
// (예: 태그 목록을 쉼표로 구분된 문자열로 저장)
class StringListConverter {
    // 쉼표로 구분된 문자열을 List<String>으로 변환하는 메서드
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        // value가 null이면 null을 반환하고, 아니면 쉼표(,)를 기준으로 문자열을 분리하여 리스트 생성
        // 각 문자열의 앞뒤 공백을 제거하고, 비어있지 않은 문자열만 필터링
        return value?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    // List<String>을 쉼표로 구분된 단일 문자열로 변환하는 메서드
    @TypeConverter
    fun toString(list: List<String>?): String? {
        // list가 null이면 null을 반환하고, 아니면 리스트의 각 문자열을 쉼표(,)로 연결하여 단일 문자열 생성
        return list?.joinToString(",")
    }
}