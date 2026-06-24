package bakery.pivot

import bakery.BakeryConstants
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Session 159 — Tâche Gradle generatePivotYaml.
 *
 * Transforme un fichier AsciiDoc en YAML pivot (séparation structure/contenu
 * éditorial). Le YAML pivot est consommé par les pipelines i18n et les outils
 * de transformation éditoriale.
 *
 * Pipeline :
 * ```
 * input.adoc → AsciiDocParser → PivotArticle → PivotYamlRenderer → output.yaml
 * ```
 *
 * Usage CLI (Session 161 — options natives `@Option`) :
 * ```
 * ./gradlew generatePivotYaml --input=article.adoc --output=article.pivot.yaml
 * ```
 *
 * Si `--output` est omis, la convention dérive le chemin depuis `--input` :
 * `article.adoc` → `article.pivot.yaml` (voir [PivotOutputResolver]).
 *
 * La tâche est indépendante du pipeline JBake et n'exige pas de site.yml.
 */
@DisableCachingByDefault(because = "Transformation AsciiDoc → YAML pivot — résultat dépendant du fichier d'entrée")
abstract class GeneratePivotYamlTask : DefaultTask {

    constructor() {
        group = BakeryConstants.GENERATE_GROUP
        description = "Transforme un fichier AsciiDoc en YAML pivot (séparation structure/contenu éditorial)"
        inputPath.convention("")
        outputPath.convention("")
    }

    /**
     * Chemin du fichier AsciiDoc source (relatif au projet ou absolu).
     * CLI natif : `--input=article.adoc`.
     */
    @get:Input
    @get:Optional
    @get:Option(option = "input", description = "Chemin du fichier AsciiDoc source à transformer")
    abstract val inputPath: Property<String>

    /**
     * Chemin du fichier YAML pivot destination (relatif au projet ou absolu).
     * CLI natif : `--output=article.pivot.yaml`.
     * Si non fourni, dérive de [inputPath] via [PivotOutputResolver].
     */
    @get:Input
    @get:Optional
    @get:Option(option = "output", description = "Chemin du fichier YAML pivot destination")
    abstract val outputPath: Property<String>

    @TaskAction
    fun generate() {
        val inputRaw = inputPath.get().trim()
        if (inputRaw.isEmpty()) {
            throw GradleException(
                "[generatePivotYaml] Paramètre --input requis. " +
                    "Usage : ./gradlew generatePivotYaml --input=article.adoc [--output=article.pivot.yaml]"
            )
        }

        val input = project.file(inputRaw)
        if (!input.exists()) {
            throw GradleException(
                "[generatePivotYaml] Le fichier d'entrée '$inputRaw' n'existe pas. " +
                    "Résolu vers : ${input.absolutePath}"
            )
        }

        val outputRaw = outputPath.get().trim()
        val resolvedOutput = PivotOutputResolver.resolveOutput(inputRaw, outputRaw)
        val output = project.file(resolvedOutput)

        val adoc = input.readText()
        val article = AsciiDocParser().parse(adoc)
        val yaml = PivotYamlRenderer().render(article)

        output.parentFile?.mkdirs()

        if (output.exists()) {
            val existing = output.readText()
            if (existing == yaml) {
                logger.lifecycle(
                    "[generatePivotYaml] ⏭ ${input.name} → ${output.name} (identique, skip)"
                )
                return
            }
        }

        output.writeText(yaml)

        logger.lifecycle(
            "[generatePivotYaml] ✅ ${input.name} → ${output.name} (${yaml.lines().size} lignes)"
        )
    }
}