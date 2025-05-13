package com.example.freshbox.ui.addedit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // viewModels 델리게이트 사용
import androidx.appcompat.app.AppCompatActivity
import com.example.freshbox.R // R 클래스 import
import com.example.freshbox.data.FoodItem
import com.example.freshbox.databinding.ActivityAddEditFoodBinding
import java.text.ParseException // ParseException import
import java.text.SimpleDateFormat
import java.util.*

class AddEditFoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditFoodBinding
    // AddEditViewModel 클래스가 올바르게 정의되어 있고, 같은 패키지에 있거나 import 되어야 합니다.
    private val viewModel: AddEditViewModel by viewModels()
    private var currentFoodItemId: Int = -1
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

    private lateinit var barcodeScanAppLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupCategorySpinner()
        setupBarcodeScannerLauncher()

        currentFoodItemId = intent.getIntExtra(EXTRA_FOOD_ID, -1)

        if (currentFoodItemId != -1) {
            supportActionBar?.title = "식품 정보 수정"
            viewModel.loadFoodItem(currentFoodItemId)
        } else {
            supportActionBar?.title = "식품 추가"
            viewModel.setNewItemMode()
        }

        // 수정 시 데이터 로드 관찰
        viewModel.foodItem.observe(this) { foodItem ->
            // foodItem이 null이 아니고, 현재 수정 모드이며, ID가 일치할 때만 UI 업데이트
            if (foodItem != null && currentFoodItemId != -1 && foodItem.id == currentFoodItemId) {
                populateUI(foodItem)
                // 수정 모드에서 아이템 로드 시, 해당 아이템의 바코드로 ViewModel 상태 업데이트
                viewModel.processBarcode(foodItem.barcode ?: "")
            }
        }

        // 저장 완료 이벤트 관찰
        viewModel.saveEvent.observe(this) { // SingleLiveEvent는 Unit 타입이므로 it 사용 안함
            Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 바코드 스캔 결과 관찰
        viewModel.barcodeResultEvent.observe(this) { scannedFoodItem -> // 파라미터 이름 명확히
            // scannedFoodItem은 null이 될 수 없음 (SingleLiveEvent<FoodItem>이므로)
            val scannedBarcode = scannedFoodItem.barcode ?: "정보 없음"
            Toast.makeText(this, "스캔된 바코드: $scannedBarcode", Toast.LENGTH_SHORT).show()

            // id가 0이면 DB에 없는 새 바코드 정보(ViewModel에서 생성), 아니면 기존 상품 정보
            if (scannedFoodItem.id != 0 && scannedFoodItem.name.isNotBlank()) { // 기존 상품 정보가 있는 경우
                binding.editTextName.setText(scannedFoodItem.name)
                binding.editTextName.requestFocus()
                scannedFoodItem.category?.let { binding.autoCompleteCategory.setText(it, false) }
            } else { // 신규 바코드이거나, DB에 정보 없는 바코드
                // 이름 필드를 비워두거나, "새 상품 (바코드: ...)" 등으로 채울 수 있음
                // 여기서는 이름 입력 유도
                binding.editTextName.requestFocus()
            }
            // 스캔된 바코드 값은 ViewModel의 currentBarcode에 저장되어 있음
        }


        // --- 리스너 설정 ---
        binding.editTextExpiryDate.setOnClickListener { showDatePickerDialog(binding.editTextExpiryDate) }
        binding.editTextPurchaseDate.setOnClickListener { showDatePickerDialog(binding.editTextPurchaseDate) }
        binding.buttonScanBarcode.setOnClickListener { launchBarcodeScanner() }
        binding.buttonSave.setOnClickListener { saveFoodItemData() } // 메서드명 변경 (saveFoodItem -> saveFoodItemData)
    }

    private fun setupBarcodeScannerLauncher() {
        barcodeScanAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val contents = result.data?.getStringExtra("SCAN_RESULT")
                if (!contents.isNullOrEmpty()) {
                    viewModel.processBarcode(contents)
                } else {
                    Toast.makeText(this, "바코드 값을 읽지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            // 스캔 취소 시 별도 메시지 없음
        }
    }

    private fun launchBarcodeScanner() {
        try {
            val intent = Intent("com.google.zxing.client.android.SCAN")
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE")
            barcodeScanAppLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "바코드 스캔 앱을 설치해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.food_categories)
        // 기본 안드로이드 제공 레이아웃으로 변경
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
        binding.autoCompleteCategory.setOnItemClickListener { _, _, _, _ ->
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun populateUI(foodItem: FoodItem) {
        binding.editTextName.setText(foodItem.name)
        binding.editTextExpiryDate.setText(dateFormat.format(foodItem.expiryDate))
        foodItem.purchaseDate?.let { binding.editTextPurchaseDate.setText(dateFormat.format(it)) }
        binding.editTextQuantity.setText(foodItem.quantity)
        binding.autoCompleteCategory.setText(foodItem.category, false)
        // TODO: storageLocation, memo 등 나머지 필드 UI에 채우기
        // 예: binding.editTextStorage.setText(foodItem.storageLocation ?: "")
        // 예: binding.editTextMemo.setText(foodItem.memo ?: "")
    }

    private fun showDatePickerDialog(targetEditText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        val currentText = targetEditText.text.toString()
        if (currentText.isNotEmpty()) {
            try {
                calendar.time = dateFormat.parse(currentText) ?: Date()
            } catch (e: ParseException) { /* 날짜 파싱 실패 시 오늘 날짜 사용 */ }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }.time
            targetEditText.setText(dateFormat.format(selectedDate))
        }, year, month, day)

        // 소비기한은 오늘 이전 날짜 선택 불가
        if (targetEditText.id == R.id.editTextExpiryDate) {
            datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
        }
        // 구매일은 오늘 이후 날짜 선택 불가
        if (targetEditText.id == R.id.editTextPurchaseDate) {
            datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
        }

        datePickerDialog.show()
    }

    // 메서드명을 saveFoodItemData로 변경 (기존 saveFoodItem이 AndroidViewModel 내에 있을 수 있는 이름과 충돌 방지)
    private fun saveFoodItemData() {
        val name = binding.editTextName.text.toString().trim()
        val expiryDateStr = binding.editTextExpiryDate.text.toString()
        val purchaseDateStr = binding.editTextPurchaseDate.text.toString()
        val quantity = binding.editTextQuantity.text.toString().ifBlank { "1" }
        val category = binding.autoCompleteCategory.text.toString().takeIf { it.isNotBlank() }
        // TODO: storageLocation, memo 값 UI에서 가져오기
        val storageLocation: String? = null // 예시, 실제로는 UI에서 가져와야 함
        val memo: String? = null // 예시

        if (name.isEmpty() || expiryDateStr.isEmpty()) {
            Toast.makeText(this, "식품 이름과 소비기한은 필수입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val expiryDate = dateFormat.parse(expiryDateStr)
                ?: throw ParseException("소비기한 날짜를 올바르게 입력해주세요.", 0)

            val purchaseDate = if (purchaseDateStr.isNotEmpty()) {
                dateFormat.parse(purchaseDateStr)
                    ?: throw ParseException("구매일 날짜를 올바르게 입력해주세요.", 0)
            } else {
                null
            }

            viewModel.saveOrUpdateFoodItem(
                name, expiryDate, quantity, category, purchaseDate,
                storageLocation, memo
            )

        } catch (e: ParseException) {
            Toast.makeText(this, "날짜 형식이 올바르지 않습니다 (yyyy.MM.dd).", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "저장 중 오류 발생: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_FOOD_ID = "com.example.freshbox.EXTRA_FOOD_ID" // 패키지명 포함 권장
    }
}