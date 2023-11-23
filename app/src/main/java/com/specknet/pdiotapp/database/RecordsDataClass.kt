package com.specknet.pdiotapp.database

data class ActivityTypeDuration(
    val activityType: Int,
    val totalDuration: Long
)

data class DayOfWeekDuration(
    val dayOfWeek: Int,
    val activityType: Int,
    val totalDuration: Long
)
