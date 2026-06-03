package bakery.lens

import org.gradle.api.Action

/**
 * DSL configuration du contexte augmenté — Pattern LENTILLE.
 *
 * 3 couches :
 * 1. SÉGRÉGATION : lens { scope, communities, ... }
 * 2. ENRICHISSEMENT : lens { rules { ... }, rag { ... } }
 * 3. BUDGET : budget { maxArticlesPerPage, minSimilarity } (LENS-3)
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
 *         budget {
 *             maxArticlesPerPage = 4
 *             minSimilarity = 0.7
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

    /** Configuration du budget LENS (maxArticlesPerPage + minSimilarity) — BKY-LENS-3 */
    val budget: LensBudget = LensBudget()

    /** Configuration de la lentille (ségrégation + enrichissement). */
    val lens: LensConfig = LensConfig()

    /** Nombre maximum d'augmented entries à injecter dans metadata.json (BKY-LENS-5).
     *  Défaut = budget.maxArticlesPerPage. 0 = pas de limite.
     */
    var maxArticles: Int = budget.maxArticlesPerPage

    /** DSL : bakery { augmentedContext { lens { ... } } } */
    fun lens(action: Action<LensConfig>) {
        action.execute(lens)
    }

    /** DSL : bakery { augmentedContext { budget { ... } } } — BKY-LENS-3 */
    fun budget(action: Action<LensBudget>) {
        action.execute(budget)
    }
}