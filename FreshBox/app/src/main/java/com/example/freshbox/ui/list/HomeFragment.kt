// File: app/src/main/java/com/example/freshbox/ui/list/HomeFragment.kt
package com.example.freshbox.ui.list

import android.app.AlertDialog
// import android.content.Context // Context는 requireContext()로 가져오므로 명시적 import는 보통 불필요
import android.content.DialogInterface // AlertDialog 리스너에서 사용
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView // AutoCompleteTextView의 onItemClickListener 사용을 위해 필요
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // KTX를 사용한 ViewModel 초기화를 위해 필요
import androidx.lifecycle.Observer // LiveData를 관찰하기 위해 필요
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.R // 리소스 ID 참조 (예: R.string.theme_light)
import com.example.freshbox.data.Category // Category 데이터 모델
import com.example.freshbox.data.FoodItem // FoodItem 데이터 모델 (Room Entity)
import com.example.freshbox.databinding.FragmentHomeBinding // ViewBinding 클래스
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment // 식품 추가/수정 UI
import com.example.freshbox.ui.all.AllFoodsActivity // 전체 식품 목록 Activity
import com.example.freshbox.util.ThemeHelper // 테마 변경 유틸리티
import java.io.File // 이미지 파일 접근
import java.text.SimpleDateFormat // 날짜 포맷팅
import java.util.* // Date, Calendar, Locale 등

// 앱의 홈 화면을 담당하는 메인 Fragment
class HomeFragment : Fragment() {
    // ViewBinding을 위한 프로퍼티
    // _binding은 nullable이지만, onDestroyView에서 null로 설정하여 메모리 누수 방지
    private var _binding: FragmentHomeBinding? = null
    // binding 프로퍼티는 _binding이 null이 아님을 보장하여 UI 요소 접근 시 null 체크를 줄여줌
    // (onCreateView에서 초기화되고 onDestroyView에서 해제됨)
    private val binding get() = _binding!!

    // FoodListViewModel 인스턴스를 KTX의 by viewModels() 델리게이트를 사용하여 가져옴
    // Fragment의 생명주기에 맞춰 ViewModel이 관리됨
    private val viewModel: FoodListViewModel by viewModels()

    // 소비기한 만료 식품 목록을 위한 RecyclerView 어댑터
    private lateinit var expiredAdapter: FoodListAdapter
    // 소비기한 임박 식품 목록을 위한 RecyclerView 어댑터
    private lateinit var expiringAdapter: FoodListAdapter

    // 날짜 표시 형식을 정의하는 SimpleDateFormat 객체
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 카테고리 ID(Long)를 카테고리 이름(String)으로 매핑하기 위한 Map
    // ViewModel로부터 전체 카테고리 목록을 받으면 이 Map이 업데이트됨 (상세 정보 표시 등에 사용)
    private var allCategoriesMap: Map<Long, String> = emptyMap()

    // 카테고리 필터 AutoCompleteTextView에 표시될 문자열 목록 (예: "전체", "과일", "채소")
    private var categoryFilterDisplayList: MutableList<String> = mutableListOf()
    // ViewModel로부터 받은 실제 Category 객체 목록. 선택된 이름으로 ID를 찾을 때 사용.
    private var actualCategoryList: List<Category> = emptyList()
    // AutoCompleteTextView에 사용될 ArrayAdapter
    private lateinit var categoryFilterAdapter: ArrayAdapter<String>
    // 사용자가 필터에서 현재 선택한 카테고리 이름을 임시 저장 (UI 상태 복원 시도용)
    // Fragment 재생성 시 이 값은 초기화될 수 있으므로 ViewModel 상태와 동기화하는 로직이 중요.
    private var currentSelectedCategoryNameInFilter: String? = null


