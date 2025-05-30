// File: app/src/main/java/com/example/freshbox/ui/list/HomeFragment.kt
package com.example.freshbox.ui.list

import android.app.AlertDialog
import android.content.Context // Context import 추가 (필요시)
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter // ArrayAdapter import 추가
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

    // 카테고리 필터용 멤버 변수 추가
    private var categoryFilterDisplayList: MutableList<String> = mutableListOf()
    private var actualCategoryList: List<Category> = emptyList()
    private lateinit var categoryFilterAdapter: ArrayAdapter<String>
    private var currentSelectedCategoryNameInFilter: String? = null // 현재 선택된 카테고리 이름 저장용


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupRecyclerViews()
        setupCategoryFilter() // 카테고리 필터 설정 호출
        setupListeners()      // setupListeners는 setupCategoryFilter 이후에 호출하여 리스너 중복 방지
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

    // 카테고리 필터 AutoCompleteTextView 설정
    private fun setupCategoryFilter() {
        // XML에 binding.autoCompleteCategoryFilter ID가 있다고 가정
        categoryFilterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryFilterDisplayList)
        binding.autoCompleteCategoryFilter.setAdapter(categoryFilterAdapter)

        binding.autoCompleteCategoryFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            currentSelectedCategoryNameInFilter = selectedName // 현재 선택된 이름 저장
            var categoryIdToSet: Long? = null
            if (selectedName == "카테고리") {
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
                viewModel.setSearchKeyword(keyword)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        // 카테고리 필터 리스너는 setupCategoryFilter()에서 이미 설정됨
    }

    private fun observeViewModel() {
        viewModel.allCategories.observe(viewLifecycleOwner, Observer { categories ->
            actualCategoryList = categories ?: emptyList()
            allCategoriesMap = actualCategoryList.associateBy({ it.id }, { it.name })

            // 카테고리 필터 UI 어댑터 데이터 업데이트
            categoryFilterDisplayList.clear()
            categoryFilterDisplayList.add("카테고리") // "전체" 옵션 항상 추가
            categoryFilterDisplayList.addAll(actualCategoryList.map { it.name })
            categoryFilterAdapter.notifyDataSetChanged()

            // 만약 이전에 선택된 카테고리 이름이 있다면, 목록 업데이트 후 다시 설정 시도
            currentSelectedCategoryNameInFilter?.let { name ->
                if (categoryFilterDisplayList.contains(name)) {
                    binding.autoCompleteCategoryFilter.setText(name, false)
                } else { // 이전 선택 항목이 새 목록에 없으면 "전체 카테고리"로
                    binding.autoCompleteCategoryFilter.setText("카테고리", false)
                    viewModel.setCategoryFilter(null) // ViewModel 상태도 초기화
                    currentSelectedCategoryNameInFilter = "카테고리"
                }
            } ?: run { // 이전에 선택된 것 없으면 "전체 카테고리"로 초기 설정
                binding.autoCompleteCategoryFilter.setText("카테고리", false)
                // viewModel.setCategoryFilter(null) // ViewModel의 초기값은 null이므로 중복 호출 방지
            }
        })

        viewModel.homeExpiringSoonItems.observe(viewLifecycleOwner, Observer { items ->
            Log.d("HomeFragment", "Observed homeExpiringSoonItems. Count: ${items?.size ?: 0}")
            expiringAdapter.submitList(items?.take(10) ?: emptyList()) // null일 경우 emptyList 전달
            updateEmptyViewVisibility()
        })

        viewModel.homeExpiredItems.observe(viewLifecycleOwner, Observer { items ->
            Log.d("HomeFragment", "Observed homeExpiredItems. Count: ${items?.size ?: 0}")
            expiredAdapter.submitList(items?.take(10) ?: emptyList()) // null일 경우 emptyList 전달
            updateEmptyViewVisibility()
        })
    }

    // ... (updateEmptyViewVisibility, showDeleteConfirmationDialog, showFoodDetailDialog, onDestroyView는 이전과 동일하게 유지) ...
    private fun updateEmptyViewVisibility() {
        // ListAdapter는 currentList를 사용합니다.
        val isExpiringEmpty = expiringAdapter.currentList.isEmpty()
        val isExpiredEmpty = expiredAdapter.currentList.isEmpty()
        binding.textViewEmptyMessage.visibility = if (isExpiringEmpty && isExpiredEmpty) View.VISIBLE else View.GONE
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
        // XML에 정의된 ID (예: R.id.textCategory)를 사용해야 합니다.
        dialogView.findViewById<TextView>(R.id.textCategory).text = "카테고리: $categoryName"

        dialogView.findViewById<TextView>(R.id.textStorage).text = "보관 위치: ${item.storageLocation ?: ""}"
        dialogView.findViewById<TextView>(R.id.textPurchaseDate).text = "구매일: ${item.purchaseDate?.let { dateFormat.format(it) } ?: ""}"
        dialogView.findViewById<TextView>(R.id.textMemo).text = "메모: ${item.memo ?: ""}"

        // 태그 표시 (dialog_food_detail.xml에 R.id.textViewDialogTags 와 같은 ID의 TextView가 있다고 가정)
        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags) // 예시 ID
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