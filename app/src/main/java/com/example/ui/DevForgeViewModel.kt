package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DevForgeViewModel(
    private val repository: DevForgeRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("devforge_prefs", android.content.Context.MODE_PRIVATE)

    private val _userApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    fun updateApiKey(newKey: String) {
        _userApiKey.value = newKey
        sharedPrefs.edit().putString("gemini_api_key", newKey).apply()
    }

    fun updateProfile(name: String, email: String) {
        _username.value = name
        _userEmail.value = email
        sharedPrefs.edit()
            .putString("username", name)
            .putString("user_email", email)
            .apply()
    }

    // Projects
    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Int?>(null)
    val selectedProjectId: StateFlow<Int?> = _selectedProjectId.asStateFlow()

    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // Files
    private val _files = MutableStateFlow<List<FileEntity>>(emptyList())
    val files: StateFlow<List<FileEntity>> = _files.asStateFlow()

    private val _selectedFile = MutableStateFlow<FileEntity?>(null)
    val selectedFile: StateFlow<FileEntity?> = _selectedFile.asStateFlow()

    // Editor Tabs
    private val _openTabs = MutableStateFlow<List<FileEntity>>(emptyList())
    val openTabs: StateFlow<List<FileEntity>> = _openTabs.asStateFlow()

    // Database Tables
    private val _tables = MutableStateFlow<List<DatabaseTableEntity>>(emptyList())
    val tables: StateFlow<List<DatabaseTableEntity>> = _tables.asStateFlow()

    // Comments
    private val _comments = MutableStateFlow<List<TeamCommentEntity>>(emptyList())
    val comments: StateFlow<List<TeamCommentEntity>> = _comments.asStateFlow()

    // Terminal
    private val _terminalHistory = MutableStateFlow<List<String>>(listOf("DevForge Cloud Terminal v1.0.0", "Type 'help' for commands."))
    val terminalHistory: StateFlow<List<String>> = _terminalHistory.asStateFlow()

    private val _currentPath = MutableStateFlow("/workspace")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // AI Status
    private val _aiStatus = MutableStateFlow<String>("idle") // "idle", "thinking", "generating", "success", "error"
    val aiStatus: StateFlow<String> = _aiStatus.asStateFlow()

    private val _aiLogs = MutableStateFlow<List<String>>(emptyList())
    val aiLogs: StateFlow<List<String>> = _aiLogs.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // Deployment
    private val _deploymentLogs = MutableStateFlow<List<String>>(emptyList())
    val deploymentLogs: StateFlow<List<String>> = _deploymentLogs.asStateFlow()

    // Profile & Authentication
    private val _username = MutableStateFlow(sharedPrefs.getString("username", "Developer Pro") ?: "Developer Pro")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "dev@devforge.ai") ?: "dev@devforge.ai")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow(true)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    // Search and Replace in Editor
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    // Code editing text changes
    private val _currentEditorContent = MutableStateFlow("")
    val currentEditorContent: StateFlow<String> = _currentEditorContent.asStateFlow()

    init {
        // Collect files, tables, comments for selected project dynamically
        viewModelScope.launch {
            _selectedProjectId.collectLatest { id ->
                if (id != null) {
                    val proj = repository.getProjectById(id)
                    _selectedProject.value = proj
                    
                    // Launch collections
                    launch {
                        repository.getFilesForProject(id).collect {
                            _files.value = it
                        }
                    }
                    launch {
                        repository.getTablesForProject(id).collect {
                            _tables.value = it
                        }
                    }
                    launch {
                        repository.getCommentsForProject(id).collect {
                            _comments.value = it
                        }
                    }
                } else {
                    _selectedProject.value = null
                    _files.value = emptyList()
                    _tables.value = emptyList()
                    _comments.value = emptyList()
                    _selectedFile.value = null
                    _openTabs.value = emptyList()
                }
            }
        }
    }

    fun selectProject(projectId: Int?) {
        _selectedProjectId.value = projectId
        // Reset editor/workspace states
        _selectedFile.value = null
        _openTabs.value = emptyList()
        _currentEditorContent.value = ""
    }

    fun selectFile(file: FileEntity) {
        _selectedFile.value = file
        _currentEditorContent.value = file.content
        
        // Add to tabs if not already present
        val currentTabs = _openTabs.value.toMutableList()
        if (!currentTabs.any { it.filePath == file.filePath }) {
            currentTabs.add(file)
            _openTabs.value = currentTabs
        }
    }

    fun updateEditorContent(newContent: String) {
        _currentEditorContent.value = newContent
        // Update local file entity state
        _selectedFile.value?.let { file ->
            _selectedFile.value = file.copy(content = newContent)
        }
    }

    fun saveCurrentFile() {
        val file = _selectedFile.value ?: return
        val currentContent = _currentEditorContent.value
        viewModelScope.launch {
            val updated = file.copy(content = currentContent)
            repository.updateFile(updated)
            
            // Sync in tabs list
            val tabs = _openTabs.value.map {
                if (it.id == file.id) updated else it
            }
            _openTabs.value = tabs
        }
    }

    fun closeTab(file: FileEntity) {
        val tabs = _openTabs.value.toMutableList()
        tabs.removeIf { it.id == file.id }
        _openTabs.value = tabs
        
        if (_selectedFile.value?.id == file.id) {
            _selectedFile.value = tabs.lastOrNull()
            _currentEditorContent.value = tabs.lastOrNull()?.content ?: ""
        }
    }

    fun updateSearchAndReplace(search: String, replace: String) {
        _searchQuery.value = search
        _replaceQuery.value = replace
    }

    fun performReplaceAll() {
        val search = _searchQuery.value
        val replace = _replaceQuery.value
        val content = _currentEditorContent.value
        if (search.isNotEmpty()) {
            val newContent = content.replace(search, replace)
            updateEditorContent(newContent)
            saveCurrentFile()
        }
    }

    // Project creation from Template
    fun createProjectFromTemplate(name: String, description: String, templateName: String, platformType: String) {
        viewModelScope.launch {
            val project = ProjectEntity(
                name = name,
                description = description,
                templateName = templateName,
                platformType = platformType,
                gitRepoUrl = "https://github.com/devforge-ai/${name.lowercase().replace(" ", "-")}"
            )
            val projectId = repository.insertProject(project).toInt()
            seedProjectTemplate(projectId, templateName)
            selectProject(projectId)
        }
    }

    // AI Code explanation
    fun explainCurrentCode(onResult: (String) -> Unit) {
        val file = _selectedFile.value ?: return
        val code = _currentEditorContent.value
        viewModelScope.launch {
            _aiStatus.value = "thinking"
            val prompt = "Explain this code from file ${file.filePath} briefly, listing its purpose and key functions:\n\n$code"
            val response = GeminiService.generateCode(
                prompt = prompt,
                systemInstruction = "You are DevForge AI, a top-tier coding assistant. Keep descriptions conversational, visual, clear, and highly professional.",
                customApiKey = _userApiKey.value
            )
            onResult(response)
            _aiStatus.value = "idle"
        }
    }

    // AI Bug Fixer
    fun fixCurrentCode(onResult: (String) -> Unit) {
        val file = _selectedFile.value ?: return
        val code = _currentEditorContent.value
        viewModelScope.launch {
            _aiStatus.value = "thinking"
            val prompt = "Find and fix any bugs in this code from ${file.filePath}. Return ONLY the fully corrected code with no introductory text or conversational fluff, just the code block itself.\n\n$code"
            val response = GeminiService.generateCode(
                prompt = prompt,
                systemInstruction = "You are a code optimizer. Return only the raw corrected source code. Do not wrap in markdown or explain anything.",
                customApiKey = _userApiKey.value
            )
            
            // Clean markdown blocks if returned
            val cleanedCode = response.trim().removePrefix("```").trim().removePrefix("typescript").trim().removePrefix("javascript").trim().removePrefix("kotlin").trim().removePrefix("python").trim().removeSuffix("```").trim()
            
            updateEditorContent(cleanedCode)
            saveCurrentFile()
            onResult("Successfully analyzed and applied bug fixes to ${file.filePath}!")
            _aiStatus.value = "idle"
        }
    }

    // AI Complete / Refactor File
    fun refactorCurrentCode(instruction: String, onResult: (String) -> Unit) {
        val file = _selectedFile.value ?: return
        val code = _currentEditorContent.value
        viewModelScope.launch {
            _aiStatus.value = "thinking"
            val prompt = "Refactor or modify the code in ${file.filePath} based on this instruction: '$instruction'. Original code:\n\n$code\n\nReturn ONLY the modified source code. Do not write any explanations."
            val response = GeminiService.generateCode(
                prompt = prompt,
                systemInstruction = "You are a software refactoring agent. Return only the raw refactored code. Do not write anything else.",
                customApiKey = _userApiKey.value
            )
            val cleanedCode = response.trim().removePrefix("```").trim().removePrefix("typescript").trim().removePrefix("javascript").trim().removePrefix("kotlin").trim().removePrefix("python").trim().removeSuffix("```").trim()
            updateEditorContent(cleanedCode)
            saveCurrentFile()
            onResult("Applied refactoring successfully!")
            _aiStatus.value = "idle"
        }
    }

    // Full Project AI Generator (Interactive Multi-file Generation)
    fun generateProjectAI(prompt: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            _aiStatus.value = "generating"
            _aiLogs.value = listOf(
                "Analyzing user requirements...",
                "Synthesizing database schemas...",
                "Drafting API route specifications..."
            )
            _aiError.value = null

            val systemInstruction = """
                You are DevForge AI, an expert software architect.
                Generate a list of source files representing a fully complete app based on the user's prompt.
                Your output MUST be a valid JSON array of objects, where each object has:
                - 'filePath': relative path from root (e.g. 'package.json', 'src/App.tsx', 'server.js')
                - 'content': complete content of the file.
                
                Generate realistic code files for the specified platform type (e.g. package.json, index.html, CSS, or TypeScript/JavaScript/Python modules).
                Generate exactly 3 to 5 crucial, highly functioning files. Do not output ANY markdown or explanations. Just output the raw JSON array.
            """.trimIndent()

            val aiPrompt = "Create an application that does: $prompt"
            
            val response = GeminiService.generateCode(aiPrompt, systemInstruction, customApiKey = _userApiKey.value)
            
            try {
                val cleaned = response.trim()
                val jsonString = if (cleaned.startsWith("```json")) {
                    cleaned.removePrefix("```json").removeSuffix("```").trim()
                } else if (cleaned.startsWith("```")) {
                    cleaned.removePrefix("```").removeSuffix("```").trim()
                } else {
                    cleaned
                }

                val array = JSONArray(jsonString)
                val generatedFiles = mutableListOf<FileEntity>()
                val logs = _aiLogs.value.toMutableList()
                
                logs.add("Gemini completed code synthesis.")
                logs.add("Parsing generated files structure...")
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val path = obj.getString("filePath")
                    val content = obj.getString("content")
                    
                    logs.add("Writing file: $path")
                    generatedFiles.add(
                        FileEntity(
                            projectId = projectId,
                            filePath = path,
                            content = content,
                            isFolder = false
                        )
                    )
                }

                repository.insertFiles(generatedFiles)
                logs.add("All files written to disk successfully!")
                _aiLogs.value = logs
                _aiStatus.value = "success"
            } catch (e: Exception) {
                _aiError.value = "Failed to parse generated files: ${e.message}\nRaw output: $response"
                _aiStatus.value = "error"
            }
        }
    }

    // Simulated Terminal Commands
    fun executeTerminalCommand(input: String) {
        val cmd = input.trim()
        if (cmd.isEmpty()) return

        val history = _terminalHistory.value.toMutableList()
        history.add("${_currentPath.value}$ $cmd")

        val parts = cmd.split(" ")
        val baseCmd = parts[0].lowercase()

        when (baseCmd) {
            "help" -> {
                history.add("DevForge Cloud Terminal Emulator v1.2.0")
                history.add("Available Unix & Development commands:")
                history.add("  help               - List all available terminal commands")
                history.add("  neofetch           - Display beautiful system details and CPU specs")
                history.add("  pwd                - Print current working directory")
                history.add("  cd <dir>           - Change directory context")
                history.add("  ls                 - List all source files in active project")
                history.add("  cat <file>         - Display code contents of specified file")
                history.add("  touch <file>       - Create a new blank source file in workspace")
                history.add("  mkdir <dir>        - Create a new directory in workspace")
                history.add("  rm <file>          - Remove file from the active project")
                history.add("  echo <text>        - Print specified text string to screen")
                history.add("  ai <prompt>        - Ask Gemini AI to generate code directly inside CLI")
                history.add("  npm run dev        - Start DevForge live server (hot-reload enabled)")
                history.add("  npm install        - Fetch and install node_modules dependencies")
                history.add("  git status         - Check current git staging & commit state")
                history.add("  git log            - Display project commit graphs and hashes")
                history.add("  clear              - Wipe console terminal history logs")
            }
            "clear" -> {
                _terminalHistory.value = listOf("Terminal cleared. Type 'help' for assistance.")
                return
            }
            "pwd" -> {
                history.add(_currentPath.value)
            }
            "cd" -> {
                if (parts.size < 2) {
                    _currentPath.value = "/workspace"
                } else {
                    val target = parts[1]
                    if (target == "..") {
                        if (_currentPath.value != "/workspace") {
                            _currentPath.value = "/workspace"
                        }
                    } else {
                        val cleanedTarget = target.removePrefix("/").removePrefix("workspace/")
                        _currentPath.value = "/workspace/$cleanedTarget"
                        history.add("Navigated to dir: $target")
                    }
                }
            }
            "neofetch" -> {
                history.add("    /\\_/\\      DevForge AI OS v1.2.0")
                history.add("   ( o.o )     Kernel: Linux 5.15-sandbox-arm64")
                history.add("    > ^ <      Shell: bash-devforge-v1.2")
                history.add("   /     \\     Uptime: 2 hours, 14 mins")
                history.add("               User: ${_username.value}")
                history.add("               CPU: Gemini Multi-modal Neural Engine")
                history.add("               RAM: 128 GB (85% free virtual cache)")
                history.add("               Workspace: ${_selectedProject.value?.name ?: "None"}")
                history.add("               AI Status: ${if (_userApiKey.value.isNotEmpty()) "ENTERPRISE PRO" else "SANDBOX FREE"}")
            }
            "echo" -> {
                val text = cmd.removePrefix("echo ").trim()
                history.add(text)
            }
            "touch" -> {
                if (parts.size < 2) {
                    history.add("Usage: touch <filename>")
                } else {
                    val filename = parts[1]
                    val projectId = _selectedProjectId.value
                    if (projectId != null) {
                        viewModelScope.launch {
                            val newFile = FileEntity(
                                projectId = projectId,
                                filePath = filename,
                                content = "// Custom file created via touch CLI\n",
                                isFolder = false
                            )
                            repository.insertFiles(listOf(newFile))
                        }
                        history.add("Created file $filename successfully.")
                    } else {
                        history.add("Error: No active project workspace selected.")
                    }
                }
            }
            "mkdir" -> {
                if (parts.size < 2) {
                    history.add("Usage: mkdir <dirname>")
                } else {
                    val dirname = parts[1]
                    val projectId = _selectedProjectId.value
                    if (projectId != null) {
                        viewModelScope.launch {
                            val newDir = FileEntity(
                                projectId = projectId,
                                filePath = dirname,
                                content = "",
                                isFolder = true
                            )
                            repository.insertFiles(listOf(newDir))
                        }
                        history.add("Created directory $dirname/ successfully.")
                    } else {
                        history.add("Error: No active project workspace selected.")
                    }
                }
            }
            "rm" -> {
                if (parts.size < 2) {
                    history.add("Usage: rm <filename>")
                } else {
                    val filename = parts[1]
                    val projectId = _selectedProjectId.value
                    val file = _files.value.find { it.filePath == filename || it.filePath.endsWith(filename) }
                    if (file != null && projectId != null) {
                        viewModelScope.launch {
                            repository.deleteFileByPath(projectId, file.filePath)
                        }
                        history.add("Removed file ${file.filePath} successfully.")
                    } else {
                        history.add("rm: cannot remove '$filename': No such file in active workspace.")
                    }
                }
            }
            "ai", "gemini" -> {
                if (parts.size < 2) {
                    history.add("Usage: ai <your prompt here>")
                } else {
                    val prompt = cmd.removePrefix("ai ").removePrefix("gemini ").trim()
                    history.add("Connecting to Gemini Neural Network...")
                    history.add("DevForge AI: Analyzing prompter requirements: '$prompt'...")
                    _terminalHistory.value = history.toList()
                    viewModelScope.launch {
                        _aiStatus.value = "thinking"
                        val response = GeminiService.generateCode(
                            prompt = prompt,
                            systemInstruction = "You are DevForge AI integrated directly into the Unix terminal as a CLI command. Respond in a brief, concise, command-line terminal style. Max 10 lines.",
                            customApiKey = _userApiKey.value
                        )
                        val updatedHistory = _terminalHistory.value.toMutableList()
                        updatedHistory.add("----------------- AI CLI OUTPUT -----------------")
                        response.lines().forEach { line ->
                            updatedHistory.add("  $line")
                        }
                        updatedHistory.add("-------------------------------------------------")
                        _terminalHistory.value = updatedHistory
                        _aiStatus.value = "idle"
                    }
                    return // return immediately since we update asynchronously inside the coroutine
                }
            }
            "ls" -> {
                val projFiles = _files.value
                if (projFiles.isEmpty()) {
                    history.add("No files found in workspace.")
                } else {
                    projFiles.forEach {
                        history.add("  ${if (it.isFolder) "DIR " else "FILE"} ${it.filePath}")
                    }
                }
            }
            "cat" -> {
                if (parts.size < 2) {
                    history.add("Usage: cat <filename>")
                } else {
                    val target = parts[1]
                    val file = _files.value.find { it.filePath.endsWith(target) }
                    if (file != null) {
                        history.add("--- ${file.filePath} ---")
                        file.content.lines().forEach { history.add(it) }
                    } else {
                        history.add("File not found: $target")
                    }
                }
            }
            "npm" -> {
                if (parts.size >= 3 && parts[1] == "run" && parts[2] == "dev") {
                    history.add("> devforge-app@1.0.0 dev")
                    history.add("> next dev")
                    history.add("Ready in 1.4s")
                    history.add("Local server running at http://localhost:3000 (Hot-Reload Enabled)")
                } else if (parts.size >= 2 && parts[1] == "install") {
                    history.add("Resolving package versions...")
                    history.add("Fetched 348 dependencies in 2.1s")
                    history.add("Added 124 packages, audited 349 packages in 3s")
                    history.add("found 0 vulnerabilities")
                } else {
                    history.add("npm command recognized. Mock execution successful.")
                }
            }
            "git" -> {
                if (parts.size >= 2 && parts[1] == "status") {
                    history.add("On branch ${_selectedProject.value?.gitBranch ?: "main"}")
                    history.add("Your branch is up to date.")
                    history.add("Changes not staged for commit:")
                    history.add("  (use 'git commit -m <msg>' to save changes)")
                    history.add("    modified:   package.json")
                } else if (parts.size >= 2 && parts[1] == "log") {
                    history.add("commit f3b20c918a28f (HEAD -> main)")
                    history.add("Author: ${_username.value} <${_userEmail.value}>")
                    history.add("Date:   Tue Jul 14 09:15:26 2026")
                    history.add("    Initial commit from DevForge template")
                } else if (cmd.contains("commit")) {
                    history.add("[main 8a93e2b] Saved changes locally")
                    history.add(" 1 file changed, 14 insertions(+)")
                } else {
                    history.add("git action simulated.")
                }
            }
            else -> {
                history.add("bash: $baseCmd: command simulated inside sandbox.")
            }
        }

        _terminalHistory.value = history
    }

    // One-click simulated deployment
    fun deployProject(platform: String) {
        val project = _selectedProject.value ?: return
        viewModelScope.launch {
            _deploymentLogs.value = listOf(
                "Initializing deployment environment for $platform...",
                "Retrieving project source from git branch '${project.gitBranch}'..."
            )
            val logs = _deploymentLogs.value.toMutableList()
            
            // Step 2
            kotlinx.coroutines.delay(1000)
            logs.add("Running builds: npm run build...")
            logs.add("Successfully compiled client components.")
            logs.add("Generating statically rendered pages (Next.js Edge)...")
            _deploymentLogs.value = logs.toList()

            // Step 3
            kotlinx.coroutines.delay(1000)
            logs.add("Optimizing server-side functions & databases...")
            logs.add("Uploading assets to edge CDN clusters...")
            _deploymentLogs.value = logs.toList()

            // Step 4
            kotlinx.coroutines.delay(1000)
            val subUrl = project.name.lowercase().replace(" ", "-") + ".devforge.app"
            val updatedProj = project.copy(
                deploymentStatus = "Success",
                deploymentUrl = "https://$subUrl"
            )
            repository.updateProject(updatedProj)
            _selectedProject.value = updatedProj

            logs.add("Deployment Complete! 🚀")
            logs.add("Live URL: https://$subUrl")
            _deploymentLogs.value = logs.toList()
        }
    }

    // Git Actions (GitHub tab)
    fun commitAndPushToGitHub(message: String) {
        val project = _selectedProject.value ?: return
        viewModelScope.launch {
            val updated = project.copy(gitRepoUrl = "https://github.com/devforge-ai/${project.name.lowercase().replace(" ", "-")}")
            repository.updateProject(updated)
            _selectedProject.value = updated
        }
    }

    // Add Collaborator Comment
    fun addComment(filePath: String, lineNumber: Int, content: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val comment = TeamCommentEntity(
                projectId = projectId,
                filePath = filePath,
                lineNumber = lineNumber,
                author = _username.value,
                content = content
            )
            repository.insertComment(comment)
        }
    }

    // Visual Database builder Actions
    fun createDatabaseTable(tableName: String, columns: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val table = DatabaseTableEntity(
                projectId = projectId,
                tableName = tableName,
                columnsData = columns
            )
            repository.insertTable(table)
        }
    }

    fun deleteDatabaseTable(tableId: Int) {
        viewModelScope.launch {
            repository.deleteTableById(tableId)
        }
    }

    // Internal template seeding
    private suspend fun seedProjectTemplate(projectId: Int, templateName: String) {
        val files = when (templateName) {
            "SaaS App" -> listOf(
                FileEntity(projectId = projectId, filePath = "package.json", isFolder = false, content = """
{
  "name": "devforge-saas",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint"
  },
  "dependencies": {
    "next": "15.0.0",
    "react": "19.0.0",
    "react-dom": "19.0.0",
    "tailwindcss": "^3.4.0",
    "prisma": "^5.0.0"
  }
}
                """.trimIndent()),
                FileEntity(projectId = projectId, filePath = "prisma/schema.prisma", isFolder = false, content = """
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model User {
  id        String   @id @default(cuid())
  email     String   @unique
  name      String?
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
  plan      String   @default("FREE")
}
                """.trimIndent()),
                FileEntity(projectId = projectId, filePath = "src/app/page.tsx", isFolder = false, content = """
import React from 'react';

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col items-center justify-center p-6">
      <header className="mb-12 text-center">
        <h1 className="text-5xl font-extrabold tracking-tight bg-gradient-to-r from-teal-400 to-blue-500 bg-clip-text text-transparent">
          DevForge SaaS Platform
        </h1>
        <p className="mt-4 text-slate-400 text-lg">
          The ultimate platform for modern developers to launch ideas rapidly.
        </p>
      </header>
      
      <main className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl">
        <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800">
          <h2 className="text-xl font-bold mb-2 text-teal-400">⚡ Superfast</h2>
          <p className="text-slate-400 text-sm">Built with Next.js App Router for optimal rendering speed.</p>
        </div>
        <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800">
          <h2 className="text-xl font-bold mb-2 text-teal-400">🔒 Secure</h2>
          <p className="text-slate-400 text-sm">NextAuth integration out of the box for modern social logins.</p>
        </div>
        <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800">
          <h2 className="text-xl font-bold mb-2 text-teal-400">📦 Scalable</h2>
          <p className="text-slate-400 text-sm">Prisma ORM connected to scalable PostgreSQL databases.</p>
        </div>
      </main>
    </div>
  );
}
                """.trimIndent())
            )
            "E-commerce Store" -> listOf(
                FileEntity(projectId = projectId, filePath = "package.json", isFolder = false, content = """
{
  "name": "devforge-ecommerce",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2",
    "sqlite3": "^5.1.6",
    "cors": "^2.8.5"
  }
}
                """.trimIndent()),
                FileEntity(projectId = projectId, filePath = "server.js", isFolder = false, content = """
const express = require('express');
const cors = require('cors');
const app = express();
app.use(cors());
app.use(express.json());

const products = [
  { id: 1, name: "Wireless Headphones", price: 99.99, image: "/images/headphones.jpg" },
  { id: 2, name: "Mechanical Keyboard", price: 129.99, image: "/images/keyboard.jpg" }
];

app.get('/api/products', (req, res) => {
  res.json(products);
});

app.listen(3001, () => Log('Server running on port 3001'));
                """.trimIndent()),
                FileEntity(projectId = projectId, filePath = "src/App.js", isFolder = false, content = """
import React, { useEffect, useState } from 'react';

function App() {
  const [products, setProducts] = useState([]);

  useEffect(() => {
    fetch('http://localhost:3001/api/products')
      .then(res => res.json())
      .then(data => setProducts(data));
  }, []);

  return (
    <div className="container">
      <h1>E-Commerce Storefront</h1>
      <div className="grid">
        {products.map(p => (
          <div key={p.id} className="card">
            <h3>{p.name}</h3>
            <p>${'$'}{p.price}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
                """.trimIndent())
            )
            "AI Chatbot" -> listOf(
                FileEntity(projectId = projectId, filePath = "package.json", isFolder = false, content = """
{
  "name": "ai-chatbot",
  "version": "1.0.0",
  "dependencies": {
    "next": "15.0.0",
    "react": "19.0.0",
    "google-generative-ai": "^0.1.0"
  }
}
                """.trimIndent()),
                FileEntity(projectId = projectId, filePath = "src/app/page.tsx", isFolder = false, content = """
"use client";
import React, { useState } from 'react';

export default function Chat() {
  const [messages, setMessages] = useState([{ role: "assistant", text: "Hello! How can I help you build software today?" }]);
  const [input, setInput] = useState("");

  const send = () => {
    if(!input) return;
    setMessages(prev => [...prev, { role: "user", text: input }]);
    setInput("");
    // Call AI mock response
    setTimeout(() => {
      setMessages(prev => [...prev, { role: "assistant", text: "I am drafting the Next.js routes now!" }]);
    }, 1000);
  };

  return (
    <div className="chat-container">
      {messages.map((m, i) => <div key={i} className={m.role}>{m.text}</div>)}
      <input value={input} onChange={e => setInput(e.target.value)} />
      <button onClick={send}>Send</button>
    </div>
  );
}
                """.trimIndent())
            )
            else -> listOf(
                FileEntity(projectId = projectId, filePath = "index.html", isFolder = false, content = """
<!DOCTYPE html>
<html>
<head>
    <title>DevForge Project</title>
    <style>
        body { font-family: 'Inter', sans-serif; background: #0b0f19; color: #fff; text-align: center; padding-top: 100px; }
        h1 { color: #2dd4bf; }
    </style>
</head>
<body>
    <h1>Welcome to DevForge AI</h1>
    <p>Your workspace is generated and ready to customize.</p>
</body>
</html>
                """.trimIndent())
            )
        }
        repository.insertFiles(files)
    }
}
