// File: app/src/main/java/com/example/freshbox/ui/all/AllFoodsActivity.kt
package com.example.freshbox.ui.all

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // by viewModels() 사용을 위해 (Activity용)
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer // LiveData 관찰을 위해
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.freshbox.data.FoodItem // data.FoodItem (Room Entity) 사용
import com.example.freshbox.databinding.ActivityAllFoodsBinding
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment // AddFoodBottomSheetFragment import 추가
import com.example.freshbox.ui.list.FoodListAdapter
import com.example.freshbox.ui.list.FoodListViewModel
import com.example.freshbox.ui.list.FoodFilterType // FoodFilterType enum import

class AllFoodsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllFoodsBinding
    private lateinit var adapter: FoodListAdapter

    private val viewModel: FoodListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllFoodsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FoodListAdapter(
            onItemClick = { foodItem ->
                // 클릭된 FoodItem의 ID를 사용하여 AddFoodBottomSheetFragment를 수정 모드로 실행
                // AddFoodBottomSheetFragment의 companion object에 TAG_EDIT 및 newInstance(id)가 정의되어 있어야 함
                val editFragment = AddFoodBottomSheetFragment.newInstance(foodItem.id)
                editFragment.show(supportFragmentManager, AddFoodBottomSheetFragment.TAG_EDIT)
            },
            onItemLongClick = { foodItem ->
                // TODO: 삭제 확인 다이얼로그 표시 로직 (HomeFragment의 showDeleteConfirmationDialog 참고하여 구현 가능)
                // 예시: showDeleteConfirmationDialogInAllFoods(foodItem)
                Toast.makeText(this, "길게 클릭: ${foodItem.name} (삭제 기능 구현 필요)", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewAllFoods.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAllFoods.adapter = adapter

        observeViewModel()
        setupSwipeToDelete()

        viewModel.setFilterTypeForAllActivity(FoodFilterType.ALL)
        viewModel.setCategoryFilter(null)
        viewModel.setSearchKeyword(null)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "모든 식품 목록"
    }

    private fun observeViewModel() {
        viewModel.filteredFoodItemsForAllActivity.observe(this, Observer { items: List<FoodItem>? ->
            adapter.submitList(items ?: emptyList())
            binding.textViewEmpty.visibility = if (items.isNullOrEmpty()) View.VISIBLE else View.GONE
        })
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) { // 유효한 포지션인지 확인
                    val itemToDelete = adapter.currentList[position]
                    viewModel.deleteFoodItem(itemToDelete)
                    Toast.makeText(this@AllFoodsActivity, "삭제됨: ${itemToDelete.name}", Toast.LENGTH_SHORT).show()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewAllFoods)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // 기본 뒤로가기 동작 실행
        return true
    }

    // (선택 사항) 삭제 확인 다이얼로그 함수
    /*
    private fun showDeleteConfirmationDialogInAllFoods(item: FoodItem) {
        AlertDialog.Builder(this)
            .setTitle("삭제 확인")
            .setMessage("'${item.name}' 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteFoodItem(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    */
}