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
                // TODO: 상세 보기 또는 수정 화면으로 이동 로직
                Toast.makeText(this, "Clicked: ${foodItem.name}", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { foodItem ->
                // TODO: 삭제 확인 다이얼로그 표시 로직
                Toast.makeText(this, "Long Clicked: ${foodItem.name}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewAllFoods.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAllFoods.adapter = adapter

        observeViewModel()
        setupSwipeToDelete()

        // Activity가 처음 생성될 때 모든 아이템을 보도록 필터 타입 설정
        viewModel.setFilterTypeForAllActivity(FoodFilterType.ALL) // <<< 모든 아이템을 보도록 필터 설정
        // 필요하다면 카테고리 필터나 태그 검색어도 초기화
        viewModel.setCategoryFilter(null)
        viewModel.setSearchKeyword(null)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "모든 식품 목록"
    }

    private fun observeViewModel() {
        // FoodListViewModel의 filteredFoodItems LiveData를 관찰
        viewModel.filteredFoodItemsForAllActivity.observe(this, Observer { items: List<com.example.freshbox.data.FoodItem>? -> // <<< LiveData 이름 변경
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
                val itemToDelete = adapter.currentList.getOrNull(position)

                itemToDelete?.let {
                    viewModel.deleteFoodItem(it)
                    Toast.makeText(this@AllFoodsActivity, "삭제됨: ${it.name}", Toast.LENGTH_SHORT).show()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewAllFoods)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}