package com.example.freshbox.ui.list

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.R
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
        expiredAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) },
            onItemLongClick = { foodItem -> showDeleteDialog(foodItem) }
        )

        expiringAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) },
            onItemLongClick = { foodItem -> showDeleteDialog(foodItem) }
        )

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

    // 메인화면에서 이미지 클릭시 상세 정보 출력
    private fun showFoodDetailDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detail, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFood)
        val imageFile = File(item.imagePath)
        if (imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        dialogView.findViewById<TextView>(R.id.textFoodName).text = "식품명: ${item.name}"
        dialogView.findViewById<TextView>(R.id.textExpiryDate).text = "소비기한: ${item.expiryDate}"
        dialogView.findViewById<TextView>(R.id.textQuantity).text = "수량: ${item.quantity}"
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: ${item.category}"
        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo}"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("식품 상세정보")
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .setNeutralButton("수정") { _, _ ->
                // AddFoodBottomSheetFragment로 항목 전달
                val fragment = AddFoodBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putString("mode", "edit")
                        putSerializable("item", item)  // FoodItem은 Serializable이어야 함
                    }
                }
                fragment.show(parentFragmentManager, "EditFood")
            }
            .setNegativeButton("삭제") { _, _ ->
                showDeleteDialog(item)
            }
            .create()

        dialog.show()
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
