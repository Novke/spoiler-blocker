package trif.novica.spoilerchecker.ml

import trif.novica.spoilerchecker.data.model.NotificationInfo

class SpoilerDetector(private val embeddingModel: EmbeddingModel) {

    companion object {
        const val DEFAULT_THRESHOLD = 0.5f
    }

    data class ScoredNotification(
        val notification: NotificationInfo,
        val similarityScore: Float
    )

    suspend fun findSpoilerNotifications(
        query: String,
        notifications: List<NotificationInfo>,
        threshold: Float = DEFAULT_THRESHOLD
    ): List<ScoredNotification> {
        android.util.Log.d("SpoilerShield", "findSpoilerNotifications called with query='$query', threshold=$threshold")

        if (query.isBlank() || notifications.isEmpty()) {
            android.util.Log.d("SpoilerShield", "Early return: query blank or no notifications")
            return emptyList()
        }

        if (!embeddingModel.isReady()) {
            android.util.Log.d("SpoilerShield", "Early return: model not ready!")
            return emptyList()
        }

        val queryEmbedding = embeddingModel.getEmbedding(query)
        android.util.Log.d("SpoilerShield", "Query embedding generated, first 5 values: ${queryEmbedding.take(5)}")

        val allScored = notifications
            .filter { it.fullText.isNotBlank() }
            .map { notification ->
                val notificationEmbedding = embeddingModel.getEmbedding(notification.fullText)
                val similarity = cosineSimilarity(queryEmbedding, notificationEmbedding)
                android.util.Log.d("SpoilerShield", "  Score ${"%.3f".format(similarity)} for: '${notification.fullText.take(60)}...'")
                ScoredNotification(notification, similarity)
            }

        val filtered = allScored
            .filter { it.similarityScore >= threshold }
            .sortedByDescending { it.similarityScore }

        android.util.Log.d("SpoilerShield", "Filtered ${allScored.size} -> ${filtered.size} notifications above threshold $threshold")
        return filtered
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        return dotProduct
    }
}
