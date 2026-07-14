package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// Glassmorphism aesthetic tokens
private val GlassBackgroundDark = Color(0x990A0F1E)
private val GlassBorderDark = Color(0x26FFFFFF)
private val GlassBackgroundLight = Color(0xB3FFFFFF)
private val GlassBorderLight = Color(0x33000000)

private val DeepSlateBlue = Color(0xFF0F172A)
private val CyberNeonTeal = Color(0xFF2DD4BF)
private val CyberNeonPurple = Color(0xFFA855F7)
private val EditorDarkBackground = Color(0xFF0E131F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevForgeApp(viewModel: DevForgeViewModel) {
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    var showSettings by remember { mutableStateOf(false) }

    // Main deep futuristic ambient gradient
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF080C14),
                Color(0xFF0D1527),
                Color(0xFF05070D)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        AnimatedContent(
            targetState = selectedProjectId,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { projId ->
            if (projId == null) {
                DashboardScreen(viewModel = viewModel, onSettingsClick = { showSettings = true })
            } else {
                WorkspaceScreen(viewModel = viewModel, onSettingsClick = { showSettings = true })
            }
        }

        if (showSettings) {
            SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
        }
    }
}

// ==========================================
// 1. DASHBOARD & PROJECT SELECTION LOBBY
// ==========================================

