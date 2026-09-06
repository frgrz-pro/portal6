package com.portal6.haremote

import android.app.Application
import android.content.Context
import com.portal6.haremote.data.ConfigStore
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.MockLightsRepository
import com.portal6.haremote.data.MockTvRepository
import com.portal6.haremote.data.TvRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Injection manuelle : l'app est trop petite pour Hilt. Le point important est
 * que les dépôts soient des **singletons de process** — l'UI et les tuiles des
 * réglages rapides tournent dans le même process et doivent voir le même état.
 */
class AppContainer(context: Context) {
    val store = ConfigStore(context)
    val lights: LightsRepository = MockLightsRepository(store)
    val tv: TvRepository = MockTvRepository(store)

    /** Portée des actions déclenchées hors UI (tuiles), non liée à un écran. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

class Portal6App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Raccourci depuis une Activity, un Service ou une tuile. */
val Context.container: AppContainer
    get() = (applicationContext as Portal6App).container
