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
 * Test to determine optimal semantic similarity threshold.
 *
 * Run with: ./gradlew connectedAndroidTest --tests "*.SemanticThresholdTest"
 */
@RunWith(AndroidJUnit4::class)
class SemanticThresholdTest {

    companion object {
        private const val TAG = "SemanticThresholdTest"
    }

    private lateinit var embeddingModel: EmbeddingModel

    // Test cases: Pair of (query, notification) with expected match (true/false)
    data class TestCase(
        val query: String,
        val notification: String,
        val shouldMatch: Boolean
    )

    private val testCases = listOf(
        // === CLIPPERS vs CAVALIERS ===
        TestCase("Clippers vs Cavaliers", "Los Angeles Clippers - Cleveland Cavaliers VIDEO: Highlights", true),
        TestCase("Clippers vs Cavaliers", "Houston Rockets - Boston Celtics VIDEO: Rockets 93-88", false),
        TestCase("Clippers vs Cavaliers", "San Antonio Spurs - Oklahoma City Thunder 3rd quarter", false),
        TestCase("Clippers vs Cavaliers", "Milwaukee Bucks - New Orleans Pelicans Match finished", false),
        TestCase("Clippers vs Cavaliers", "Toronto Raptors - Minnesota Timberwolves Match finished", false),
        TestCase("Clippers vs Cavaliers", "Andrew Huberman Explains Why Scientists Hung Around", false),
        TestCase("Clippers vs Cavaliers", "MediaOngoingActivity", false),

        // === LAKERS vs WARRIORS ===
        TestCase("Lakers vs Warriors", "Los Angeles Lakers - Golden State Warriors FINAL: Lakers win 112-108", true),
        TestCase("Lakers vs Warriors", "LeBron James scores 35 points in Lakers victory", true),
        TestCase("Lakers vs Warriors", "Stephen Curry with 28 points for Warriors", true),
        TestCase("Lakers vs Warriors", "Miami Heat - Phoenix Suns highlights", false),
        TestCase("Lakers vs Warriors", "Denver Nuggets defeat Dallas Mavericks", false),
        TestCase("Lakers vs Warriors", "NFL: Chiefs vs Eagles Super Bowl preview", false),

        // === CELTICS vs HEAT ===
        TestCase("Celtics vs Heat", "Boston Celtics - Miami Heat Match finished 105-98", true),
        TestCase("Celtics vs Heat", "Jayson Tatum leads Celtics with 32 points", true),
        TestCase("Celtics vs Heat", "Jimmy Butler injury update for Heat", true),
        TestCase("Celtics vs Heat", "New York Knicks - Brooklyn Nets rivalry game", false),
        TestCase("Celtics vs Heat", "Philadelphia 76ers trade rumors", false),

        // === THUNDER vs NUGGETS ===
        TestCase("Thunder vs Nuggets", "Oklahoma City Thunder - Denver Nuggets VIDEO: OKC wins", true),
        TestCase("Thunder vs Nuggets", "Shai Gilgeous-Alexander 41 points Thunder", true),
        TestCase("Thunder vs Nuggets", "Nikola Jokic triple-double Nuggets", true),
        TestCase("Thunder vs Nuggets", "Portland Trail Blazers - Sacramento Kings recap", false),
        TestCase("Thunder vs Nuggets", "Utah Jazz rebuilding season continues", false),

        // === BUCKS vs 76ERS ===
        TestCase("Bucks vs 76ers", "Milwaukee Bucks - Philadelphia 76ers Halftime: Bucks lead", true),
        TestCase("Bucks vs 76ers", "Giannis Antetokounmpo dominates with 38 points", true),
        TestCase("Bucks vs 76ers", "Joel Embiid questionable for Sixers", true),
        TestCase("Bucks vs 76ers", "Indiana Pacers - Charlotte Hornets game delayed", false),
        TestCase("Bucks vs 76ers", "Atlanta Hawks sign new player", false),

        // === SUNS vs MAVERICKS ===
        TestCase("Suns vs Mavericks", "Phoenix Suns - Dallas Mavericks FINAL: Suns 118-115", true),
        TestCase("Suns vs Mavericks", "Kevin Durant with 30 for Phoenix", true),
        TestCase("Suns vs Mavericks", "Luka Doncic triple-double Dallas", true),
        TestCase("Suns vs Mavericks", "Memphis Grizzlies - Houston Rockets preview", false),
        TestCase("Suns vs Mavericks", "Chicago Bulls coaching change", false),

        // === KNICKS vs NETS ===
        TestCase("Knicks vs Nets", "New York Knicks - Brooklyn Nets NYC rivalry game", true),
        TestCase("Knicks vs Nets", "Jalen Brunson 29 points Knicks win", true),
        TestCase("Knicks vs Nets", "Mikal Bridges returns to Brooklyn", true),
        TestCase("Knicks vs Nets", "Detroit Pistons - Washington Wizards tanking", false),
        TestCase("Knicks vs Nets", "Orlando Magic playoff push", false),

        // === RANDOM NON-SPORTS ===
        TestCase("Lakers vs Warriors", "New iPhone 16 announced by Apple", false),
        TestCase("Celtics vs Heat", "Weather forecast: Rain expected tomorrow", false),
        TestCase("Thunder vs Nuggets", "Breaking news: Stock market update", false),
        TestCase("Bucks vs 76ers", "Netflix releases new documentary series", false),
        TestCase("Suns vs Mavericks", "SpaceX launches new satellite", false),
    )

