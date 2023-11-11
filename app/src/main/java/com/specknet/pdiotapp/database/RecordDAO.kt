package com.specknet.pdiotapp.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordDao {

    @Insert
    suspend fun insert(entity: Records)

    @Query("SELECT * FROM Records")
    suspend fun getAllEntities(): List<Records>
}
