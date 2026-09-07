package com.portal6.haremote.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.HaSettings
import com.portal6.haremote.data.SettingsStore
import com.portal6.haremote.data.ha.HaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore,
    val connection: StateFlow<String>,
) : ViewModel() {

    val settings: StateFlow<HaSettings> = store.settings

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

    fun save(url: String, token: String) = store.save(url, token)

    /** Essaie `GET /api/` avec les valeurs saisies, sans les enregistrer. */
    fun test(url: String, token: String) {
        viewModelScope.launch {
            _testResult.value = "Test en cours…"
            _testResult.value = runCatching { HaClient(HaSettings(url, token)).ping() }
                .fold({ "OK — HA répond : $it" }, { "Échec : ${it.message}" })
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                SettingsViewModel(app.container.settings, app.container.connection)
            }
        }
    }
}

/**
 * URL et jeton longue durée de Home Assistant. Le jeton se crée dans HA :
 * profil → Sécurité → Jetons d'accès longue durée. Il ne quitte jamais le
 * stockage privé de l'app.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    var url by rememberSaveable(settings.url) { mutableStateOf(settings.url.ifBlank { "http://192.168.0.5:8123" }) }
    var token by rememberSaveable(settings.token) { mutableStateOf(settings.token) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Home Assistant", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL (LAN)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Jeton longue durée") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedButton(onClick = { viewModel.test(url, token) }) { Text("Tester") }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = url.isNotBlank() && token.isNotBlank(),
                onClick = { viewModel.save(url, token) },
            ) { Text("Enregistrer") }
        }
        testResult?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
        Text("Liaison", style = MaterialTheme.typography.titleMedium)
        Text(connection, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            "Vider l'URL ou le jeton et enregistrer = retour au mode démo (prises simulées).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
