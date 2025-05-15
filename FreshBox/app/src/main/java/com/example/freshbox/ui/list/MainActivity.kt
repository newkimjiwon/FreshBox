package com.example.freshbox.ui.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.databinding.ActivityMainBinding
import com.example.freshbox.model.FoodItem
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment
import com.example.freshbox.ui.all.AllFoodsActivity
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expiredAdapter: FoodListAdapter
    private lateinit var expiringAdapter: FoodListAdapter

    private var allItems: List<FoodItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView 세팅
        expiredAdapter = FoodListAdapter()
        expiringAdapter = FoodListAdapter()

        binding.recyclerViewExpired.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewExpired.adapter = expiredAdapter

        binding.recyclerViewExpiring.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewExpiring.adapter = expiringAdapter

        // + 버튼 → 식품 추가 모달 띄우기
        binding.fabAddItem.setOnClickListener {
            AddFoodBottomSheetFragment().show(supportFragmentManager, "AddFood")
        }

        // 🔍 전체 보기 버튼
        binding.buttonViewAll.setOnClickListener {
            startActivity(Intent(this, AllFoodsActivity::class.java))
        }

        // 검색 기능
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateFilteredList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        allItems = loadFoodItemsFromJson(this)
        updateFilteredList(binding.editTextSearch.text?.toString() ?: "")
    }

    private fun updateFilteredList(query: String) {
        val lower = query.lowercase()

        // 자정 기준
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val expired = allItems.filter {
            it.expiryDate.toMillis() < today &&
                    (it.name.lowercase().contains(lower) || it.category.lowercase().contains(lower))
        }

        val expiring = allItems.filter {
            val diff = it.expiryDate.toMillis() - today
            diff in 0..(3 * 24 * 60 * 60 * 1000L) &&
                    (it.name.lowercase().contains(lower) || it.category.lowercase().contains(lower))
        }

        expiredAdapter.submitList(expired)
        expiringAdapter.submitList(expiring)

        val isEmpty = expired.isEmpty() && expiring.isEmpty()
        binding.textViewEmptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loadFoodItemsFromJson(context: Context): List<FoodItem> {
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

    private fun String.toMillis(): Long {
        return try {
            val millis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(this)?.time ?: 0L
            Log.d("toMillis()", "📅 입력값: \"$this\" → 변환된 millis: $millis")
            millis
        } catch (e: Exception) {
            Log.e("toMillis()", "⚠️ 날짜 변환 실패: \"$this\"", e)
            0L
        }
    }
}
