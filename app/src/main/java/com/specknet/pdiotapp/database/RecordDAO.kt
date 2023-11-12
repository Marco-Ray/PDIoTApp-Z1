package com.specknet.pdiotapp.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.Date

@Dao
interface RecordDao {

    @Insert
    suspend fun insert(entity: Records)

    @Query("SELECT * FROM Records")
    suspend fun getAllEntities(): List<Records>

    @Query("SELECT activityType, SUM(duration) as totalDuration FROM Records " +
            "WHERE date = :selectedDate and userName = :userName " +
            "GROUP BY activityType ")
    suspend fun getTotalDurationByActivityTypeInSelectedDate(userName: String, selectedDate: String): List<ActivityTypeDuration>

    @Query("SELECT * FROM Records WHERE date = :selectedDate")
    suspend fun getEntitiesByDate(selectedDate: String): List<Records>

    @Query("SELECT * FROM Records WHERE date >= :startDate and date <= :endDate")
    suspend fun getEntitiesByDateRange(startDate: String, endDate: String): List<Records>
}
