package bakery.llm

/**
 * Service de complétion LLM partagé — contrat N0 implicite.
 *
 * Ce contrat est inspiré de [codebase.koog.llm.LlmProvider] (N1) mais déclaré ici (N2)
 * pour respecter la règle DAG : un borough N2 ne dépend pas d'un borough N1.
 *
 * Pattern : fun interface + appel asynchrone suspendable ([kotlinx.coroutines]).
 * Les adaptateurs concrets ([OllamaLlmService]) implémentent ce contrat sans que
 * les consommateurs (DSL, tâches Gradle) ne connaissent LangChain4j.
 */
fun interface LlmService {
    suspend fun complete(prompt: String): String
}
