package trif.novica.spoilerchecker.di

import android.content.Context
import trif.novica.spoilerchecker.data.local.AppDatabase
import trif.novica.spoilerchecker.data.remote.EspnApiService
import trif.novica.spoilerchecker.data.repository.GameRepository
import trif.novica.spoilerchecker.data.repository.SpoilerRepository
import trif.novica.spoilerchecker.data.repository.TeamRepository
import trif.novica.spoilerchecker.detection.KeywordMatcher
import trif.novica.spoilerchecker.detection.PlayerMatcher
import trif.novica.spoilerchecker.detection.SemanticMatcher
import trif.novica.spoilerchecker.detection.SpoilerDetector
import trif.novica.spoilerchecker.ml.EmbeddingModel

class AppModule(private val context: Context) {

    // ===== Database =====
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val teamDao by lazy { database.teamDao() }
    private val playerDao by lazy { database.playerDao() }
    private val favoriteTeamDao by lazy { database.favoriteTeamDao() }
    private val cleaningResultDao by lazy { database.cleaningResultDao() }

    // ===== API =====
    private val espnApi by lazy { EspnApiService.api }

    // ===== Repositories =====
    val teamRepository: TeamRepository by lazy {
        TeamRepository(teamDao, playerDao, favoriteTeamDao, espnApi)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(teamDao, espnApi)
    }

    val spoilerRepository: SpoilerRepository by lazy {
        SpoilerRepository(cleaningResultDao)
    }

    // ===== ML =====
    val embeddingModel: EmbeddingModel by lazy {
        EmbeddingModel(context)
    }

    // ===== Detection =====
    val keywordMatcher: KeywordMatcher by lazy {
        KeywordMatcher()
    }

    val playerMatcher: PlayerMatcher by lazy {
        PlayerMatcher()
    }

    val semanticMatcher: SemanticMatcher by lazy {
        SemanticMatcher(embeddingModel)
    }

    val spoilerDetector: SpoilerDetector by lazy {
        SpoilerDetector(keywordMatcher, playerMatcher, semanticMatcher)
    }
}
