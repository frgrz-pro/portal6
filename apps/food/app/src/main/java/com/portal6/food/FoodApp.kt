package com.portal6.food

import android.app.Application
import com.portal6.food.data.FoodRepository

/** Le dépôt est un singleton de process (même modèle que ha-remote : `Portal6App` + container). */
class FoodApp : Application() {
    lateinit var repository: FoodRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FoodRepository(this)
    }
}
