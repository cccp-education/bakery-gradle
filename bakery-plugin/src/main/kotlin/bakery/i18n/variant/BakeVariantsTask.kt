package bakery.i18n.variant

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * BKY-LANG-NAV-8 — bakes every deployable `i18n/{lang}` variant into a `{lang}/`
 * sub-directory of the site output.
 *
 * The reference language is baked at the root by the normal `bake` path. This
 * task is the missing brick of a full i18n rollout: a site whose variants live
 * under `i18n/{lang}/` (materialised from a frozen bundle by
 * [bakery.i18n.MaterializeTemplatesTask], or LLM-translated by
 * `migrateContentI18n`) obtains a deployable `{lang}/` tree ready to publish
 * alongside `index.html`.
 *
 * Each variant is baked from a throwaway assembled root
 * ([VariantBakeAssembler] overlays the shared assets and `jbake.properties`) so
 * the variant trees stay content-only. An undeployable variant (missing a
 * reference template) is skipped, never baked: one missing template aborts the
 * whole JBake render (S-049). The output sub-directory is wiped before the bake
 * — `bake` never cleans its output.
 */
@DisableCachingByDefault(because = "Bake de variantes — I/O fichiers + JBake, non-cacheable")
abstract class BakeVariantsTask : DefaultTask() {
    @get:Internal
    var siteDir: File? = null

    /** Target language codes, in bake order (reference language excluded). */
    @get:Input
    abstract val languages: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val referenceLanguage: org.gradle.api.provider.Property<String>

    @get:Internal
    abstract val destDir: DirectoryProperty

    /** Classpath used to run the JBake launcher (pattern `serve`). */
    @get:Internal
    var jbakeRuntime: org.gradle.api.artifacts.Configuration? = null

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = BakeryConstants.TRANSFORM_GROUP
        description =
            "Bake chaque variante i18n/{lang} déployable dans {dest}/{lang}/ (JBake, garde de déployabilité S-049)"
        referenceLanguage.convention("fr")
    }

    @TaskAction
    fun execute() {
        val site =
            siteDir ?: run {
                logger.warn("[bakeVariants] siteDir non configuré — ignoré")
                return
            }
        val referenceDir = site.resolve("templates")
        if (!referenceDir.exists()) {
            logger.warn("[bakeVariants] Aucun répertoire templates dans {}", site.absolutePath)
            return
        }
        val dest = destDir.orNull?.asFile
        if (dest == null) {
            logger.warn("[bakeVariants] destDir non configuré — ignoré")
            return
        }

        val layout = VariantLayout(site)
        val reference = referenceLanguage.get()
        val supported = languages.get()

        val undeployable = VariantDeployability.undeployableVariants(layout, supported, reference)
        undeployable.forEach { (language, missing) ->
            logger.warn(
                "[bakeVariants] [{}] variante non déployable — templates manquants : {} — ignorée",
                language,
                missing.joinToString(", "),
            )
        }
        val plan = VariantBakePlan.languagesToBake(layout, supported, reference)
        if (plan.isEmpty()) {
            logger.lifecycle("[bakeVariants] Aucune variante déployable à baker.")
            return
        }

        val baked =
            VariantBaker(layout) { sourceRoot, outputDir ->
                bakeRoot(sourceRoot, outputDir)
            }.bakeAll(plan, dest)

        logger.lifecycle("[bakeVariants] {} variante(s) bakée(s) : {}", baked.size, baked.joinToString(", "))
    }

    private fun bakeRoot(
        sourceRoot: File,
        outputDir: File,
    ) {
        val runtime =
            jbakeRuntime ?: throw org.gradle.api.GradleException(
                "[bakeVariants] Classpath JBake non configuré.",
            )
        outputDir.mkdirs()
        val result =
            execOperations.javaexec { spec ->
                spec.mainClass.set("org.jbake.launcher.Main")
                spec.classpath = runtime
                spec.environment("GEM_PATH", runtime.asPath)
                spec.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                )
                spec.args =
                    listOf(
                        sourceRoot.absolutePath,
                        outputDir.absolutePath,
                    )
            }
        if (result.exitValue != 0) {
            throw org.gradle.api.GradleException(
                "JBake a échoué pour ${sourceRoot.absolutePath} (exit ${result.exitValue})",
            )
        }
    }
}
