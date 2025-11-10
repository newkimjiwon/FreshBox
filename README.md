# FreshBox

**FreshBox**는 냉장고 및 보관함 속 식품을 체계적으로 관리할 수 있는 Android 애플리케이션입니다.  
식품의 **구매일**, **소비기한** 등을 기록하여 **효율적인 식품 소비**를 도와줍니다.


## 주요 기능

### 식품 등록/수정/삭제
- 이름, 수량, 소비기한, 구매일, 카테고리, 보관 위치, 메모, 이미지, 냉동 여부, 태그 입력 가능
- `Room` DB 사용
- `AddFoodBottomSheetFragment`를 통해 UI 제공

### 식품 목록 표시
- `HomeFragment`: 소비기한 **임박 식품** 및 **만료 식품**을 **가로 스크롤**로 구분 표시
- `AllFoodsActivity`: 전체 식품 목록 확인

### 캘린더 뷰
- `CalendarFragment`에서 **월별 달력** 제공
- 날짜별로 **구매일**, **소비기한** 여부를 마커로 표시
- 날짜 클릭 시 해당 식품 목록 하단에 표시, 상세정보/수정/삭제 가능

### 검색 및 필터링
- **이름, 태그, 카테고리 이름**으로 검색 가능
- **카테고리별 필터링** (전체 포함)

### 테마 변경 (라이트 / 다크 모드)
- `설정 아이콘` 클릭 시 테마 변경 가능 (라이트 / 다크 / 시스템 설정 따름)
- 선택된 테마는 앱 재시작 후에도 유지 (`SharedPreferences`)

### 소비기한 D-day 표시
- 식품 목록 항목에 **남은 일수(D-day)** 또는 **지나간 일수** 색상과 함께 표시

### 스와이프 삭제
- `AllFoodsActivity`에서 항목 **좌우 스와이프**로 삭제

### 유통기한 만료 알림 (WorkManager)
- 매일 정해진 시간에 유통기한이 **오늘 만료되는 식품**을 알림으로 안내
- 알림 클릭 시 앱 실행

## 사용 기술 스택

| 기술 | 설명 |
|------|------|
| **Kotlin** | Android 앱 개발 주언어 |
| **Android Jetpack** | MVVM, Room, ViewModel, LiveData, WorkManager 등 |
| MVVM Architecture | UI와 로직 분리 |
| Room | SQLite 기반 ORM, 식품/카테고리 저장 |
| ViewModel / LiveData | 수명주기 고려한 상태 관리 |
| WorkManager | 신뢰성 높은 백그라운드 작업 수행 (알림 등) |
| **Coroutines** | 비동기 처리 |
| **Material Components** | UI 구성 (BottomSheet, TextInputLayout 등) |
| **ViewBinding** | View 접근 안전성 확보 |
| **RecyclerView** | 목록 UI 구성 |
| **Kizitonwose CalendarView** | 커스터마이징 가능한 달력 라이브러리 |
| **SharedPreferences** | 사용자 설정 (테마 등) 저장 |


## 프로젝트 구조

com.example.freshbox/ <br/>
├── data/ # Entity, DAO, DB, TypeConverters <br/>
├── databinding/ # ViewBinding (자동 생성) <br/>
├── model/ # (이전 JSON 방식 - 현재는 미사용) <br/>
├── repository/ # FoodRepository <br/>
├── ui/ # UI 관련 Activity, Fragment, Adapter 등 <br/>
│ ├── addedit/ # AddFoodBottomSheetFragment 등 <br/>
│ ├── all/ # AllFoodsActivity <br/> 
│ ├── calendar/ # CalendarFragment <br/>
│ ├── list/ # HomeFragment, ViewModel, Adapter <br/>
│ └── splash/ # SplashActivity <br/>
├── util/ # 공통 유틸 (ThemeHelper, NotificationHelper 등) <br/>
├── worker/ # ExpiryCheckWorker (WorkManager) <br/> 
└── MyApplication.kt # 앱 초기화, 테마/알림 채널 등록 등 <br/>


## 빌드 및 실행 방법

- **Android Studio 버전**: `2023.2.1 Patch 1 (Iguana)` 또는 이후 버전 추천
- SDK & Gradle 도구 설치 확인 후 **Sync Now**
- 에뮬레이터 또는 실제 기기(API 26+)에서 실행
- Android 13(API 33+) 이상에서는 알림 권한 허용 필요

## 향후 개선 방향

- `CalendarFragment`: 클릭 시 하단 식품 리스트 표시 + 수정/삭제 기능 (일부 구현됨)
- 목록 및 상세정보에서 **태그/카테고리 이름** 더 명확하게 표시
- **알림 클릭 → 만료된 식품 목록 화면 직접 이동**
- 알림 시간, 반복 주기 사용자 설정 기능
- Android 13+ 알림 권한 요청 UI 개선
- 전반적인 디자인 개선, 애니메이션 추가
- 유효성 검사 강화, 오류 처리 보완
- 단위 테스트, UI 테스트 추가
- 바코드 스캔 (ML Kit), OCR, 클라우드 동기화 등 고도화 계획
