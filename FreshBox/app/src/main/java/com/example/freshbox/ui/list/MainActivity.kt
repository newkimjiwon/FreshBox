package com.example.freshbox.ui.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.freshbox.databinding.ActivityMainBinding
import com.example.freshbox.ui.addedit.AddEditFoodActivity // AddEditFoodActivity import 확인
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FoodListViewModel by viewModels() // FoodListViewModel 사용
    private lateinit var foodListAdapter: FoodListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupTabs()

        viewModel.filteredFoodItems.observe(this) { items ->
            foodListAdapter.submitList(items ?: emptyList())
        }

        binding.fabAddItem.setOnClickListener {
            val intent = Intent(this, AddEditFoodActivity::class.java)
            startActivity(intent)
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < foodListAdapter.currentList.size) {
                    val foodItem = foodListAdapter.currentList[position]
                    viewModel.deleteFoodItem(foodItem) // 또는 viewModel.onFoodItemSwiped(foodItem)
                    Snackbar.make(binding.root, "${foodItem.name} 삭제됨", Snackbar.LENGTH_LONG)
                        .setAction("실행 취소") {
                            // viewModel.undoDelete() // 삭제 취소 기능 구현 시
                        }.show()
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewFoodItems)
    }

    private fun setupRecyclerView() {
        foodListAdapter = FoodListAdapter { foodItem ->
            val intent = Intent(this, AddEditFoodActivity::class.java)
            intent.putExtra(AddEditFoodActivity.EXTRA_FOOD_ID, foodItem.id)
            startActivity(intent)
        }
        binding.recyclerViewFoodItems.apply {
            adapter = foodListAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0 -> viewModel.setFilter(FoodFilterType.ACTIVE)
                        1 -> viewModel.setFilter(FoodFilterType.EXPIRING_SOON)
                        2 -> viewModel.setFilter(FoodFilterType.EXPIRED)
                        3 -> viewModel.setFilter(FoodFilterType.ALL)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // 초기 탭 선택 및 필터 설정
        // ACTIVE 탭 (0번 인덱스)을 기본으로 선택하여 리스너가 호출되도록 함
        binding.tabLayout.getTabAt(0)?.select()
        // 만약 select() 호출로 onTabSelected가 바로 호출되지 않는다면 (호출되는 것이 일반적임)
        // 또는 ViewModel의 초기 필터 상태와 동기화하려면 아래처럼 명시적으로 호출할 수 있습니다.
        // if (viewModel.filterType.value != FoodFilterType.ACTIVE) { // ViewModel에 현재 필터 상태 LiveData가 있다면
        //    viewModel.setFilter(FoodFilterType.ACTIVE)
        // }
        // 혹은, 가장 간단하게는 ViewModel의 _filterType 기본값이 ACTIVE이므로,
        // init 블록에서 updateFilteredData가 호출되어 초기 데이터가 로드됩니다.
        // 따라서 아래 코드는 불필요할 수 있습니다.
        // viewModel.setFilter(FoodFilterType.ACTIVE)
    }
}