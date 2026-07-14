package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val templateName: String, // e.g. "SaaS App", "E-commerce Store"
    val platformType: String, // e.g. "React", "Next.js", "Node.js", "Flutter"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deploymentStatus: String = "Not Deployed", // "Success", "Building", "Failed", "Not Deployed"
    val deploymentUrl: String = "",
    val gitRepoUrl: String = "",
    val gitBranch: String = "main"
)

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val filePath: String, // e.g. "src/App.tsx"
    val content: String,
    val isFolder: Boolean
)

@Entity(
    tableName = "database_tables",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class DatabaseTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val tableName: String,
    val columnsData: String // e.g., "id:Int, name:String, email:String"
)

@Entity(
    tableName = "team_comments",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TeamCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val filePath: String,
    val lineNumber: Int,
    val author: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DevForgeDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Files
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY isFolder DESC, filePath ASC")
    fun getFilesForProject(projectId: Int): Flow<List<FileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND filePath = :path LIMIT 1")
    suspend fun getFileByPath(projectId: Int, path: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM project_files WHERE id = :id")
    suspend fun deleteFileById(id: Int)

    @Query("DELETE FROM project_files WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun deleteFileByPath(projectId: Int, filePath: String)

    // Database Tables
    @Query("SELECT * FROM database_tables WHERE projectId = :projectId")
    fun getTablesForProject(projectId: Int): Flow<List<DatabaseTableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: DatabaseTableEntity): Long

    @Query("DELETE FROM database_tables WHERE id = :id")
    suspend fun deleteTableById(id: Int)

    // Comments
    @Query("SELECT * FROM team_comments WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getCommentsForProject(projectId: Int): Flow<List<TeamCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: TeamCommentEntity): Long

    @Query("DELETE FROM team_comments WHERE id = :id")
    suspend fun deleteCommentById(id: Int)
}

@Database(
    entities = [
        ProjectEntity::class,
        FileEntity::class,
        DatabaseTableEntity::class,
        TeamCommentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun devForgeDao(): DevForgeDao
}
