package com.example.freshbox.model

import java.io.Serializable

data class FoodItem(
    val name: String,
    val quantity: String,
    val category: String,
    val storageLocation: String,
    val memo: String,
    val purchaseDate: String,
    val expiryDate: String,
    val imagePath: String
) : Serializable
