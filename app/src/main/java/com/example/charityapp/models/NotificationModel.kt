package com.example.charityapp.models

import com.google.firebase.Timestamp

data class NotificationModel(
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null
)