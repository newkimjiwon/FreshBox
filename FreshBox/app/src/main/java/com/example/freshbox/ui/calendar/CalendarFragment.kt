package com.example.freshbox.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.freshbox.R
import com.example.freshbox.databinding.FragmentCalendarBinding
import com.example.freshbox.model.FoodItem
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import org.json.JSONArray
import java.io.File

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var purchaseDates: Set<LocalDate> = emptySet()
    private var expiryDates: Set<LocalDate> = emptySet()
    private val today = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val foodItems = loadFoodItemsFromJson(requireContext())
        val (purchases, expiries) = extractDatesFromFoodItems(foodItems)
        purchaseDates = purchases
        expiryDates = expiries
        binding.calendarView.notifyCalendarChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val daysOfWeek = daysOfWeekFromLocale()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.bind(day)
            }
        }

        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())

        binding.calendarView.post {
            binding.calendarView.scrollToMonth(currentMonth)
            updateMonthYearText(currentMonth)
        }

        binding.calendarView.monthScrollListener = { month ->
            updateMonthYearText(month.yearMonth)
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

    private fun extractDatesFromFoodItems(foodItems: List<FoodItem>): Pair<Set<LocalDate>, Set<LocalDate>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val purchases = foodItems.mapNotNull {
            runCatching { LocalDate.parse(it.purchaseDate, formatter) }.getOrNull()
        }.toSet()
        val expiries = foodItems.mapNotNull {
            runCatching { LocalDate.parse(it.expiryDate, formatter) }.getOrNull()
        }.toSet()
        return purchases to expiries
    }

    private fun updateMonthYearText(yearMonth: YearMonth?) {
        yearMonth?.let {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            binding.textViewMonthYear.text = it.format(formatter)
        }
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        private val textView: TextView

        init {
            val itemView = LayoutInflater.from(view.context).inflate(
                R.layout.item_calendar_day,
                view as ViewGroup,
                false
            )
            view.addView(itemView)
            textView = itemView.findViewById(R.id.textViewDay)
        }

        fun bind(day: CalendarDay) {
            textView.text = day.date.dayOfMonth.toString()

            if (day.position != DayPosition.MonthDate) {
                textView.setBackgroundColor(0xFFFFFFFF.toInt())
                textView.setTextColor(0xFFCCCCCC.toInt())
                return
            }

            when (day.date) {
                today -> {
                    textView.setBackgroundColor(0xFF81C784.toInt()) // 연초록 (오늘)
                    textView.setTextColor(0xFFFFFFFF.toInt())
                }
                in expiryDates -> {
                    textView.setBackgroundColor(0xFF4CAF50.toInt()) // 진초록 (소비기한)
                    textView.setTextColor(0xFFFFFFFF.toInt())
                }
                in purchaseDates -> {
                    textView.setBackgroundColor(0xFFFFEB3B.toInt()) // 노랑 (구매일)
                    textView.setTextColor(0xFF000000.toInt())
                }
                else -> {
                    textView.setBackgroundColor(0xFFFFFFFF.toInt())
                    textView.setTextColor(0xFF000000.toInt())
                }
            }
        }
    }

    private fun loadFoodItemsFromJson(context: android.content.Context): List<FoodItem> {
        val file = File(context.filesDir, "FreshBox/items.json")
        if (!file.exists()) return emptyList()
        val json = JSONArray(file.readText())
        return (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            FoodItem(
                name = obj.getString("name"),
                quantity = obj.getString("quantity"),
                category = obj.getString("category"),
                storageLocation = obj.getString("storageLocation"),
                memo = obj.getString("memo"),
                purchaseDate = obj.getString("purchaseDate"),
                expiryDate = obj.getString("expiryDate"),
                imagePath = obj.getString("imagePath")
            )
        }
    }

    private fun daysOfWeekFromLocale(): List<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val daysOfWeek = DayOfWeek.values().toList()
        return if (firstDayOfWeek == DayOfWeek.MONDAY) daysOfWeek
        else daysOfWeek.dropWhile { it != firstDayOfWeek } + daysOfWeek.takeWhile { it != firstDayOfWeek }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
