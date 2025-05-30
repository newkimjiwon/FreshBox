// File: app/src/main/java/com/example/freshbox/ui/calendar/CalendarFragment.kt
package com.example.freshbox.ui.calendar

import android.app.Activity // ActivityResultLauncher 콜백에서 Activity.RESULT_OK 사용 시 필요
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap // Bitmap 클래스 import
import android.graphics.BitmapFactory
import android.os.Build // Build.VERSION 확인 시 필요
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // <<< ImageView import 추가

import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult // ActivityResultLauncher 콜백에서 ActivityResult 사용 시 필요
import androidx.activity.result.ActivityResultLauncher // ActivityResultLauncher import
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultLauncher 계약 import
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.R
import com.example.freshbox.data.Category // Category import 추가
import com.example.freshbox.data.FoodItem
import com.example.freshbox.databinding.FragmentCalendarBinding
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment
import com.example.freshbox.ui.list.FoodListAdapter
import com.example.freshbox.ui.list.FoodListViewModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth // MonthViewContainer에서 사용
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder // MonthHeaderFooterBinder import
import com.kizitonwose.calendar.view.ViewContainer
import java.io.File
import java.io.FileOutputStream // saveBitmapToFile에서 사용
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle // DayOfWeek.getDisplayName에서 사용
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FoodListViewModel by viewModels()

    private var purchaseDates: Set<LocalDate> = emptySet()
    private var expiryDates: Set<LocalDate> = emptySet()
    private val today = LocalDate.now()

    private lateinit var calendarItemsAdapter: FoodListAdapter
    private val dateFormatDisplay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var allCategoriesMap: Map<Long, String> = emptyMap() // <<< allCategoriesMap 멤버 변수 선언 확인

    // ActivityResultLauncher는 onCreate 또는 onAttach에서 초기화하는 것이 더 안전할 수 있습니다.
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent> // 상세 다이얼로그에서 사진 수정 기능을 넣는다면 필요


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 상세 다이얼로그에서 사진 촬영/선택 기능이 필요 없다면 이 부분은 CalendarFragment에 없어도 됩니다.
        // 만약 AddFoodBottomSheetFragment처럼 여기서도 사진을 다룬다면 필요합니다.
        // 지금은 showCalendarFoodDetailDialog에서 직접 카메라를 호출하지 않으므로 주석 처리 또는 삭제 가능
        /*
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // ... 로직 ...
                }
            }
        */
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendarItemsRecyclerView()
        observeViewModel() // ViewModel 관찰 먼저 시작

        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view) { calendarDay ->
                Log.d("CalendarFragment", "Date clicked: ${calendarDay.date}")
                viewModel.loadItemsExpiringOnDate(calendarDay.date)
            }
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.bind(day)
            }
        }

        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> { // 타입 명시
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) { // 타입 명시
                container.bind(data)
            }
        }

        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        binding.calendarView.post {
            binding.calendarView.scrollToMonth(currentMonth)
            updateMonthYearText(currentMonth)
        }

        binding.calendarView.monthScrollListener = { calendarMonth ->
            updateMonthYearText(calendarMonth.yearMonth)
        }

        binding.buttonPrevMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        binding.buttonNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
    }

    private fun setupCalendarItemsRecyclerView() {
        calendarItemsAdapter = FoodListAdapter(
            onItemClick = { foodItem ->
                showCalendarFoodDetailDialog(foodItem)
            },
            onItemLongClick = { foodItem ->
                showCalendarDeleteConfirmationDialog(foodItem)
            }
        )
        binding.recyclerViewCalendarItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarItemsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.allCategories.observe(viewLifecycleOwner, Observer { categories: List<Category>? -> // 타입 명시
            allCategoriesMap = categories?.associateBy({ it.id }, { it.name }) ?: emptyMap()
        })

        viewModel.allFoodItemsForCalendar.observe(viewLifecycleOwner, Observer { foodItemsList: List<FoodItem>? ->
            foodItemsList?.let { items ->
                Log.d("CalendarFragment", "Observed allFoodItemsForCalendar. Count: ${items.size}")
                val (purchases, expiries) = extractDatesFromFoodItems(items)
                purchaseDates = purchases
                expiryDates = expiries
                binding.calendarView.notifyCalendarChanged()
            }
        })

        viewModel.itemsForCalendarSelectedDate.observe(viewLifecycleOwner, Observer { itemsForDate: List<FoodItem>? ->
            itemsForDate?.let { items ->
                Log.d("CalendarFragment", "Items for selected date: ${items.joinToString { it.name }}")
                calendarItemsAdapter.submitList(items)
                binding.textViewCalendarNoItems.isVisible = items.isEmpty()
                binding.recyclerViewCalendarItems.isVisible = items.isNotEmpty()
            } ?: run {
                calendarItemsAdapter.submitList(emptyList())
                binding.textViewCalendarNoItems.isVisible = true
                binding.recyclerViewCalendarItems.isVisible = false
            }
        })
    }

    private fun Date.toLocalDate(): LocalDate {
        return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun extractDatesFromFoodItems(foodItems: List<FoodItem>): Pair<Set<LocalDate>, Set<LocalDate>> {
        val purchases = foodItems.mapNotNull { it.purchaseDate?.toLocalDate() }.toSet()
        val expiries = foodItems.map { it.expiryDate.toLocalDate() }.toSet()
        return purchases to expiries
    }

    private fun updateMonthYearText(yearMonth: YearMonth?) {
        yearMonth?.let {
            val formatter = DateTimeFormatter.ofPattern("yyyy년 MMMM", Locale.KOREAN)
            binding.textViewMonthYear.text = it.format(formatter)
        }
    }

    inner class DayViewContainer(view: View, private val onDateClick: (CalendarDay) -> Unit) : ViewContainer(view) {
        // view는 item_calendar_day.xml의 루트 레이아웃입니다.
        val dayText: TextView = view.findViewById(R.id.calendarDayText)
        val dayItemIndicator: View? = view.findViewById(R.id.dayItemIndicator)
        private var currentDayInternal: CalendarDay? = null

        init {
            // itemView.setOnClickListener를 view.setOnClickListener로 변경
            view.setOnClickListener {
                currentDayInternal?.let {
                    if (it.position == DayPosition.MonthDate) {
                        onDateClick(it)
                    }
                }
            }
        }

        fun bind(day: CalendarDay) {
            currentDayInternal = day
            dayText.text = day.date.dayOfMonth.toString()

            dayText.background = null
            // itemView.context 대신 view.context 사용
            dayText.setTextColor(ContextCompat.getColor(view.context, R.color.default_text_color))
            dayItemIndicator?.isVisible = false
            // itemView.isClickable 대신 view.isClickable 사용
            view.isClickable = false

            if (day.position == DayPosition.MonthDate) {
                // itemView.isClickable 대신 view.isClickable 사용
                view.isClickable = true
                val showPurchaseIndicator = purchaseDates.contains(day.date)
                val showExpiryIndicator = expiryDates.contains(day.date)

                when {
                    day.date == today -> {
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_today)
                        // itemView.context 대신 view.context 사용
                        dayText.setTextColor(ContextCompat.getColor(view.context, R.color.white))
                        dayItemIndicator?.isVisible = showPurchaseIndicator || showExpiryIndicator
                    }
                    showExpiryIndicator && showPurchaseIndicator -> {
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_expiry_purchase)
                        // itemView.context 대신 view.context 사용
                        dayText.setTextColor(ContextCompat.getColor(view.context, R.color.white))
                        dayItemIndicator?.isVisible = true
                    }
                    showExpiryIndicator -> {
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_expiry)
                        // itemView.context 대신 view.context 사용
                        dayText.setTextColor(ContextCompat.getColor(view.context, R.color.white))
                        dayItemIndicator?.isVisible = true
                    }
                    showPurchaseIndicator -> {
                        dayText.setBackgroundResource(R.drawable.bg_calendar_day_purchase)
                        // itemView.context 대신 view.context 사용
                        dayText.setTextColor(ContextCompat.getColor(view.context, R.color.black))
                        dayItemIndicator?.isVisible = true
                    }
                    else -> { /* 기본 스타일 유지 */ }
                }
            } else {
                // itemView.context 대신 view.context 사용
                dayText.setTextColor(ContextCompat.getColor(view.context, R.color.disabled_text_color))
            }
        }
    }

    inner class MonthViewContainer(view: View) : ViewContainer(view) {
        private val legendLayout: ViewGroup = view as ViewGroup // item_calendar_header.xml의 루트가 ViewGroup(예: LinearLayout)이라고 가정

        fun bind(calendarMonthData: CalendarMonth) { // 파라미터 이름 명확히 (data -> calendarMonthData)
            // daysOfWeekFromLocale()이 올바른 순서의 DayOfWeek 리스트를 반환한다고 가정
            if (legendLayout.childCount == daysOfWeekFromLocale().size) {
                daysOfWeekFromLocale().forEachIndexed { index, dayOfWeek ->
                    (legendLayout.getChildAt(index) as? TextView)?.text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                }
            }
        }
    }

    private fun daysOfWeekFromLocale(): List<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val daysOfWeek = DayOfWeek.values().toList()
        return if (firstDayOfWeek == DayOfWeek.MONDAY) {
            daysOfWeek
        } else {
            val sundayIndex = daysOfWeek.indexOf(DayOfWeek.SUNDAY)
            // 일요일부터 시작하도록 리스트 회전
            daysOfWeek.subList(sundayIndex, daysOfWeek.size) + daysOfWeek.subList(0, sundayIndex)
        }
    }

    private fun showCalendarFoodDetailDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detail, null)

        // imageView를 findViewById로 가져오고 타입을 명시합니다.
        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFood) // <<< ImageView 타입 명시

        item.imagePath?.let { path ->
            if (path.isNotEmpty()) {
                val imageFile = File(path)
                if (imageFile.exists()) {
                    // BitmapFactory.decodeFile의 결과를 let으로 안전하게 처리하고, setImageBitmap 호출
                    BitmapFactory.decodeFile(imageFile.absolutePath)?.let { bitmap: Bitmap -> // <<< bitmap 타입 명시
                        imageView.setImageBitmap(bitmap) // <<< imageView의 메서드 호출
                    } ?: imageView.setImageResource(R.drawable.ic_add)
                } else {
                    imageView.setImageResource(R.drawable.ic_add) // <<< imageView의 메서드 호출
                }
            } else {
                imageView.setImageResource(R.drawable.ic_add) // <<< imageView의 메서드 호출
            }
        } ?: imageView.setImageResource(R.drawable.ic_add) // <<< imageView의 메서드 호출

        dialogView.findViewById<TextView>(R.id.textFoodName).text = "식품명: ${item.name}"
        dialogView.findViewById<TextView>(R.id.textExpiryDate).text = "소비기한: ${dateFormatDisplay.format(item.expiryDate)}"
        dialogView.findViewById<TextView>(R.id.textQuantity).text = "수량: ${item.quantity}"

        // allCategoriesMap이 Fragment의 멤버 변수로 선언되어 있고, observeViewModel에서 채워진다고 가정
        val categoryName = item.categoryId?.let { catId -> allCategoriesMap[catId] } ?: "미지정" // <<< allCategoriesMap 참조
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: $categoryName"

        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation ?: ""}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate?.let { dateFormatDisplay.format(it) } ?: ""}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo ?: ""}"

        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags)
        if (item.tags.isNotEmpty()) {
            tagsTextView.text = "태그: ${item.tags.joinToString(", ")}"
            tagsTextView.visibility = View.VISIBLE
        } else {
            tagsTextView.visibility = View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("식품 상세정보")
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .setNeutralButton("수정") { _, _ ->
                val editFragment = AddFoodBottomSheetFragment.newInstance(item.id)
                editFragment.show(parentFragmentManager, AddFoodBottomSheetFragment.TAG_EDIT)
            }
            .setNegativeButton("삭제") { _, _ -> showCalendarDeleteConfirmationDialog(item) }
            .create().show()
    }

    private fun showCalendarDeleteConfirmationDialog(item: FoodItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${item.name}' 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteFoodItem(item) }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.calendarView.dayBinder = null
        binding.calendarView.monthHeaderBinder = null
        _binding = null
    }

    // Companion object는 AddFoodBottomSheetFragment에서 TAG_EDIT 등을 참조할 때 필요할 수 있으나,
    // CalendarFragment 자체를 외부에서 newInstance로 생성하지 않는다면 필수는 아님.
    // 필요에 따라 추가:
    // companion object {
    //     const val TAG = "CalendarFragment"
    // }
}