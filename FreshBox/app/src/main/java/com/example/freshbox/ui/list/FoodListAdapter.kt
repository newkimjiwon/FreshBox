package com.example.freshbox.ui.list

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.freshbox.R
import com.example.freshbox.model.FoodItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FoodListAdapter : RecyclerView.Adapter<FoodListAdapter.FoodViewHolder>() {

    private var foodList: List<FoodItem> = emptyList()

    fun submitList(newList: List<FoodItem>) {
        foodList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foodList[position])
    }

    override fun getItemCount(): Int = foodList.size

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: FoodItem) {
            val nameView = itemView.findViewById<TextView>(R.id.textViewName)
            val expiryView = itemView.findViewById<TextView>(R.id.textViewExpiry)
            val ddayView = itemView.findViewById<TextView>(R.id.textViewDday)
            val imageView = itemView.findViewById<ImageView>(R.id.imageViewFood)

            nameView.text = item.name
            expiryView.text = "소비기한: ${item.expiryDate}"

            // D-day 계산
            val today = System.currentTimeMillis()
            val expiryMillis = item.expiryDate.toMillis()
            val diffDays = ((expiryMillis - today) / (1000 * 60 * 60 * 24)).toInt()

            ddayView.text = when {
                diffDays > 0 -> "D-$diffDays"
                diffDays == 0 -> "D-day"
                else -> "D+${-diffDays}"
            }

            // 이미지 표시
            val file = File(item.imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        private fun String.toMillis(): Long {
            return try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(this)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
}
