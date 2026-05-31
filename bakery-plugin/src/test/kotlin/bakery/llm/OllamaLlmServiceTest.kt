package bakery.llm

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.TokenUsage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OllamaLlmServiceTest {

    @Test
    fun `factory creates service with real OllamaChatModel`() {
        val service = OllamaLlmService.create(
            baseUrl = "http://localhost:11465",
            modelName = "gpt-oss:120b-cloud"
        )
        assertNotNull(service)
    }

    @Test
    fun `complete delegates prompt to ChatModel and returns response`() = runBlocking {
        val fakeModel = FakeChatModel("Ceci est la réponse du LLM.")
        val service = OllamaLlmService(fakeModel)

        val result = service.complete("Quelle est la capitale de la France ?")

        assertEquals("Ceci est la réponse du LLM.", result)
        assertEquals(1, fakeModel.messagesReceived.size)
        assertTrue((fakeModel.messagesReceived[0] as UserMessage).singleText().contains("capitale de la France"))
    }

    @Test
    fun `complete handles multiline prompt correctly`() = runBlocking {
        val fakeModel = FakeChatModel("Résumé généré.")
        val service = OllamaLlmService(fakeModel)

        val result = service.complete(
            """
            Résume le texte suivant en 3 phrases :
            
            Le langage Kotlin est un langage de programmation moderne
            qui s'exécute sur la JVM. Il est concis, expressif et
            interopérable avec Java.
            """.trimIndent()
        )

        assertEquals("Résumé généré.", result)
        assertEquals(1, fakeModel.messagesReceived.size)
        assertTrue((fakeModel.messagesReceived[0] as UserMessage).singleText().contains("Kotlin"))
    }

    @Test
    fun `complete preserves empty response from model`() = runBlocking {
        val fakeModel = FakeChatModel("")
        val service = OllamaLlmService(fakeModel)

        val result = service.complete("")

        assertEquals("", result)
    }
}

private class FakeChatModel(private val response: String) : ChatModel {
    val messagesReceived = mutableListOf<ChatMessage>()

    override fun chat(messages: MutableList<ChatMessage>?): ChatResponse {
        messages?.let { messagesReceived.addAll(it) }
        return ChatResponse.builder()
            .aiMessage(AiMessage.from(response))
            .tokenUsage(TokenUsage(0, 0, 0))
            .build()
    }

    override fun chat(messages: Array<out ChatMessage>?): ChatResponse {
        messages?.let { messagesReceived.addAll(it) }
        return ChatResponse.builder()
            .aiMessage(AiMessage.from(response))
            .tokenUsage(TokenUsage(0, 0, 0))
            .build()
    }
}
