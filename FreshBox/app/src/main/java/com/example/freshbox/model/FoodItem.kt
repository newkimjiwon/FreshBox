// File: app/src/main/java/com/example/freshbox/model/FoodItem.kt
package com.example.freshbox.model // 이 클래스가 속한 패키지를 정의합니다.

import java.io.Serializable // 객체를 직렬화(데이터 스트림으로 변환)할 수 있도록 Serializable 인터페이스를 import 합니다.
// 이를 통해 Intent를 통해 객체를 전달하거나, 파일에 저장하는 등의 작업이 가능해집니다.

/**
 * 애플리케이션의 UI 계층 또는 도메인 로직에서 사용될 식품 아이템을 나타내는 데이터 클래스입니다.
 * 데이터베이스 Entity와는 별개로 UI 표시에 적합한 형태로 데이터를 관리하거나,
 * 다른 컴포넌트로 데이터를 전달하는 용도로 사용될 수 있습니다.
 * 모든 필드가 val로 선언되어 불변(immutable) 객체로 설계되었습니다.
 *
 * @property name 식품의 이름 (String 타입).
 * @property quantity 식품의 수량 (String 타입, 예: "1개", "200g").
 * @property category 식품의 카테고리 (String 타입, 예: "과일", "채소").
 * @property storageLocation 식품의 보관 위치 (String 타입, 예: "냉장실", "팬트리").
 * @property memo 식품에 대한 추가 메모 (String 타입).
 * @property purchaseDate 구매 날짜 (String 타입, 예: "2023-10-27"). UI 표시용으로 포맷팅된 문자열.
 * @property expiryDate 소비기한 날짜 (String 타입, 예: "2023-11-10"). UI 표시용으로 포맷팅된 문자열.
 * @property imagePath 식품 이미지의 파일 경로 (String 타입).
 */
data class FoodItem(
    val name: String,
    val quantity: String,
    val category: String,
    val storageLocation: String,
    val memo: String,
    val purchaseDate: String,
    val expiryDate: String,
    val imagePath: String
) : Serializable // Serializable 인터페이스를 구현하여 이 객체가 직렬화 가능함을 나타냅니다.