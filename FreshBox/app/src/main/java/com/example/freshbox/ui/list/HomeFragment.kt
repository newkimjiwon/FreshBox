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
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.R
import com.example.freshbox.data.Category
import com.example.freshbox.data.FoodItem
import com.example.freshbox.databinding.FragmentHomeBinding
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment
import com.example.freshbox.ui.all.AllFoodsActivity
import com.example.freshbox.util.ThemeHelper // ThemeHelper import 추가
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FoodListViewModel by viewModels()

    private lateinit var expiredAdapter: FoodListAdapter
    private lateinit var expiringAdapter: FoodListAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var allCategoriesMap: Map<Long, String> = emptyMap()

    private var categoryFilterDisplayList: MutableList<String> = mutableListOf()
    private var actualCategoryList: List<Category> = emptyList()
    private lateinit var categoryFilterAdapter: ArrayAdapter<String>
    private var currentSelectedCategoryNameInFilter: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupRecyclerViews()
        setupCategoryFilter()
        setupListeners() // 설정 아이콘 리스너가 여기에 포함될 것임
        observeViewModel()
    }

    private fun setupAdapters() {
        expiredAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) },
            onItemLongClick = { foodItem -> showDeleteConfirmationDialog(foodItem) }
        )
        expiringAdapter = FoodListAdapter(
            onItemClick = { foodItem -> showFoodDetailDialog(foodItem) },
            onItemLongClick = { foodItem -> showDeleteConfirmationDialog(foodItem) }
        )
    }

    private fun setupRecyclerViews() {
        binding.recyclerViewExpired.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpired.adapter = expiredAdapter

        binding.recyclerViewExpiring.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpiring.adapter = expiringAdapter
    }

    private fun setupCategoryFilter() {
        // XML에 binding.autoCompleteCategoryFilter ID가 있다고 가정
        // (fragment_home.xml에 해당 ID의 AutoCompleteTextView가 있어야 합니다)
        categoryFilterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryFilterDisplayList)
        binding.autoCompleteCategoryFilter.setAdapter(categoryFilterAdapter) // XML ID: autoCompleteCategoryFilter

        binding.autoCompleteCategoryFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            currentSelectedCategoryNameInFilter = selectedName
            var categoryIdToSet: Long? = null
            if (selectedName == "전체") { // "전체 카테고리" 대신 "전체"로 통일 (observeViewModel과 일치)
                categoryIdToSet = null
            } else {
                val selectedCategory = actualCategoryList.find { it.name == selectedName }
                categoryIdToSet = selectedCategory?.id
            }
            Log.d("HomeFragment_Category", "Category selected: '$selectedName', ID to set in ViewModel: $categoryIdToSet")
            viewModel.setCategoryFilter(categoryIdToSet)
        }
    }

    private fun setupListeners() {
        binding.fabAddItem.setOnClickListener {
            // AddFoodBottomSheetFragment의 companion object에 TAG가 정의되어 있어야 함
            AddFoodBottomSheetFragment().show(parentFragmentManager, AddFoodBottomSheetFragment.TAG)
        }
        binding.buttonViewAll.setOnClickListener {
            startActivity(Intent(requireContext(), AllFoodsActivity::class.java))
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim().ifEmpty { null }
                Log.d("HomeFragment_Search", "Search keyword input: $keyword")
                viewModel.setSearchKeyword(keyword) // ViewModel에 검색어 전달
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 설정 아이콘 클릭 리스너 추가
        // fragment_home.xml의 imageViewSettings ID를 가진 View에 직접 접근
        binding.imageViewSettings.setOnClickListener {
            showThemeSelectionDialog() // 이 함수는 HomeFragment 내에 정의되어 있어야 함
        }
        // 카테고리 필터 리스너는 setupCategoryFilter()에서 이미 설정됨 (이전 답변 내용)
    }

    private fun showThemeSelectionDialog() {
        val themeOptions = arrayOf(
            getString(R.string.theme_light),    // strings.xml에 정의된 문자열 사용
            getString(R.string.theme_dark),
            getString(R.string.theme_system_default)
        )

        val themeModes = intArrayOf(
            ThemeHelper.LIGHT_MODE,
            ThemeHelper.DARK_MODE,
            ThemeHelper.DEFAULT_MODE
        )

        val currentThemeMode = ThemeHelper.getThemeMode(requireContext())
        var checkedItem = themeModes.indexOf(currentThemeMode)
        if (checkedItem == -1) {
            // 기본값으로 "시스템 설정 따름" 또는 LIGHT_MODE 선호도에 따라 설정
            checkedItem = themeModes.indexOf(ThemeHelper.DEFAULT_MODE) // DEFAULT_MODE를 기본으로
            if (checkedItem == -1) checkedItem = 0 // 혹시 모를 경우 첫 번째 옵션
        }


        AlertDialog.Builder(requireContext())
            .setTitle(R.string.theme_dialog_title) // strings.xml에 정의된 문자열 사용
            .setSingleChoiceItems(themeOptions, checkedItem) { dialog, which ->
                val selectedMode = themeModes[which]
                if (ThemeHelper.getThemeMode(requireContext()) != selectedMode) {
                    ThemeHelper.setThemeMode(requireContext(), selectedMode)
                    // 테마 변경을 즉시 적용하기 위해 Activity 재시작
                    activity?.recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.allCategories.observe(viewLifecycleOwner, Observer { categories ->
            actualCategoryList = categories ?: emptyList()
            allCategoriesMap = actualCategoryList.associateBy({ it.id }, { it.name })

            categoryFilterDisplayList.clear()
            categoryFilterDisplayList.add("전체") // "전체 카테고리" 대신 "전체"로 통일
            categoryFilterDisplayList.addAll(actualCategoryList.map { it.name })
            categoryFilterAdapter.notifyDataSetChanged()

            currentSelectedCategoryNameInFilter?.let { name ->
                if (categoryFilterDisplayList.contains(name)) {
                    binding.autoCompleteCategoryFilter.setText(name, false)
                } else {
                    binding.autoCompleteCategoryFilter.setText("전체", false)
                    viewModel.setCategoryFilter(null)
                    currentSelectedCategoryNameInFilter = "전체"
                }
            } ?: run {
                binding.autoCompleteCategoryFilter.setText("전체", false)
            }
        })

        viewModel.homeExpiringSoonItems.observe(viewLifecycleOwner, Observer { items ->
            Log.d("HomeFragment", "Observed homeExpiringSoonItems. Count: ${items?.size ?: 0}")
            expiringAdapter.submitList(items?.take(10) ?: emptyList())
        })

        viewModel.homeExpiredItems.observe(viewLifecycleOwner, Observer { items ->
            Log.d("HomeFragment", "Observed homeExpiredItems. Count: ${items?.size ?: 0}")
            expiredAdapter.submitList(items?.take(10) ?: emptyList())
        })
    }

    private fun showDeleteConfirmationDialog(item: FoodItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${item.name}' 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> viewModel.deleteFoodItem(item) }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showFoodDetailDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detail, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFood)
        item.imagePath?.let { path ->
            if (path.isNotEmpty()) {
                val imageFile = File(path)
                if (imageFile.exists()) {
                    BitmapFactory.decodeFile(imageFile.absolutePath)?.let { imageView.setImageBitmap(it) }
                        ?: imageView.setImageResource(R.drawable.ic_add) // 사용 가능한 플레이스홀더로 변경
                } else { imageView.setImageResource(R.drawable.ic_add) }
            } else { imageView.setImageResource(R.drawable.ic_add) }
        } ?: imageView.setImageResource(R.drawable.ic_add)

        dialogView.findViewById<TextView>(R.id.textFoodName).text = "식품명: ${item.name}"
        dialogView.findViewById<TextView>(R.id.textExpiryDate).text = "소비기한: ${dateFormat.format(item.expiryDate)}"
        dialogView.findViewById<TextView>(R.id.textQuantity).text = "수량: ${item.quantity}"
        val categoryName = item.categoryId?.let { allCategoriesMap[it] } ?: "미지정"
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: $categoryName"

        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation ?: ""}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate?.let { dateFormat.format(it) } ?: ""}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo ?: ""}"

        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags) // XML에 이 ID가 있어야 함
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
            .setNegativeButton("삭제") { _, _ -> showDeleteConfirmationDialog(item) }
            .create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}