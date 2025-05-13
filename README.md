# 🧊 FreshBox

FreshBox는 냉장고 속 식품을 체계적으로 관리할 수 있는 Android 애플리케이션입니다. 소비기한이 지난 식품을 줄이고, 구매한 식품을 효율적으로 소비하도록 돕습니다.

## 📱 주요 기능

- **식품 등록/수정/삭제**
  - 식품 이름, 수량, 소비기한, 구매일, 카테고리, 보관 위치, 메모 입력 가능
- **바코드 스캔 등록**
  - 바코드 스캔을 통한 빠른 등록 및 기존 식품 자동 조회
- **소비기한 필터링**
  - `전체`, `소비기한 임박`, `소비기한 지남`, `현재 유효` 식품 필터링
- **자동 색상 표시**
  - 남은 일수에 따라 색상으로 상태 시각화 (녹색/노랑/빨강)
- **스와이프 삭제 및 Undo**
  - 스와이프로 항목 삭제 및 Snackbar로 되돌리기 가능

## 🧩 기술 스택

| 기술 | 설명 |
|------|------|
| Kotlin | 안드로이드 기본 개발 언어 |
| MVVM | Android Architecture Pattern |
| Room | SQLite 기반의 로컬 DB 관리 |
| LiveData & ViewModel | 반응형 데이터 처리 및 생명주기 관리 |
| Material UI | Google Material Design 적용 |
| ZXing | 바코드 스캔 기능 활용 |

## 🗂️ 프로젝트 구조

com.example.freshbox  <br/>
├── data/ # Room Entity, DAO, DB 정의 <br/>
├── repository/ # Repository (데이터 처리 로직) <br/>
├── ui/  <br/>
│ ├── list/ # 메인 식품 리스트 화면 (MainActivity, ViewModel, Adapter)  <br/>
│ └── addedit/ # 식품 추가/수정 화면  <br/>
├── util/ # 공통 유틸리티 (SingleLiveEvent 등)  <br/>
└── MyApplication.kt # 앱 전체 초기화  <br/>


## ⚙️ 빌드 및 실행 방법
1. Android Studio에서 프로젝트 열기  
2. `build.gradle.kts` 동기화  
3. ZXing 바코드 스캔 앱 설치 필요 (외부 인텐트 사용)  
4. 에뮬레이터 또는 실기기에서 실행  

## 🧪 향후 개발 예정
- 알림 기능: 소비기한 임박 알림  
- OCR 기능: 유통기한 자동 인식  
- 클라우드 동기화 및 다중 기기 지원  

## 📸 스크린샷
> 여기에 `screenshots/` 폴더 이미지 추가  
> 예시:  
> - `screenshots/food_list.png`  
> - `screenshots/add_food.png`  

## 📄 라이선스
이 프로젝트는 MIT 라이선스를 따릅니다. 자세한 내용은 `LICENSE` 파일을 참고해주세요.

---

> **Made with ❤️ for efficient food management**
