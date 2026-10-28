package com.example.charityapp.models

data class Campaign(
    var campaignId: String = "",
    var title: String = "",
    var description: String = "",
    var targetAmount: Double = 0.0,
    var currentAmount: Double = 0.0,
    var imageUrl: String = "",
    var startDate: String = "",
    var endDate: String = ""
)
