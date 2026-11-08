package com.lifeos.launcher.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

object Sec {
    const val NOW = "now"
    const val TODAY = "today"
    const val UPCOMING = "upcoming"
    const val WORK = "work"
}

object Period {
    const val MORNING = "morning"
    const val AFTERNOON = "afternoon"
    const val EVENING = "evening"
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val tag: String = "",
    val estimateMin: Int = 0,
    val section: String = Sec.TODAY,
    val dueLabel: String = "",
    val done: Boolean = false,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val period: String = Period.MORNING,
    /** ISO date (yyyy-MM-dd) the routine was last completed; done == (doneDate == today). */
    val doneDate: String = "",
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val category: String = "PERSONAL",
    val colorIdx: Int = 0,
    val progress: Int = 0,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id")
    fun observe(): Flow<List<TaskEntity>>

    @Insert suspend fun insert(t: TaskEntity): Long
    @Update suspend fun update(t: TaskEntity)
    @Delete suspend fun delete(t: TaskEntity)

    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun clearDone()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun observe(): Flow<List<NoteEntity>>

    @Insert suspend fun insert(n: NoteEntity): Long
    @Delete suspend fun delete(n: NoteEntity)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id")
    fun observe(): Flow<List<RoutineEntity>>

    @Insert suspend fun insert(r: RoutineEntity): Long
    @Update suspend fun update(r: RoutineEntity)
    @Delete suspend fun delete(r: RoutineEntity)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id")
    fun observe(): Flow<List<ProjectEntity>>

    @Insert suspend fun insert(p: ProjectEntity): Long
    @Update suspend fun update(p: ProjectEntity)
    @Delete suspend fun delete(p: ProjectEntity)
}

@Database(
    entities = [TaskEntity::class, NoteEntity::class, RoutineEntity::class, ProjectEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun notes(): NoteDao
    abstract fun routines(): RoutineDao
    abstract fun projects(): ProjectDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "lifeos.db")
                .build()
                .also { instance = it }
        }
    }
}
