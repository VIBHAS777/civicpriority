package com.civic.priority

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.priority.ui.screens.ContentScreen
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.ui.theme.CivicPriorityTheme
import com.civic.priority.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivicPriorityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CivicColors.Background
                ) {
                    val viewModel: AppViewModel = viewModel()
                    ContentScreen(viewModel = viewModel)
                }
            }
        }
    }
}
