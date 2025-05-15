package com.example.freshbox.ui.all

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.freshbox.databinding.ActivityAllFoodsBinding
import com.example.freshbox.model.FoodItem
import com.example.freshbox.ui.list.FoodListAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AllFoodsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllFoodsBinding
    private lateinit var adapter: FoodListAdapter
    private var foodItems: MutableList<FoodItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllFoodsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FoodListAdapter()
        binding.recyclerViewAllFoods.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAllFoods.adapter = adapter

        loadItems()
        setupSwipeToDelete()
    }

    private fun loadItems() {
        foodItems = loadFoodItemsFromJson(this).toMutableList()
        adapter.submitList(foodItems.toList())
        binding.textViewEmpty.visibility = if (foodItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedItem = foodItems.removeAt(position)
                adapter.submitList(foodItems.toList())
                saveAllToJson(foodItems, this@AllFoodsActivity)
                Toast.makeText(this@AllFoodsActivity, "삭제됨: ${removedItem.name}", Toast.LENGTH_SHORT).show()
                binding.textViewEmpty.visibility = if (foodItems.isEmpty()) View.VISIBLE else View.GONE
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewAllFoods)
    }

    private fun loadFoodItemsFromJson(context: Context): List<FoodItem> {
        val file = File(context.filesDir, "FreshBox/items.json")
        if (!file.exists()) return emptyList()

        val json = JSONArray(file.readText())
        return (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            FoodItem(
                name = obj.getString("name"),
                quantity = obj.getString("quantity"),
                category = obj.getString("category"),
                storageLocation = obj.getString("storageLocation"),
                memo = obj.getString("memo"),
                purchaseDate = obj.getString("purchaseDate"),
                expiryDate = obj.getString("expiryDate"),
                imagePath = obj.getString("imagePath")
            )
        }
    }

    private fun saveAllToJson(list: List<FoodItem>, context: Context) {
        val file = File(context.filesDir, "FreshBox/items.json")
        val json = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("name", item.name)
                put("quantity", item.quantity)
                put("category", item.category)
                put("storageLocation", item.storageLocation)
                put("memo", item.memo)
                put("purchaseDate", item.purchaseDate)
                put("expiryDate", item.expiryDate)
                put("imagePath", item.imagePath)
            }
            json.put(obj)
        }
        file.writeText(json.toString())
    }
}
