package trif.novica.spoilerchecker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import trif.novica.spoilerchecker.data.model.NotificationInfo
import trif.novica.spoilerchecker.ml.EmbeddingModel
import trif.novica.spoilerchecker.ml.SpoilerDetector

/**
 * Real-world test cases from actual device notifications.
 * These tests document expected behavior for common search patterns.
 */
@RunWith(AndroidJUnit4::class)
class RealWorldSpoilerTest {

    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var spoilerDetector: SpoilerDetector

    // Real notifications captured from device
    private val realNotifications = listOf(
        NotificationInfo(
            key = "whatsapp_1",
            packageName = "com.whatsapp",
            title = "Skijanje 21.03",
            text = "2 new messages",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "whatsapp_2",
            packageName = "com.whatsapp",
            title = "Skijanje 21.03 (2 messages)",
            text = "Luka Jovanović: Platio",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "todos_1",
            packageName = "com.microsoft.todos",
            title = "Reminder",
            text = "odaberi predmet za pracenje",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "sofascore_gsw_phi",
            packageName = "com.sofascore.results",
            title = "Golden State Warriors - Philadelphia 76ers",
            text = "VIDEO: Warriors 94 - 113 76ers",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "sofascore_okc_orl",
            packageName = "com.sofascore.results",
            title = "Oklahoma City Thunder - Orlando Magic",
            text = "VIDEO: Thunder 128 - 92 Magic",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "sofascore_dal_bos",
            packageName = "com.sofascore.results",
            title = "Dallas Mavericks - Boston Celtics",
            text = "VIDEO: Mavericks 100 - 110 Celtics",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "sofascore_was_nyk",
            packageName = "com.sofascore.results",
            title = "Washington Wizards - New York Knicks",
            text = "VIDEO: Wizards 101 - 132 Knicks",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "sofascore_bkn_lal",
            packageName = "com.sofascore.results",
            title = "Brooklyn Nets - Los Angeles Lakers",
            text = "Game stats: Luka Dončić",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "youtube_1",
            packageName = "com.google.android.youtube",
            title = "Zašto je Aleksandar Makedonski bio najbolji general ikada?",
            text = "Ozbiljne Teme",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "ytmusic_1",
            packageName = "com.google.android.apps.youtube.music",
            title = "Your Wife (feat. Dr. Dre)",
            text = "Nate Dogg",
            postTime = System.currentTimeMillis()
        )
    )

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        embeddingModel = EmbeddingModel(context)
        embeddingModel.initialize()
        spoilerDetector = SpoilerDetector(embeddingModel)
    }

    // ===== CITY NAME QUERIES =====

    @Test
    fun testQuery_Philadelphia_shouldDetect76ersGame() = runBlocking {
        val query = "Philadelphia"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Philadelphia' should detect 76ers game (sofascore_gsw_phi). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_76ers_shouldDetect76ersGame() = runBlocking {
        val query = "76ers"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query '76ers' should detect 76ers game (sofascore_gsw_phi). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    // ===== ABBREVIATION QUERIES =====

    @Test
    fun testQuery_OKC_shouldDetectThunderGame() = runBlocking {
        val query = "OKC"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'OKC' should detect Thunder game (sofascore_okc_orl). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_okc_orl")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_GSW_shouldDetectWarriorsGame() = runBlocking {
        val query = "GSW"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'GSW' should detect Warriors game (sofascore_gsw_phi). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_LAL_shouldDetectLakersGame() = runBlocking {
        val query = "LAL"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'LAL' should detect Lakers game (sofascore_bkn_lal). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_bkn_lal")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    // ===== FULL TEAM NAME QUERIES =====

    @Test
    fun testQuery_Warriors_shouldDetectWarriorsGame() = runBlocking {
        val query = "Warriors"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Warriors' should detect Warriors game (sofascore_gsw_phi). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Thunder_shouldDetectThunderGame() = runBlocking {
        val query = "Thunder"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Thunder' should detect Thunder game (sofascore_okc_orl). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_okc_orl")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Lakers_shouldDetectLakersGame() = runBlocking {
        val query = "Lakers"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Lakers' should detect Lakers game (sofascore_bkn_lal). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_bkn_lal")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Celtics_shouldDetectCelticsGame() = runBlocking {
        val query = "Celtics"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Celtics' should detect Celtics game (sofascore_dal_bos). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_dal_bos")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Mavericks_shouldDetectMavericksGame() = runBlocking {
        val query = "Mavericks"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Mavericks' should detect Mavericks game (sofascore_dal_bos). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_dal_bos")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    // ===== CITY NAME EDGE CASES =====

    @Test
    fun testQuery_Washington_shouldDetectWizardsGame() = runBlocking {
        val query = "Washington"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Washington' should detect Wizards game (sofascore_was_nyk). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_was_nyk")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Oklahoma_shouldDetectThunderGame() = runBlocking {
        val query = "Oklahoma"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Oklahoma' should detect Thunder game (sofascore_okc_orl). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_okc_orl")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Dallas_shouldDetectMavericksGame() = runBlocking {
        val query = "Dallas"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Dallas' should detect Mavericks game (sofascore_dal_bos). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_dal_bos")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Boston_shouldDetectCelticsGame() = runBlocking {
        val query = "Boston"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Boston' should detect Celtics game (sofascore_dal_bos). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_dal_bos")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_Brooklyn_shouldDetectNetsGame() = runBlocking {
        val query = "Brooklyn"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Brooklyn' should detect Nets game (sofascore_bkn_lal). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_bkn_lal")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_NewYork_shouldDetectKnicksGame() = runBlocking {
        val query = "New York"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'New York' should detect Knicks game (sofascore_was_nyk). " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_was_nyk")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    // ===== FULL MATCH DESCRIPTION QUERIES =====

    @Test
    fun testQuery_WarriorsVs76ers_shouldDetectExactGame() = runBlocking {
        val query = "Warriors vs 76ers"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Warriors vs 76ers' should detect that game. " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_GoldenStateVsPhiladelphia_shouldDetectExactGame() = runBlocking {
        val query = "Golden State vs Philadelphia"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Golden State vs Philadelphia' should detect that game. " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_gsw_phi")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    @Test
    fun testQuery_ThunderVsMagic_shouldDetectExactGame() = runBlocking {
        val query = "Thunder vs Magic"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val keys = spoilers.map { it.notification.key }

        assertTrue(
            "Query 'Thunder vs Magic' should detect that game. " +
            "Scores: ${getScoresDebug(query)}",
            keys.contains("sofascore_okc_orl")
        )
        assertEquals("Should detect exactly 1 notification", 1, spoilers.size)
    }

    // ===== FALSE POSITIVE TESTS =====
    // These queries should NOT match any sports notifications

    @Test
    fun testQuery_Skijanje_noFalsePositives() = runBlocking {
        val query = "Skijanje"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val sportsKeys = spoilers.map { it.notification.key }
            .filter { it.startsWith("sofascore") }

        assertTrue(
            "Query 'Skijanje' should NOT detect any sports notifications. " +
            "False positives: $sportsKeys. Scores: ${getScoresDebug(query)}",
            sportsKeys.isEmpty()
        )
    }

    @Test
    fun testQuery_AleksandarMakedonski_noFalsePositives() = runBlocking {
        val query = "Aleksandar Makedonski"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val sportsKeys = spoilers.map { it.notification.key }
            .filter { it.startsWith("sofascore") }

        assertTrue(
            "Query 'Aleksandar Makedonski' should NOT detect any sports notifications. " +
            "False positives: $sportsKeys. Scores: ${getScoresDebug(query)}",
            sportsKeys.isEmpty()
        )
    }

    @Test
    fun testQuery_DrDre_noFalsePositives() = runBlocking {
        val query = "Dr. Dre"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val sportsKeys = spoilers.map { it.notification.key }
            .filter { it.startsWith("sofascore") }

        assertTrue(
            "Query 'Dr. Dre' should NOT detect any sports notifications. " +
            "False positives: $sportsKeys. Scores: ${getScoresDebug(query)}",
            sportsKeys.isEmpty()
        )
    }

    @Test
    fun testQuery_Meeting_noFalsePositives() = runBlocking {
        val query = "Meeting"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        val sportsKeys = spoilers.map { it.notification.key }
            .filter { it.startsWith("sofascore") }

        assertTrue(
            "Query 'Meeting' should NOT detect any sports notifications. " +
            "False positives: $sportsKeys. Scores: ${getScoresDebug(query)}",
            sportsKeys.isEmpty()
        )
    }

    @Test
    fun testQuery_LukaJovanovic_noFalsePositives() = runBlocking {
        // This is tricky - "Luka" appears in sports (Luka Dončić) but Luka Jovanović is a person in WhatsApp
        val query = "Luka Jovanović"
        val spoilers = spoilerDetector.findSpoilerNotifications(query, realNotifications)

        // Should match WhatsApp, should NOT match sports notifications
        val sportsKeys = spoilers.map { it.notification.key }
            .filter { it.startsWith("sofascore") }

        assertTrue(
            "Query 'Luka Jovanović' should NOT detect sports notifications (different Luka). " +
            "False positives: $sportsKeys. Scores: ${getScoresDebug(query)}",
            sportsKeys.isEmpty()
        )
    }

    // ===== HELPER =====

    private suspend fun getScoresDebug(query: String): String {
        val queryEmbedding = embeddingModel.getEmbedding(query)
        return realNotifications
            .filter { it.fullText.isNotBlank() }
            .map { notification ->
                val embedding = embeddingModel.getEmbedding(notification.fullText)
                val score = cosineSimilarity(queryEmbedding, embedding)
                "${notification.key}=${"%.3f".format(score)}"
            }
            .joinToString(", ")
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }
}
