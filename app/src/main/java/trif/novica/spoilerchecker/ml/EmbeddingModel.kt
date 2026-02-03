package trif.novica.spoilerchecker.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import kotlin.math.sqrt

class EmbeddingModel(private val context: Context) {
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: BertTokenizer? = null
    private var isInitialized = false
    private val mutex = Mutex()

    companion object {
        private const val MODEL_FILE = "model.onnx"
        private const val EMBEDDING_SIZE = 384
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isInitialized) return@withContext

            ortEnvironment = OrtEnvironment.getEnvironment()

            val modelBytes = context.assets.open(MODEL_FILE).use { it.readBytes() }

            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(2)
            }

            ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)
            tokenizer = BertTokenizer(context)
            isInitialized = true
        }
    }

    fun isReady(): Boolean = isInitialized

    suspend fun getEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        check(isInitialized) { "EmbeddingModel not initialized. Call initialize() first." }

        val tokenized = tokenizer!!.tokenize(text)

        val env = ortEnvironment!!
        val session = ortSession!!

        val shape = longArrayOf(1, BertTokenizer.MAX_SEQ_LENGTH.toLong())

        val inputIdsTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokenized.inputIds),
            shape
        )
        val attentionMaskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokenized.attentionMask),
            shape
        )
        val tokenTypeIdsTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokenized.tokenTypeIds),
            shape
        )

        try {
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            val results = session.run(inputs)

            @Suppress("UNCHECKED_CAST")
            val lastHiddenState = results[0].value as Array<Array<FloatArray>>

            val embedding = meanPooling(lastHiddenState[0], tokenized.attentionMask)
            normalize(embedding)

            embedding
        } finally {
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
        }
    }

    private fun meanPooling(hiddenStates: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val embedding = FloatArray(EMBEDDING_SIZE)
        var validTokens = 0f

        for (i in hiddenStates.indices) {
            if (attentionMask[i] == 1L) {
                for (j in 0 until EMBEDDING_SIZE) {
                    embedding[j] += hiddenStates[i][j]
                }
                validTokens++
            }
        }

        if (validTokens > 0) {
            for (j in 0 until EMBEDDING_SIZE) {
                embedding[j] /= validTokens
            }
        }

        return embedding
    }

    private fun normalize(embedding: FloatArray) {
        var norm = 0f
        for (value in embedding) {
            norm += value * value
        }
        norm = sqrt(norm)

        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }

    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
        ortSession = null
        ortEnvironment = null
        tokenizer = null
        isInitialized = false
    }
}
