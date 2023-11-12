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

    @Query("SELECT strftime('%w', date) as dayOfWeek, activityType, SUM(duration) as totalDuration FROM Records " +
            "WHERE date >= :startDate and date <= :endDate and userName = :userName " +
            "GROUP BY dayOfWeek, activityType")
    suspend fun getTotalDurationByDayOfWeekInDateRange(userName: String, startDate: String, endDate: String): List<DayOfWeekDuration>
}
