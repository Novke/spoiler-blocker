package trif.novica.spoilerchecker.di

import android.content.Context
import trif.novica.spoilerchecker.data.local.AppDatabase
import trif.novica.spoilerchecker.data.repository.SpoilerRepository
import trif.novica.spoilerchecker.ml.EmbeddingModel
import trif.novica.spoilerchecker.ml.SpoilerDetector

class AppModule(private val context: Context) {

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val favoriteTeamDao by lazy { database.favoriteTeamDao() }
    private val cleaningResultDao by lazy { database.cleaningResultDao() }

    val repository: SpoilerRepository by lazy {
        SpoilerRepository(favoriteTeamDao, cleaningResultDao)
    }

    val embeddingModel: EmbeddingModel by lazy {
        EmbeddingModel(context)
    }

    val spoilerDetector: SpoilerDetector by lazy {
        SpoilerDetector(embeddingModel)
    }
}
