package com.example.freshbox.ui.list

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.databinding.FragmentHomeBinding
import com.example.freshbox.model.FoodItem
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment
import com.example.freshbox.ui.all.AllFoodsActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var expiredAdapter: FoodListAdapter
    private lateinit var expiringAdapter: FoodListAdapter
    private var allItems: List<FoodItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        expiredAdapter = FoodListAdapter { foodItem -> showDeleteDialog(foodItem) }
        expiringAdapter = FoodListAdapter { foodItem -> showDeleteDialog(foodItem) }

        binding.recyclerViewExpired.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpired.adapter = expiredAdapter

        binding.recyclerViewExpiring.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpiring.adapter = expiringAdapter

        binding.fabAddItem.setOnClickListener {
            AddFoodBottomSheetFragment().show(parentFragmentManager, "AddFood")
        }

        binding.buttonViewAll.setOnClickListener {
            startActivity(Intent(requireContext(), AllFoodsActivity::class.java))
        }

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
        allItems = loadFoodItemsFromJson(requireContext())
        updateFilteredList(binding.editTextSearch.text?.toString() ?: "")
    }

    private fun updateFilteredList(query: String) {
        val lower = query.lowercase()
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

        expiredAdapter.submitList(expired.take(4))
        expiringAdapter.submitList(expiring.take(4))

        val isEmpty = expired.isEmpty() && expiring.isEmpty()
        binding.textViewEmptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showDeleteDialog(item: FoodItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("${item.name} 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> deleteFoodItem(item) }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteFoodItem(item: FoodItem) {
        allItems = allItems.filter { it != item }
        saveAllToJson(allItems, requireContext())
        updateFilteredList(binding.editTextSearch.text?.toString() ?: "")
    }

    private fun saveAllToJson(list: List<FoodItem>, context: Context) {
        val file = File(context.filesDir, "FreshBox/items.json")
        val json = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("name", item.name)
                put("quantity", item.quantity)
                put("category", item.category)
                put("storageLocation", item.storageLocation)
                put("memo", item.memo)
                put("purchaseDate", item.purchaseDate)
                put("expiryDate", item.expiryDate)
                put("imagePath", item.imagePath)
            }
            json.put(obj)
        }
        file.writeText(json.toString())
    }

    private fun String.toMillis(): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(this)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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

}
