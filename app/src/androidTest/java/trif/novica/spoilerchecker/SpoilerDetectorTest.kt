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

@RunWith(AndroidJUnit4::class)
class SpoilerDetectorTest {

    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var spoilerDetector: SpoilerDetector

    // Mock notifications - mix of spoilers and non-spoilers
    private val mockNotifications = listOf(
        // SPOILERS for "Lakers vs Celtics"
        NotificationInfo(
            key = "espn_1",
            packageName = "com.espn.score_center",
            title = "NBA Final Score",
            text = "Lakers defeat Celtics 112-108 in overtime thriller",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "espn_2",
            packageName = "com.espn.score_center",
            title = "LeBron James dominates",
            text = "LeBron scores 42 points as Lakers win against Boston",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "bleacher_1",
            packageName = "com.bleacherreport.android.teamstream",
            title = "Breaking: Lakers victory!",
            text = "Los Angeles Lakers beat the Celtics at home",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "twitter_1",
            packageName = "com.twitter.android",
            title = "@NBAonTNT",
            text = "FINAL: LAL 112 - BOS 108. What a game! #LakeShow",
            postTime = System.currentTimeMillis()
        ),

        // NOT SPOILERS - different sports/topics
        NotificationInfo(
            key = "gmail_1",
            packageName = "com.google.android.gm",
            title = "Meeting reminder",
            text = "Don't forget your 3pm meeting with the team",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "whatsapp_1",
            packageName = "com.whatsapp",
            title = "Mom",
            text = "Are you coming for dinner tonight?",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "news_1",
            packageName = "com.google.android.apps.magazines",
            title = "Weather Update",
            text = "Rain expected tomorrow, bring an umbrella",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "espn_3",
            packageName = "com.espn.score_center",
            title = "NFL Update",
            text = "Chiefs vs Eagles Super Bowl preview",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "youtube_1",
            packageName = "com.google.android.youtube",
            title = "New video from MKBHD",
            text = "iPhone 16 Pro Review: The Real Deal",
            postTime = System.currentTimeMillis()
        ),

        // EDGE CASES - partial matches
        NotificationInfo(
            key = "news_2",
            packageName = "com.cnn.mobile.android.phone",
            title = "Sports Roundup",
            text = "NBA action tonight: Warriors face Heat, Lakers rest",
            postTime = System.currentTimeMillis()
        ),
        NotificationInfo(
            key = "reddit_1",
            packageName = "com.reddit.frontpage",
            title = "r/nba",
            text = "Celtics fans react to devastating loss",
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

    @Test
    fun testDetectLakersCelticsSpoilers() = runBlocking {
        val query = "Lakers vs Celtics"

        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = mockNotifications,
            threshold = 0.5f
        )

        // Should detect the clear spoilers
        val spoilerKeys = spoilers.map { it.notification.key }

        assertTrue("Should detect ESPN final score", spoilerKeys.contains("espn_1"))
        assertTrue("Should detect LeBron game notification", spoilerKeys.contains("espn_2"))
        assertTrue("Should detect Bleacher Report notification", spoilerKeys.contains("bleacher_1"))
        assertTrue("Should detect Twitter NBA notification", spoilerKeys.contains("twitter_1"))

        // Should NOT detect unrelated notifications
        assertFalse("Should NOT detect Gmail", spoilerKeys.contains("gmail_1"))
        assertFalse("Should NOT detect WhatsApp", spoilerKeys.contains("whatsapp_1"))
        assertFalse("Should NOT detect Weather", spoilerKeys.contains("news_1"))
        assertFalse("Should NOT detect YouTube", spoilerKeys.contains("youtube_1"))
        assertFalse("Should NOT detect NFL notification", spoilerKeys.contains("espn_3"))

        // Print results for debugging
        println("=== Spoiler Detection Results for '$query' ===")
        spoilers.forEach { scored ->
            println("  [${scored.similarityScore}] ${scored.notification.title}: ${scored.notification.text}")
        }
    }

    @Test
    fun testDetectDenverNuggetsSpoilers() = runBlocking {
        val nuggetNotifications = listOf(
            // SPOILERS
            NotificationInfo(
                key = "s1",
                packageName = "com.espn",
                title = "Nuggets Win!",
                text = "Denver Nuggets defeat Miami Heat 104-93 to win NBA Finals",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "s2",
                packageName = "com.espn",
                title = "Jokic MVP",
                text = "Nikola Jokic named Finals MVP as Nuggets claim first title",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "s3",
                packageName = "com.twitter",
                title = "@NBA",
                text = "The Denver Nuggets are your 2023 NBA Champions!",
                postTime = System.currentTimeMillis()
            ),

            // NOT SPOILERS
            NotificationInfo(
                key = "n1",
                packageName = "com.weather",
                title = "Denver Weather",
                text = "Sunny skies in Denver today, high of 75°F",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "n2",
                packageName = "com.uber",
                title = "Your ride is here",
                text = "Your UberX is arriving now",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "n3",
                packageName = "com.espn",
                title = "MLB Scores",
                text = "Yankees beat Red Sox 5-3 in extra innings",
                postTime = System.currentTimeMillis()
            )
        )

        val query = "Denver Nuggets"
        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = nuggetNotifications,
            threshold = 0.5f
        )

        val spoilerKeys = spoilers.map { it.notification.key }

        assertTrue("Should detect Nuggets win notification", spoilerKeys.contains("s1"))
        assertTrue("Should detect Jokic MVP notification", spoilerKeys.contains("s2"))
        assertTrue("Should detect NBA Champions notification", spoilerKeys.contains("s3"))

        assertFalse("Should NOT detect Denver weather", spoilerKeys.contains("n1"))
        assertFalse("Should NOT detect Uber notification", spoilerKeys.contains("n2"))
        assertFalse("Should NOT detect MLB notification", spoilerKeys.contains("n3"))

        println("=== Spoiler Detection Results for '$query' ===")
        spoilers.forEach { scored ->
            println("  [${scored.similarityScore}] ${scored.notification.title}: ${scored.notification.text}")
        }
    }

