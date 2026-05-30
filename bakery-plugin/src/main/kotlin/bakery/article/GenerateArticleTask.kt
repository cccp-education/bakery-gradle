package bakery.article

import bakery.llm.LlmService
import bakery.llm.OllamaLlmService
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tâche Gradle de génération d'article assistée IA.
 *
 * Pipeline :
 * ```
 * generateSite → generateArticle → bake → deploySite
 * ```
 *
 * Usage CLI :
 * ```
 * ./gradlew generateArticle -Ptopic="Introduction à Kotlin"
 * ./gradlew generateArticle -Ptopic="Kotlin pour Gradle" -ParticleLang=en
 * ```
 *
 * Injecte le [LlmService] depuis la configuration [BakeryExtension.ia] :
 * - Production : [OllamaLlmService] (Ollama via langchain4j)
 * - Test : [FakeLlmService] (déterministe, zéro réseau)
 */
@DisableCachingByDefault(because = "Génération IA — résultat non-déterministe, non-cacheable")
abstract class GenerateArticleTask : DefaultTask() {

    /**
     * Service LLM injectable.
     * En production : [OllamaLlmService] configuré depuis `bakery { ia { ... } }`.
     * En test : `FakeLlmService`.
     */
    @get:Internal
    var llmService: LlmService? = null

    /**
     * Répertoire racine du contenu du site JBake.
     * Exemple : `site/content/blog/2026/05/`
     * Résolu depuis `bake.srcPath` du site config.
     */
    @get:Internal
    var contentRootDir: File? = null

    /**
     * Sujet de l'article (obligatoire).
     * CLI : `-Ptopic="Introduction à Kotlin"`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "topic", description = "Sujet de l'article à générer")
    abstract val topic: Property<String>

    /**
     * Langue de l'article.
     * Valeurs : "fr" (défaut), "en"
     */
    @get:Input
    @get:Optional
    @get:Option(option = "articleLang", description = "Langue de l'article (fr/en)")
    abstract val articleLang: Property<String>

    init {
        group = "generate"
        description = "Génère un article de blog assisté IA via Ollama — injecte dans content/blog/YYYY/MM/"
        topic.convention("")
        articleLang.convention("fr")
    }

    @TaskAction
    fun executeGenerate() {
        val topicValue = topic.orNull
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "Aucun sujet spécifié. Utilisez -Ptopic=\"Votre sujet\" " +
                "ou configurez bakery { generateArticle { topic = \"...\" } }"
            )

        logger.lifecycle("[generateArticle] Génération article sur : {}", topicValue)

        // Résoudre le service LLM
        val service = llmService
            ?: throw IllegalStateException(
                "Aucun LlmService injecté. Configurez bakery { ia { ... } } " +
                "ou injectez FakeLlmService en test."
            )

        // Résoudre le répertoire de destination
        val targetDir = resolveTargetDir()
        targetDir.mkdirs()

        // Générer l'article
        val generator = ArticleGenerator()
        val article = runBlocking {
            generator.generate(topicValue, service)
        }

        // Écrire le fichier
        val articleFile = targetDir.resolve("${article.slug}.adoc")
        val content = buildArticleContent(article)
        articleFile.writeText(content, Charsets.UTF_8)

        logger.lifecycle("[generateArticle] Article créé : {}", articleFile.absolutePath)
        logger.lifecycle("[generateArticle] Titre : {}", article.titre)
        logger.lifecycle("[generateArticle] Tags : {}", article.tags.joinToString(", "))
        logger.lifecycle("[generateArticle] Slug : {}", article.slug)
    }

    /**
     * Résout le répertoire de destination pour l'article.
     * Chemin : `{contentRootDir}/content/blog/YYYY/MM/`
     */
    private fun resolveTargetDir(): File {
        val root = contentRootDir
            ?: throw IllegalStateException(
                "Aucun contentRootDir configuré. " +
                "Le site doit être initialisé avec generateSite d'abord."
            )

        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        return root.resolve("content/blog/${yearMonth.year}/${yearMonth.monthValue}")
            .also { it.mkdirs() }
    }

    /**
     * Construit le contenu complet du fichier article AsciiDoc.
     */
    private fun buildArticleContent(article: ArticleOutput): String = buildString {
        appendLine("= ${article.titre}")
        appendLine(":description: ${article.description}")
        appendLine(":tags: ${article.tags.joinToString(", ")}")
        appendLine(":date: ${article.date}")
        appendLine(":slug: ${article.slug}")
        appendLine(":page-liquid: blog")
        appendLine()
        appendLine(article.body)
        appendLine()
        appendLine("[NOTE]")
        appendLine("=====")
        appendLine("Article généré par l'assistant IA bakery-gradle.") 
        appendLine("=====")
    }
}
