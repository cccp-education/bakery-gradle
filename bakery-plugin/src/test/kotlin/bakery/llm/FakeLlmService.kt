package bakery.llm

/**
 * Double de test déterministe pour [LlmService].
 *
 * Retourne une réponse configurée à l'avance, enregistre tous les prompts reçus.
 * Inspiré de [codebase.koog.llm.FakeLlmProvider].
 */
class FakeLlmService(private val response: String) : LlmService {
    val promptsReceived = mutableListOf<String>()

    override suspend fun complete(prompt: String): String {
        promptsReceived.add(prompt)
        return response
    }
}
