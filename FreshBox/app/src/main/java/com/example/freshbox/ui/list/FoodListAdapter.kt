// File: app/src/main/java/com/example/freshbox/ui/list/FoodListAdapter.kt
package com.example.freshbox.ui.list

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil // DiffUtil import
import androidx.recyclerview.widget.ListAdapter // ListAdapter import
import com.example.freshbox.R
import com.example.freshbox.data.FoodItem // Room Entity com.example.freshbox.data.FoodItem 사용
// import com.example.freshbox.model.FoodItem // 기존 model.FoodItem import는 제거 또는 주석 처리
import java.io.File
// SimpleDateFormat, Locale, Date는 data.FoodItem의 Date 객체를 직접 사용하므로 여기서 불필요할 수 있음
import java.util.* // Calendar 사용을 위해 유지
import androidx.recyclerview.widget.RecyclerView

// RecyclerView.Adapter 대신 ListAdapter 사용
class FoodListAdapter(
    private val onItemClick: ((com.example.freshbox.data.FoodItem) -> Unit)? = null, // data.FoodItem으로 타입 변경
    private val onItemLongClick: ((com.example.freshbox.data.FoodItem) -> Unit)? = null // data.FoodItem으로 타입 변경
) : ListAdapter<com.example.freshbox.data.FoodItem, FoodListAdapter.FoodViewHolder>(FoodItemDiffCallback()) { // data.FoodItem 및 DiffUtil.ItemCallback 사용

    // submitList 메서드는 ListAdapter에 이미 구현되어 있으므로 직접 구현할 필요 없음
    // private var foodList: List<com.example.freshbox.data.FoodItem> = emptyList()
    // fun submitList(newList: List<com.example.freshbox.data.FoodItem>) {
    //     foodList = newList
    //     notifyDataSetChanged() // ListAdapter는 notifyDataSetChanged() 대신 내부적으로 더 효율적인 갱신 처리
    // }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = getItem(position) // ListAdapter의 getItem() 사용
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(item)
            true
        }
    }

    // getItemCount()는 ListAdapter 사용 시 직접 구현할 필요 없음 (내부적으로 처리)
    // override fun getItemCount(): Int = foodList.size

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 레이아웃 파일(item_food.xml)에 정의된 ID를 사용해야 합니다.
        // 예시 ID: textViewFoodName, textViewCategory, textViewTags, textViewDday, imageViewFood
        private val nameView: TextView = itemView.findViewById(R.id.textViewFoodName) // 식품 이름을 위한 TextView (ID 예시)
        private val ddayView: TextView = itemView.findViewById(R.id.textViewDday)
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewFood)
        // TODO: item_food.xml에 카테고리명과 태그를 표시할 TextView 또는 ChipGroup 추가 가정
        // private val categoryView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        // private val tagsView: com.google.android.material.chip.ChipGroup = itemView.findViewById(R.id.chipGroupTagsItem)


        fun bind(item: com.example.freshbox.data.FoodItem) { // data.FoodItem으로 타입 변경
            nameView.text = item.name // 식품 이름 표시

            // D-day 계산
            val today = Calendar.getInstance().timeInMillis // 현재 시간을 밀리초로
            val expiryMillis = item.expiryDate.time // Date 객체의 time 속성 사용 (밀리초)
            val diffMillis = expiryMillis - today
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            ddayView.text = when {
                diffDays > 0 -> {
                    ddayView.setBackgroundResource(R.drawable.bg_dday_soon)
                    "D-$diffDays"
                }
                diffDays == 0 -> {
                    ddayView.setBackgroundResource(R.drawable.bg_dday_soon)
                    "D-day"
                }
                else -> {
                    ddayView.setBackgroundResource(R.drawable.bg_dday_expired)
                    "D+${-diffDays}"
                }
            }

            // 이미지 표시 (data.FoodItem Entity에 imagePath 필드가 있다고 가정)
            item.imagePath?.let { path -> // imagePath가 nullable일 수 있으므로 let 사용
                if (path.isNotEmpty()) {
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            imageView.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            imageView.setImageResource(R.drawable.ic_launcher_foreground) // 오류 시 기본 이미지
                        }
                    } else {
                        imageView.setImageResource(R.drawable.ic_launcher_foreground) // 파일 없을 시 기본 이미지
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground) // 경로 없을 시 기본 이미지
                }
            } ?: imageView.setImageResource(R.drawable.ic_launcher_foreground) // imagePath가 null일 경우 기본 이미지

            // TODO: 카테고리 이름 표시
            // item.categoryId를 사용하여 Category 이름을 가져와 categoryView에 설정하는 로직 필요
            // (ViewModel에서 FoodItem과 Category 이름을 매핑한 데이터를 전달받거나,
            //  Fragment/Activity에서 Category 목록을 가져와 ID로 이름을 찾아 설정)
            // 예: categoryView.text = getCategoryNameFromId(item.categoryId)

            // TODO: 태그 표시
            // item.tags (List<String>)를 사용하여 tagsView(ChipGroup 등)에 태그들을 동적으로 추가
            // 예: tagsView.removeAllViews()
            // item.tags.forEach { tagName ->
            //     val chip = Chip(itemView.context).apply { text = tagName }
            //     tagsView.addView(chip)
            // }
        }

        // String.toMillis() 확장 함수는 ViewHolder 내에서는 더 이상 필요 없음
        // private fun String.toMillis(): Long { ... }
    }

    // DiffUtil.ItemCallback 구현
    class FoodItemDiffCallback : DiffUtil.ItemCallback<com.example.freshbox.data.FoodItem>() {
        override fun areItemsTheSame(oldItem: com.example.freshbox.data.FoodItem, newItem: com.example.freshbox.data.FoodItem): Boolean {
            return oldItem.id == newItem.id // ID를 기준으로 아이템 동일 여부 비교
        }

        override fun areContentsTheSame(oldItem: com.example.freshbox.data.FoodItem, newItem: com.example.freshbox.data.FoodItem): Boolean {
            return oldItem == newItem // 데이터 내용 전체 비교 (data class의 equals 활용)
        }
    }
}