// File: app/src/main/java/com/example/freshbox/ui/list/HomeFragment.kt
package com.example.freshbox.ui.list

import android.app.AlertDialog
import android.content.DialogInterface
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freshbox.R
import com.example.freshbox.data.Category
import com.example.freshbox.data.FoodItem
import com.example.freshbox.databinding.FragmentHomeBinding
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment
import com.example.freshbox.ui.all.AllFoodsActivity
import com.example.freshbox.util.ThemeHelper
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
    private var actualCategoryList: List<Category> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupRecyclerViews()
        setupCategoryFilterListener()
        setupListeners()
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
        binding.recyclerViewExpired.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpired.adapter = expiredAdapter
        binding.recyclerViewExpiring.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewExpiring.adapter = expiringAdapter
    }

    private fun setupCategoryFilterListener() {
        binding.autoCompleteCategoryFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val categoryIdToSet: Long? = if (selectedName == "전체") {
                null
            } else {
                actualCategoryList.find { it.name == selectedName }?.id
            }
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
                viewModel.setSearchKeyword(keyword)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.imageViewSettings.setOnClickListener {
            showThemeSelectionDialog()
        }

        // '카테고리 관리' 아이콘 버튼에 대한 클릭 리스너 설정
        binding.imageViewManageCategories.setOnClickListener {
            showCategoryManagementDialog()
        }
    }

    /**
     * 사용자가 직접 추가한 카테고리를 관리(삭제)할 수 있는 다이얼로그를 표시합니다.
     */
    private fun showCategoryManagementDialog() {
        // ViewModel의 .value를 직접 읽는 대신, LiveData 옵저버를 통해 이미 업데이트된
        // 프래그먼트의 'actualCategoryList'를 사용합니다. 이렇게 하면 타이밍 문제를 피할 수 있습니다.
        val customCategories = actualCategoryList.filter { it.isCustom }

        // 삭제할 카테고리가 없으면 사용자에게 Toast 메시지를 보여주고 함수를 종료합니다.
        if (customCategories.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 수 있는 카테고리가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 다이얼로그에 표시할 카테고리 이름 목록을 만듭니다.
        val categoryNames = customCategories.map { it.name }.toTypedArray()

        // AlertDialog를 생성하여 카테고리 목록을 보여줍니다.
        AlertDialog.Builder(requireContext())
            .setTitle("카테고리 삭제")
            .setItems(categoryNames) { dialog, which ->
                // 사용자가 목록에서 특정 카테고리를 선택했을 때
                val selectedCategory = customCategories[which]

                // 삭제를 한 번 더 확인하는 다이얼로그를 띄웁니다.
                AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("'${selectedCategory.name}' 카테고리를 삭제하시겠습니까?\n\n해당 카테고리의 모든 식품은 '미지정' 상태로 변경됩니다.")
                    .setPositiveButton("삭제") { _, _ ->
                        // ViewModel의 deleteCategory 함수를 호출하여 삭제를 실행합니다.
                        viewModel.deleteCategory(selectedCategory)
                        Toast.makeText(requireContext(), "'${selectedCategory.name}' 카테고리가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themeOptions = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system_default))
        val themeModes = intArrayOf(ThemeHelper.LIGHT_MODE, ThemeHelper.DARK_MODE, ThemeHelper.DEFAULT_MODE)
        val currentThemeMode = ThemeHelper.getThemeMode(requireContext())
        var checkedItem = themeModes.indexOf(currentThemeMode).let { if (it == -1) 0 else it }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(themeOptions, checkedItem) { dialog: DialogInterface, which: Int ->
                val selectedMode = themeModes[which]
                if (ThemeHelper.getThemeMode(requireContext()) != selectedMode) {
                    ThemeHelper.setThemeMode(requireContext(), selectedMode)
                    activity?.recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            actualCategoryList = categories ?: emptyList()
            allCategoriesMap = actualCategoryList.associateBy({ it.id }, { it.name })
            val categoryNames = mutableListOf("전체")
            categoryNames.addAll(actualCategoryList.map { it.name })
            val newAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.autoCompleteCategoryFilter.setAdapter(newAdapter)
        }

        viewModel.selectedCategoryId.observe(viewLifecycleOwner) { categoryId ->
            val categoryName = if (categoryId == null) "전체" else allCategoriesMap[categoryId] ?: "전체"
            if (binding.autoCompleteCategoryFilter.text.toString() != categoryName) {
                binding.autoCompleteCategoryFilter.setText(categoryName, false)
            }
        }

        viewModel.homeExpiredItems.observe(viewLifecycleOwner) { items ->
            expiredAdapter.submitList(items)
        }
        viewModel.homeExpiringSoonItems.observe(viewLifecycleOwner) { items ->
            expiringAdapter.submitList(items)
        }
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
                        ?: imageView.setImageResource(R.drawable.ic_add)
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

        val tagsTextView = dialogView.findViewById<TextView>(R.id.textViewDialogTags)
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
            .setNegativeButton("삭제") { _, _ ->
                showDeleteConfirmationDialog(item)
            }
            .create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}