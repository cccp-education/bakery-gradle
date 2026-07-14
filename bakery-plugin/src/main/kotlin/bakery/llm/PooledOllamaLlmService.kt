package bakery.llm

import com.cheroliv.graphify.apikey.ApiKeyPool
import com.cheroliv.graphify.apikey.Provider
import com.cheroliv.graphify.apikey.RotationStrategy
import com.cheroliv.graphify.apikey.ServiceType
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaChatModel.builder
import graphify.apikey.ApiKeyEntry
import graphify.apikey.QuotaConfig
import java.time.Duration

/**
 * [LlmService] that rotates across multiple Ollama endpoints using [ApiKeyPool].
 *
 * Each call to [complete] picks the next endpoint via round-robin (or any
 * [RotationStrategy]) and builds a fresh [OllamaChatModel] for that port.
 *
 * Usage:
 * ```
 * val service = PooledOllamaLlmService.create(11437..11465, "gemma4:31b-cloud")
 * val response = service.complete("Translate this text.")
 * ```
 */
class PooledOllamaLlmService(
    private val pool: ApiKeyPool,
    private val modelName: String,
    private val timeout: Duration
) : LlmService {

    override suspend fun complete(prompt: String): String {
        val messages = listOf<ChatMessage>(UserMessage.from(prompt))
        var lastError: Exception? = null
        for (attempt in 0 until pool.size()) {
            val entry = pool.getNextKey()
            val baseUrl = entry.metadata["endpoint"] ?: entry.keyRef
            try {
                val model: ChatModel = builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .timeout(timeout)
                    .build()
                val response = model.chat(messages)
                return response.aiMessage().text()
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: RuntimeException("All endpoints failed")
    }

    companion object {
        fun create(
            portRange: IntRange,
            modelName: String,
            timeout: Duration = Duration.ofSeconds(120),
            strategy: RotationStrategy = RotationStrategy.ROUND_ROBIN
        ): PooledOllamaLlmService {
            val entries = portRange.map { port ->
                ApiKeyEntry(
                    id = "ollama-$port",
                    email = "n/a",
                    name = "Ollama port $port",
                    keyRef = "ollama://localhost:$port",
                    provider = Provider.OLLAMA,
                    services = listOf(ServiceType.CHAT_COMPLETION),
                    quota = QuotaConfig(limitValue = 1000),
                    metadata = mapOf("endpoint" to "http://localhost:$port", "weight" to "1.0")
                )
            }
            val pool = ApiKeyPool(entries, strategy)
            return PooledOllamaLlmService(pool, modelName, timeout)
        }
    }
}