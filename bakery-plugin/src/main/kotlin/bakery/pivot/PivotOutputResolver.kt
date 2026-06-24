package bakery.pivot

/**
 * Session 161 — Résolution du chemin de sortie YAML pivot.
 *
 * Concept pur (DDD) — sans dépendance Gradle, testable unitairement.
 * Encapsule la convention de nommage par défaut du fichier YAML pivot :
 * `{inputSansExt}.pivot.yaml`. Remplace la logique inline de
 * [PivotTaskRegistrar] (S159) qui calculait le défaut avant création de tâche.
 *
 * Convention cohérente avec les autres tâches bakery : chemin relatif au
 * projet, préservation du répertoire de l'entrée, extension `.pivot.yaml`.
 */
object PivotOutputResolver {

    /**
     * Calcule le chemin de sortie par défaut depuis le chemin d'entrée.
     *
     * Règles :
     * - `{stem}.pivot.yaml` où `stem` = chemin sans la dernière extension.
     * - Si `stem` vide (ex. `.adoc`, `""`) → `pivot.pivot.yaml`.
     * - Préserve le répertoire de l'entrée (`site/content/article.adoc`
     *   → `site/content/article.pivot.yaml`).
     */
    fun resolveDefaultOutput(input: String): String {
        val stem = input.substringBeforeLast('.')
        val safeStem = if (stem.isEmpty()) "pivot" else stem
        return "$safeStem.pivot.yaml"
    }

    /**
     * Résout le chemin de sortie final selon priorité :
     * 1. [explicitOutput] non-blank → valeur explicite (CLI/DSL).
     * 2. sinon → [resolveDefaultOutput] calculé depuis [input].
     */
    fun resolveOutput(input: String, explicitOutput: String?): String {
        return if (!explicitOutput.isNullOrBlank()) explicitOutput
        else resolveDefaultOutput(input)
    }
}