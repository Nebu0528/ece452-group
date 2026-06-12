package ca.uwaterloo.ece452.discoveruwaterloo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ca.uwaterloo.ece452.discoveruwaterloo.ui.theme.DiscoverUWaterlooTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscoverUWaterlooTheme {
                AppNavigation(viewModel)
            }
        }
    }
}