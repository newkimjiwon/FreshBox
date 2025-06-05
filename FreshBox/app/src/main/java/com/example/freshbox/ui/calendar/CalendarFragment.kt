// File: app/src/main/java/com/example/freshbox/ui/calendar/CalendarFragment.kt
package com.example.freshbox.ui.calendar

import android.app.Activity // ActivityResultLauncher 콜백에서 Activity.RESULT_OK 사용 시 필요할 수 있음 (현재 코드에서는 직접 사용 X)
import android.app.AlertDialog // 사용자에게 확인/선택을 받기 위한 다이얼로그
import android.content.Intent // 다른 Activity 실행 또는 데이터 전달 시 사용 (현재 코드에서는 직접 사용 X)
import android.graphics.Bitmap // 이미지 비트맵 객체
import android.graphics.BitmapFactory // 파일 경로로부터 비트맵 이미지를 디코딩하기 위해 사용
import android.os.Build // 안드로이드 OS 버전 확인 시 사용 (현재 코드에서는 직접 사용 X)
import android.os.Bundle
import android.util.Log // 디버깅 로그 출력
import android.view.LayoutInflater // XML 레이아웃 파일을 View 객체로 inflate 하기 위해 사용
import android.view.View // UI의 기본 구성 요소
import android.view.ViewGroup // View들의 컨테이너
import android.widget.ImageView // 이미지 표시를 위한 View
import android.widget.TextView // 텍스트 표시를 위한 View
import android.widget.Toast // 간단한 메시지를 사용자에게 잠깐 보여주기 위해 사용
import androidx.activity.result.ActivityResult // ActivityResultLauncher의 결과 타입 (현재 코드에서는 직접 사용 X)
import androidx.activity.result.ActivityResultLauncher // Activity 실행 및 결과 처리를 위한 최신 API (현재 코드에서는 선언만 됨)
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultLauncher의 표준 계약 정의 (현재 코드에서는 직접 사용 X)
import androidx.core.content.ContextCompat // 리소스(색상, 드로어블 등) 접근 시 테마 호환성 제공
import androidx.core.view.isVisible // View의 visibility를 코틀린스럽게 제어 (예: view.isVisible = true)
import androidx.fragment.app.Fragment // Fragment 기본 클래스
import androidx.fragment.app.viewModels // KTX를 사용한 ViewModel 초기화
import androidx.lifecycle.Observer // LiveData의 변경 사항을 관찰
import androidx.recyclerview.widget.LinearLayoutManager // RecyclerView의 아이템들을 선형으로 배치
import com.example.freshbox.R // 프로젝트 리소스(layout, drawable, string, color 등) ID 참조
import com.example.freshbox.data.Category // Category 데이터 Entity (allCategoriesMap에서 사용)
import com.example.freshbox.data.FoodItem // FoodItem 데이터 Entity
import com.example.freshbox.databinding.FragmentCalendarBinding // ViewBinding 클래스 (fragment_calendar.xml 기반)
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment // 식품 추가/수정 BottomSheet
import com.example.freshbox.ui.list.FoodListAdapter // 식품 목록을 표시하는 RecyclerView 어댑터
import com.example.freshbox.ui.list.FoodListViewModel // UI 로직 및 데이터 관리를 위한 ViewModel
import com.kizitonwose.calendar.core.CalendarDay // Kizitonwose 캘린더의 특정 날짜 객체
import com.kizitonwose.calendar.core.CalendarMonth // Kizitonwose 캘린더의 특정 월 객체
import com.kizitonwose.calendar.core.DayPosition // 날짜가 현재 월에 속하는지 등의 위치 정보
import com.kizitonwose.calendar.view.MonthDayBinder // 각 날짜 셀(Day)을 어떻게 만들고 바인딩할지 정의
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder // 각 월의 헤더(요일 표시줄)를 어떻게 만들고 바인딩할지 정의
import com.kizitonwose.calendar.view.ViewContainer // Kizitonwose 캘린더의 각 셀을 위한 ViewHolder 기본 클래스
import java.io.File // 파일 시스템의 파일 객체 (이미지 로드)
import java.io.FileOutputStream // 파일에 데이터를 쓰기 위해 사용 (DayViewContainer의 saveBitmapToFile에서 사용될 수 있었으나 현재 코드에는 없음)
import java.text.SimpleDateFormat // Date 객체를 특정 문자열 형식으로 포맷팅 (상세 정보 다이얼로그용)
import java.time.DayOfWeek // 요일 (월, 화, 수...)을 나타내는 Enum
import java.time.LocalDate // 날짜(연,월,일) 정보를 다루는 Java 8 시간 API 클래스
import java.time.YearMonth // 연도와 월 정보를 다루는 Java 8 시간 API 클래스
import java.time.ZoneId // 시간대 정보를 다루기 위해 사용 (Date <-> LocalDate 변환)
import java.time.format.DateTimeFormatter // 날짜/시간을 특정 패턴의 문자열로 포맷팅 (월/년도 표시용)
import java.time.format.TextStyle // 요일 이름을 짧은 형태(예: "월")로 표시하기 위해 사용
import java.time.temporal.WeekFields // 로케일에 따른 주의 첫 번째 요일 정보를 가져오기 위해 사용
import java.util.Date // 날짜와 시간을 나타내는 Java 기본 클래스 (FoodItem Entity에서 사용)
import java.util.Locale // 지역(국가 및 언어) 정보를 나타냄 (날짜 포맷팅, 요일 표시 등)