@Composable
fun DashboardScreen(viewModel: DevForgeViewModel, onSettingsClick: () -> Unit) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    // Stepper states for Dialog
    var projName by remember { mutableStateOf("") }
    var projDesc by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("SaaS App") }
    var platformType by remember { mutableStateOf("Next.js") }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = CyberNeonTeal,
                contentColor = Color.Black,
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Project")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Hero Banner
            item {
                Spacer(modifier = Modifier.height(20.dp))
                DashboardHeader(username = username, onSettingsClick = onSettingsClick)
            }

            // Quick Platform AI Generator Hero
            item {
                AIAppGeneratorHeroCard(onCreateClicked = { showCreateDialog = true })
            }

            // Recent Projects
            item {
                Text(
                    text = "Recent Projects",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DeepSlateBlue
                    )
                )
            }

            if (projects.isEmpty()) {
                item {
                    EmptyProjectsCard(onCreateClicked = { showCreateDialog = true })
                }
            } else {
                items(projects) { project ->
                    ProjectRowCard(
                        project = project,
                        onClick = { viewModel.selectProject(project.id) }
                    )
                }
            }

            // Project Templates Section
            item {
                Text(
                    text = "Project Templates",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DeepSlateBlue
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                TemplatesCarousel { templateName, platform ->
                    viewModel.createProjectFromTemplate(
                        name = "My $templateName",
                        description = "A custom generated $templateName project built with DevForge.",
                        templateName = templateName,
                        platformType = platform
                    )
                    Toast.makeText(context, "Seeded $templateName template!", Toast.LENGTH_SHORT).show()
                }
            }

            // AI Marketplace Section
            item {
                Text(
                    text = "AI Marketplace Extensions",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DeepSlateBlue
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                MarketplaceExtensionsGrid()
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Setup Create Project Dialog
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Forge New Project",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DeepSlateBlue
                        )
                    )

                    OutlinedTextField(
                        value = projName,
                        onValueChange = { projName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth().testTag("proj_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonTeal,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = projDesc,
                        onValueChange = { projDesc = it },
                        label = { Text("Description (e.g. A CRM for realtors)") },
                        modifier = Modifier.fillMaxWidth().testTag("proj_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonTeal,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Text("Select Starter Template:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val templates = listOf("SaaS App", "E-commerce Store", "AI Chatbot", "Static Webpage")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { t ->
                            val isSelected = selectedTemplate == t
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTemplate = t
                                    platformType = when (t) {
                                        "SaaS App" -> "Next.js"
                                        "E-commerce Store" -> "Express"
                                        "AI Chatbot" -> "React"
                                        else -> "HTML"
                                    }
                                },
                                label = { Text(t) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberNeonTeal,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (projName.isNotEmpty()) {
                                    viewModel.createProjectFromTemplate(
                                        name = projName,
                                        description = projDesc.ifEmpty { "A beautiful custom DevForge project." },
                                        templateName = selectedTemplate,
                                        platformType = platformType
                                    )
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberNeonTeal,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Forge")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(username: String, onSettingsClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome to DevForge AI",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else DeepSlateBlue,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Build complete apps in real-time.",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDark) Color.LightGray else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(CyberNeonTeal, CyberNeonPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (username.length >= 2) username.take(2).uppercase() else "DV",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun AIAppGeneratorHeroCard(onCreateClicked: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    val cardBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFFF8FAFC)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush)
            .border(
                1.dp,
                if (isDark) GlassBorderDark else GlassBorderLight,
                shape
            )
            .clickable { onCreateClicked() }
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberNeonTeal.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI GENERATOR ACTIVE",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonTeal
                    )
                )
            }

            Text(
                text = "Describe your vision, and see the architecture unfold.",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DeepSlateBlue
                )
            )

            Text(
                text = "DevForge automatically codes frontends, backends, database models, schemas, deployment assets, and runs them instantly.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Start Prompter",
                    color = CyberNeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = null,
                    tint = CyberNeonPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyProjectsCard(onCreateClicked: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassBackgroundDark else GlassBackgroundLight
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = CyberNeonTeal,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "No Forge Workspaces Yet",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DeepSlateBlue
                )
            )

            Text(
                text = "Create your first high-powered workspace now to start building.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            )

            Button(
                onClick = onCreateClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberNeonTeal,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("New Project Workspace")
            }
        }
    }
}

@Composable
fun ProjectRowCard(project: ProjectEntity, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassBackgroundDark else GlassBackgroundLight
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberNeonPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = CyberNeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = project.name,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepSlateBlue
                        )
                    )
                    Text(
                        text = project.description,
                        style = TextStyle(fontSize = 12.sp, color = Color.Gray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Badge(containerColor = CyberNeonTeal.copy(alpha = 0.15f)) {
                            Text(project.platformType, color = CyberNeonTeal, fontSize = 10.sp)
                        }
                        if (project.deploymentStatus == "Success") {
                            Badge(containerColor = Color(0xFF22C55E).copy(alpha = 0.15f)) {
                                Text("Live", color = Color(0xFF22C55E), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = "Open",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TemplatesCarousel(onTemplateSelect: (String, String) -> Unit) {
    val templates = listOf(
        Triple("SaaS App", "Next.js", Icons.Default.Cloud),
        Triple("E-commerce Store", "Express", Icons.Default.ShoppingBag),
        Triple("AI Chatbot", "React", Icons.Default.SmartToy),
        Triple("Static Webpage", "HTML", Icons.Default.Language)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(templates) { (name, platform, icon) ->
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .clickable { onTemplateSelect(name, platform) },
                colors = CardDefaults.cardColors(containerColor = Color(0x33A855F7)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberNeonPurple.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = name, tint = CyberNeonTeal, modifier = Modifier.size(28.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text(platform, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceExtensionsGrid() {
    val extensions = listOf(
        Pair("NextAuth Auth module", "Preconfigured social connectors"),
        Pair("Prisma ORM kit", "Database tables migrations"),
        Pair("Tailwind Glassmorphic Theme", "Vibrant futuristic layouts"),
        Pair("OpenAI API proxy", "Cognitive AI completions")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(extensions) { (title, desc) ->
            Card(
                modifier = Modifier.width(220.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1F000000)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = CyberNeonPurple)
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Install", fontSize = 10.sp, color = Color.White)
                        }
                    }
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                        Text(desc, fontSize = 11.sp, color = Color.Gray, maxLines = 2, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. THE CROWN JEWEL: CLOUD IDE WORKSPACE
// ==========================================

@Composable
fun WorkspaceScreen(viewModel: DevForgeViewModel, onSettingsClick: () -> Unit) {
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600

    var currentTab by remember { mutableStateOf("📁 Files") }
    val tabs = listOf("📁 Files", "💻 Code", "🗃️ DB", "🖥️ Term", "🐙 Git", "🚀 Deploy", "👁️ Preview", "👥 Team")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            selectedProject?.let { proj ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF0F172A) else Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { viewModel.selectProject(null) }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) Color.White else DeepSlateBlue)
                        }
                        Column {
                            Text(
                                proj.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isDark) Color.White else DeepSlateBlue
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DevForge Sandbox Server Online", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Badge(containerColor = CyberNeonTeal.copy(alpha = 0.15f)) {
                            Text(proj.platformType, color = CyberNeonTeal, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isDark) Color.White else DeepSlateBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isWideScreen) {
                // Bottom tab navigation
                NavigationBar(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                    contentColor = if (isDark) Color.White else DeepSlateBlue
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Text(
                                    tab.take(2),
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            label = { Text(tab.drop(2), fontSize = 9.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberNeonTeal,
                                selectedTextColor = CyberNeonTeal,
                                indicatorColor = CyberNeonTeal.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                // Wide Screen Side Navigation Rail
                NavigationRail(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                    contentColor = if (isDark) Color.White else DeepSlateBlue
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Text(
                                    tab.take(2),
                                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            label = { Text(tab.drop(2), fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CyberNeonTeal,
                                selectedTextColor = CyberNeonTeal,
                                indicatorColor = CyberNeonTeal.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            // Central Display Panel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        slideInHorizontally(animationSpec = tween(300)) { width -> width / 3 } + fadeIn(animationSpec = tween(300)) togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { width -> -width / 3 } + fadeOut(animationSpec = tween(300))
                    },
                    label = "WorkspaceTabTransition"
                ) { tab ->
                    when (tab) {
                        "📁 Files" -> FileExplorerGeneratorTab(viewModel = viewModel)
                        "💻 Code" -> CodeEditorTab(viewModel = viewModel)
                        "🗃️ DB" -> DatabaseDesignerTab(viewModel = viewModel)
                        "🖥️ Term" -> CloudTerminalTab(viewModel = viewModel)
                        "🐙 Git" -> GitHubGitTab(viewModel = viewModel)
                        "🚀 Deploy" -> OneClickDeploymentTab(viewModel = viewModel)
                        "👁️ Preview" -> RealtimeLivePreviewTab(viewModel = viewModel)
                        "👥 Team" -> TeamCollaborationTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2A. FILE EXPLORER & AI GENERATOR TAB
// ==========================================

@Composable
fun FileExplorerGeneratorTab(viewModel: DevForgeViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val aiLogs by viewModel.aiLogs.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var aiPromptInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Generator Input box
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberNeonTeal.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberNeonTeal)
                    Text("AI App & Website Generator", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) Color.White else DeepSlateBlue)
                }

                Text(
                    "Prompt DevForge to auto-generate entire pages, backends, databases, and structural code configurations.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text("Build a modern signup page with Tailwind styles...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).testTag("ai_prompt_input_field"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (aiPromptInput.isNotEmpty()) {
                                viewModel.generateProjectAI(aiPromptInput)
                                aiPromptInput = ""
                            }
                        })
                    )
                    Button(
                        onClick = {
                            if (aiPromptInput.isNotEmpty()) {
                                viewModel.generateProjectAI(aiPromptInput)
                                aiPromptInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Generate")
                    }
                }

                // Generator Progress logs
                if (aiStatus == "generating" || aiStatus == "success" || aiStatus == "error") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (aiStatus == "generating") {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = CyberNeonTeal)
                                } else {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (aiStatus == "success") Color.Green else Color.Red))
                                }
                                Text("DevForge Orchestrator Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            
                            aiLogs.forEach { log ->
                                Text("> $log", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                            }

                            aiError?.let { err ->
                                Text("Error: $err", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }

        // File Structure Tree
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) GlassBackgroundDark else GlassBackgroundLight
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Project File Explorer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else DeepSlateBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No workspace files seeded. Prompt AI above!", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = selectedFile?.id == file.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyberNeonTeal.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { viewModel.selectFile(file) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (file.isFolder) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (file.isFolder) CyberNeonPurple else CyberNeonTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = file.filePath,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = if (isSelected) CyberNeonTeal else (if (isDark) Color.White else DeepSlateBlue)
                                        )
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Active",
                                        tint = CyberNeonTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2B. CODES EDITOR TAB WITH MULTI-TABS & AI ACTIONS
// ==========================================

@Composable
fun CodeEditorTab(viewModel: DevForgeViewModel) {
    val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val currentContent by viewModel.currentEditorContent.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val replaceQuery by viewModel.replaceQuery.collectAsStateWithLifecycle()

    var explainResultText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedFile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CodeOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Text("Select a file from File Explorer to edit", color = Color.Gray, fontSize = 14.sp)
                }
            }
            return
        }

        // Multi-tab bar switcher
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(openTabs) { tab ->
                val isActive = selectedFile?.id == tab.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (isActive) EditorDarkBackground else Color.Black.copy(alpha = 0.2f))
                        .clickable { viewModel.selectFile(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tab.filePath.substringAfterLast("/"),
                        color = if (isActive) CyberNeonTeal else Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(10.dp)
                            .clickable { viewModel.closeTab(tab) }
                    )
                }
            }
        }

        // Search & Replace Editor Tools
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchAndReplace(it, replaceQuery) },
                    placeholder = { Text("Search text...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { viewModel.updateSearchAndReplace(searchQuery, it) },
                    placeholder = { Text("Replace with...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )
                Button(
                    onClick = { viewModel.performReplaceAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonPurple, contentColor = Color.White),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Replace All", fontSize = 10.sp)
                }
            }
        }

        // Main Editor Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EditorDarkBackground)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Editor Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedFile?.filePath ?: "", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    // AI Assist & Save buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                viewModel.explainCurrentCode { explainResultText = it }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Explain Code", tint = CyberNeonTeal, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.fixCurrentCode { explainResultText = it }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Auto Fix", tint = CyberNeonPurple, modifier = Modifier.size(16.dp))
                        }
                        Button(
                            onClick = { viewModel.saveCurrentFile() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal, contentColor = Color.Black),
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Save File", fontSize = 10.sp)
                        }
                    }
                }

                // Code Input Text Area
                BasicTextField(
                    value = currentContent,
                    onValueChange = { viewModel.updateEditorContent(it) },
                    textStyle = TextStyle(
                        color = Color(0xFF22D3EE),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberNeonTeal)
                )
            }

            // Spinner overlay for AI
            if (aiStatus == "thinking") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberNeonTeal)
                }
            }
        }

        // Explanations Result Sheet
        explainResultText?.let { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DevForge AI Coding Companion:", fontWeight = FontWeight.Bold, color = CyberNeonTeal, fontSize = 12.sp)
                        IconButton(onClick = { explainResultText = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(result, fontSize = 11.sp, color = Color.White, modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 120.dp))
                }
            }
        }
    }
}

// ==========================================
// 2C. VISUAL DATABASE DESIGNER TAB
// ==========================================

@Composable
fun DatabaseDesignerTab(viewModel: DevForgeViewModel) {
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    var showAddTableDialog by remember { mutableStateOf(false) }

    var tableNameInput by remember { mutableStateOf("") }
    var columnsInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Visual Database Designer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color.White else DeepSlateBlue)
                Text("Design PostgreSQL / Prisma Schemas visually", fontSize = 11.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddTableDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Table")
            }
        }

        if (tables.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BackupTable, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(42.dp))
                    Text("No Tables yet. Create table visually above!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tables) { table ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberNeonPurple.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = CyberNeonTeal, modifier = Modifier.size(18.dp))
                                    Text(table.tableName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (isDark) Color.White else DeepSlateBlue)
                                }
                                IconButton(onClick = { viewModel.deleteDatabaseTable(table.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Table", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // Draw Schema Columns
                            table.columnsData.split(",").forEach { col ->
                                val parts = col.trim().split(":")
                                val cName = parts.getOrNull(0) ?: ""
                                val cType = parts.getOrNull(1) ?: "String"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cName, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.LightGray)
                                    Text(cType, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberNeonPurple)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Visual Schema Prisma Output Panel
        if (tables.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EditorDarkBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Auto-Generated Prisma Schema:", fontWeight = FontWeight.Bold, color = CyberNeonTeal, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            tables.forEach { table ->
                                appendLine("model ${table.tableName} {")
                                appendLine("  id    String @id @default(cuid())")
                                table.columnsData.split(",").forEach { col ->
                                    val parts = col.trim().split(":")
                                    val cName = parts.getOrNull(0) ?: ""
                                    val cType = parts.getOrNull(1) ?: "String"
                                    if(cName.isNotEmpty() && cName.lowercase() != "id") {
                                        appendLine("  $cName  $cType")
                                    }
                                }
                                appendLine("}")
                                appendLine()
                            }
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Green,
                        modifier = Modifier.heightIn(max = 100.dp).verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showAddTableDialog) {
        Dialog(onDismissRequest = { showAddTableDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add Database Table", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    
                    OutlinedTextField(
                        value = tableNameInput,
                        onValueChange = { tableNameInput = it },
                        label = { Text("Table Name (e.g. Product)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = columnsInput,
                        onValueChange = { columnsInput = it },
                        label = { Text("Columns Schema (name:String, price:Float)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddTableDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tableNameInput.isNotEmpty() && columnsInput.isNotEmpty()) {
                                    viewModel.createDatabaseTable(tableNameInput, columnsInput)
                                    tableNameInput = ""
                                    columnsInput = ""
                                    showAddTableDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal)
                        ) {
                            Text("Create Table")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2D. BUILT-IN BASHS TERMINAL TAB
// ==========================================

@Composable
fun CloudTerminalTab(viewModel: DevForgeViewModel) {
    val history by viewModel.terminalHistory.collectAsStateWithLifecycle()
    val path by viewModel.currentPath.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                Text("Isolated Ubuntu container sandbox", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Badge(containerColor = Color.Green.copy(alpha = 0.2f)) {
                Text("npm/python enabled", color = Color.Green, fontSize = 8.sp)
            }
        }

        Divider(color = Color.White.copy(alpha = 0.1f))

        // Terminal history logs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                history.forEach { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (log.contains("$")) Color(0xFF38BDF8) else Color.White
                    )
                }
            }
        }

        // Input bash prompter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$path$ ", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Green)
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.weight(1f).testTag("terminal_input_field"),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.isNotEmpty()) {
                        viewModel.executeTerminalCommand(input)
                        input = ""
                    }
                })
            )
        }
    }
}

