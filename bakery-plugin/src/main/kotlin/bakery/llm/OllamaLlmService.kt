package bakery.llm

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaChatModel.builder
import java.time.Duration
import java.time.Duration.ofSeconds

/**
 * Adaptateur LangChain4j → [LlmService] pour Ollama.
 *
 * Wrapper minimal autour de [dev.langchain4j.model.ollama.OllamaChatModel].
 * Respecte le contrat [LlmService] (fun interface avec coroutine).
 *
 * Usage en production :
 * ```
 * val service = OllamaLlmService.create("http://localhost:11464", "gpt-oss:120b-cloud")
 * val reponse = service.complete("Génère un résumé de 3 phrases.")
 * ```
 *
 * Usage en test :
 * ```
 * val fakeModel = FakeChatModel("réponse déterministe")
 * val service = OllamaLlmService(fakeModel)
 * ```
 */
class OllamaLlmService(private val model: ChatModel) : LlmService {

    override suspend fun complete(prompt: String): String {
        val messages = listOf<ChatMessage>(UserMessage.from(prompt))
        val response = model.chat(messages)
        return response.aiMessage().text()
    }

    companion object {
        /**
         * Factory pour instancier un [OllamaChatModel] réel.
         *
         * @param baseUrl URL du serveur Ollama (ex: "http://localhost:11464")
         * @param modelName Nom du modèle (ex: "gpt-oss:120b-cloud")
         * @param timeout Timeout HTTP (par défaut 120 secondes)
         */
        fun create(
            baseUrl: String,
            modelName: String,
            timeout: Duration = ofSeconds(120)
        ): OllamaLlmService = OllamaLlmService(
            builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .timeout(timeout)
            .build())
    }
}
