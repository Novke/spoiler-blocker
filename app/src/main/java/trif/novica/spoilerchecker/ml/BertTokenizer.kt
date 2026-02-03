package trif.novica.spoilerchecker.ml

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class BertTokenizer(context: Context) {
    private val vocab: Map<String, Int>
    private val unkTokenId: Int
    private val clsTokenId: Int
    private val sepTokenId: Int
    private val padTokenId: Int

    companion object {
        const val MAX_SEQ_LENGTH = 128
        private const val UNK_TOKEN = "[UNK]"
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val PAD_TOKEN = "[PAD]"
    }

    init {
        vocab = loadVocabulary(context)
        unkTokenId = vocab[UNK_TOKEN] ?: 100
        clsTokenId = vocab[CLS_TOKEN] ?: 101
        sepTokenId = vocab[SEP_TOKEN] ?: 102
        padTokenId = vocab[PAD_TOKEN] ?: 0
    }

    private fun loadVocabulary(context: Context): Map<String, Int> {
        val vocabMap = mutableMapOf<String, Int>()
        context.assets.open("vocab.txt").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEachIndexed { index, token ->
                    vocabMap[token] = index
                }
            }
        }
        return vocabMap
    }

    fun tokenize(text: String): TokenizedOutput {
        val tokens = mutableListOf<Int>()
        tokens.add(clsTokenId)

        val words = basicTokenize(text)
        for (word in words) {
            val subTokens = wordPieceTokenize(word)
            if (tokens.size + subTokens.size >= MAX_SEQ_LENGTH - 1) {
                val remaining = MAX_SEQ_LENGTH - 1 - tokens.size
                tokens.addAll(subTokens.take(remaining))
                break
            }
            tokens.addAll(subTokens)
        }

        tokens.add(sepTokenId)

        val inputIds = LongArray(MAX_SEQ_LENGTH) { padTokenId.toLong() }
        val attentionMask = LongArray(MAX_SEQ_LENGTH) { 0L }
        val tokenTypeIds = LongArray(MAX_SEQ_LENGTH) { 0L }

        tokens.forEachIndexed { index, tokenId ->
            inputIds[index] = tokenId.toLong()
            attentionMask[index] = 1L
        }

        return TokenizedOutput(inputIds, attentionMask, tokenTypeIds)
    }

    private fun basicTokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    private fun wordPieceTokenize(word: String): List<Int> {
        if (word.isEmpty()) return emptyList()

        val tokens = mutableListOf<Int>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var found = false

            while (start < end) {
                val substr = if (start > 0) "##${word.substring(start, end)}" else word.substring(start, end)
                val tokenId = vocab[substr]

                if (tokenId != null) {
                    tokens.add(tokenId)
                    found = true
                    break
                }
                end--
            }

            if (!found) {
                tokens.add(unkTokenId)
                start++
            } else {
                start = end
            }
        }

        return tokens
    }
}

data class TokenizedOutput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TokenizedOutput
        return inputIds.contentEquals(other.inputIds) &&
                attentionMask.contentEquals(other.attentionMask) &&
                tokenTypeIds.contentEquals(other.tokenTypeIds)
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        result = 31 * result + tokenTypeIds.contentHashCode()
        return result
    }
}
