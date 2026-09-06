package com.portal6.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portal6.remote.ui.LightsScreen
import com.portal6.remote.ui.LightsViewModel
import com.portal6.remote.ui.TvScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
            ) {
                RemoteApp()
            }
        }
    }
}

private enum class Tab(val label: String) { Lights("Lights"), Tv("TV") }

@Composable
private fun RemoteApp() {
    var currentTab by rememberSaveable { mutableStateOf(Tab.Lights) }
    val lightsViewModel: LightsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == Tab.Lights,
                    onClick = { currentTab = Tab.Lights },
                    icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null) },
                    label = { Text(Tab.Lights.label) },
                )
                NavigationBarItem(
                    selected = currentTab == Tab.Tv,
                    onClick = { currentTab = Tab.Tv },
                    icon = { Icon(Icons.Filled.Tv, contentDescription = null) },
                    label = { Text(Tab.Tv.label) },
                )
            }
        },
    ) { innerPadding ->
        when (currentTab) {
            Tab.Lights -> LightsScreen(
                viewModel = lightsViewModel,
                modifier = Modifier.padding(innerPadding),
            )
            Tab.Tv -> TvScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