// 캘린더 화면을 담당하는 Fragment
class CalendarFragment : Fragment() {

    // ViewBinding을 위한 프로퍼티. onDestroyView에서 null로 설정하여 메모리 누수 방지.
    private var _binding: FragmentCalendarBinding? = null
    // _binding이 null이 아님을 보장하는 getter. onCreateView 이후부터 onDestroyView 이전까지 유효.
    private val binding get() = _binding!!

    // FoodListViewModel 인스턴스를 KTX의 by viewModels() 델리게이트를 사용하여 가져옴.
    // Fragment의 생명주기에 맞춰 ViewModel이 관리됨.
    private val viewModel: FoodListViewModel by viewModels()

    // 캘린더에 구매일과 소비기한을 마킹하기 위한 LocalDate 세트.
    private var purchaseDates: Set<LocalDate> = emptySet()
    private var expiryDates: Set<LocalDate> = emptySet()
    private val today = LocalDate.now() // 오늘 날짜 (캘린더 스타일링에 사용).

    // 캘린더 하단에 선택된 날짜의 식품 목록을 표시할 RecyclerView의 어댑터.
    private lateinit var calendarItemsAdapter: FoodListAdapter
    // 식품 상세 정보 다이얼로그에서 날짜를 "yyyy-MM-dd" 형식으로 표시하기 위한 포맷터.
    private val dateFormatDisplay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 카테고리 ID와 이름을 매핑하기 위한 Map (상세 정보 다이얼로그에서 카테고리 이름 표시용).
    // viewModel.allCategories를 관찰하여 업데이트됨.
    private var allCategoriesMap: Map<Long, String> = emptyMap()

    // ActivityResultLauncher 선언. 현재 코드에서는 onCreate에서 초기화 부분이 주석 처리되어 있음.
    // 만약 이 Fragment 또는 상세 다이얼로그에서 사진 촬영/선택 기능을 사용한다면 초기화 및 사용 필요.
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>


