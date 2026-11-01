package bakery.llm

import com.cheroliv.graphify.apikey.ApiKeyPool
import com.cheroliv.graphify.apikey.Provider
import com.cheroliv.graphify.apikey.RotationStrategy
import com.cheroliv.graphify.apikey.ServiceType
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.TokenUsage
import graphify.apikey.ApiKeyEntry
import graphify.apikey.QuotaConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PooledOllamaLlmServiceAttemptsTest {
    @Test
    fun `first successful endpoint short-circuits the pool`() =
        runBlocking {
            val attempted = mutableListOf<String>()
            val service =
                service(poolSize = 3) { endpoint ->
                    attempted += endpoint
                    if (endpoint.endsWith(":11438")) StubChatModel("first answer") else StubChatModel()
                }

            val result = service.attemptAll(listOf(UserMessage.from("hello")))

            assertEquals(
                "first answer",
                result.fold(ifLeft = { error("expected success, got $it") }, ifRight = { it }),
            )
            assertEquals(listOf("http://localhost:11437", "http://localhost:11438"), attempted)
        }

    @Test
    fun `all endpoints failing reports one failure per attempt`() =
        runBlocking {
            val service = service(poolSize = 3) { StubChatModel() }

            val result = service.attemptAll(listOf(UserMessage.from("hello")))

            val exhausted = result.fold(ifLeft = { it }, ifRight = { error("expected failure, got $it") })
            assertEquals(3, exhausted.attempts)
            assertEquals(3, exhausted.failures.size)
            assertEquals("boom", exhausted.failures.first().cause)
            assertEquals("http://localhost:11437", exhausted.failures.first().endpoint)
        }

    private fun service(
        poolSize: Int,
        modelFor: (String) -> ChatModel,
    ): PooledOllamaLlmService = PooledOllamaLlmService(pool(poolSize), modelFor)

    private fun pool(size: Int): ApiKeyPool =
        ApiKeyPool(
            (0 until size).map { index ->
                val port = 11437 + index
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
            },
            RotationStrategy.ROUND_ROBIN,
        )
}

private class StubChatModel(
    private val response: String? = null,
) : ChatModel {
    override fun chat(messages: MutableList<ChatMessage>?): ChatResponse = respond()

    override fun chat(messages: Array<out ChatMessage>?): ChatResponse = respond()

    private fun respond(): ChatResponse {
        val text = response ?: throw IllegalStateException("boom")
        return ChatResponse
            .builder()
            .aiMessage(AiMessage.from(text))
            .tokenUsage(TokenUsage(0, 0, 0))
            .build()
    }
}
