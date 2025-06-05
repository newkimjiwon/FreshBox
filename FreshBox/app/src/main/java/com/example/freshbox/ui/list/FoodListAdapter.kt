// File: app/src/main/java/com/example/freshbox/ui/list/FoodListAdapter.kt
package com.example.freshbox.ui.list

import android.graphics.BitmapFactory
import android.util.Log // Log 사용을 위해 추가 (선택 사항)
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil // RecyclerView의 리스트 변경 사항을 효율적으로 계산하기 위해 사용
import androidx.recyclerview.widget.ListAdapter // RecyclerView.Adapter를 개선한 버전, DiffUtil과 함께 사용
import androidx.recyclerview.widget.RecyclerView // RecyclerView 관련 클래스 사용을 위해 import
import com.example.freshbox.R // R.layout.item_food 등 리소스 ID 참조
import com.example.freshbox.data.FoodItem // Room Entity com.example.freshbox.data.FoodItem 사용
// import com.example.freshbox.model.FoodItem // 이전 모델 클래스, 현재는 data.FoodItem 사용
import java.io.File // 이미지 파일 접근
import java.util.* // Calendar 클래스 사용

// ListAdapter를 상속받아 RecyclerView에 식품 목록을 표시합니다.
// ListAdapter는 DiffUtil을 사용하여 리스트 업데이트를 효율적으로 처리합니다.
class FoodListAdapter(
    // 아이템 클릭 시 실행될 람다 함수 (Nullable, 외부에서 설정 가능)
    private val onItemClick: ((com.example.freshbox.data.FoodItem) -> Unit)? = null,
    // 아이템 롱클릭 시 실행될 람다 함수 (Nullable, 외부에서 설정 가능)
    private val onItemLongClick: ((com.example.freshbox.data.FoodItem) -> Unit)? = null
    // data.FoodItem 타입의 아이템을 사용하고, ViewHolder는 FoodListAdapter.FoodViewHolder를 사용합니다.
    // FoodItemDiffCallback은 아이템 변경 시 DiffUtil이 비교하는 방법을 정의합니다.
) : ListAdapter<com.example.freshbox.data.FoodItem, FoodListAdapter.FoodViewHolder>(FoodItemDiffCallback()) {

    // ListAdapter를 사용하면 submitList 메서드는 이미 구현되어 있으므로,
    // 직접 foodList 변수를 관리하거나 notifyDataSetChanged()를 호출할 필요가 없습니다.
    // ListAdapter가 내부적으로 리스트를 관리하고, DiffUtil을 통해 변경 사항만 효율적으로 업데이트합니다.

    // 새로운 ViewHolder 인스턴스를 생성합니다.
    // 이 메서드는 RecyclerView가 화면에 새로운 아이템을 표시해야 할 때 호출됩니다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        // item_food.xml 레이아웃 파일을 inflate하여 View 객체를 생성합니다.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view) // 생성된 View로 FoodViewHolder 인스턴스를 만듭니다.
    }

    // 특정 위치(position)의 아이템 데이터를 ViewHolder의 View들에 바인딩(연결)합니다.
    // 이 메서드는 RecyclerView가 화면에 아이템을 그리거나 업데이트할 때 호출됩니다.
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = getItem(position) // ListAdapter의 getItem() 메서드를 사용하여 현재 위치의 FoodItem 객체를 가져옵니다.
        holder.bind(item) // ViewHolder의 bind 메서드를 호출하여 데이터를 View에 설정합니다.

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item) // 생성자에서 전달받은 onItemClick 람다 함수 실행
        }

        // 아이템 롱클릭 리스너 설정
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(item) // 생성자에서 전달받은 onItemLongClick 람다 함수 실행
            true // 롱클릭 이벤트를 소비했음을 반환 (다른 클릭 이벤트 방지)
        }
    }

    // ListAdapter를 사용하면 getItemCount() 메서드를 직접 구현할 필요가 없습니다.
    // ListAdapter가 내부적으로 현재 리스트의 크기를 관리합니다.

    // 각 아이템 View의 구성요소를 저장하고, 아이템 데이터를 해당 View에 바인딩하는 역할을 하는 ViewHolder 클래스입니다.
    // RecyclerView.ViewHolder를 상속받습니다.
    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // item_food.xml 레이아웃 내의 View들을 ID로 찾아와 프로퍼티로 저장합니다.
        // 예시 ID: textViewFoodName (식품 이름), textViewDday (D-day), imageViewFood (식품 이미지)
        // 이 ID들이 item_food.xml에 정의되어 있어야 합니다.
        private val nameView: TextView = itemView.findViewById(R.id.textViewFoodName)
        private val ddayView: TextView = itemView.findViewById(R.id.textViewDday)
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewFood)
        // TODO: item_food.xml에 카테고리명과 태그를 표시할 UI 요소(TextView, ChipGroup 등)를 추가하고,
        //       여기에 해당 View들에 대한 프로퍼티를 선언해야 합니다.
        // 예: private val categoryView: TextView = itemView.findViewById(R.id.textViewItemCategory)
        // 예: private val tagsChipGroup: com.google.android.material.chip.ChipGroup = itemView.findViewById(R.id.chipGroupItemTags)


        // FoodItem 객체의 데이터를 ViewHolder의 View들에 설정(바인딩)하는 메서드입니다.
        fun bind(item: com.example.freshbox.data.FoodItem) { // 파라미터 타입을 data.FoodItem으로 사용
            nameView.text = item.name // 식품 이름을 TextView에 설정

            // D-day 계산 로직
            val todayMillis = Calendar.getInstance().timeInMillis // 현재 시간을 밀리초로 가져옵니다.
            val expiryMillis = item.expiryDate.time // FoodItem의 expiryDate(Date 객체)를 밀리초로 변환합니다.
            val diffMillis = expiryMillis - todayMillis // 소비기한까지 남은 시간을 밀리초로 계산합니다.
            // 밀리초를 일(day) 단위로 변환합니다. (1000ms * 60s * 60min * 24h)
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            // D-day 결과에 따라 TextView의 텍스트와 배경을 다르게 설정합니다.
            ddayView.text = when {
                diffDays > 0 -> { // 소비기한이 남은 경우
                    ddayView.setBackgroundResource(R.drawable.bg_dday_soon) // bg_dday_soon 드로어블 배경 사용
                    "D-$diffDays"
                }
                diffDays == 0 -> { // 소비기한이 오늘인 경우
                    ddayView.setBackgroundResource(R.drawable.bg_dday_soon)
                    "D-day"
                }
                else -> { // 소비기한이 지난 경우
                    ddayView.setBackgroundResource(R.drawable.bg_dday_expired) // bg_dday_expired 드로어블 배경 사용
                    "D+${-diffDays}" // 지난 일수를 양수로 표시
                }
            }

            // 이미지 표시 로직 (FoodItem Entity에 imagePath 필드가 있다고 가정)
            item.imagePath?.let { path -> // imagePath가 null이 아니고
                if (path.isNotEmpty()) { // 경로 문자열이 비어있지 않다면
                    val file = File(path) // 파일 객체 생성
                    if (file.exists()) { // 파일이 실제로 존재한다면
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath) // 파일로부터 비트맵 디코딩
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap) // ImageView에 비트맵 설정
                            } else {
                                // 비트맵 디코딩 실패 시 (예: 파일 형식 오류) 로그 남기고 기본 이미지 설정
                                Log.e("FoodListAdapter", "BitmapFactory.decodeFile returned null for path: $path")
                                imageView.setImageResource(R.drawable.ic_launcher_foreground) // 적절한 플레이스홀더 이미지로 교체 필요
                            }
                        } catch (e: Exception) {
                            // 기타 예외 발생 시 (예: OOM 등) 로그 남기고 기본 이미지 설정
                            Log.e("FoodListAdapter", "Error decoding bitmap: ${e.message}", e)
                            imageView.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    } else { // 파일이 존재하지 않으면 기본 이미지 설정
                        imageView.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                } else { // 경로 문자열이 비어있으면 기본 이미지 설정
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } ?: imageView.setImageResource(R.drawable.ic_launcher_foreground) // imagePath 자체가 null이면 기본 이미지 설정

            // TODO: 카테고리 이름 표시 로직
            // item.categoryId (Long 타입)를 사용하여 카테고리 이름을 가져와 categoryView에 설정해야 합니다.
            // 이 정보는 Fragment/Activity 레벨에서 FoodListViewModel의 allCategoriesMap 등을 통해 변환하여
            // FoodItem과 함께 어댑터에 전달하거나, 어댑터 생성 시 allCategoriesMap을 전달받아 여기서 조회할 수 있습니다.
            // 예: categoryView.text = getCategoryNameById(item.categoryId, allCategoriesMapFromFragment)

            // TODO: 태그 표시 로직
            // item.tags (List<String>)를 사용하여 tagsChipGroup 등에 Chip을 동적으로 추가하여 표시합니다.
            // 예: tagsChipGroup.removeAllViews()
            // item.tags.forEach { tagName ->
            //     val chip = com.google.android.material.chip.Chip(itemView.context)
            //     chip.text = tagName
            //     // chip 스타일 설정 (선택 사항)
            //     tagsChipGroup.addView(chip)
            // }
        }
    }

    // DiffUtil.ItemCallback 구현 클래스
    // ListAdapter가 리스트의 변경 사항을 효율적으로 계산하고 애니메이션을 적용하는 데 사용됩니다.
    class FoodItemDiffCallback : DiffUtil.ItemCallback<com.example.freshbox.data.FoodItem>() {
        // 두 아이템이 동일한 항목을 나타내는지 확인합니다. (보통 고유 ID로 비교)
        override fun areItemsTheSame(oldItem: com.example.freshbox.data.FoodItem, newItem: com.example.freshbox.data.FoodItem): Boolean {
            return oldItem.id == newItem.id // FoodItem의 id 프로퍼티로 비교
        }

        // 두 아이템의 내용(데이터)이 동일한지 확인합니다.
        // data class는 equals()가 자동으로 구현되므로, 객체 자체를 비교하면 내용 비교가 됩니다.
        override fun areContentsTheSame(oldItem: com.example.freshbox.data.FoodItem, newItem: com.example.freshbox.data.FoodItem): Boolean {
            return oldItem == newItem
        }
    }
}