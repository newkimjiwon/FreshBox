// File: app/src/main/java/com/example/freshbox/ui/addedit/AddFoodBottomSheetFragment.kt
package com.example.freshbox.ui.addedit

import android.app.Activity // ActivityResultLauncher 콜백에서 Activity.RESULT_OK 사용 시 필요
import android.app.AlertDialog // 새 카테고리 추가 시 확인 다이얼로그 등에 사용
import android.app.DatePickerDialog // 날짜 선택 다이얼로그
import android.content.Intent // 카메라 앱 호출 등 외부 Activity 실행 시 필요
import android.graphics.Bitmap // 이미지 비트맵 객체
import android.graphics.BitmapFactory // 파일로부터 비트맵 이미지 디코딩
import android.os.Build // 안드로이드 OS 버전 확인 (API 레벨에 따른 분기 처리 시)
import android.os.Bundle
import android.provider.MediaStore // 카메라 앱 호출을 위한 Action 정의
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter // AutoCompleteTextView에 목록을 제공하기 위한 어댑터
import android.widget.EditText // 새 카테고리 입력 다이얼로그 등에 사용
import android.widget.Toast // 간단한 메시지 사용자에게 표시
import androidx.activity.result.ActivityResult // ActivityResultLauncher의 결과 타입
import androidx.activity.result.ActivityResultLauncher // Activity 실행 및 결과 처리를 위한 최신 API
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultLauncher의 표준 계약 정의
import androidx.fragment.app.viewModels // KTX를 사용한 ViewModel 초기화
import com.google.android.material.bottomsheet.BottomSheetDialogFragment // BottomSheet 형태의 DialogFragment
import com.example.freshbox.R // 리소스 ID 참조 (예: R.string.some_text, R.drawable.some_icon)
import com.example.freshbox.data.Category // Category 데이터 모델 (ViewModel에서 카테고리 목록 받을 때 사용)
// FoodItem Entity는 ViewModel을 통해 주로 다루므로 직접적인 import는 필수 아님 (AddEditViewModel이 data.FoodItem을 사용)
import com.example.freshbox.databinding.BottomSheetAddFoodBinding // ViewBinding 클래스 (bottom_sheet_add_food.xml 기반)
import java.io.File // 파일 시스템의 파일 객체 (이미지 저장/로드 시)
import java.io.FileOutputStream // 파일에 데이터를 쓰기 위해 사용 (이미지 저장 시)
import java.text.SimpleDateFormat // 날짜 포맷팅
import java.util.* // Date, Calendar, Locale 등 유틸리티 클래스
// import kotlin.collections.ArrayList // 현재 코드에서 명시적으로 ArrayList를 직접 사용하지 않음

// 식품 추가 또는 수정을 위한 BottomSheetDialogFragment
class AddFoodBottomSheetFragment : BottomSheetDialogFragment() {

    // ViewBinding을 위한 프로퍼티
    private var _binding: BottomSheetAddFoodBinding? = null
    // _binding이 null이 아님을 보장 (onCreateView ~ onDestroyView 사이에서만 사용)
    private val binding get() = _binding!!

    // AddEditViewModel 인스턴스를 KTX 델리게이트를 사용하여 가져옴
    // 이 Fragment의 생명주기에 맞춰 ViewModel이 관리됨
    private val viewModel: AddEditViewModel by viewModels()

    // 날짜 문자열 포맷을 정의하는 객체 (예: "2023-10-27")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 현재 선택/촬영된 사진의 파일 경로를 저장하는 변수
    // 이 경로는 Room DB의 FoodItem Entity에 imagePath로 저장될 수 있음
    private var currentPhotoPath: String? = null

