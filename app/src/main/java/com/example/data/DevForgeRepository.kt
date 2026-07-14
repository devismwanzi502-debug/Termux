package com.example.data

import kotlinx.coroutines.flow.Flow

class DevForgeRepository(private val dao: DevForgeDao) {
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: Int): ProjectEntity? = dao.getProjectById(id)
    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProjectById(id: Int) = dao.deleteProjectById(id)

    fun getFilesForProject(projectId: Int): Flow<List<FileEntity>> = dao.getFilesForProject(projectId)
    suspend fun getFileByPath(projectId: Int, path: String): FileEntity? = dao.getFileByPath(projectId, path)
    suspend fun insertFile(file: FileEntity): Long = dao.insertFile(file)
    suspend fun insertFiles(files: List<FileEntity>) = dao.insertFiles(files)
    suspend fun updateFile(file: FileEntity) = dao.updateFile(file)
    suspend fun deleteFileById(id: Int) = dao.deleteFileById(id)
    suspend fun deleteFileByPath(projectId: Int, filePath: String) = dao.deleteFileByPath(projectId, filePath)

    fun getTablesForProject(projectId: Int): Flow<List<DatabaseTableEntity>> = dao.getTablesForProject(projectId)
    suspend fun insertTable(table: DatabaseTableEntity): Long = dao.insertTable(table)
    suspend fun deleteTableById(id: Int) = dao.deleteTableById(id)

    fun getCommentsForProject(projectId: Int): Flow<List<TeamCommentEntity>> = dao.getCommentsForProject(projectId)
    suspend fun insertComment(comment: TeamCommentEntity): Long = dao.insertComment(comment)
    suspend fun deleteCommentById(id: Int) = dao.deleteCommentById(id)
}
