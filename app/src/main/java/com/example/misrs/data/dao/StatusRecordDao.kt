    package com.example.misrs.data.dao

    import androidx.room.Dao
    import androidx.room.Insert
    import androidx.room.OnConflictStrategy
    import androidx.room.Query
    import com.example.misrs.data.entities.StatusRecord
    import kotlinx.coroutines.flow.Flow


    @Dao
    interface StatusRecordDao {

        @Query("SELECT * FROM status_record WHERE sync_status = 0 ORDER BY record_time ASC LIMIT :limit")
        suspend fun getUnsyncedRecords(limit: Int): List<StatusRecord>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertRecord(record: StatusRecord)

        @Query("SELECT * FROM status_record ORDER BY record_time DESC LIMIT 1")
        fun getLastRecord(): StatusRecord?

        @Query("SELECT * FROM status_record ORDER BY record_time DESC LIMIT 10")
        fun getLast10RecordsFlow(): Flow<List<StatusRecord>>

        @Query("DELETE FROM status_record WHERE uuid IN (:uuids)")
        suspend fun deleteRecords(uuids: List<String>)

//        @Query("SELECT * FROM status_record WHERE sync_status = 0")
//        suspend fun getUnsyncedRecords(): List<StatusRecord>

        @Query("UPDATE status_record SET sync_status = 1 WHERE uuid IN (:uuids)")
        suspend fun markAsSynced(uuids: List<String>)
    }
