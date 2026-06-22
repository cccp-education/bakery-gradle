package bakery.scaffold

import bakery.FileSystemManager.yamlMapper
import bakery.intention.ResolveIntention
import bakery.intention.ResolveIntentionError
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

/**
 * Tache Gradle de scaffolding de site assiste par IA.
 *
 * Pipeline :
 * ```
 * generateSiteFromIntention → (scaffold IA) → structure de site JBake
 * ```
 *
 * Usage CLI (description seule) :
 * ```
 * ./gradlew generateSiteFromIntention -PscaffoldDescription="Un blog tech sur Kotlin"
 * ```
 *
 * Usage CLI (intention enrichie) :
 * ```
 * ./gradlew generateSiteFromIntention -PscaffoldDescription="Documentation API" -PsiteType=doc -PprojectName=my-docs
 * ```
 *
 * Usage DSL :
 * ```
 * bakery {
 *     scaffoldIntention {
 *         description = "Portfolio professionnel Kotlin"
 *         siteType = "portfolio"
 *         lang = "en"
 *         projectName = "kotlin-dev"
 *     }
 * }
 * ```
 *
 * Injecte le [LlmService] depuis la configuration [bakery.BakeryExtension.ia] :
 * - Production : [OllamaLlmService] (Ollama via langchain4j)
 * - Test : [bakery.llm.FakeLlmService] (deterministe, zero reseau)
 */
@DisableCachingByDefault(because = "Generation IA — resultat non-deterministe, non-cacheable")
abstract class GenerateSiteFromIntentionTask : DefaultTask() {

    /**
     * Service LLM injectable.
     * En production : [OllamaLlmService] configure depuis `bakery { ia { ... } }`.
     * En test : `FakeLlmService`.
     */
    @get:Internal
    var llmService: LlmService? = null

    /**
     * Repertoire cible du site JBake.
     * Resolution : `sitesBaseDir/siteName` ou `projectDir`.
     */
    @get:Internal
    var targetDir: File? = null

    /**
     * Description du site en langage naturel (obligatoire).
     * CLI : `-PscaffoldDescription="Un blog tech sur Kotlin"`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "scaffoldDescription", description = "Description du site a generer")
    abstract val scaffoldDescription: Property<String>

    /**
     * Type de site : blog, portfolio, doc, formation.
     * CLI : `-PsiteType=blog`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "siteType", description = "Type de site (blog/portfolio/doc/formation)")
    abstract val siteType: Property<String>

    /**
     * Langue du site : fr (defaut), en.
     * CLI : `-Plang=en`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "scaffoldLang", description = "Langue du site (fr/en)")
    abstract val scaffoldLang: Property<String>

    /**
     * Nom du projet (slug URL-friendly).
     * CLI : `-PprojectName=my-site`
     */
    @get:Input
    @get:Optional
    @get:Option(option = "projectName", description = "Nom du projet (slug)")
    abstract val projectName: Property<String>

    /**
     * Intention DSL injectee depuis [bakery.BakeryExtension.scaffoldIntention].
     * Priorite : CLI > DSL > defauts.
     */
    @get:Internal
    var dslIntention: ScaffoldIntention? = null

    init {
        group = "generate"
        description = "Genere la structure d'un site assistee par IA — scaffold interactif"
        scaffoldDescription.convention("")
        siteType.convention("")
        scaffoldLang.convention("")
        projectName.convention("")
    }

    @TaskAction
    fun executeGenerate() {
        val resolvedIntention = resolveIntention()

        logger.lifecycle("[generateSiteFromIntention] Scaffolding IA pour : {}", resolvedIntention.description)
        logger.lifecycle("[generateSiteFromIntention] Type : {}, Lang : {}, Projet : {}",
            resolvedIntention.siteType, resolvedIntention.lang, resolvedIntention.projectName)

        // Resoudre le service LLM
        val service = llmService
            ?: throw IllegalStateException(
                "Aucun LlmService injecte. Configurez bakery { ia { ... } } " +
                "ou injectez FakeLlmService en test."
            )

        // Generer la structure
        val generator = ScaffoldGenerator()
        val output = runBlocking {
            generator.generate(resolvedIntention, service)
        }

        logger.lifecycle("[generateSiteFromIntention] Structure generee : {}", output.projectName)
        logger.lifecycle("[generateSiteFromIntention] Type : {}", output.siteType)
        logger.lifecycle("[generateSiteFromIntention] Templates : {}", output.templates.joinToString(", "))
        logger.lifecycle("[generateSiteFromIntention] Metadonnees : titre={}, tags={}",
            output.metadata.title, output.metadata.tags.joinToString(", "))

        // Appliquer la structure au site
        applyScaffoldOutput(output)
    }

