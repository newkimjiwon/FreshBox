package com.example.freshbox.ui.addedit

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.freshbox.databinding.BottomSheetAddFoodBinding
import com.example.freshbox.model.FoodItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddFoodBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddFoodBinding? = null
    private val binding get() = _binding!!
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentPhotoPath: String? = null
    private var originalItem: FoodItem? = null  // ✅ 수정 모드용 변수

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupCategoryDropdown()
        setupListeners()

        // 수정모드일 경우 기존 값 채우기
        originalItem = arguments?.getSerializable("item") as? FoodItem
        originalItem?.let { item ->
            binding.editTextName.setText(item.name)
            binding.editTextQuantity.setText(item.quantity)
            binding.autoCompleteCategory.setText(item.category)
            binding.editTextStorage.setText(item.storageLocation)
            binding.editTextMemo.setText(item.memo)
            binding.editTextPurchaseDate.setText(item.purchaseDate)
            binding.editTextExpiryDate.setText(item.expiryDate)

            val file = File(item.imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.imageViewFood.setImageBitmap(bitmap)
                currentPhotoPath = item.imagePath
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf("과일", "채소", "유제품", "음료", "기타")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.editTextPurchaseDate.setOnClickListener {
            showDatePicker(binding.editTextPurchaseDate)
        }
        binding.editTextExpiryDate.setOnClickListener {
            showDatePicker(binding.editTextExpiryDate)
        }

        binding.buttonTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 100)
        }

        binding.buttonSave.setOnClickListener {
            saveItem()
        }
    }

    private fun showDatePicker(target: com.google.android.material.textfield.TextInputEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            target.setText(dateFormat.format(cal.time))
        }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            photo?.let {
                val dir = File(requireContext().filesDir, "FreshBox")
                if (!dir.exists()) dir.mkdir()
                val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                currentPhotoPath = file.absolutePath
                binding.imageViewFood.setImageBitmap(it)
            }
        }
    }

    private fun saveItem() {
        val name = binding.editTextName.text.toString().trim()
        val expiryDate = binding.editTextExpiryDate.text.toString()
        if (name.isEmpty() || expiryDate.isEmpty()) {
            Toast.makeText(requireContext(), "식품명과 소비기한은 필수입니다", Toast.LENGTH_SHORT).show()
            return
        }

        val item = FoodItem(
            name = name,
            quantity = binding.editTextQuantity.text.toString().ifBlank { "1" },
            category = binding.autoCompleteCategory.text.toString(),
            storageLocation = binding.editTextStorage.text.toString(),
            memo = binding.editTextMemo.text.toString(),
            purchaseDate = binding.editTextPurchaseDate.text.toString(),
            expiryDate = expiryDate,
            imagePath = currentPhotoPath ?: ""
        )

        saveToJson(item, requireContext())
        Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun saveToJson(item: FoodItem, context: Context) {
        val dir = File(context.filesDir, "FreshBox")
        if (!dir.exists()) dir.mkdir()
        val file = File(dir, "items.json")
        val list = if (file.exists()) JSONArray(file.readText()) else JSONArray()
        val updatedList = JSONArray()

        for (i in 0 until list.length()) {
            val obj = list.getJSONObject(i)
            if (originalItem != null &&
                obj.getString("name") == originalItem!!.name &&
                obj.getString("expiryDate") == originalItem!!.expiryDate) {
                // 기존 항목 수정
                updatedList.put(JSONObject().apply {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("category", item.category)
                    put("storageLocation", item.storageLocation)
                    put("memo", item.memo)
                    put("purchaseDate", item.purchaseDate)
                    put("expiryDate", item.expiryDate)
                    put("imagePath", item.imagePath)
                })
            } else {
                updatedList.put(obj)
            }
        }

        if (originalItem == null) {
            // 새 항목 추가
            updatedList.put(JSONObject().apply {
                put("name", item.name)
                put("quantity", item.quantity)
                put("category", item.category)
                put("storageLocation", item.storageLocation)
                put("memo", item.memo)
                put("purchaseDate", item.purchaseDate)
                put("expiryDate", item.expiryDate)
                put("imagePath", item.imagePath)
            })
        }

        file.writeText(updatedList.toString())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}