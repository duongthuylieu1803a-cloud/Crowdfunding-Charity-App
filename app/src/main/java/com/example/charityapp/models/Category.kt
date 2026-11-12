package com.example.charityapp.models

data class Category(
    val id: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)