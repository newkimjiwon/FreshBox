// File: app/src/main/java/com/example/freshbox/ui/addedit/AddFoodBottomSheetFragment.kt
package com.example.freshbox.ui.addedit

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels // by viewModels() 사용을 위해
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.freshbox.R // R.layout.dialog_add_category 등 사용 시 (필요시)
import com.example.freshbox.data.Category // data.Category 사용
// FoodItem Entity는 ViewModel을 통해 주로 다루므로 직접적인 import는 필수 아님
import com.example.freshbox.databinding.BottomSheetAddFoodBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList // 사용되지 않으면 제거
import androidx.activity.result.ActivityResult

class AddFoodBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddFoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditViewModel by viewModels()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentPhotoPath: String? = null // Room FoodItem Entity에 imagePath 필드 추가 시 사용

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private var selectedCategoryId: Long? = null
    private var categoryListForAdapter: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        data?.extras?.get("data") as? Bitmap
                    }

                    imageBitmap?.let { bitmap ->
                        saveBitmapToFile(bitmap)?.let { filePath ->
                            currentPhotoPath = filePath
                            binding.imageViewFood.setImageBitmap(bitmap)
                        }
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        setupTagInput() // 현재는 특별한 로직 없음, 필요시 구현
        setupListeners()
        observeViewModel()

        // 수정 모드 또는 새 아이템 모드 처리
        val foodItemId = arguments?.getInt(ARG_FOOD_ITEM_ID, -1) ?: -1 // ARG_FOOD_ITEM_ID는 아래 companion object에 정의
        if (foodItemId != -1) {
            viewModel.loadFoodItem(foodItemId)
        } else {
            viewModel.setNewItemMode()
            // 새 아이템 모드 시 UI 기본값 설정
            binding.editTextExpiryDate.setText(dateFormat.format(Date())) // 소비기한 오늘 날짜로
            // 필요시 다른 필드도 초기화
        }
    }

    private fun setupListeners() {
        binding.editTextPurchaseDate.setOnClickListener {
            showDatePickerDialog(binding.editTextPurchaseDate)
        }
        binding.editTextExpiryDate.setOnClickListener {
            showDatePickerDialog(binding.editTextExpiryDate)
        }

        binding.buttonTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // TODO: 카메라 권한 확인 로직 추가 권장
            takePictureLauncher.launch(intent)
        }

        binding.buttonSave.setOnClickListener {
            saveFoodItemToDatabase()
        }

        binding.buttonAddNewCategory.setOnClickListener { // XML에 ID가 buttonAddNewCategory라고 가정
            showAddNewCategoryDialog()
        }
    }

    private fun showDatePickerDialog(targetEditText: com.google.android.material.textfield.TextInputEditText) {
        val cal = Calendar.getInstance()
        targetEditText.text?.toString()?.let {
            if (it.isNotEmpty()) {
                try {
                    cal.time = dateFormat.parse(it) ?: Date()
                } catch (e: Exception) { /* 날짜 파싱 실패 시 현재 날짜 유지 */ }
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                targetEditText.setText(dateFormat.format(selectedDate.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val context = requireContext()
        val dir = File(context.filesDir, "FreshBox_Images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.autoCompleteCategory.setAdapter(adapter) // XML ID: autoCompleteCategory

        binding.autoCompleteCategory.setOnItemClickListener { parent, _, position, _ ->
            val selectedCategoryName = parent.getItemAtPosition(position) as String
            selectedCategoryId = categoryListForAdapter.find { it.name == selectedCategoryName }?.id
        }
    }

    private fun setupTagInput() {
        // 현재는 EditText (ID: editTextTags)를 사용하여 쉼표로 구분된 문자열을 입력받는다고 가정.
        // ChipGroup 등을 사용한다면 여기에 관련 UI 설정 로직 추가.
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryListForAdapter = categories ?: emptyList()
            val categoryNames = categoryListForAdapter.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.autoCompleteCategory.setAdapter(adapter)

            // foodItem LiveData가 이미 로드된 상태에서 categories가 나중에 업데이트될 수 있으므로,
            // foodItem 관찰자 내에서 카테고리를 설정하는 것이 더 안전할 수 있음.
            // 또는, foodItem과 categories를 모두 사용하여 초기 선택을 설정하는 로직을 여기에 추가.
            viewModel.foodItem.value?.categoryId?.let { catId ->
                setCategorySelection(catId)
            }
        }

        viewModel.foodItem.observe(viewLifecycleOwner) { foodItem ->
            foodItem?.let { item ->
                binding.editTextName.setText(item.name)
                binding.editTextQuantity.setText(item.quantity)
                item.categoryId?.let {
                    setCategorySelection(it)
                } ?: run {
                    binding.autoCompleteCategory.setText("", false)
                    selectedCategoryId = null
                }
                binding.editTextStorage.setText(item.storageLocation)
                binding.editTextMemo.setText(item.memo)
                binding.editTextPurchaseDate.setText(item.purchaseDate?.let { dateFormat.format(it) })
                binding.editTextExpiryDate.setText(dateFormat.format(item.expiryDate))
                binding.checkboxIsFrozen.isChecked = item.isFrozen // XML ID: checkboxIsFrozen
                binding.editTextTags.setText(item.tags.joinToString(", ")) // XML ID: editTextTags

                // 이미지 경로 처리 (FoodItem Entity에 imagePath 필드가 있고, Room에 저장된 경우)
                // currentPhotoPath = item.imagePath
                // if (!item.imagePath.isNullOrEmpty()) {
                //     val file = File(item.imagePath!!)
                //     if (file.exists()) {
                //         binding.imageViewFood.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                //     } else {
                //         binding.imageViewFood.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
                //     }
                // } else {
                //    binding.imageViewFood.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
                // }
            }
        }

        viewModel.saveEvent.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "저장되었습니다!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        viewModel.categoryAddedEvent.observe(viewLifecycleOwner) { result ->
            result?.let { (success, message) ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 새 카테고리가 추가되면, 드롭다운에 반영된 후 해당 카테고리가 선택되도록 할 수 있음
                    // (categories LiveData가 자동으로 업데이트되므로 어댑터는 갱신됨)
                    // 필요시 추가 로직 구현
                }
            }
        }
    }

    private fun setCategorySelection(categoryIdToSelect: Long) {
        val categoryToSelect = categoryListForAdapter.find { it.id == categoryIdToSelect }
        categoryToSelect?.let {
            binding.autoCompleteCategory.setText(it.name, false)
            selectedCategoryId = it.id
        }
    }


    private fun showAddNewCategoryDialog() {
        val editTextCategoryName = EditText(requireContext()).apply {
            hint = "새 카테고리 이름"
            // AlertDialog 내부 EditText에 패딩 등을 주고 싶다면 XML 레이아웃을 사용하는 것이 좋음
            val paddingDp = 16
            val density = resources.displayMetrics.density
            val paddingPixel = (paddingDp * density).toInt()
            this.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel)

        }
        // AlertDialog의 컨텍스트를 Fragment의 requireContext()로 사용
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("새 카테고리 추가")
            .setView(editTextCategoryName)
            .setPositiveButton("추가") { _, _ -> // dialog 파라미터 명시적 사용 회피
                val categoryName = editTextCategoryName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    viewModel.addNewCategory(categoryName)
                } else {
                    Toast.makeText(requireContext(), "카테고리 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }

    private fun saveFoodItemToDatabase() {
        val name = binding.editTextName.text.toString().trim()
        val expiryDateStr = binding.editTextExpiryDate.text.toString()

        if (name.isEmpty()) {
            binding.textFieldName?.error = "식품명을 입력해주세요." // TextInputLayout 사용 시
            // binding.editTextName.error = "식품명을 입력해주세요." // EditText 직접 사용 시
            Toast.makeText(requireContext(), "식품명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.textFieldName?.error = null
        }

        if (expiryDateStr.isEmpty()) {
            binding.textFieldExpiryDate?.error = "소비기한을 입력해주세요."
            Toast.makeText(requireContext(), "소비기한을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.textFieldExpiryDate?.error = null
        }

        val expiryDate: Date
        try {
            expiryDate = dateFormat.parse(expiryDateStr) ?: run {
                Toast.makeText(requireContext(), "소비기한 날짜 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            binding.textFieldExpiryDate?.error = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)."
            Toast.makeText(requireContext(), "소비기한 날짜 형식이 올바르지 않습니다 (YYYY-MM-DD).", Toast.LENGTH_SHORT).show()
            return
        }

        val purchaseDateStr = binding.editTextPurchaseDate.text.toString()
        val purchaseDate: Date? = if (purchaseDateStr.isNotEmpty()) {
            try {
                dateFormat.parse(purchaseDateStr)
            } catch (e: Exception) {
                binding.textFieldPurchaseDate?.error = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)."
                Toast.makeText(requireContext(), "구매일 날짜 형식이 올바르지 않습니다 (YYYY-MM-DD).", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            null
        }

        val quantity = binding.editTextQuantity.text.toString().ifBlank { "1" }
        val storageLocationValue = binding.editTextStorage.text.toString().trim().ifEmpty { null } // storageLocation으로 변수명 변경 (명확성)
        val memo = binding.editTextMemo.text.toString().trim().ifEmpty { null }
        val isFrozenValue = binding.checkboxIsFrozen.isChecked // isFrozen 값 가져오기

        val tagsString = binding.editTextTags.text.toString().trim()
        val tagsList = if (tagsString.isEmpty()) {
            emptyList()
        } else {
            tagsString.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }

        viewModel.saveOrUpdateFoodItem(
            name = name,
            expiryDate = expiryDate,
            quantity = quantity,
            categoryId = selectedCategoryId,
            tags = tagsList,
            purchaseDate = purchaseDate,
            storage = storageLocationValue, // ViewModel 파라미터 이름이 storage라면 storageLocationValue 전달
            memo = memo,
            isFrozen = isFrozenValue,         // <<< isFrozen 값 전달
            imagePath = currentPhotoPath    // <<< currentPhotoPath (이미지 경로) 전달
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddFoodBottomSheet"       // 일반적인 BottomSheet 태그
        const val ARG_FOOD_ITEM_ID = "food_item_id" // ID 전달용 인수 이름
        const val TAG_EDIT = "EditFoodBottomSheetFragment" // <<< 이 상수를 확인하고 없다면 추가하세요.

        fun newInstance(foodItemId: Int): AddFoodBottomSheetFragment {
            return AddFoodBottomSheetFragment().apply { // apply 스코프 함수 사용
                arguments = Bundle().apply {
                    putInt(ARG_FOOD_ITEM_ID, foodItemId)
                }
            }
        }
        // fun newInstance(): AddFoodBottomSheetFragment = AddFoodBottomSheetFragment() // 새 아이템용
    }
}