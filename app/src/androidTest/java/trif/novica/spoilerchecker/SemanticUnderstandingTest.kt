package trif.novica.spoilerchecker

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import trif.novica.spoilerchecker.ml.EmbeddingModel

/**
 * Test to understand semantic matching behavior with real-world examples.
 * No assertions - just logs scores for analysis.
 *
 * Run with: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=trif.novica.spoilerchecker.SemanticUnderstandingTest
 */
@RunWith(AndroidJUnit4::class)
class SemanticUnderstandingTest {

    companion object {
        private const val TAG = "SemanticUnderstanding"
    }

    private lateinit var embeddingModel: EmbeddingModel

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = java.io.File(appContext.filesDir, "model.onnx")
        if (modelFile.exists()) modelFile.delete()

        embeddingModel = EmbeddingModel(appContext)
        runBlocking { embeddingModel.initialize() }
    }

    @Test
    fun understandSemanticMatching() {
        runBlocking {
            val query = "Denver Nuggets vs LA Lakers"

            val testNotifications = listOf(
                // Other teams - should NOT match
                "LA Clippers vs Detroit Pistons" to "different teams",
                "LA Clippers vs Denver Nuggets" to "partial match (Denver but not Lakers)",
                "Boston Celtics vs Miami Heat" to "completely different teams",
                "Golden State Warriors vs Phoenix Suns" to "completely different teams",

                // Correct teams - SHOULD match
                "LA Lakers vs Detroit Pistons" to "partial match (Lakers but not Nuggets)",
                "Denver Nuggets vs Los Angeles Lakers" to "exact match different format",
                "Los Angeles Lakers vs Denver Nuggets" to "exact match reversed",
                "Lakers vs Nuggets" to "short form exact",
                "Nuggets vs Lakers" to "short form reversed",

                // Player mentions - context dependent
                "Nikola Jokic with 30 points tonight" to "Nuggets player",
                "LeBron James scores 40 in loss" to "Lakers player",
                "Jokic and LeBron battle it out" to "both players",
                "Anthony Davis dominates the paint" to "Lakers player",
                "Jamal Murray returns from injury" to "Nuggets player",

                // Game narrative - SHOULD match
                "Nuggets dominate LeBron once again!" to "team + opposing player",
                "Nuggets dominate Lakers once again!" to "both teams mentioned",
                "Lakers upset Nuggets in overtime thriller" to "both teams",
                "Nuggets get 4th straight road win" to "one team only",
                "Lakers extend losing streak to 5 games" to "one team only",
                "Denver wins against Los Angeles" to "cities not team names",

                // Ambiguous / edge cases
                "NBA highlights: Best dunks of the night" to "generic NBA",
                "Jokic MVP race heats up" to "player without game context",
                "Lakers trade rumors intensify" to "team news not game",
                "Denver weather forecast: Snow expected" to "city name different context",
                "LA traffic causes delays" to "city abbreviation different context",

                // Sofascore-style notifications
                "Denver Nuggets - Los Angeles Lakers VIDEO: Highlights" to "Sofascore format",
                "Los Angeles Lakers - Denver Nuggets Match finished 112-108" to "Sofascore result",
                "Denver Nuggets - Los Angeles Lakers 3rd quarter" to "Sofascore live",
                "Los Angeles Lakers - Denver Nuggets Halftime: Lakers lead" to "Sofascore halftime",

                // Non-sports
                "New iPhone announced by Apple" to "tech news",
                "Breaking: Stock market crashes" to "finance news",
                "Netflix releases new documentary" to "entertainment",
            )

            Log.d(TAG, "")
            Log.d(TAG, "=" .repeat(80))
            Log.d(TAG, "SEMANTIC UNDERSTANDING TEST")
            Log.d(TAG, "Query: \"$query\"")
            Log.d(TAG, "Threshold: 0.55 (current setting)")
            Log.d(TAG, "=" .repeat(80))
            Log.d(TAG, "")

            val queryEmbedding = embeddingModel.getEmbedding(query)

            for ((notification, description) in testNotifications) {
                val notifEmbedding = embeddingModel.getEmbedding(notification)
                val score = cosineSimilarity(queryEmbedding, notifEmbedding)

                val status = when {
                    score >= 0.55f -> "✓ MATCH"
                    score >= 0.40f -> "? CLOSE"
                    else -> "✗ NO   "
                }

                Log.d(TAG, String.format("%s %.3f | %-50s | %s",
                    status, score, notification.take(50), description))
            }

            Log.d(TAG, "")
            Log.d(TAG, "=" .repeat(80))
            Log.d(TAG, "LEGEND: ✓ MATCH (>=0.55) | ? CLOSE (0.40-0.55) | ✗ NO (<0.40)")
            Log.d(TAG, "=" .repeat(80))
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }
}