    @Before
    fun setup() {
        // Use targetContext (the app under test), not the test APK context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Delete any corrupted model file to force re-copy from assets
        val modelFile = java.io.File(appContext.filesDir, "model.onnx")
        if (modelFile.exists()) {
            modelFile.delete()
        }

        embeddingModel = EmbeddingModel(appContext)
        runBlocking {
            embeddingModel.initialize()
        }
    }

    @Test
    fun analyzeThresholds() {
        runBlocking {
        Log.d(TAG, "\n" + "=".repeat(80))
        Log.d(TAG, "SEMANTIC SIMILARITY THRESHOLD ANALYSIS")
        Log.d(TAG, "=".repeat(80))

        val results = mutableListOf<Triple<TestCase, Float, Boolean>>()

        for (testCase in testCases) {
            val queryEmbedding = embeddingModel.getEmbedding(testCase.query)
            val notifEmbedding = embeddingModel.getEmbedding(testCase.notification)
            val similarity = cosineSimilarity(queryEmbedding, notifEmbedding)

            results.add(Triple(testCase, similarity, testCase.shouldMatch))

            val matchIcon = if (testCase.shouldMatch) "[SHOULD MATCH]" else "[SHOULD NOT]  "
            Log.d(TAG, String.format("%.3f %s %s -> %s",
                similarity, matchIcon, testCase.query, testCase.notification.take(50)))
        }

        // Analyze by threshold
        Log.d(TAG, "\n" + "=".repeat(80))
        Log.d(TAG, "THRESHOLD ANALYSIS")
        Log.d(TAG, "=".repeat(80))

        val thresholds = listOf(0.25f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.55f, 0.60f, 0.65f, 0.70f)

        for (threshold in thresholds) {
            var truePositives = 0
            var falsePositives = 0
            var trueNegatives = 0
            var falseNegatives = 0

            for ((testCase, similarity, shouldMatch) in results) {
                val predicted = similarity >= threshold

                when {
                    predicted && shouldMatch -> truePositives++
                    predicted && !shouldMatch -> falsePositives++
                    !predicted && !shouldMatch -> trueNegatives++
                    !predicted && shouldMatch -> falseNegatives++
                }
            }

            val precision = if (truePositives + falsePositives > 0)
                truePositives.toFloat() / (truePositives + falsePositives) else 0f
            val recall = if (truePositives + falseNegatives > 0)
                truePositives.toFloat() / (truePositives + falseNegatives) else 0f
            val f1 = if (precision + recall > 0)
                2 * precision * recall / (precision + recall) else 0f
            val accuracy = (truePositives + trueNegatives).toFloat() / results.size

            Log.d(TAG, String.format(
                "Threshold %.2f: TP=%2d FP=%2d TN=%2d FN=%2d | Precision=%.2f Recall=%.2f F1=%.2f Accuracy=%.2f",
                threshold, truePositives, falsePositives, trueNegatives, falseNegatives,
                precision, recall, f1, accuracy
            ))
        }

        // Show score distribution
        Log.d(TAG, "\n" + "=".repeat(80))
        Log.d(TAG, "SCORE DISTRIBUTION")
        Log.d(TAG, "=".repeat(80))

        val shouldMatchScores = results.filter { it.third }.map { it.second }.sorted()
        val shouldNotMatchScores = results.filter { !it.third }.map { it.second }.sorted()

        Log.d(TAG, "SHOULD MATCH scores (${shouldMatchScores.size} cases):")
        Log.d(TAG, "  Min: %.3f, Max: %.3f, Avg: %.3f".format(
            shouldMatchScores.minOrNull() ?: 0f,
            shouldMatchScores.maxOrNull() ?: 0f,
            shouldMatchScores.average().toFloat()
        ))
        Log.d(TAG, "  All: ${shouldMatchScores.map { "%.3f".format(it) }}")

        Log.d(TAG, "\nSHOULD NOT MATCH scores (${shouldNotMatchScores.size} cases):")
        Log.d(TAG, "  Min: %.3f, Max: %.3f, Avg: %.3f".format(
            shouldNotMatchScores.minOrNull() ?: 0f,
            shouldNotMatchScores.maxOrNull() ?: 0f,
            shouldNotMatchScores.average().toFloat()
        ))
        Log.d(TAG, "  All: ${shouldNotMatchScores.map { "%.3f".format(it) }}")

        // Find optimal threshold (maximize F1)
        Log.d(TAG, "\n" + "=".repeat(80))
        Log.d(TAG, "RECOMMENDATION")
        Log.d(TAG, "=".repeat(80))

        val maxShouldNotMatch = shouldNotMatchScores.maxOrNull() ?: 0f
        val minShouldMatch = shouldMatchScores.minOrNull() ?: 1f

        if (maxShouldNotMatch < minShouldMatch) {
            val optimalThreshold = (maxShouldNotMatch + minShouldMatch) / 2
            Log.d(TAG, "Clear separation found!")
            Log.d(TAG, "Optimal threshold: %.3f (between %.3f and %.3f)".format(
                optimalThreshold, maxShouldNotMatch, minShouldMatch))
        } else {
            Log.d(TAG, "WARNING: Score ranges overlap!")
            Log.d(TAG, "Should-match min: %.3f, Should-not-match max: %.3f".format(
                minShouldMatch, maxShouldNotMatch))
            Log.d(TAG, "Consider using keyword matching as primary method.")
        }

        Log.d(TAG, "=".repeat(80))
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