    /**
     * Resout la [ScaffoldIntention] finale en fusionnant CLI > DSL > defauts.
     *
     * Priorite de resolution :
     * 1. CLI : `-PscaffoldDescription`, `-PsiteType`, `-PscaffoldLang`, `-PprojectName`
     * 2. DSL : `bakery { scaffoldIntention { ... } }`
     * 3. Defauts : siteType=blog, lang=fr, projectName=""
     */
    internal fun resolveIntention(): ScaffoldIntention {
        val resolvedDescription = ResolveIntention.fromCliRequired(
            scaffoldDescription.orNull,
            dslIntention?.description,
            ResolveIntentionError.MissingRequiredField(
                cliFlag = "-PscaffoldDescription",
                dslPath = "bakery { scaffoldIntention { description = \"...\" } }",
            ),
        ).fold(
            ifLeft = { throw it.toException() },
            ifRight = { it },
        )

        val resolvedSiteType = ResolveIntention.fromCli(
            siteType.orNull,
            dslIntention?.siteType?.name?.lowercase(),
            ScaffoldSiteType.BLOG.name.lowercase(),
        )

        val resolvedLang = ResolveIntention.fromCli(
            scaffoldLang.orNull,
            dslIntention?.lang,
            "fr",
        )

        val resolvedProjectName = ResolveIntention.fromCli(
            projectName.orNull,
            dslIntention?.projectName,
            "",
        )

        return ScaffoldIntention(
            description = resolvedDescription,
            siteType = ScaffoldSiteType.fromStringOrDefault(resolvedSiteType),
            lang = resolvedLang,
            projectName = resolvedProjectName
        )
    }

    /**
     * Applique la structure generee au repertoire cible.
     *
     * Pour l'instant, cette methode ecrit un fichier `site.yml` suggere
     * par l'IA dans le repertoire cible. Le scaffolding mecanique existant
     * (SiteScaffolder) cree la structure de fichiers.
     */
    private fun applyScaffoldOutput(output: ScaffoldOutput) {
        val dir = targetDir
            ?: throw IllegalStateException(
                "Aucun targetDir configure. Le site doit etre initialise d'abord."
            )

        dir.mkdirs()

        // Ecrire le site.yml suggere par l'IA
        val siteYml = buildSiteYml(output)
        val siteYmlFile = dir.resolve("site.yml")
        siteYmlFile.writeText(siteYml, Charsets.UTF_8)

        logger.lifecycle("[generateSiteFromIntention] site.yml cree : {}", siteYmlFile.absolutePath)
    }

    /**
     * Construit le contenu du fichier site.yml suggere par l'IA.
     */
    private fun buildSiteYml(output: ScaffoldOutput): String {
        val yml = mapOf(
            "bake" to mapOf("srcPath" to "site"),
            "site" to mapOf(
                "title" to output.metadata.title,
                "projectName" to output.projectName,
                "description" to output.metadata.description,
                "language" to output.metadata.language,
                "tags" to output.metadata.tags.joinToString(", ")
            )
        )
        val header = buildString {
            appendLine("# Site genere par bakery-gradle (scaffolding IA)")
            appendLine("# Type : ${output.siteType.label}")
            appendLine("# Description : ${output.description}")
            appendLine()
        }
        val templatesComment = if (output.templates.isNotEmpty()) {
            buildString {
                appendLine("# Templates suggerees par l'IA :")
                output.templates.forEach { template ->
                    appendLine("#   - $template")
                }
                appendLine()
            }
        } else ""
        return header + yamlMapper.writeValueAsString(yml) + "\n" + templatesComment
    }
}