// ==========================================
// 2E. GITHUB SYSTEM AND GIT GRAPH TAB
// ==========================================

@Composable
fun GitHubGitTab(viewModel: DevForgeViewModel) {
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    var commitMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("GitHub Source Control", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color.White else DeepSlateBlue)

        Card(
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = CyberNeonTeal)
                    Text("Repository Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = project?.gitRepoUrl?.ifEmpty { "Repository disconnected" } ?: "GitHub Repository disconnected",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberNeonTeal
                )

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit message") },
                    modifier = Modifier.fillMaxWidth().testTag("git_commit_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (commitMessage.isNotEmpty()) {
                                viewModel.commitAndPushToGitHub(commitMessage)
                                commitMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Commit & Push")
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Pull Request")
                    }
                }
            }
        }

        // Git history graph representation
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = EditorDarkBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Git Revision History Graph", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GitHistoryItem(sha = "f3b20c9", author = "Developer Pro", msg = "Saved changes locally")
                    GitHistoryItem(sha = "a8f34bc", author = "DevForge Orchestrator", msg = "Automated NextAuth config generation")
                    GitHistoryItem(sha = "9c298ee", author = "System", msg = "Initial repository template setup")
                }
            }
        }
    }
}

@Composable
fun GitHistoryItem(sha: String, author: String, msg: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberNeonTeal))
            Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.5f)))
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sha, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberNeonPurple)
                Text(author, fontSize = 11.sp, color = Color.Gray)
            }
            Text(msg, fontSize = 12.sp, color = Color.White)
        }
    }
}