    // 카메라 앱을 실행하고 그 결과를 받기 위한 ActivityResultLauncher
    // onCreate 또는 onAttach에서 초기화하는 것이 더 안전할 수 있음 (Fragment 생명주기 고려)
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    // 사용자가 AutoCompleteTextView에서 선택한 카테고리의 ID를 저장하는 변수
    private var selectedCategoryId: Long? = null
    // ViewModel로부터 받은 실제 Category 객체 목록 (이름으로 ID를 찾거나 할 때 사용)
    private var categoryListForAdapter: List<Category> = emptyList()

    // Fragment가 처음 생성될 때 호출 (View 생성 전)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ActivityResultLauncher 초기화: 카메라 앱 실행(ActivityResultContracts.StartActivityForResult()) 및 결과 처리 콜백 정의
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult -> // 결과 타입 명시
                // 카메라 앱 실행 결과가 성공(Activity.RESULT_OK)인지 확인
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data // 결과 Intent 가져오기
                    // Intent의 extra에서 "data" 키로 Bitmap 이미지 가져오기
                    // Android Tiramisu (API 33) 이상에서는 타입 안전한 getParcelable 사용 권장
                    val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        @Suppress("DEPRECATION") // 이전 버전 호환성을 위해 deprecated된 get("data") 사용
                        data?.extras?.get("data") as? Bitmap
                    }

                    imageBitmap?.let { bitmap -> // Bitmap이 null이 아니면
                        // Bitmap을 파일로 저장하고, 성공 시 파일 경로를 currentPhotoPath에 저장하고 ImageView에 표시
                        saveBitmapToFile(bitmap)?.let { filePath ->
                            currentPhotoPath = filePath
                            binding.imageViewFood.setImageBitmap(bitmap)
                        }
                    }
                }
            }
    }

    // Fragment의 View를 생성하고 반환하는 단계
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // BottomSheetAddFoodBinding을 사용하여 bottom_sheet_add_food.xml 레이아웃을 inflate
        _binding = BottomSheetAddFoodBinding.inflate(inflater, container, false)
        return binding.root // inflate된 레이아웃의 루트 View 반환
    }

    // Fragment의 View가 성공적으로 생성된 후 호출되는 단계
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 관련 초기화 함수들 호출
        setupCategoryDropdown() // 카테고리 선택 드롭다운 UI 설정
        setupTagInput()         // 태그 입력 UI 설정 (현재는 특별한 로직 없음)
        setupListeners()        // 버튼 등의 이벤트 리스너 설정
        observeViewModel()      // ViewModel의 LiveData 관찰 시작

        // Fragment arguments를 통해 전달받은 foodItemId를 확인하여 수정 모드 또는 새 아이템 모드 처리
        // ARG_FOOD_ITEM_ID는 companion object에 정의된 상수여야 함
        val foodItemId = arguments?.getInt(ARG_FOOD_ITEM_ID, -1) ?: -1
        if (foodItemId != -1) { // 유효한 ID가 있다면 (수정 모드)
            viewModel.loadFoodItem(foodItemId) // ViewModel에 해당 ID의 식품 정보 로드 요청
        } else { // ID가 없다면 (새 아이템 모드)
            viewModel.setNewItemMode() // ViewModel을 새 아이템 모드로 설정
            // 새 아이템 추가 시 소비기한 입력 필드에 오늘 날짜를 기본값으로 설정
            binding.editTextExpiryDate.setText(dateFormat.format(Date()))
        }
    }

    // 주요 UI 요소들의 이벤트 리스너를 설정하는 함수
    private fun setupListeners() {
        // 구매일 입력 필드 클릭 시 DatePickerDialog 표시
        binding.editTextPurchaseDate.setOnClickListener {
            showDatePickerDialog(binding.editTextPurchaseDate)
        }
        // 소비기한 입력 필드 클릭 시 DatePickerDialog 표시
        binding.editTextExpiryDate.setOnClickListener {
            showDatePickerDialog(binding.editTextExpiryDate)
        }

        // "사진 촬영/선택" 버튼 클릭 시 카메라 앱 실행
        binding.buttonTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE) // 카메라 앱을 실행하기 위한 Intent
            // TODO: 카메라 권한 확인 및 요청 로직 추가 권장
            takePictureLauncher.launch(intent) // 위에서 초기화한 ActivityResultLauncher로 카메라 앱 실행
        }

        // "저장" 버튼 클릭 시 saveFoodItemToDatabase 함수 호출
        binding.buttonSave.setOnClickListener {
            saveFoodItemToDatabase()
        }

        // "새 카테고리 추가" 버튼 클릭 시 새 카테고리 입력 다이얼로그 표시
        // (XML 레이아웃에 buttonAddNewCategory ID를 가진 버튼이 있어야 함)
        binding.buttonAddNewCategory.setOnClickListener {
            showAddNewCategoryDialog()
        }
    }

    // DatePickerDialog를 표시하는 헬퍼 함수
    // targetEditText: 선택된 날짜를 설정할 TextInputEditText
    private fun showDatePickerDialog(targetEditText: com.google.android.material.textfield.TextInputEditText) {
        val cal = Calendar.getInstance() // 현재 날짜/시간으로 Calendar 객체 초기화
        // 만약 targetEditText에 이미 날짜가 있다면, 그 날짜를 DatePicker의 초기값으로 사용
        targetEditText.text?.toString()?.let {
            if (it.isNotEmpty()) {
                try {
                    cal.time = dateFormat.parse(it) ?: Date() // 문자열을 Date로 파싱
                } catch (e: Exception) { /* 파싱 실패 시 현재 날짜 유지 */ }
            }
        }

        // DatePickerDialog 생성 및 표시
        DatePickerDialog(
            requireContext(), // Context
            { _, year, month, dayOfMonth -> // 사용자가 날짜를 선택했을 때의 콜백
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                targetEditText.setText(dateFormat.format(selectedDate.time)) // 선택된 날짜를 포맷에 맞춰 EditText에 설정
            },
            cal.get(Calendar.YEAR), // DatePicker 초기 연도
            cal.get(Calendar.MONTH), // DatePicker 초기 월
            cal.get(Calendar.DAY_OF_MONTH) // DatePicker 초기 일
        ).show()
    }

    // Bitmap 이미지를 파일로 저장하고, 저장된 파일의 절대 경로를 반환하는 함수
    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val context = requireContext()
        // 앱 내부 저장 공간의 "FreshBox_Images" 디렉토리에 이미지 저장 (디렉토리 없으면 생성)
        val dir = File(context.filesDir, "FreshBox_Images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // 파일 이름은 현재 시간을 사용하여 중복 방지 (예: img_1622505000000.jpg)
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { out -> // FileOutputStream으로 파일에 쓰기
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // JPEG 형식, 품질 90으로 압축
            }
            file.absolutePath // 저장된 파일의 절대 경로 반환
        } catch (e: Exception) { // 파일 저장 중 오류 발생 시
            e.printStackTrace()
            Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            null // 실패 시 null 반환
        }
    }

    // 카테고리 선택용 AutoCompleteTextView를 설정하는 함수
    private fun setupCategoryDropdown() {
        // ArrayAdapter 초기화. categoryListForAdapter는 처음에는 비어있음.
        // 실제 데이터는 observeViewModel에서 viewModel.categories를 관찰하여 채워짐.
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.autoCompleteCategory.setAdapter(adapter) // XML ID: autoCompleteCategory

        // 사용자가 드롭다운 목록에서 카테고리를 선택했을 때의 동작
        binding.autoCompleteCategory.setOnItemClickListener { parent, _, position, _ ->
            val selectedCategoryName = parent.getItemAtPosition(position) as String // 선택된 카테고리 이름
            // categoryListForAdapter (실제 Category 객체 리스트)에서 해당 이름을 가진 Category 객체를 찾아 ID를 가져옴
            selectedCategoryId = categoryListForAdapter.find { it.name == selectedCategoryName }?.id
        }
    }

    // 태그 입력 UI를 설정하는 함수 (현재는 특별한 로직 없음)
    // EditText (ID: editTextTags)를 사용하여 쉼표로 구분된 문자열을 입력받는다고 가정.
    // 만약 ChipGroup 등을 사용한다면 여기에 관련 UI 설정 로직 추가.
    private fun setupTagInput() {
        // 예: binding.editTextTags.addTextChangedListener(...) 등
    }

    // ViewModel의 LiveData들을 관찰하여 UI를 업데이트하는 함수
    private fun observeViewModel() {
        // 1. ViewModel의 전체 카테고리 목록(categories) 관찰
        viewModel.categories.observe(viewLifecycleOwner) { categories -> // 타입 추론에 맡기거나 명시적 타입 사용 가능
            categoryListForAdapter = categories ?: emptyList() // 실제 Category 객체 리스트 업데이트
            val categoryNames = categoryListForAdapter.map { it.name } // 드롭다운에 표시할 이름 목록 생성

            // AutoCompleteTextView에 연결된 ArrayAdapter를 새로 만들거나 기존 어댑터의 데이터를 갱신
            // (주의: setupCategoryDropdown에서 어댑터를 멤버 변수로 가지고 있다면 그 어댑터의 데이터를 변경하고 notifyDataSetChanged() 호출)
            // 현재 setupCategoryDropdown에서는 지역 변수 adapter를 사용하므로, 여기서 새 어댑터 설정 또는 멤버 변수 활용
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.autoCompleteCategory.setAdapter(adapter)

            // 수정 모드일 경우, 로드된 foodItem의 categoryId에 해당하는 카테고리를
            // AutoCompleteTextView에 미리 선택(표시)되도록 함.
            // 이 로직은 foodItem LiveData의 Observer 내부 또는 여기서 foodItem.value를 직접 확인하여 처리 가능.
            viewModel.foodItem.value?.categoryId?.let { catId ->
                setCategorySelection(catId) // 아래 정의된 헬퍼 함수 호출
            }
        }

        // 2. ViewModel의 현재 편집/조회 중인 식품 정보(foodItem) 관찰
        viewModel.foodItem.observe(viewLifecycleOwner) { foodItem -> // 타입 추론 또는 명시
            foodItem?.let { item -> // foodItem이 null이 아니면 (즉, 기존 아이템 로드 완료 또는 새 아이템 정보 일부 설정)
                // 각 UI 입력 필드에 아이템 정보 채우기
                binding.editTextName.setText(item.name)
                binding.editTextQuantity.setText(item.quantity)

                // 카테고리 설정 (item.categoryId 사용)
                item.categoryId?.let {
                    setCategorySelection(it) // 위에서 categories LiveData가 먼저 처리되어 categoryListForAdapter가 채워져 있어야 함
                } ?: run { // 카테고리 ID가 없으면
                    binding.autoCompleteCategory.setText("", false) // 텍스트 비우기 (onItemClickListener 트리거 방지)
                    selectedCategoryId = null // 내부 선택 ID도 null로
                }
                binding.editTextStorage.setText(item.storageLocation)
                binding.editTextMemo.setText(item.memo)
                binding.editTextPurchaseDate.setText(item.purchaseDate?.let { dateFormat.format(it) })
                binding.editTextExpiryDate.setText(dateFormat.format(item.expiryDate)) // 소비기한은 필수라고 가정
                binding.checkboxIsFrozen.isChecked = item.isFrozen // XML ID: checkboxIsFrozen
                binding.editTextTags.setText(item.tags.joinToString(", ")) // XML ID: editTextTags

                // 이미지 경로 처리 (FoodItem Entity에 imagePath가 있고, Room에 저장된 경우)
                currentPhotoPath = item.imagePath // currentPhotoPath도 동기화
                if (!item.imagePath.isNullOrEmpty()) {
                    val file = File(item.imagePath!!)
                    if (file.exists()) {
                        binding.imageViewFood.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    } else {
                        binding.imageViewFood.setImageResource(R.drawable.ic_launcher_foreground) // 적절한 기본 이미지
                    }
                } else {
                    binding.imageViewFood.setImageResource(R.drawable.ic_launcher_foreground) // 적절한 기본 이미지
                }
            }
        }

        // 3. 저장 완료 이벤트(saveEvent) 관찰
        viewModel.saveEvent.observe(viewLifecycleOwner) { // it: Unit?
            Toast.makeText(requireContext(), "저장되었습니다!", Toast.LENGTH_SHORT).show()
            dismiss() // BottomSheet 닫기
        }

        // 4. 새 카테고리 추가 결과(categoryAddedEvent) 관찰
        viewModel.categoryAddedEvent.observe(viewLifecycleOwner) { result -> // result: Pair<Boolean, String>?
            result?.let { (success, message) -> // null이 아닐 때만 처리
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 새 카테고리가 성공적으로 추가되면, categories LiveData가 자동으로 업데이트되므로
                    // 드롭다운 목록도 갱신됨. 필요시 새로 추가된 카테고리를 자동으로 선택하게 할 수 있음.
                }
            }
        }
    }

    // 전달받은 categoryId에 해당하는 카테고리 이름을 AutoCompleteTextView에 설정하는 헬퍼 함수
    private fun setCategorySelection(categoryIdToSelect: Long) {
        // categoryListForAdapter (실제 Category 객체 리스트)가 채워져 있어야 함
        val categoryToSelect = categoryListForAdapter.find { it.id == categoryIdToSelect }
        categoryToSelect?.let {
            // AutoCompleteTextView에 텍스트 설정. 두 번째 파라미터 false는 onItemClickListener 트리거 방지.
            binding.autoCompleteCategory.setText(it.name, false)
            selectedCategoryId = it.id // 내부 선택 ID도 업데이트
        }
    }

    // 새 카테고리 추가 다이얼로그를 표시하는 함수
    private fun showAddNewCategoryDialog() {
        val editTextCategoryName = EditText(requireContext()).apply {
            hint = "새 카테고리 이름"
            // AlertDialog 내부 EditText의 패딩 설정 (선택 사항)
            val paddingDp = 16
            val density = resources.displayMetrics.density
            val paddingPixel = (paddingDp * density).toInt()
            this.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("새 카테고리 추가")
            .setView(editTextCategoryName)
            .setPositiveButton("추가") { _, _ ->
                val categoryName = editTextCategoryName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    viewModel.addNewCategory(categoryName) // ViewModel에 새 카테고리 추가 요청
                } else {
                    Toast.makeText(requireContext(), "카테고리 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 현재 UI 입력 값들을 가져와 FoodItem 객체로 만들어 ViewModel을 통해 저장하는 함수
    private fun saveFoodItemToDatabase() {
        val name = binding.editTextName.text.toString().trim()
        val expiryDateStr = binding.editTextExpiryDate.text.toString()

        // 필수 값(식품명, 소비기한) 입력 여부 확인
        if (name.isEmpty()) {
            // TextInputLayout을 사용한다면 binding.textFieldName.error 사용
            binding.textFieldName?.error = "식품명을 입력해주세요."
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

        // 소비기한 문자열을 Date 객체로 파싱
        val expiryDate: Date
        try {
            expiryDate = dateFormat.parse(expiryDateStr) ?: run { // 파싱 실패 시 또는 null 반환 시
                Toast.makeText(requireContext(), "소비기한 날짜 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) { // 그 외 파싱 예외
            binding.textFieldExpiryDate?.error = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)."
            Toast.makeText(requireContext(), "소비기한 날짜 형식이 올바르지 않습니다 (YYYY-MM-DD).", Toast.LENGTH_SHORT).show()
            return
        }

        // 구매일 문자열을 Date 객체로 파싱 (선택 사항이므로 null일 수 있음)
        val purchaseDateStr = binding.editTextPurchaseDate.text.toString()
        val purchaseDate: Date? = if (purchaseDateStr.isNotEmpty()) {
            try {
                dateFormat.parse(purchaseDateStr)
            } catch (e: Exception) {
                binding.textFieldPurchaseDate?.error = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)."
                Toast.makeText(requireContext(), "구매일 날짜 형식이 올바르지 않습니다 (YYYY-MM-DD).", Toast.LENGTH_SHORT).show()
                return // 오류 시 저장 중단
            }
        } else {
            null // 비어있으면 null
        }

        // 나머지 정보 가져오기
        val quantity = binding.editTextQuantity.text.toString().ifBlank { "1" } // 비어있으면 기본값 "1"
        val storageLocationValue = binding.editTextStorage.text.toString().trim().ifEmpty { null } // 보관 위치
        val memo = binding.editTextMemo.text.toString().trim().ifEmpty { null } // 메모
        val isFrozenValue = binding.checkboxIsFrozen.isChecked // 냉동 여부 (XML ID: checkboxIsFrozen)

        // 태그 문자열을 List<String>으로 파싱
        val tagsString = binding.editTextTags.text.toString().trim() // XML ID: editTextTags
        val tagsList = if (tagsString.isEmpty()) {
            emptyList()
        } else {
            tagsString.split(',').map { it.trim() }.filter { it.isNotEmpty() } // 쉼표로 구분, 각 태그 앞뒤 공백 제거, 빈 태그 제거
        }

        // ViewModel의 saveOrUpdateFoodItem 메서드 호출하여 저장/업데이트 요청
        // 이 메서드는 AddEditViewModel에 imagePath와 isFrozen 파라미터를 받도록 수정되어 있어야 함
        viewModel.saveOrUpdateFoodItem(
            name = name,
            expiryDate = expiryDate,
            quantity = quantity,
            categoryId = selectedCategoryId, // setupCategoryDropdown에서 선택된 ID
            tags = tagsList,
            purchaseDate = purchaseDate,
            storage = storageLocationValue,
            memo = memo,
            isFrozen = isFrozenValue,       // isFrozen 값 전달
            imagePath = currentPhotoPath  // 현재 사진 경로 전달 (FoodItem Entity 및 ViewModel에 imagePath 필드 필요)
        )
    }

    // Fragment의 View가 파괴될 때 호출 (메모리 누수 방지)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // ViewBinding 객체 참조 해제
    }

    // 이 Fragment를 외부에서 생성하거나 식별하기 위한 상수 정의 (companion object)
    companion object {
        // BottomSheet를 표시할 때 사용할 태그
        const val TAG = "AddFoodBottomSheetFragment"
        // 수정 모드로 Fragment를 열 때 FoodItem의 ID를 전달하기 위한 Argument 키
        const val ARG_FOOD_ITEM_ID = "food_item_id"
        // 수정 모드임을 나타내는 태그 (show 할 때 사용)
        const val TAG_EDIT = "EditFoodBottomSheetFragment"

        // 수정 모드로 이 Fragment의 새 인스턴스를 생성하는 팩토리 메서드
        fun newInstance(foodItemId: Int): AddFoodBottomSheetFragment {
            return AddFoodBottomSheetFragment().apply { // apply 스코프 함수 사용
                arguments = Bundle().apply { // arguments Bundle에 foodItemId 저장
                    putInt(ARG_FOOD_ITEM_ID, foodItemId)
                }
            }
        }
        // 새 아이템 추가 시에는 인자 없이 기본 생성자 호출 또는 newInstance() 오버로딩 가능
        // fun newInstance(): AddFoodBottomSheetFragment = AddFoodBottomSheetFragment()
    }
}