package bakery.pivot

import bakery.BakeryConstants
import org.gradle.api.Project

/**
 * Session 159 — Registrar pour la tâche generatePivotYaml.
 *
 * Enregistre la tâche indépendamment du pipeline JBake (pas de site.yml requis).
 *
 * Session 161 — Les paramètres CLI `--input`/`--output` sont désormais des
 * options natives (`@Option`) sur [GeneratePivotYamlTask]. La résolution
 * manuelle via `providers.gradleProperty` est supprimée : Gradle peuple
 * directement les `Property<String>` depuis la CLI `--input=...`/`--output=...`.
 *
 * La convention du chemin de sortie (défaut `{stem}.pivot.yaml`) est gérée par
 * [PivotOutputResolver] dans la tâche elle-même, au moment de l'exécution.
 */
object PivotTaskRegistrar {

    internal fun Project.registerGeneratePivotYamlTask() {
        tasks.register("generatePivotYaml", GeneratePivotYamlTask::class.java) { task ->
            task.group = BakeryConstants.GENERATE_GROUP
            task.description = "Transforme un fichier AsciiDoc en YAML pivot (séparation structure/contenu éditorial)"
        }
    }
}