// ==========================================
// 2F. ONE-CLICK DEPLOYMENTS PIPELINES
// ==========================================

@Composable
fun OneClickDeploymentTab(viewModel: DevForgeViewModel) {
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val logs by viewModel.deploymentLogs.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Deploy App Pipeline", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color.White else DeepSlateBlue)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val providers = listOf("Vercel", "Netlify", "Railway", "Firebase")
            providers.forEach { provider ->
                Button(
                    onClick = { viewModel.deployProject(provider) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(provider, fontSize = 10.sp)
                }
            }
        }

        // Main deployment logs terminal
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deployment Server Output Logs", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No deployments triggered yet. Click a provider above!", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(logs) { log ->
                            Text("> $log", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Green)
                        }
                    }
                }
            }
        }

        // Deployment Live status card
        if (project?.deploymentUrl?.isNotEmpty() == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x3322C55E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Deployment Successful", fontWeight = FontWeight.Bold, color = Color.Green, fontSize = 12.sp)
                        Text(project?.deploymentUrl ?: "", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Icon(Icons.Default.Launch, contentDescription = "Open", tint = Color.Green)
                }
            }
        }
    }
}

// ==========================================
// 2G. REAL-TIME PREVIEW SIMULATOR PANEL
// ==========================================

@Composable
fun RealtimeLivePreviewTab(viewModel: DevForgeViewModel) {
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    // Parse the file content to show realistic interactive visual mockup
    val homeFile = files.find { it.filePath.contains("page") || it.filePath.contains("index") || it.filePath.contains("App") }
    val content = homeFile?.content ?: ""

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Real-Time Hot Reload Sandbox", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) Color.White else DeepSlateBlue)
                Text("Direct client rendering inside DevForge sandbox", fontSize = 11.sp, color = Color.Gray)
            }
            Badge(containerColor = Color.Green.copy(alpha = 0.2f)) {
                Text("Active Port: 3000", color = Color.Green, fontSize = 9.sp)
            }
        }

        // Split design representation based on contents of generated file!
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Reactive Parser Engine
                if (content.contains("SaaS")) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DevForge SaaS Landing",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberNeonTeal,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Enterprise cloud-native orchestration pipeline",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.size(width = 80.dp, height = 80.dp)) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("Basic", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("${'$'}0/mo", fontSize = 13.sp, color = CyberNeonPurple)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.size(width = 80.dp, height = 80.dp)) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("Premium", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("${'$'}19/mo", fontSize = 13.sp, color = CyberNeonTeal)
                                }
                            }
                        }
                    }
                } else if (content.contains("Store") || content.contains("ecommerce")) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Forge E-Commerce Store", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.width(100.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                    Text("Headphones", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("${'$'}99.99", fontSize = 8.sp, color = CyberNeonTeal)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.width(100.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                    Text("Keyboard", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("${'$'}129.99", fontSize = 8.sp, color = CyberNeonTeal)
                                }
                            }
                        }
                    }
                } else if (content.contains("Chatbot") || content.contains("chat")) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI Chatbot UI Simulator", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)).padding(8.dp)) {
                                Text("Hello! Ask me any coding requests.", color = Color.White, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(8.dp)).background(CyberNeonPurple.copy(alpha = 0.3f)).padding(8.dp)) {
                                Text("Explain Prisma schema.", color = Color.White, fontSize = 11.sp)
                            }
                        }
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Simulated entry...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                    }
                } else {
                    // Default Landing Page template
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(48.dp), tint = CyberNeonTeal)
                        Text(project?.name ?: "DevForge App", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Hot-Reload Active. Make changes in Code Editor to update.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2H. TEAM COLLABORATION & COMMENTS TAB
// ==========================================

@Composable
fun TeamCollaborationTab(viewModel: DevForgeViewModel) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var commentInput by remember { mutableStateOf("") }
    var lineInput by remember { mutableStateOf("1") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Collaborator Activity & Comments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) Color.White else DeepSlateBlue)

        // Comment composer input
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("Add code review feedback...") },
                        modifier = Modifier.weight(1f).testTag("comment_input_field")
                    )
                    OutlinedTextField(
                        value = lineInput,
                        onValueChange = { lineInput = it },
                        label = { Text("Line") },
                        modifier = Modifier.width(60.dp)
                    )
                }
                Button(
                    onClick = {
                        if (commentInput.isNotEmpty()) {
                            viewModel.addComment("src/App.tsx", lineInput.toIntOrNull() ?: 1, commentInput)
                            commentInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Post Comment")
                }
            }
        }

        // Active workspace collaborators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Now:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                CollaboratorAvatar(color = CyberNeonTeal, initial = "S")
                CollaboratorAvatar(color = CyberNeonPurple, initial = "A")
                CollaboratorAvatar(color = Color(0xFF22C55E), initial = "D")
            }
            Text("3 team members coding", fontSize = 11.sp, color = Color.Gray)
        }

        // Comments Timeline
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBackgroundDark else GlassBackgroundLight),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Workspace Comment Stream", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDark) Color.White else DeepSlateBlue)
                Spacer(modifier = Modifier.height(10.dp))

                if (comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No feedback comments left yet.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(comments) { comment ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberNeonTeal)
                                        Text("on ${comment.filePath}:${comment.lineNumber}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(comment.timestamp))
                                    Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(comment.content, fontSize = 12.sp, color = if (isDark) Color.White else DeepSlateBlue)
                                Divider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollaboratorAvatar(color: Color, initial: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color(0xFF0F172A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(viewModel: DevForgeViewModel, onDismiss: () -> Unit) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userApiKey.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var nameInput by remember { mutableStateOf(username) }
    var emailInput by remember { mutableStateOf(userEmail) }
    var apiKeyInput by remember { mutableStateOf(userApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(2.dp, Brush.linearGradient(listOf(CyberNeonTeal, CyberNeonPurple)), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberNeonTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = CyberNeonTeal)
                    }
                    Column {
                        Text(
                            text = "DevForge AI Configuration",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else DeepSlateBlue
                            )
                        )
                        Text(
                            text = "Fine-tune your local environment settings",
                            style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) Color.DarkGray else Color.LightGray, thickness = 1.dp)

                // 1. Developer Handle
                Text("DEVELOPER PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberNeonTeal, letterSpacing = 1.sp)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Developer Username") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_username_input"),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberNeonTeal,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Developer Email") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_email_input"),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberNeonTeal,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Custom Gemini API Key
                Text("API INTEGRATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberNeonPurple, letterSpacing = 1.sp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input"),
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (apiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonPurple,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    
                    Text(
                        text = "Leave empty to use DevForge default sandbox keys. Providing a key bypasses quota constraints and lets you run larger synthesis models directly.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                }

                // API Connection Status Indicator Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (apiKeyInput.isNotEmpty()) CyberNeonTeal.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f))
                        .border(1.dp, if (apiKeyInput.isNotEmpty()) CyberNeonTeal.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (apiKeyInput.isNotEmpty()) Color(0xFF22C55E) else CyberNeonPurple)
                        )
                        Text(
                            text = if (apiKeyInput.isNotEmpty()) "Personal Gemini API Key Configured (ACTIVE)" else "Sandbox API Keys Active (FALLBACK)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (apiKeyInput.isNotEmpty()) Color(0xFF22C55E) else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            viewModel.updateProfile(nameInput, emailInput)
                            viewModel.updateApiKey(apiKeyInput)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberNeonTeal,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Configurations", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
