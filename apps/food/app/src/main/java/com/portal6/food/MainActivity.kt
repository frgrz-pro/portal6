package com.portal6.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portal6.food.ui.FoodTheme
import com.portal6.food.ui.FoodViewModel
import com.portal6.food.ui.IdeasScreen
import com.portal6.food.ui.LikedScreen
import com.portal6.food.ui.RecipeSheet
import com.portal6.food.ui.StockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FoodTheme { FoodRoot() } }
    }
}

private enum class Tab(val label: String) { IDEAS("Idées"), LIKED("Validées"), STOCK("Stock") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodRoot(vm: FoodViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val persons by vm.persons.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.IDEAS) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.toasts.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍽️ Food", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(16.dp))
                        // Slider global « nombre de personnes » : pilote la jauge de toutes les cartes.
                        Text("👥", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = persons.toFloat(),
                            onValueChange = { vm.setPersons(it.roundToInt()) },
                            valueRange = 1f..8f,
                            modifier = Modifier.width(120.dp).padding(horizontal = 6.dp),
                        )
                        Text("$persons", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            val likedCount = state.liked.size + state.leftovers.size
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label) },
                        icon = {
                            val icon = when (t) {
                                Tab.IDEAS -> Icons.Filled.Whatshot
                                Tab.LIKED -> Icons.Filled.Favorite
                                Tab.STOCK -> Icons.Filled.Kitchen
                            }
                            if (t == Tab.LIKED && likedCount > 0) {
                                BadgedBox(badge = { Badge { Text("$likedCount") } }) { Icon(icon, t.label) }
                            } else Icon(icon, t.label)
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.IDEAS -> IdeasScreen(vm, state, persons)
                Tab.LIKED -> LikedScreen(vm, state, persons)
                Tab.STOCK -> StockScreen(vm, state)
            }
        }
    }

    detail?.let { (recipeId, leftoverId) ->
        state.recipeById[recipeId]?.let { recipe ->
            RecipeSheet(vm, state, recipe, initialPersons = persons, leftoverId = leftoverId, onDismiss = vm::closeDetail)
        }
    }
}
