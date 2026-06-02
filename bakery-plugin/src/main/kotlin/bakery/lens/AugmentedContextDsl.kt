package bakery.lens

import org.gradle.api.Action

/**
 * DSL configuration du contexte augmenté — Pattern LENTILLE.
 *
 * 3 couches :
 * 1. SÉGRÉGATION : lens { scope, communities, ... }
 * 2. ENRICHISSEMENT : lens { rules { ... }, rag { ... } }
 * 3. BUDGET : maxArticles, minSimilarity (LENS-3)
 *
 * Usage:
 * ```
 * bakery {
 *     augmentedContext {
 *         enabled = true
 *         lens {
 *             scope = LensScope.SUBGRAPH
 *             communities = listOf("bakery-gradle")
 *             rules {
 *                 excludeDrafts = true
 *                 prioritizeCrossReferences = true
 *             }
 *             rag {
 *                 enabled = true
 *                 similarityThreshold = 0.7
 *                 topK = 20
 *             }
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

    /** Seuil de similarité minimum pour les suggestions (défaut: 0.7). */
    var minSimilarity: Double = 0.7

    /** Configuration de la lentille (ségrégation + enrichissement). */
    val lens: LensConfig = LensConfig()

    /** DSL : bakery { augmentedContext { lens { ... } } } */
    fun lens(action: Action<LensConfig>) {
        action.execute(lens)
    }
}