    // Fragment가 처음 생성될 때 호출 (View 생성 전).
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ActivityResultLauncher 초기화 (현재는 주석 처리됨).
        // 이 Fragment에서 직접 카메라를 사용하지 않는다면 이 부분은 필요 없음.
        /*
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // ... 사진 촬영 결과 처리 로직 ...
                }
            }
        */
    }

    // Fragment의 View를 생성하고 반환하는 단계.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // FragmentCalendarBinding을 사용하여 fragment_calendar.xml 레이아웃을 inflate.
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        // inflate된 레이아웃의 루트 View 반환.
        return binding.root
    }

    // Fragment의 View가 성공적으로 생성된 후 호출되는 단계.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 관련 초기화 함수들 호출.
        setupCalendarItemsRecyclerView() // 하단 RecyclerView 설정.
        observeViewModel()               // ViewModel의 LiveData 관찰 시작.

        // 캘린더 설정.
        val daysOfWeek = daysOfWeekFromLocale() // 현재 로케일에 맞는 요일 순서 가져오기.
        val currentMonth = YearMonth.now()      // 현재 연월.
        val startMonth = currentMonth.minusMonths(12) // 캘린더 표시 시작 범위 (현재로부터 1년 전).
        val endMonth = currentMonth.plusMonths(12)   // 캘린더 표시 종료 범위 (현재로부터 1년 후).

        // Kizitonwose CalendarView의 각 날짜 셀(Day)을 어떻게 만들고 데이터를 바인딩할지 정의.
        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            // 새로운 날짜 셀 View가 필요할 때 호출되어 DayViewContainer 인스턴스 생성.
            // DayViewContainer 생성 시, 날짜 셀 클릭 시 실행될 람다 함수를 전달.
            override fun create(view: View): DayViewContainer = DayViewContainer(view) { calendarDay ->
                Log.d("CalendarFragment", "Date clicked: ${calendarDay.date}")
                // ViewModel에 선택된 날짜에 만료되는 식품 목록을 로드하도록 요청.
                viewModel.loadItemsExpiringOnDate(calendarDay.date)
            }
            // 날짜 셀 View에 실제 날짜 데이터(calendarDay)를 바인딩(연결)할 때 호출.
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.bind(day) // DayViewContainer의 bind 메서드 호출하여 UI 업데이트.
            }
        }

        // Kizitonwose CalendarView의 각 월 헤더(요일 표시줄)를 어떻게 만들고 바인딩할지 정의.
        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> { // 타입 명시.
            // 새로운 월 헤더 View가 필요할 때 호출되어 MonthViewContainer 인스턴스 생성.
            override fun create(view: View) = MonthViewContainer(view)
            // 월 헤더 View에 실제 월 정보(data: CalendarMonth)를 바인딩할 때 호출.
            override fun bind(container: MonthViewContainer, data: CalendarMonth) { // 타입 명시.
                container.bind(data) // MonthViewContainer의 bind 메서드 호출하여 요일 텍스트 설정.
            }
        }

        // CalendarView 최종 설정 (시작월, 종료월, 주의 첫 요일).
        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        // CalendarView가 그려진 후, 현재 월로 스크롤하고 상단 텍스트 업데이트.
        binding.calendarView.post {
            binding.calendarView.scrollToMonth(currentMonth)
            updateMonthYearText(currentMonth) // 초기 월/년도 텍스트 설정.
        }

        // 캘린더 월 스크롤 시 상단 월/년도 텍스트 업데이트 리스너.
        binding.calendarView.monthScrollListener = { calendarMonth -> // calendarMonth는 CalendarMonth 객체.
            updateMonthYearText(calendarMonth.yearMonth) // YearMonth 객체 전달.
        }

        // 이전 달 버튼 클릭 리스너.
        binding.buttonPrevMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let { // 현재 보이는 첫 번째 월 정보를 가져옴.
                binding.calendarView.scrollToMonth(it.yearMonth.minusMonths(1)) // 이전 월로 스크롤.
            }
        }

        // 다음 달 버튼 클릭 리스너.
        binding.buttonNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.plusMonths(1)) // 다음 월로 스크롤.
            }
        }
    }

    // 캘린더 하단에 위치할 RecyclerView (선택된 날짜의 식품 목록 표시용)를 설정하는 함수.
    private fun setupCalendarItemsRecyclerView() {
        // FoodListAdapter 초기화 (아이템 클릭/롱클릭 리스너 전달).
        calendarItemsAdapter = FoodListAdapter(
            onItemClick = { foodItem ->
                showCalendarFoodDetailDialog(foodItem) // 아이템 클릭 시 상세 정보 다이얼로그 표시.
            },
            onItemLongClick = { foodItem ->
                showCalendarDeleteConfirmationDialog(foodItem) // 아이템 롱클릭 시 삭제 확인 다이얼로그 표시.
            }
        )
        // RecyclerView에 LayoutManager와 Adapter 연결.
        // binding.recyclerViewCalendarItems는 fragment_calendar.xml에 정의된 RecyclerView ID.
        binding.recyclerViewCalendarItems.apply {
            layoutManager = LinearLayoutManager(requireContext()) // 수직 리스트 형태.
            adapter = calendarItemsAdapter
        }
    }

    // ViewModel의 LiveData들을 관찰하여 UI를 업데이트하는 함수.
    private fun observeViewModel() {
        // 1. ViewModel의 전체 카테고리 목록(allCategories) 관찰.
        // (상세 정보 다이얼로그에서 카테고리 ID를 이름으로 변환하여 표시하기 위해 사용).
        viewModel.allCategories.observe(viewLifecycleOwner, Observer { categories: List<Category>? -> // 타입 명시.
            allCategoriesMap = categories?.associateBy({ it.id }, { it.name }) ?: emptyMap() // ID-이름 Map 생성.
        })

        // 2. ViewModel의 전체 식품 목록(allFoodItemsForCalendar) 관찰.
        // (캘린더의 각 날짜에 구매일 또는 소비기한에 해당하는 아이템이 있는지 마킹하기 위해 사용).
        viewModel.allFoodItemsForCalendar.observe(viewLifecycleOwner, Observer { foodItemsList: List<FoodItem>? -> // 타입 명시.
            foodItemsList?.let { items -> // Nullable 리스트 처리.
                Log.d("CalendarFragment", "Observed allFoodItemsForCalendar. Count: ${items.size}")
                // 식품 목록에서 구매일과 소비기한 날짜 세트를 추출.
                val (purchases, expiries) = extractDatesFromFoodItems(items)
                purchaseDates = purchases // Fragment 멤버 변수 업데이트.
                expiryDates = expiries   // Fragment 멤버 변수 업데이트.
                binding.calendarView.notifyCalendarChanged() // 캘린더 View에 변경 사항을 알려 다시 그리도록 함 (날짜 마커 업데이트).
            }
        })

        // 3. ViewModel의 선택된 날짜에 해당하는 식품 목록(itemsForCalendarSelectedDate) 관찰.
        viewModel.itemsForCalendarSelectedDate.observe(viewLifecycleOwner, Observer { itemsForDate: List<FoodItem>? -> // 타입 명시.
            itemsForDate?.let { items -> // Nullable 리스트 처리.
                Log.d("CalendarFragment", "Items for selected date: ${items.joinToString { it.name }}")
                calendarItemsAdapter.submitList(items) // 하단 RecyclerView 어댑터에 새 목록 제출.

                // 목록 유무에 따라 "아이템 없음" 메시지 및 RecyclerView의 visibility 조절.
                // (fragment_calendar.xml에 textViewCalendarNoItems와 recyclerViewCalendarItems ID가 있어야 함).
                binding.textViewCalendarNoItems.isVisible = items.isEmpty()
                binding.recyclerViewCalendarItems.isVisible = items.isNotEmpty()
            } ?: run { // itemsForDate가 null일 경우 (예: 초기 상태 또는 데이터 로드 실패).
                calendarItemsAdapter.submitList(emptyList()) // 빈 리스트로 설정.
                binding.textViewCalendarNoItems.isVisible = true // "아이템 없음" 메시지 표시.
                binding.recyclerViewCalendarItems.isVisible = false // RecyclerView 숨김.
            }
        })
    }

    // java.util.Date 객체를 java.time.LocalDate 객체로 변환하는 확장 함수.
    private fun Date.toLocalDate(): LocalDate {
        // Date 객체를 Instant로 변환 후, 시스템 기본 시간대를 사용하여 LocalDate로 변환.
        return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // List<FoodItem> (Room Entity)으로부터 구매일 세트와 소비기한일 세트를 추출하는 함수.
    private fun extractDatesFromFoodItems(foodItems: List<FoodItem>): Pair<Set<LocalDate>, Set<LocalDate>> {
        // purchaseDate는 nullable이므로 toLocalDate() 호출 전에 null 체크.
        val purchases = foodItems.mapNotNull { it.purchaseDate?.toLocalDate() }.toSet()
        // expiryDate는 NonNull이라고 가정 (FoodItem Entity 정의에 따라 다름).
        val expiries = foodItems.map { it.expiryDate.toLocalDate() }.toSet()
        return purchases to expiries // Pair(구매일 세트, 소비기한일 세트) 반환.
    }

    // 상단의 월/년도 텍스트를 업데이트하는 함수.
    private fun updateMonthYearText(yearMonth: YearMonth?) {
        yearMonth?.let {
            // "yyyy년 MMMM" 형식으로 포맷 (예: "2025년 6월").
            val formatter = DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREAN) // 한국어 로케일 사용.
            binding.textViewMonthYear.text = it.format(formatter)
        }
    }

    // Kizitonwose CalendarView의 각 날짜 셀을 위한 ViewContainer 클래스 (inner class).
    // 이 클래스는 item_calendar_day.xml 레이아웃의 View들을 관리하고 데이터를 바인딩합니다.
    inner class DayViewContainer(view: View, private val onDateClick: (CalendarDay) -> Unit) : ViewContainer(view) {
        // view는 item_calendar_day.xml의 루트 레이아웃입니다.
        // 해당 XML 파일에 calendarDayText ID와 dayItemIndicator ID가 정의되어 있어야 합니다.
        val dayText: TextView = view.findViewById(R.id.calendarDayText) // 날짜 숫자 표시용 TextView.
        val dayItemIndicator: View? = view.findViewById(R.id.dayItemIndicator) // 이벤트 표시용 인디케이터 View (null일 수 있음).

        private var currentDayInternal: CalendarDay? = null // 현재 ViewHolder에 바인딩된 날짜 정보 저장.

        init {
            // 날짜 셀 전체(view 또는 itemView)에 클릭 리스너 설정.
            // itemView은 ViewContainer의 프로퍼티로, 생성자에서 받은 view와 동일.
            // 이전 답변에서 itemView 관련 오류 해결을 위해 view를 직접 사용하도록 수정했었음.
            view.setOnClickListener {
                currentDayInternal?.let { // 현재 바인딩된 날짜가 있다면.
                    if (it.position == DayPosition.MonthDate) { // 현재 표시된 달의 날짜일 경우에만.
                        onDateClick(it) // Fragment로 클릭된 날짜(CalendarDay) 정보 전달.
                    }
                }
            }
        }

        // 특정 날짜(day: CalendarDay)의 데이터를 View에 바인딩하는 함수.
        fun bind(day: CalendarDay) {
            currentDayInternal = day // 클릭 시 사용하기 위해 현재 날짜 정보 저장.
            dayText.text = day.date.dayOfMonth.toString() // TextView에 날짜 숫자 설정.

            // --- 날짜 셀 스타일링 초기화 ---
            dayText.background = null // 배경 초기화.
            // 기본 텍스트 색상 설정. values/colors.xml 및 values-night/colors.xml에 정의된
            // R.color.calendar_day_text_default (또는 유사한 이름)를 참조하도록 합니다.
            // item_calendar_day.xml의 TextView에 android:textColor="?attr/colorOnSurface"를 사용하는 것이 더 좋음.
            dayText.setTextColor(ContextCompat.getColor(view.context, R.color.calendar_text_default))
            dayItemIndicator?.isVisible = false // 인디케이터 기본 숨김.
            view.isClickable = false // 기본적으로 클릭 불가능 (MonthDate일 때만 true로 변경).

            // 현재 달에 속한 날짜들에 대해서만 특별한 스타일링 및 클릭 가능 설정.
            if (day.position == DayPosition.MonthDate) {
                view.isClickable = true // 클릭 가능하도록 설정.

                // 해당 날짜에 구매 또는 소비기한 이벤트가 있는지 확인.
                val showPurchaseIndicator = purchaseDates.contains(day.date)
                val showExpiryIndicator = expiryDates.contains(day.date)
                var specificDayTextColorResource: Int? = null // 특정 상태일 때 적용할 텍스트 색상 리소스 ID.

                // 조건에 따라 날짜 셀의 배경 및 텍스트 색상, 인디케이터 가시성 설정.
                when {
                    day.date == today -> { // 오늘 날짜일 경우.
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_today) // 오늘 날짜용 배경 드로어블.
                        specificDayTextColorResource = R.color.calendar_text_today // 오늘 날짜용 텍스트 색상.
                        dayItemIndicator?.isVisible = showPurchaseIndicator || showExpiryIndicator // 오늘이면서 이벤트 있으면 인디케이터 표시.
                    }
                    showExpiryIndicator && showPurchaseIndicator -> { // 소비기한이면서 구매일인 경우.
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_expiry_purchase) // 특정 배경.
                        specificDayTextColorResource = R.color.calendar_text_event_priority // 특정 텍스트 색상.
                        dayItemIndicator?.isVisible = true
                    }
                    showExpiryIndicator -> { // 소비기한일인 경우.
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_expiry)
                        specificDayTextColorResource = R.color.calendar_text_event
                        dayItemIndicator?.isVisible = true
                    }
                    showPurchaseIndicator -> { // 구매일인 경우.
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_purchase)
                        specificDayTextColorResource = R.color.calendar_text_purchase
                        dayItemIndicator?.isVisible = true
                    }
                    else -> { // 기타 현재 달의 일반 날짜.
                        // 배경은 없음. 텍스트 색상은 위에서 초기화한 R.color.calendar_day_text_default를 따름.
                        dayItemIndicator?.isVisible = showPurchaseIndicator || showExpiryIndicator // 이벤트 있으면 인디케이터 표시.
                    }
                }

                // 특정 상태에 따른 텍스트 색상이 지정되었다면 적용.
                specificDayTextColorResource?.let { colorResId ->
                    dayText.setTextColor(ContextCompat.getColor(view.context, colorResId))
                }
                // else인 경우, 위에서 설정한 기본 텍스트 색상(R.color.calendar_day_text_default)을 따르거나,
                // item_calendar_day.xml의 TextView에 설정된 textColor(?attr/colorOnSurface)를 따릅니다.

            } else { // 이전 달 또는 다음 달의 날짜 (흐리게 처리).
                dayText.setTextColor(ContextCompat.getColor(view.context, R.color.calendar_text_disabled))
            }
        }
    }

    // Kizitonwose CalendarView의 월 헤더(요일 표시줄)를 위한 ViewContainer 클래스.
    inner class MonthViewContainer(view: View) : ViewContainer(view) {
        // item_calendar_header.xml의 루트 레이아웃 (ViewGroup, 예: LinearLayout)을 참조.
        private val legendLayout: ViewGroup = view as ViewGroup

        // 월 헤더 View에 요일 텍스트를 설정하는 함수.
        fun bind(calendarMonthData: CalendarMonth) { // 파라미터 이름을 명확히 함 (Kizitonwose v2의 CalendarMonth).
            // legendLayout(LinearLayout)의 자식 TextView 개수와 daysOfWeekFromLocale()로 얻은 요일 개수가 같으면.
            if (legendLayout.childCount == daysOfWeekFromLocale().size) {
                // 각 TextView에 순서대로 요일 이름(짧은 형태, 한국어)을 설정.
                daysOfWeekFromLocale().forEachIndexed { index, dayOfWeek ->
                    (legendLayout.getChildAt(index) as? TextView)?.text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                }
            }
        }
    }

    // 현재 로케일(기기 설정)에 맞는 주의 시작 요일부터 순서대로 DayOfWeek 리스트를 반환하는 함수.
    private fun daysOfWeekFromLocale(): List<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek // 로케일 기반 주의 첫 요일 가져오기.
        val daysOfWeek = DayOfWeek.values().toList() // 모든 DayOfWeek 값 (월요일부터 일요일 순서).
        // 주의 첫 요일이 월요일이면 그대로 반환.
        return if (firstDayOfWeek == DayOfWeek.MONDAY) {
            daysOfWeek
        } else {
            // 그렇지 않다면 (예: 미국은 일요일 시작), 로케일에 맞게 리스트 순서 조정.
            val sundayIndex = daysOfWeek.indexOf(DayOfWeek.SUNDAY)
            // 일요일부터 시작하도록 리스트 회전.
            daysOfWeek.subList(sundayIndex, daysOfWeek.size) + daysOfWeek.subList(0, sundayIndex)
        }
    }

    // 캘린더 하단 목록의 아이템 클릭 시 보여줄 식품 상세 정보 다이얼로그.
    private fun showCalendarFoodDetailDialog(item: FoodItem) {
        // HomeFragment의 showFoodDetailDialog 로직과 매우 유사.
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detail, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFood) // ID로 ImageView 찾기.

        // 이미지 로드 로직.
        item.imagePath?.let { path ->
            if (path.isNotEmpty()) {
                val imageFile = File(path)
                if (imageFile.exists()) {
                    BitmapFactory.decodeFile(imageFile.absolutePath)?.let { bitmap: Bitmap -> // bitmap 타입 명시.
                        imageView.setImageBitmap(bitmap)
                    } ?: imageView.setImageResource(R.drawable.ic_add) // 적절한 플레이스홀더 이미지.
                } else { imageView.setImageResource(R.drawable.ic_add) }
            } else { imageView.setImageResource(R.drawable.ic_add) }
        } ?: imageView.setImageResource(R.drawable.ic_add)

        // 텍스트 정보 설정.
        dialogView.findViewById<TextView>(R.id.textFoodName).text = "식품명: ${item.name}"
        dialogView.findViewById<TextView>(R.id.textExpiryDate).text = "소비기한: ${dateFormatDisplay.format(item.expiryDate)}" // dateFormatDisplay 사용.
        dialogView.findViewById<TextView>(R.id.textQuantity).text = "수량: ${item.quantity}"
        val categoryName = item.categoryId?.let { catId -> allCategoriesMap[catId] } ?: "미지정" // allCategoriesMap 사용.
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: $categoryName" // XML ID 확인.
        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation ?: ""}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate?.let { dateFormatDisplay.format(it) } ?: ""}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo ?: ""}"

        // 태그 표시 (dialog_food_detail.xml에 textViewDialogTags ID가 있어야 함).
        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags)
        if (item.tags.isNotEmpty()) {
            tagsTextView.text = "태그: ${item.tags.joinToString(", ")}"
            tagsTextView.visibility = View.VISIBLE
        } else {
            tagsTextView.visibility = View.GONE
        }

        // AlertDialog 생성.
        AlertDialog.Builder(requireContext())
            .setTitle("식품 상세정보")
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .setNeutralButton("수정") { _, _ -> // "수정" 버튼 클릭 시.
                val editFragment = AddFoodBottomSheetFragment.newInstance(item.id) // 수정 모드로 AddFoodBottomSheetFragment 실행.
                editFragment.show(parentFragmentManager, AddFoodBottomSheetFragment.TAG_EDIT)
            }
            .setNegativeButton("삭제") { _, _ -> // "삭제" 버튼 클릭 시.
                showCalendarDeleteConfirmationDialog(item) // 삭제 확인 다이얼로그 표시.
            }
            .create().show()
    }

    // 캘린더 하단 목록의 아이템 삭제 시 보여줄 확인 다이얼로그.
    private fun showCalendarDeleteConfirmationDialog(item: FoodItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${item.name}' 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteFoodItem(item) } // ViewModel을 통해 삭제 처리.
            .setNegativeButton("취소", null)
            .show()
    }

    // Fragment의 View가 파괴될 때 호출 (메모리 누수 방지).
    override fun onDestroyView() {
        super.onDestroyView()
        // CalendarView의 바인더들을 명시적으로 해제하여 메모리 누수 방지 (Kizitonwose 라이브러리 권장 사항).
        binding.calendarView.dayBinder = null
        binding.calendarView.monthHeaderBinder = null
        _binding = null // ViewBinding 객체 참조 해제.
    }
}