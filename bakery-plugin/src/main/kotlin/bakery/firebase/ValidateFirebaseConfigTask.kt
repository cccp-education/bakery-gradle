package bakery.firebase

import bakery.ConfigResolver
import bakery.FirebaseAuthConfig
import bakery.FirebaseAuthDsl
import bakery.llm.LlmService
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

/**
 * Tâche Gradle de validation Firebase assistée par IA.
 *
 * Pipeline :
 * ```
 * validateFirebaseConfig → validation mécanique + (optionnel) validation LLM → rapport
 * ```
 *
 * Usage CLI (validation mécanique seule) :
 * ```
 * ./gradlew validateFirebaseConfig
 * ```
 *
 * Usage CLI (validation mécanique + IA) :
 * ```
 * ./gradlew validateFirebaseConfig -Pia.enabled=true
 * ```
 *
 * Usage DSL :
 * ```
 * bakery {
 *     ia { enabled = true }
 * }
 * ```
 *
 * La validation mécanique vérifie :
 * - apiKey format (AIzaSy...)
 * - authDomain format (.firebaseapp.com)
 * - projectId non-vide
 * - Cohérence croisée (authDomain contient projectId)
 *
 * La validation LLM (optionnelle) vérifie :
 * - Cohérence sémantique du bloc firebase
 * - Suggestions (newsletter, analytics)
 */
@DisableCachingByDefault(because = "Validation Firebase — résultat dépendant de la config")
abstract class ValidateFirebaseConfigTask : DefaultTask() {

    /** Service LLM injectable (null = validation mécanique seule). */
    @get:Internal
    var llmService: LlmService? = null

    /** Active la validation IA en plus de la validation mécanique. */
    @get:Input
    @get:Optional
    @get:Option(option = "firebaseValidateWithIa", description = "Active la validation IA (true/false)")
    abstract val validateWithIa: Property<String>

    /** Config Firebase Auth résolue (injection interne). */
    @get:Internal
    var resolvedAuthConfig: FirebaseAuthConfig? = null

    /** Config Firebase Contact résolue (injection interne). */
    @get:Internal
    var resolvedContactConfig: bakery.FirebaseContactFormConfig? = null

    init {
        group = "validate"
        description = "Valide la cohérence de la configuration Firebase (mécanique + IA optionnelle)"
        validateWithIa.convention("")
    }

    @TaskAction
    fun validate() {
        logger.lifecycle("[validateFirebaseConfig] Début de la validation Firebase")

        // 1. Validation mécanique (toujours exécutée)
        val mechanicalResult = validateMechanically()

        // 2. Validation IA (optionnelle)
        val iaResult = if (shouldUseIa()) {
            validateWithLlm()
        } else {
            logger.lifecycle("[validateFirebaseConfig] Validation IA désactivée — utilisez -Pia.enabled=true ou dsl ia.enabled=true")
            FirebaseValidationResult()
        }

        // 3. Fusionner les résultats
        val result = mechanicalResult.merge(iaResult)

        // 4. Rapport
        reportResult(result)
    }

    private fun validateMechanically(): FirebaseValidationResult {
        var result = FirebaseValidationResult()

        resolvedAuthConfig?.let { authConfig ->
            val authResult = FirebaseConfigValidator.validateAuthConfig(authConfig)
            result = result.merge(authResult)
            logger.lifecycle("[validateFirebaseConfig] Auth config: {} erreurs, {} avertissements",
                authResult.errors.size, authResult.warnings.size)
        }

        resolvedContactConfig?.let { contactConfig ->
            val contactResult = FirebaseConfigValidator.validateContactConfig(contactConfig)
            result = result.merge(contactResult)
            logger.lifecycle("[validateFirebaseConfig] Contact config: {} erreurs, {} avertissements",
                contactResult.errors.size, contactResult.warnings.size)
        }

        if (resolvedAuthConfig == null && resolvedContactConfig == null) {
            logger.lifecycle("[validateFirebaseConfig] Aucune configuration Firebase trouvée — rien à valider")
        }

        return result
    }

    private fun shouldUseIa(): Boolean {
        val cliValue = validateWithIa.orNull?.takeIf { it.isNotBlank() }
        if (cliValue != null) return cliValue.toBoolean()
        return false
    }

    private fun validateWithLlm(): FirebaseValidationResult {
        val service = llmService
            ?: throw IllegalStateException("Aucun LlmService injecté. Configurez bakery { ia { ... } }.")

        val prompt = buildLlmPrompt()
        val response = runBlocking { service.complete(prompt) }
        return parseLlmResponse(response)
    }