    // Fragment의 View를 생성하고 반환하는 메서드
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // FragmentHomeBinding을 사용하여 fragment_home.xml 레이아웃을 inflate
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // inflate된 레이아웃의 루트 View를 반환
        return binding.root
    }

    // Fragment의 View가 성공적으로 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI 관련 초기화 함수들 호출
        setupAdapters()         // RecyclerView 어댑터들 초기화
        setupRecyclerViews()    // RecyclerView들 설정 (LayoutManager, Adapter 연결)
        setupCategoryFilter()   // 카테고리 필터 UI (AutoCompleteTextView) 설정
        setupListeners()        // 각종 버튼 및 UI 요소들의 이벤트 리스너 설정
        observeViewModel()      // ViewModel의 LiveData 관찰 시작하여 UI 업데이트
    }

    // RecyclerView 어댑터들을 초기화하는 함수
    private fun setupAdapters() {
        expiredAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) }, // 아이템 클릭 시 상세 정보 표시
            onItemLongClick = { foodItem -> showDeleteConfirmationDialog(foodItem) } // 아이템 롱클릭 시 삭제 확인
        )
        expiringAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) },
            onItemLongClick = { foodItem -> showDeleteConfirmationDialog(foodItem) }
        )
    }

    // RecyclerView들을 설정하는 함수
    private fun setupRecyclerViews() {
        // 소비기한 만료 식품 목록 RecyclerView 설정
        binding.recyclerViewExpired.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false) // 가로 스크롤
        binding.recyclerViewExpired.adapter = expiredAdapter

        // 소비기한 임박 식품 목록 RecyclerView 설정
        binding.recyclerViewExpiring.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false) // 가로 스크롤
        binding.recyclerViewExpiring.adapter = expiringAdapter
    }

    // 카테고리 필터 UI(AutoCompleteTextView)를 설정하는 함수
    private fun setupCategoryFilter() {
        // ArrayAdapter 초기화. categoryFilterDisplayList는 처음에는 비어있음.
        // 실제 데이터는 observeViewModel에서 viewModel.allCategories를 관찰하여 채워짐.
        categoryFilterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryFilterDisplayList)
        // XML 레이아웃의 autoCompleteCategoryFilter ID를 가진 AutoCompleteTextView에 어댑터 설정
        binding.autoCompleteCategoryFilter.setAdapter(categoryFilterAdapter)

        // 사용자가 드롭다운 목록에서 특정 카테고리를 "선택"했을 때의 동작 정의
        binding.autoCompleteCategoryFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String // 선택된 카테고리 이름
            currentSelectedCategoryNameInFilter = selectedName // 현재 선택된 이름을 임시 저장 (UI 복원 시도용)

            // 선택된 이름에 따라 ViewModel에 전달할 categoryId 결정
            var categoryIdToSet: Long? = null
            if (selectedName == "전체") { // "전체" 옵션 선택 시
                categoryIdToSet = null // categoryId를 null로 설정하여 전체 목록을 보도록 함
            } else {
                // "전체"가 아닐 경우, actualCategoryList(실제 Category 객체 목록)에서 해당 이름을 가진 Category를 찾아 ID를 가져옴
                val selectedCategory = actualCategoryList.find { it.name == selectedName }
                categoryIdToSet = selectedCategory?.id
            }
            Log.d("HomeFragment_Category", "Category selected: '$selectedName', ID to set in ViewModel: $categoryIdToSet")
            // ViewModel에 선택된 카테고리 필터 값 설정 요청
            // (ViewModel 내부에서는 이 값 변경 시 LiveData를 업데이트하여 목록을 다시 필터링해야 함)
            viewModel.setCategoryFilter(categoryIdToSet)
        }
    }

    // 주요 UI 요소들의 이벤트 리스너를 설정하는 함수
    private fun setupListeners() {
        // FAB(Floating Action Button) 클릭 시 식품 추가 BottomSheet 표시
        binding.fabAddItem.setOnClickListener {
            // AddFoodBottomSheetFragment의 companion object에 TAG가 정의되어 있어야 함
            AddFoodBottomSheetFragment().show(parentFragmentManager, AddFoodBottomSheetFragment.TAG)
        }
        // "전체 보기" 버튼 클릭 시 AllFoodsActivity 실행
        binding.buttonViewAll.setOnClickListener {
            startActivity(Intent(requireContext(), AllFoodsActivity::class.java))
        }
        // 검색창(editTextSearch)의 텍스트 변경 감지 리스너
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            // 텍스트가 변경될 때마다 호출됨
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim().ifEmpty { null } // 입력된 텍스트 (공백 제거, 비었으면 null)
                Log.d("HomeFragment_Search", "Search keyword input: $keyword")
                viewModel.setSearchKeyword(keyword) // ViewModel에 검색어 전달하여 필터링 요청
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 설정 아이콘(imageViewSettings) 클릭 시 테마 선택 다이얼로그 표시
        // fragment_home.xml에 imageViewSettings ID가 있어야 함
        binding.imageViewSettings.setOnClickListener {
            showThemeSelectionDialog() // 아래 정의된 테마 선택 다이얼로그 표시 함수 호출
        }
        // 카테고리 필터의 아이템 클릭 리스너는 setupCategoryFilter()에서 이미 설정됨
    }

    // 테마 선택 다이얼로그를 표시하는 함수
    private fun showThemeSelectionDialog() {
        // 다이얼로그에 표시될 옵션 문자열 (R.string.* 리소스 참조)
        val themeOptions = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system_default)
        )
        // 각 옵션에 해당하는 실제 테마 모드 값 (ThemeHelper에 정의된 상수)
        val themeModes = intArrayOf(
            ThemeHelper.LIGHT_MODE,
            ThemeHelper.DARK_MODE,
            ThemeHelper.DEFAULT_MODE
        )
        // 현재 적용된 테마 모드 가져오기
        val currentThemeMode = ThemeHelper.getThemeMode(requireContext())
        // 현재 테마에 해당하는 옵션의 인덱스 찾기 (다이얼로그에서 기본 선택으로 표시)
        var checkedItem = themeModes.indexOf(currentThemeMode)
        if (checkedItem == -1) { // 저장된 값이 없거나 배열에 없는 예외적인 경우
            checkedItem = themeModes.indexOf(ThemeHelper.DEFAULT_MODE) // 시스템 기본 설정을 우선
            if (checkedItem == -1) checkedItem = 0 // 그래도 없으면 첫 번째 옵션(라이트 모드)을 기본으로
        }

        // AlertDialog 생성 및 표시
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.theme_dialog_title) // 다이얼로그 제목
            .setSingleChoiceItems(themeOptions, checkedItem) { dialog: DialogInterface, which: Int -> // 단일 선택 목록, dialog 타입 명시
                val selectedMode = themeModes[which] // 사용자가 선택한 테마 모드 값
                // 현재 테마와 사용자가 선택한 테마가 다를 경우에만 변경 작업 수행
                if (ThemeHelper.getThemeMode(requireContext()) != selectedMode) {
                    ThemeHelper.setThemeMode(requireContext(), selectedMode) // 선택된 테마 저장 및 즉시 적용 요청
                    activity?.recreate() // 테마 변경을 화면에 즉시 반영하기 위해 Activity 재시작
                }
                dialog.dismiss() // 다이얼로그 닫기
            }
            .setNegativeButton("취소", null) // "취소" 버튼
            .show()
    }

    // ViewModel의 LiveData들을 관찰하여 UI를 업데이트하는 함수
    private fun observeViewModel() {
        // 1. ViewModel의 전체 카테고리 목록(allCategories) 관찰
        viewModel.allCategories.observe(viewLifecycleOwner, Observer { categories: List<Category>? -> // categories 타입을 List<Category>?로 받음
            Log.d("HomeFragment_Observe", "allCategories Observer. Received: ${categories?.size ?: "null"} categories")
            actualCategoryList = categories ?: emptyList() // null이면 빈 리스트로 초기화, 실제 Category 객체 리스트 업데이트
            allCategoriesMap = actualCategoryList.associateBy({ it.id }, { it.name }) // ID-이름 Map 생성

            // 카테고리 필터 드롭다운에 표시될 이름 목록 업데이트
            categoryFilterDisplayList.clear()
            categoryFilterDisplayList.add("전체") // "전체" 옵션 추가
            categoryFilterDisplayList.addAll(actualCategoryList.map { it.name }) // 실제 카테고리 이름들 추가
            // 어댑터에 데이터 변경 알림 (어댑터가 초기화된 후에 호출되어야 함)
            if (::categoryFilterAdapter.isInitialized) {
                categoryFilterAdapter.notifyDataSetChanged()
            }
            Log.d("HomeFragment_Observe", "Category filter adapter updated. DisplayList size: ${categoryFilterDisplayList.size}.")

            // currentSelectedCategoryNameInFilter를 사용하여 AutoCompleteTextView의 텍스트를 설정.
            // 이 방식은 Fragment 재생성 시 currentSelectedCategoryNameInFilter가 초기화될 수 있어
            // 테마 변경 후 카테고리 선택 상태가 "전체"로 돌아갈 수 있음.
            // 더 안정적인 방법은 ViewModel의 selectedCategoryId를 관찰하여 UI를 업데이트하는 것.
            currentSelectedCategoryNameInFilter?.let { name ->
                if (categoryFilterDisplayList.contains(name)) {
                    binding.autoCompleteCategoryFilter.setText(name, false) // false: onItemClickListener 트리거 방지
                } else { // 이전에 선택한 이름이 현재 목록에 없으면 "전체"로 설정
                    binding.autoCompleteCategoryFilter.setText("전체", false)
                    // 현재 선택된 카테고리 이름이 목록에 없으므로, ViewModel의 필터도 "전체"(null)로 설정할지 고려.
                    // (사용자가 "전체"를 의도한 것과 다를 수 있으므로 주의)
                    if (viewModel.selectedCategoryId.value != null) {
                        // viewModel.setCategoryFilter(null) // 주석 처리: 이로 인해 의도치 않은 필터링 발생 가능
                    }
                    currentSelectedCategoryNameInFilter = "전체" // 임시 상태도 업데이트
                }
            } ?: run { // 이전에 선택된 이름이 없으면(null이면) "전체"로 기본 설정
                binding.autoCompleteCategoryFilter.setText("전체", false)
            }
        })

        // (주석) ViewModel의 selectedCategoryId 변경을 직접 관찰하여 UI를 동기화하는 옵저버를
        // 추가하는 것을 이전 답변에서 제안했었습니다. 이는 테마 변경 후 카테고리 필터 상태를
        // ViewModel의 상태와 일치시키는 데 더 안정적일 수 있습니다.
        // 예: viewModel.selectedCategoryId.observe(viewLifecycleOwner, Observer { categoryId ->
        //         updateAutoCompleteTextViewWithVmState(categoryId) // 헬퍼 함수 사용
        //     })


        // 2. 필터링된 소비기한 임박 식품 목록(homeExpiringSoonItems) 관찰
        viewModel.homeExpiringSoonItems.observe(viewLifecycleOwner, Observer { items: List<FoodItem>? -> // items 타입을 List<FoodItem>?로 받음
            Log.d("HomeFragment_Observe", "Observed homeExpiringSoonItems. Count: ${items?.size ?: 0}")
            // 최대 10개 또는 원하는 개수만큼 가져오기 (null이면 빈 리스트)
            expiringAdapter.submitList(items?.take(10) ?: emptyList()) // 어댑터에 새 목록 제출
            // "식품 없음" 메시지 관련 기능은 사용자 요청에 따라 updateEmptyViewVisibility 함수 호출을 제거한 상태
        })

        // 3. 필터링된 소비기한 만료 식품 목록(homeExpiredItems) 관찰
        viewModel.homeExpiredItems.observe(viewLifecycleOwner, Observer { items: List<FoodItem>? -> // items 타입을 List<FoodItem>?로 받음
            Log.d("HomeFragment_Observe", "Observed homeExpiredItems. Count: ${items?.size ?: 0}")
            expiredAdapter.submitList(items?.take(10) ?: emptyList())
            // "식품 없음" 메시지 관련 기능은 사용자 요청에 따라 updateEmptyViewVisibility 함수 호출을 제거한 상태
        })
    }

    // 식품 삭제 확인 다이얼로그를 표시하는 함수
    private fun showDeleteConfirmationDialog(item: FoodItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${item.name}' 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteFoodItem(item) } // ViewModel을 통해 삭제 처리
            .setNegativeButton("취소", null)
            .show()
    }

    // 식품 상세 정보 다이얼로그를 표시하는 함수
    private fun showFoodDetailDialog(item: FoodItem) {
        // dialog_food_detail.xml 레이아웃을 inflate
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detail, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFood) // ID로 ImageView 찾기

        // 이미지 경로가 있으면 이미지 로드, 없으면 플레이스홀더 표시
        item.imagePath?.let { path ->
            if (path.isNotEmpty()) {
                val imageFile = File(path)
                if (imageFile.exists()) {
                    BitmapFactory.decodeFile(imageFile.absolutePath)?.let { imageView.setImageBitmap(it) }
                        ?: imageView.setImageResource(R.drawable.ic_add) // ic_add를 적절한 플레이스홀더로 변경
                } else { imageView.setImageResource(R.drawable.ic_add) }
            } else { imageView.setImageResource(R.drawable.ic_add) }
        } ?: imageView.setImageResource(R.drawable.ic_add)

        // 각 TextView에 식품 정보 설정
        dialogView.findViewById<TextView>(R.id.textFoodName).text = "식품명: ${item.name}"
        dialogView.findViewById<TextView>(R.id.textExpiryDate).text = "소비기한: ${dateFormat.format(item.expiryDate)}"
        dialogView.findViewById<TextView>(R.id.textQuantity).text = "수량: ${item.quantity}"
        // 카테고리 ID를 이름으로 변환하여 표시 (allCategoriesMap 사용)
        val categoryName = item.categoryId?.let { allCategoriesMap[it] } ?: "미지정"
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: $categoryName" // XML에 textCategory ID가 있어야 함
        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation ?: ""}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate?.let { dateFormat.format(it) } ?: ""}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo ?: ""}"

        // 태그 표시 (dialog_food_detail.xml에 textViewDialogTags ID가 있어야 함)
        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags)
        if (item.tags.isNotEmpty()) {
            tagsTextView.text = "태그: ${item.tags.joinToString(", ")}"
            tagsTextView.visibility = View.VISIBLE
        } else {
            tagsTextView.visibility = View.GONE
        }

        // AlertDialog 생성 및 표시
        AlertDialog.Builder(requireContext())
            .setTitle("식품 상세정보")
            .setView(dialogView)
            .setPositiveButton("닫기", null) // "닫기" 버튼
            .setNeutralButton("수정") { _, _ -> // "수정" 버튼 -> AddFoodBottomSheetFragment 실행
                val editFragment = AddFoodBottomSheetFragment.newInstance(item.id) // item.id 전달
                editFragment.show(parentFragmentManager, AddFoodBottomSheetFragment.TAG_EDIT)
            }
            .setNegativeButton("삭제") { _, _ -> // "삭제" 버튼 -> 삭제 확인 다이얼로그 호출
                showDeleteConfirmationDialog(item)
            }
            .create().show()
    }

    // Fragment의 View가 파괴될 때 호출됨 (메모리 누수 방지)
    override fun onDestroyView() {
        Log.d("HomeFragmentLifecycle", "onDestroyView called")
        super.onDestroyView()
        _binding = null // _binding 참조를 명시적으로 해제하여 메모리 누수 방지
    }
}