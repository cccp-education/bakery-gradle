package bakery.lens

import org.gradle.api.Action

/**
 * DSL configuration du contexte augmenté — Pattern LENTILLE.
 *
 * Contient la config `lens` (ségrégation) qui sera enrichie en BKY-LENS-2
 * avec `rag` et `rules`, puis en BKY-LENS-3 avec `budget`.
 *
 * Usage:
 * ```
 * bakery {
 *     augmentedContext {
 *         enabled = true
 *         lens {
 *             scope = LensScope.SUBGRAPH
 *             communities = listOf("bakery-gradle")
 *         }
 *     }
 * }
 * ```
 */
open class AugmentedContextDsl {

    /** Active/désactive le contexte augmenté (Pattern LENTILLE). */
    var enabled: Boolean = false

    /** Chemin vers le fichier composite-context.json (sortie de runner-gradle N3). */
    var contextPath: String = "build/bakery/composite-context.json"

    /** Nombre max d'articles connexes par page (défaut: 4). */
    var maxArticles: Int = 4

    /** Configuration de la lentille (ségrégation du graphe). */
    val lens: LensConfig = LensConfig()

    /** DSL : bakery { augmentedContext { lens { ... } } } */
    fun lens(action: Action<LensConfig>) {
        action.execute(lens)
    }
}