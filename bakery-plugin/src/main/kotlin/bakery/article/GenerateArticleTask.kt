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
 * Usage CLI (topic seul) :
 * ```
 * ./gradlew generateArticle -Ptopic="Introduction à Kotlin"
 * ```
 *
 * Usage CLI (intention enrichie) :
 * ```
 * ./gradlew generateArticle -Ptopic="Kotlin pour Gradle" -ParticleTon=technique -ParticleAudience=developpeur
 * ```
 *
 * Usage DSL :
 * ```
 * bakery {
 *     articleIntention {
 *         topic = "Kotlin Coroutines"
 *         ton = "technique"
 *         audience = "developpeur"
 *         keywords = listOf("suspend", "flow")
 *         lang = "en"
 *     }
 * }
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
     * Registre stylistique : informatif | technique | pédagogique | convaincre.
     * CLI : `-ParticleTon=technique`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "articleTon", description = "Ton de l'article (informatif/technique/pedagogique/convaincre)")
    abstract val articleTon: Property<String>

    /**
     * Public cible : general | developpeur | formateur.
     * CLI : `-ParticleAudience=developpeur`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "articleAudience", description = "Public cible (general/developpeur/formateur)")
    abstract val articleAudience: Property<String>

    /**
     * Mots-clés SEO/RAG (séparés par virgules).
     * CLI : `-ParticleKeywords=kotlin,gradle,dsl`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "articleKeywords", description = "Mots-clés séparés par virgules")
    abstract val articleKeywords: Property<String>

    /**
     * Langue de l'article.
     * Valeurs : "fr" (défaut), "en"
     */
    @get:Input
    @get:Optional
    @get:Option(option = "articleLang", description = "Langue de l'article (fr/en)")
    abstract val articleLang: Property<String>

    /**
     * Intention DSL injectée depuis [bakery.BakeryExtension.articleIntention].
     * Priorité : CLI > DSL > défauts.
     */
    @get:Internal
    var dslIntention: ArticleIntention? = null

    init {
        group = "generate"
        description = "Génère un article de blog assisté IA via Ollama — injecte dans content/blog/YYYY/MM/"
        topic.convention("")
        articleTon.convention("")
        articleAudience.convention("")
        articleKeywords.convention("")
        articleLang.convention("")
    }

    @TaskAction
    fun executeGenerate() {
        val resolvedIntention = resolveIntention()

        logger.lifecycle("[generateArticle] Génération article sur : {}", resolvedIntention.topic)
        logger.lifecycle("[generateArticle] Ton : {}, Audience : {}, Lang : {}",
            resolvedIntention.ton, resolvedIntention.audience, resolvedIntention.lang)

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
            generator.generate(resolvedIntention, service)
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
     * Résout l'[ArticleIntention] finale en fusionnant CLI > DSL > défauts.
     *
     * Priorité de résolution :
     * 1. CLI : `-Ptopic`, `-ParticleTon`, `-ParticleAudience`, `-ParticleKeywords`, `-ParticleLang`
     * 2. DSL : `bakery { articleIntention { ... } }`
     * 3. Défauts : ton=informatif, audience=general, lang=fr
     */
    internal fun resolveIntention(): ArticleIntention {
        // Topic CLI a priorité sur DSL
        val resolvedTopic = topic.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.topic?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "Aucun sujet spécifié. Utilisez -Ptopic=\"Votre sujet\" " +
                "ou configurez bakery { articleIntention { topic = \"...\" } }"
            )

        val resolvedTon = articleTon.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.ton?.name?.lowercase()
            ?: ArticleTon.INFORMATIF.name.lowercase()

        val resolvedAudience = articleAudience.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.audience?.name?.lowercase()
            ?: ArticleAudience.GENERAL.name.lowercase()

        val resolvedKeywords = articleKeywords.orNull?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: dslIntention?.keywords
            ?: emptyList()

        val resolvedLang = articleLang.orNull?.takeIf { it.isNotBlank() }
            ?: dslIntention?.lang
            ?: "fr"

        return ArticleIntention(
            topic = resolvedTopic,
            ton = ArticleTon.entries.firstOrNull { it.name.lowercase() == resolvedTon.lowercase() }
                ?: ArticleTon.INFORMATIF,
            audience = ArticleAudience.entries.firstOrNull { it.name.lowercase() == resolvedAudience.lowercase() }
                ?: ArticleAudience.GENERAL,
            rawKeywords = resolvedKeywords,
            lang = resolvedLang
        )
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
