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
        if (query.isBlank() || notifications.isEmpty()) {
            return emptyList()
        }

        if (!embeddingModel.isReady()) {
            return emptyList()
        }

        val queryEmbedding = embeddingModel.getEmbedding(query)

        return notifications
            .filter { it.fullText.isNotBlank() }
            .map { notification ->
                val notificationEmbedding = embeddingModel.getEmbedding(notification.fullText)
                val similarity = cosineSimilarity(queryEmbedding, notificationEmbedding)
                ScoredNotification(notification, similarity)
            }
            .filter { it.similarityScore >= threshold }
            .sortedByDescending { it.similarityScore }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        return dotProduct
    }
}
