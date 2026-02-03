package trif.novica.spoilerchecker

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import trif.novica.spoilerchecker.di.AppModule

class SpoilerShieldApp : Application() {

    lateinit var appModule: AppModule
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        appModule = AppModule(this)

        applicationScope.launch {
            try {
                appModule.embeddingModel.initialize()
                Log.d("SpoilerShieldApp", "ML model initialized successfully")
            } catch (e: Exception) {
                Log.e("SpoilerShieldApp", "Failed to initialize ML model", e)
            }
        }
    }
}
