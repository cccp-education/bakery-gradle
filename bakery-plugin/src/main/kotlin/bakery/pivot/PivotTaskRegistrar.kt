package bakery.pivot

import bakery.BakeryConstants
import org.gradle.api.Project

/**
 * Session 159 — Registrar pour la tâche generatePivotYaml.
 *
 * Enregistre la tâche indépendamment du pipeline JBake (pas de site.yml requis).
 * Résolution des paramètres CLI : --input (fichier AsciiDoc source),
 * --output (fichier YAML pivot destination).
 */
object PivotTaskRegistrar {

    internal fun Project.registerGeneratePivotYamlTask() {
        tasks.register("generatePivotYaml", GeneratePivotYamlTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Transforme un fichier AsciiDoc en YAML pivot (séparation structure/contenu éditorial)"

            val inputProp = providers.gradleProperty("input")
                .orElse(providers.gradleProperty("pivotInput"))
            if (inputProp.isPresent) {
                task.inputFile.set(layout.projectDirectory.file(inputProp.get()))
            }

            val outputProp = providers.gradleProperty("output")
                .orElse(providers.gradleProperty("pivotOutput"))
            if (outputProp.isPresent) {
                task.outputFile.set(layout.projectDirectory.file(outputProp.get()))
            } else {
                val inputFileName = inputProp.orElse("input.adoc").get()
                    .substringBeforeLast('.')
                    .let { if (it.isEmpty()) "pivot" else it }
                task.outputFile.set(layout.projectDirectory.file("$inputFileName.pivot.yaml"))
            }
        }
    }
}