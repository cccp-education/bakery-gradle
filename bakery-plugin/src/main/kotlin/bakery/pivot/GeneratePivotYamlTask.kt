package bakery.pivot

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

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
 * Usage CLI :
 * ```
 * ./gradlew generatePivotYaml --input=article.adoc --output=article.pivot.yaml
 * ```
 *
 * La tâche est indépendante du pipeline JBake et n'exige pas de site.yml.
 */
@DisableCachingByDefault(because = "Transformation AsciiDoc → YAML pivot — résultat dépendant du fichier d'entrée")
abstract class GeneratePivotYamlTask : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "generate"
        description = "Transforme un fichier AsciiDoc en YAML pivot (séparation structure/contenu éditorial)"
    }

    @TaskAction
    fun generate() {
        val input = inputFile.asFile.get()
        val adoc = input.readText()
        val article = AsciiDocParser().parse(adoc)
        val yaml = PivotYamlRenderer().render(article)

        val output = outputFile.asFile.get()
        output.parentFile?.mkdirs()
        output.writeText(yaml)

        logger.lifecycle("[generatePivotYaml] ✅ ${input.name} → ${output.name} (${yaml.lines().size} lignes)")
    }
}