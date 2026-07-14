package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.DevForgeRepository
import com.example.ui.DevForgeApp
import com.example.ui.DevForgeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room Database
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "devforge_persistent_db"
        ).fallbackToDestructiveMigration().build()

        val repository = DevForgeRepository(database.devForgeDao())
        val viewModel = DevForgeViewModel(repository, applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevForgeApp(viewModel = viewModel)
                }
            }
        }
    }
}
