package com.specknet.pdiotapp.database
// Records.kt
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Records")
data class Records(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "userName") val userName: String,
    @ColumnInfo(name = "date") val date : String,
    @ColumnInfo(name = "activityType") val activity : String,
    @ColumnInfo(name = "duration") val duration: Long,
)