    @Test
    fun testDetectFootballSpoilers() = runBlocking {
        val footballNotifications = listOf(
            // SPOILERS for Real Madrid vs Barcelona
            NotificationInfo(
                key = "f1",
                packageName = "com.espn",
                title = "El Clasico Final",
                text = "Real Madrid wins El Clasico 3-1 against Barcelona",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "f2",
                packageName = "com.onefootball",
                title = "Vinicius Jr hat-trick",
                text = "Vinicius scores 3 as Real Madrid crush Barca at Bernabeu",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "f3",
                packageName = "com.twitter",
                title = "@LaLigaEN",
                text = "FT: Real Madrid 3-1 Barcelona. Hala Madrid!",
                postTime = System.currentTimeMillis()
            ),

            // NOT SPOILERS
            NotificationInfo(
                key = "nf1",
                packageName = "com.espn",
                title = "Premier League",
                text = "Manchester United vs Liverpool kicks off at 8pm",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "nf2",
                packageName = "com.instagram",
                title = "New follower",
                text = "realmadrid_fan_123 started following you",
                postTime = System.currentTimeMillis()
            ),
            NotificationInfo(
                key = "nf3",
                packageName = "com.spotify",
                title = "New Release",
                text = "Bad Bunny just released a new album",
                postTime = System.currentTimeMillis()
            )
        )

        val query = "Real Madrid vs Barcelona"
        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = footballNotifications,
            threshold = 0.5f
        )

        val spoilerKeys = spoilers.map { it.notification.key }

        assertTrue("Should detect El Clasico result", spoilerKeys.contains("f1"))
        assertTrue("Should detect Vinicius hat-trick", spoilerKeys.contains("f2"))
        assertTrue("Should detect LaLiga final score", spoilerKeys.contains("f3"))

        assertFalse("Should NOT detect Premier League", spoilerKeys.contains("nf1"))
        assertFalse("Should NOT detect Instagram", spoilerKeys.contains("nf2"))
        assertFalse("Should NOT detect Spotify", spoilerKeys.contains("nf3"))

        println("=== Spoiler Detection Results for '$query' ===")
        spoilers.forEach { scored ->
            println("  [${scored.similarityScore}] ${scored.notification.title}: ${scored.notification.text}")
        }
    }

    @Test
    fun testEmptyQueryReturnsNoSpoilers() = runBlocking {
        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = "",
            notifications = mockNotifications,
            threshold = 0.5f
        )

        assertTrue("Empty query should return no spoilers", spoilers.isEmpty())
    }

    @Test
    fun testEmptyNotificationsReturnsNoSpoilers() = runBlocking {
        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = "Lakers vs Celtics",
            notifications = emptyList(),
            threshold = 0.5f
        )

        assertTrue("Empty notifications should return no spoilers", spoilers.isEmpty())
    }

    @Test
    fun testThresholdAffectsResults() = runBlocking {
        val query = "Lakers vs Celtics"

        val lowThreshold = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = mockNotifications,
            threshold = 0.3f
        )

        val highThreshold = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = mockNotifications,
            threshold = 0.7f
        )

        assertTrue(
            "Lower threshold should return more or equal results",
            lowThreshold.size >= highThreshold.size
        )

        println("=== Threshold comparison for '$query' ===")
        println("  Low threshold (0.3): ${lowThreshold.size} results")
        println("  High threshold (0.7): ${highThreshold.size} results")
    }

    @Test
    fun testSimilarityScoresAreNormalized() = runBlocking {
        val query = "Lakers vs Celtics"

        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = mockNotifications,
            threshold = 0.0f // Get all scores
        )

        spoilers.forEach { scored ->
            assertTrue(
                "Similarity score should be between 0 and 1, got ${scored.similarityScore}",
                scored.similarityScore in 0.0f..1.0f
            )
        }
    }

    @Test
    fun testPartialTeamNameMatch() = runBlocking {
        // Test with just "Lakers" instead of full "Lakers vs Celtics"
        val query = "Lakers"

        val spoilers = spoilerDetector.findSpoilerNotifications(
            query = query,
            notifications = mockNotifications,
            threshold = 0.5f
        )

        val spoilerKeys = spoilers.map { it.notification.key }

        // Should still detect Lakers-related notifications
        assertTrue("Should detect Lakers notifications with partial query", spoilerKeys.isNotEmpty())

        println("=== Partial match results for '$query' ===")
        spoilers.forEach { scored ->
            println("  [${scored.similarityScore}] ${scored.notification.title}: ${scored.notification.text}")
        }
    }
}
