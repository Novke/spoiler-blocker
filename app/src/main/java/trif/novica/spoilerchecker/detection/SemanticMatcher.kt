package trif.novica.spoilerchecker.detection

import trif.novica.spoilerchecker.ml.EmbeddingModel

/**
 * Level 3 detection: Semantic similarity using ONNX model.
 * This is the fallback/last resort when keyword and player matching fail.
 *
 * Uses all-MiniLM-L6-v2 model to compute semantic similarity between
 * game description and notification text.
 */
class SemanticMatcher(private val embeddingModel: EmbeddingModel) {

    companion object {
        const val DEFAULT_THRESHOLD = 0.3f  // Lower threshold since it's a fallback
    }

    /**
     * Check if text is semantically similar to query.
     *
     * @param query Match description (e.g., "Warriors vs Lakers")
     * @param text Notification text
     * @param threshold Minimum similarity score (0.0 to 1.0)
     * @return true if similarity >= threshold
     */
    suspend fun matches(query: String, text: String, threshold: Float = DEFAULT_THRESHOLD): Boolean {
        val score = score(query, text)
        return score >= threshold
    }

    /**
     * Calculate semantic similarity score between query and text.
     *
     * @return Similarity score from -1.0 to 1.0 (cosine similarity)
     */
    suspend fun score(query: String, text: String): Float {
        if (query.isBlank() || text.isBlank()) return 0f
        if (!embeddingModel.isReady()) return 0f

        val queryEmbedding = embeddingModel.getEmbedding(query)
        val textEmbedding = embeddingModel.getEmbedding(text)

        return cosineSimilarity(queryEmbedding, textEmbedding)
    }

    /**
     * Check if the model is ready for inference.
     */
    fun isReady(): Boolean = embeddingModel.isReady()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        // Embeddings are already normalized, so dot product = cosine similarity
        return dotProduct
    }
}
