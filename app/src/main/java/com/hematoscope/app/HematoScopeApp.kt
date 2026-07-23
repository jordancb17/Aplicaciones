package com.hematoscope.app

import android.app.Application
import com.hematoscope.app.data.repository.HematoRepository

/**
 * Application entry point. Owns the singleton [HematoRepository]; ViewModels read
 * it via `(application as HematoScopeApp).repository` (lightweight manual DI).
 */
class HematoScopeApp : Application() {
    val repository: HematoRepository by lazy { HematoRepository(this) }
}
