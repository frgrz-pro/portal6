package com.portal6.haremote

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.portal6.haremote.data.BackendHolder
import com.portal6.haremote.data.ConfigStore
import com.portal6.haremote.data.DelegatingLightsRepository
import com.portal6.haremote.data.DelegatingModesRepository
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.ModesRepository
import com.portal6.haremote.data.MockTvRepository
import com.portal6.haremote.data.SettingsStore
import com.portal6.haremote.data.TvRepository
import com.portal6.haremote.data.trmnl.DelegatingTrmnlRepository
import com.portal6.haremote.data.trmnl.TrmnlRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

/**
 * Injection manuelle : l'app est trop petite pour Hilt. Le point important est
 * que les dépôts soient des **singletons de process** — l'UI et les tuiles des
 * réglages rapides tournent dans le même process et doivent voir le même état.
 */
class AppContainer(context: Context) {
    /** Portée des actions déclenchées hors UI (tuiles, backend), non liée à un écran. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val store = ConfigStore(context)
    val settings = SettingsStore(context)

    private val backends = BackendHolder(settings, store, appScope)
    val lights: LightsRepository = DelegatingLightsRepository(backends, appScope)
    val modes: ModesRepository = DelegatingModesRepository(backends, appScope)

    /** État de la liaison avec Home Assistant, pour l'affichage. */
    val connection: StateFlow<String> = backends.connection

    /**
     * L'utilisateur regarde l'app (Activity reprise, tuile affichée) : on rouvre
     * le WebSocket HA tout de suite. Après une mise en veille, l'ancien socket
     * est un zombie qu'OkHttp ne détecte qu'au ping suivant (jusqu'à 40 s).
     */
    fun wake() = backends.wake()

    val tv: TvRepository = MockTvRepository(store)

    /** Rotation des écrans du TRMNL — parle directement à trmnl.com, pas via HA. */
    val trmnl: TrmnlRepository = DelegatingTrmnlRepository(settings.trmnlKey, appScope)
}

class Portal6App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = container.wake()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}

/** Raccourci depuis une Activity, un Service ou une tuile. */
val Context.container: AppContainer
    get() = (applicationContext as Portal6App).container
