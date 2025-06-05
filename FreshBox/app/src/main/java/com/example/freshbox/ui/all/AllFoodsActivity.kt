// File: app/src/main/java/com/example/freshbox/ui/all/AllFoodsActivity.kt
package com.example.freshbox.ui.all

import android.app.AlertDialog // 삭제 확인 다이얼로그를 위해 필요 (주석 처리된 부분에서 사용 가능)
import android.os.Bundle
import android.view.View // View.VISIBLE, View.GONE 사용
import android.widget.Toast // 간단한 메시지 표시
import androidx.activity.viewModels // KTX를 사용하여 ViewModel 인스턴스화
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 상속
import androidx.lifecycle.Observer // LiveData를 관찰하기 위해 필요
import androidx.recyclerview.widget.ItemTouchHelper // RecyclerView 아이템 스와이프 감지
import androidx.recyclerview.widget.LinearLayoutManager // RecyclerView의 레이아웃 매니저
import androidx.recyclerview.widget.RecyclerView // RecyclerView 위젯
import com.example.freshbox.data.FoodItem // FoodItem 데이터 Entity (Room)
import com.example.freshbox.databinding.ActivityAllFoodsBinding // ViewBinding 클래스
import com.example.freshbox.ui.addedit.AddFoodBottomSheetFragment // 식품 추가/수정 UI
import com.example.freshbox.ui.list.FoodListAdapter // 식품 목록 표시용 RecyclerView 어댑터
import com.example.freshbox.ui.list.FoodListViewModel // UI 로직 및 데이터 관리를 위한 ViewModel
import com.example.freshbox.ui.list.FoodFilterType // 식품 필터링 타입 Enum

// 모든 식품 목록을 보여주는 Activity
class AllFoodsActivity : AppCompatActivity() {

    // ViewBinding 객체 선언
    private lateinit var binding: ActivityAllFoodsBinding
    // RecyclerView에 사용될 어댑터
    private lateinit var adapter: FoodListAdapter

    // FoodListViewModel 인스턴스를 KTX 델리게이트를 사용하여 가져옴
    // Activity의 생명주기에 맞춰 ViewModel이 관리됨
    private val viewModel: FoodListViewModel by viewModels()

    // Activity가 생성될 때 호출되는 생명주기 메서드
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewBinding을 사용하여 activity_all_foods.xml 레이아웃을 inflate하고 바인딩 객체 초기화
        binding = ActivityAllFoodsBinding.inflate(layoutInflater)
        // 바인딩된 레이아웃의 루트 뷰를 Activity의 컨텐츠 뷰로 설정
        setContentView(binding.root)

        // FoodListAdapter 초기화. 아이템 클릭 및 롱클릭 시의 동작을 람다 함수로 전달.
        adapter = FoodListAdapter(
            onItemClick = { foodItem -> // 아이템 클릭 시
                // 클릭된 FoodItem의 ID를 사용하여 AddFoodBottomSheetFragment를 "수정 모드"로 실행
                val editFragment = AddFoodBottomSheetFragment.newInstance(foodItem.id)
                // AddFoodBottomSheetFragment의 companion object에 TAG_EDIT가 정의되어 있어야 함
                editFragment.show(supportFragmentManager, AddFoodBottomSheetFragment.TAG_EDIT)
            },
            onItemLongClick = { foodItem -> // 아이템 롱클릭 시
                // TODO: 삭제 확인 다이얼로그 표시 로직을 여기에 구현하거나,
                //       별도의 함수(예: showDeleteConfirmationDialogInAllFoods)를 만들어 호출할 수 있음.
                //       HomeFragment의 showDeleteConfirmationDialog 메서드를 참고하여 유사하게 구현 가능.
                Toast.makeText(this, "길게 클릭: ${foodItem.name} (삭제 기능 구현 필요)", Toast.LENGTH_SHORT).show()
            }
        )
        // RecyclerView에 LayoutManager와 위에서 초기화한 어댑터 설정
        binding.recyclerViewAllFoods.layoutManager = LinearLayoutManager(this) // 수직 리스트
        binding.recyclerViewAllFoods.adapter = adapter

        // ViewModel의 LiveData 관찰 시작
        observeViewModel()
        // RecyclerView 아이템 스와이프 삭제 기능 설정
        setupSwipeToDelete()

