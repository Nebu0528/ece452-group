package ca.uwaterloo.ece452.discoveruwaterloo.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val location: String?,
    val lat: Double?,
    val lng: Double?,
    val date: String?,
    val startTime: String?,
    val duration: Int?,
    val userId: Int,
    val reviewerId: Int?,
    val status: String,
    val tagIds: String
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

@Database(entities = [EventEntity::class], version = 7, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile private var INSTANCE: EventDatabase? = null

        fun getInstance(context: Context): EventDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, EventDatabase::class.java, "events.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
