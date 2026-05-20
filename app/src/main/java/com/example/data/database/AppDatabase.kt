package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "speed_tests")
data class SpeedTestRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val operatorName: String,
    val networkType: String
)

@Entity(tableName = "data_usage")
data class DataUsageRecord(
    @PrimaryKey val dateString: String, // format: YYYY-MM-DD
    val mobileBytes: Long,
    val wifiBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_tests ORDER BY timestamp DESC")
    fun getAllSpeedTests(): Flow<List<SpeedTestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedTest(record: SpeedTestRecord)

    @Query("DELETE FROM speed_tests")
    suspend fun clearAllTests()
}

@Dao
interface DataUsageDao {
    @Query("SELECT * FROM data_usage ORDER BY dateString DESC")
    fun getAllUsageRecords(): Flow<List<DataUsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: DataUsageRecord)

    @Query("SELECT * FROM data_usage WHERE dateString = :date LIMIT 1")
    suspend fun getUsageRecordForDate(date: String): DataUsageRecord?
}

@Database(entities = [SpeedTestRecord::class, DataUsageRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun dataUsageDao(): DataUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "net_info_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