    internal fun buildLlmPrompt(): String = buildString {
        appendLine("Tu es un expert Firebase. Valide la configuration Firebase suivante.")
        appendLine("Vérifie la cohérence entre les champs et suggère des améliorations si nécessaire.")
        appendLine()
        resolvedAuthConfig?.let { auth ->
            appendLine("[Firebase Auth]")
            appendLine("apiKey: ${auth.apiKey}")
            appendLine("authDomain: ${auth.authDomain}")
            appendLine("projectId: ${auth.projectId}")
        }
        resolvedContactConfig?.let { contact ->
            appendLine("[Firebase Contact Form]")
            appendLine("projectId: ${contact.project.projectId}")
            appendLine("apiKey: ${contact.project.apiKey}")
            appendLine("firestore.contacts: ${contact.firestore.contacts.name}")
            appendLine("firestore.messages: ${contact.firestore.messages.name}")
        }
        appendLine()
        appendLine("Réponds au format JSON strict :")
        appendLine("""{"errors": [{"field": "...", "message": "..."}], "warnings": [{"field": "...", "message": "..."}]}""")
        appendLine("Si tout est cohérent, retourne des listes vides.")
    }

    internal fun parseLlmResponse(response: String): FirebaseValidationResult {
        val jsonBlock = response.substringAfter("{").substringBeforeLast("}")
            .let { "{$it}" }
        // Simple parsing — Jackson would be better (CS-3), but this is good enough for structured LLM output
        val errors = extractIssues(jsonBlock, "errors")
        val warnings = extractIssues(jsonBlock, "warnings")
        return FirebaseValidationResult(errors = errors, warnings = warnings)
    }

    private fun extractIssues(json: String, key: String): List<ValidationIssue> {
        val arrayStart = json.indexOf("\"$key\"")
        if (arrayStart == -1) return emptyList()
        val bracketStart = json.indexOf('[', arrayStart)
        if (bracketStart == -1) return emptyList()
        val bracketEnd = json.indexOf(']', bracketStart)
        if (bracketEnd == -1) return emptyList()
        val arrayContent = json.substring(bracketStart + 1, bracketEnd)
        if (arrayContent.isBlank() || arrayContent.trim() == "") return emptyList()

        // Parse each {...} block in the array
        val issues = mutableListOf<ValidationIssue>()
        var pos = 0
        while (pos < arrayContent.length) {
            val objStart = arrayContent.indexOf('{', pos)
            if (objStart == -1) break
            val objEnd = arrayContent.indexOf('}', objStart)
            if (objEnd == -1) break
            val obj = arrayContent.substring(objStart, objEnd + 1)
            val field = extractValue(obj, "field") ?: "unknown"
            val message = extractValue(obj, "message") ?: "unknown"
            issues.add(ValidationIssue(field, message))
            pos = objEnd + 1
        }
        return issues
    }

    private fun extractValue(json: String, key: String): String? {
        val keyPattern = "\"$key\""
        val keyIdx = json.indexOf(keyPattern)
        if (keyIdx == -1) return null
        val colonIdx = json.indexOf(':', keyIdx + keyPattern.length)
        if (colonIdx == -1) return null
        val quoteStart = json.indexOf('"', colonIdx + 1)
        if (quoteStart == -1) return null
        val quoteEnd = json.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return json.substring(quoteStart + 1, quoteEnd)
    }

    private fun reportResult(result: FirebaseValidationResult) {
        if (result.errors.isEmpty() && result.warnings.isEmpty()) {
            logger.lifecycle("[validateFirebaseConfig] ✅ Configuration Firebase valide — aucune erreur ni avertissement")
            return
        }

        if (result.warnings.isNotEmpty()) {
            logger.lifecycle("[validateFirebaseConfig] ⚠️ Avertissements :")
            result.warnings.forEach { w ->
                logger.lifecycle("  - {} : {}", w.field, w.message)
            }
        }

        if (result.errors.isNotEmpty()) {
            logger.lifecycle("[validateFirebaseConfig] ❌ Erreurs :")
            result.errors.forEach { e ->
                logger.lifecycle("  - {} : {}", e.field, e.message)
            }
            throw IllegalStateException(
                "Configuration Firebase invalide : ${result.errors.size} erreur(s). " +
                result.errors.joinToString("; ") { "${it.field}: ${it.message}" }
            )
        }
    }
}
