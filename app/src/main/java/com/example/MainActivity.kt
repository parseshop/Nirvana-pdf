package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PdfViewModel
import com.example.viewmodel.PdfViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room database & repository
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database.appDao())

        // Initialize ViewModel via our customized Factory
        val viewModel: PdfViewModel by viewModels {
            PdfViewModelFactory(application, repository)
        }

        // Handle incoming PDF intent from system
        handleIntent(intent, viewModel)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val activePdfUri = viewModel.currentPdfUri
                    if (activePdfUri != null) {
                        PdfViewerScreen(
                            viewModel = viewModel,
                            uri = activePdfUri,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        HomeScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database.appDao())
        val viewModel: PdfViewModel by viewModels {
            PdfViewModelFactory(application, repository)
        }
        handleIntent(intent, viewModel)
    }

    private fun handleIntent(intent: android.content.Intent?, viewModel: PdfViewModel) {
        if (intent != null && intent.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                viewModel.openPdf(uri, this)
            }
        }
    }
}
