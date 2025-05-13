package com.example.freshbox.ui.list

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.freshbox.data.FoodItem
import com.example.freshbox.databinding.ItemFoodBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FoodListAdapter(private val onItemClicked: (FoodItem) -> Unit) :
    ListAdapter<FoodItem, FoodListAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
    }

    inner class FoodViewHolder(private val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("yyyy.MM.dd (E)", Locale.getDefault())

        fun bind(item: FoodItem) {
            binding.textViewItemName.text = item.name
            binding.textViewItemExpiryDate.text = "소비기한: ${dateFormat.format(item.expiryDate)}"
            binding.textViewItemQuantity.text = "수량: ${item.quantity}"
            binding.textViewItemCategory.text = item.category ?: "카테고리 없음"

            // 남은 일수 계산 및 표시
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val expiry = Calendar.getInstance().apply {
                time = item.expiryDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }

            val diffInMillis = expiry.timeInMillis - today.timeInMillis
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            when {
                diffInDays < 0 -> {
                    binding.textViewItemDaysRemaining.text = "${-diffInDays}일 지남"
                    binding.textViewItemDaysRemaining.setTextColor(Color.parseColor("#D32F2F")) // 진한 빨강
                }
                diffInDays == 0L -> {
                    binding.textViewItemDaysRemaining.text = "오늘까지!"
                    binding.textViewItemDaysRemaining.setTextColor(Color.parseColor("#FBC02D")) // 주황/노랑
                }
                diffInDays <= 3L -> { // 3일 이하로 남았을 때
                    binding.textViewItemDaysRemaining.text = "${diffInDays}일 남음"
                    binding.textViewItemDaysRemaining.setTextColor(Color.parseColor("#FBC02D")) // 주황/노랑
                }
                else -> {
                    binding.textViewItemDaysRemaining.text = "${diffInDays}일 남음"
                    binding.textViewItemDaysRemaining.setTextColor(Color.parseColor("#388E3C")) // 초록
                }
            }
        }
    }

    class FoodDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
    }
}