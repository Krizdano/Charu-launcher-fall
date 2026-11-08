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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    /** Id of the project this task belongs to; 0 = not part of any project. */
    val projectId: Long = 0,
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
    /** Legacy manual progress. Progress is now derived from the project's tasks. */
    val progress: Int = 0,
    /** ISO date (yyyy-MM-dd) the project is due, or "" for no deadline. */
    val dueDate: String = "",
    /** True once the project has been marked complete. */
    val done: Boolean = false,
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

    /** Moves the given tasks into a project. */
    @Query("UPDATE tasks SET projectId = :projectId WHERE id IN (:ids)")
    suspend fun assignToProject(ids: List<Long>, projectId: Long)

    /** Detaches every task from a project (used when the project is deleted). */
    @Query("UPDATE tasks SET projectId = 0 WHERE projectId = :projectId")
    suspend fun unlinkProject(projectId: Long)
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

/** v2: tasks can belong to a project; projects get a due date and a completed flag. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN projectId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE projects ADD COLUMN dueDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE projects ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
        // Tasks that were tagged with a project's name (the old way of grouping) join that project.
        db.execSQL(
            "UPDATE tasks SET projectId = COALESCE(" +
                "(SELECT id FROM projects WHERE projects.title = tasks.tag LIMIT 1), 0) WHERE tag != ''"
        )
    }
}

@Database(
    entities = [TaskEntity::class, NoteEntity::class, RoutineEntity::class, ProjectEntity::class],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
