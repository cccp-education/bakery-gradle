package bakery.llm

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
 *
 * XF-FP2-3 — failover is a fold over endpoint attempts: each attempt is an
 * immutable [LlmAttempt] value. An exhausted pool is an [LlmExhausted] error on an
 * [Either] inside the service; the boundary ([complete]) is the only place that
 * throws.
 */
class PooledOllamaLlmService internal constructor(
    private val pool: ApiKeyPool,
    private val modelFor: (String) -> ChatModel,
) : LlmService {
    constructor(
        pool: ApiKeyPool,
        modelName: String,
        timeout: Duration,
    ) : this(pool, { baseUrl -> defaultChatModel(baseUrl, modelName, timeout) })

    override suspend fun complete(prompt: String): String {
        val messages = listOf<ChatMessage>(UserMessage.from(prompt))
        return attemptAll(messages).fold(
            ifLeft = { exhausted -> throw RuntimeException("All endpoints failed", exhausted.toCause()) },
            ifRight = { it },
        )
    }

    /**
     * XF-FP2-3 — one attempt per endpoint; the first success short-circuits the
     * pool. Failures travel as an immutable list: no `var` accumulates the error.
     */
    internal suspend fun attemptAll(messages: List<ChatMessage>): Either<LlmExhausted, String> = attemptFrom(0, messages, emptyList())

    private suspend fun attemptFrom(
        index: Int,
        messages: List<ChatMessage>,
        failures: List<LlmAttempt.Failure>,
    ): Either<LlmExhausted, String> {
        if (index >= pool.size()) {
            return LlmExhausted(failures = failures, attempts = index).left()
        }
        val entry = pool.getNextKey()
        val endpoint = entry.metadata["endpoint"] ?: entry.keyRef
        return when (val outcome = attemptEndpoint(endpoint, messages)) {
            is LlmAttempt.Success -> outcome.text.right()
            is LlmAttempt.Failure -> attemptFrom(index + 1, messages, failures + outcome)
        }
    }

    private suspend fun attemptEndpoint(
        endpoint: String,
        messages: List<ChatMessage>,
    ): LlmAttempt =
        try {
            val response = modelFor(endpoint).chat(messages)
            LlmAttempt.Success(response.aiMessage().text())
        } catch (e: Exception) {
            LlmAttempt.Failure(endpoint = endpoint, cause = e.describe())
        }

    companion object {
        fun create(
            portRange: IntRange,
            modelName: String,
            timeout: Duration = Duration.ofSeconds(120),
            strategy: RotationStrategy = RotationStrategy.ROUND_ROBIN,
        ): PooledOllamaLlmService {
            val entries =
                portRange.map { port ->
                    ApiKeyEntry(
                        id = "ollama-$port",
                        email = "n/a",
                        name = "Ollama port $port",
                        keyRef = "ollama://localhost:$port",
                        provider = Provider.OLLAMA,
                        services = listOf(ServiceType.CHAT_COMPLETION),
                        quota = QuotaConfig(limitValue = 1000),
                        metadata = mapOf("endpoint" to "http://localhost:$port", "weight" to "1.0"),
                    )
                }
            val pool = ApiKeyPool(entries, strategy)
            return PooledOllamaLlmService(pool, modelName, timeout)
        }
    }
}

/** XF-FP2-3 — typed exhaustion rendered as the single boundary exception cause. */
private fun LlmExhausted.toCause(): IllegalStateException =
    IllegalStateException(
        failures.joinToString(separator = "; ") { "${it.endpoint}: ${it.cause}" },
    )

private fun Exception.describe(): String = message ?: javaClass.simpleName

private fun defaultChatModel(
    baseUrl: String,
    modelName: String,
    timeout: Duration,
): ChatModel =
    builder()
        .baseUrl(baseUrl)
        .modelName(modelName)
        .timeout(timeout)
        // CHE-I18N-22 — the pool already fails over across the whole
        // port range; langchain4j's internal retry (3 attempts per
        // endpoint) multiplies the latency on a dead or saturated
        // port. One attempt per endpoint: the pool owns the retry
        // policy (Ink Economy Law applied to wall-clock).
        .maxRetries(0)
        .build()