        // Activity가 처음 생성될 때 ViewModel의 필터 상태를 초기화합니다.
        // 여기서는 "모든 식품"을 보도록 설정합니다.
        viewModel.setFilterTypeForAllActivity(FoodFilterType.ALL)
        viewModel.setCategoryFilter(null)  // 카테고리 필터 없음
        viewModel.setSearchKeyword(null) // 검색어 없음

        // ActionBar(앱 상단 바)에 뒤로가기 버튼(Up 버튼)을 표시하도록 설정 (선택 사항)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Activity의 제목 설정
        title = "모든 식품 목록"
    }

    // ViewModel의 LiveData 변경 사항을 관찰하고 UI를 업데이트하는 함수
    private fun observeViewModel() {
        // FoodListViewModel의 filteredFoodItemsForAllActivity LiveData를 관찰합니다.
        // 이 LiveData는 현재 설정된 필터(타입, 카테고리, 검색어)에 따라 최종적으로 필터링된 식품 목록을 제공합니다.
        viewModel.filteredFoodItemsForAllActivity.observe(this, Observer { items: List<FoodItem>? ->
            // LiveData로부터 새로운 아이템 리스트를 받으면 어댑터에 제출합니다.
            // items가 null일 경우 빈 리스트로 처리하여 NullPointerException 방지.
            adapter.submitList(items ?: emptyList())
            // 목록이 비어있으면 "등록된 식품이 없습니다" 메시지(textViewEmpty)를 표시하고,
            // 그렇지 않으면 숨깁니다. (activity_all_foods.xml에 textViewEmpty ID가 있어야 함)
            binding.textViewEmpty.visibility = if (items.isNullOrEmpty()) View.VISIBLE else View.GONE
        })
    }

    // RecyclerView 아이템을 스와이프하여 삭제하는 기능을 설정하는 함수
    private fun setupSwipeToDelete() {
        // ItemTouchHelper는 RecyclerView의 아이템에 터치 이벤트(드래그, 스와이프)를 감지하고 콜백을 제공합니다.
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, // 드래그 방향 (0은 드래그 사용 안 함)
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // 스와이프 방향 (왼쪽 또는 오른쪽)
        ) {
            // 아이템이 드래그될 때 호출되는 메서드 (여기서는 사용 안 함)
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false // true를 반환하면 아이템 이동 처리

            // 아이템이 스와이프되었을 때 호출되는 메서드
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition // 스와이프된 아이템의 어댑터 내 위치
                // 유효한 포지션인지 확인 (아이템이 빠르게 삭제되거나 하는 경우 NO_POSITION일 수 있음)
                if (position != RecyclerView.NO_POSITION) {
                    // ListAdapter의 currentList를 사용하여 현재 어댑터가 가지고 있는 목록에서 아이템을 가져옴
                    val itemToDelete = adapter.currentList[position]
                    // ViewModel을 통해 데이터베이스에서 해당 아이템 삭제 요청
                    viewModel.deleteFoodItem(itemToDelete)
                    // 사용자에게 삭제되었음을 Toast 메시지로 알림
                    Toast.makeText(this@AllFoodsActivity, "삭제됨: ${itemToDelete.name}", Toast.LENGTH_SHORT).show()
                    // "등록된 식품이 없습니다" 메시지의 visibility는 LiveData 관찰자를 통해 자동으로 업데이트됨
                }
            }
        })
        // ItemTouchHelper를 RecyclerView에 연결하여 스와이프 기능을 활성화합니다.
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewAllFoods)
    }

    // ActionBar의 Up 버튼(뒤로가기 버튼)이 클릭되었을 때 호출되는 메서드
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // 시스템의 기본 뒤로가기 동작 실행
        return true
    }

    // (선택 사항) 아이템 롱클릭 시 삭제 확인 다이얼로그를 보여주는 함수
    // HomeFragment의 showDeleteConfirmationDialog와 유사하게 구현할 수 있습니다.
    /*
    private fun showDeleteConfirmationDialogInAllFoods(item: FoodItem) {
        AlertDialog.Builder(this) // Activity에서는 this 또는 applicationContext 사용 가능
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