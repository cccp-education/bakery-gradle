package bakery.lens

import org.gradle.api.Action

/**
 * Configuration du Pattern LENTILLE — Ségrégation du Knowledge Graph.
 *
 * Contrôle le filtrage du graphe global (203k nœuds) en un sous-graphe
 * ciblé (~200 nœuds) pour injection dans les pages JBake.
 *
 * 3 couches du Pattern LENTILLE :
 * 1. SÉGRÉGATION (cette config) : filtrer par communautés, types, edges, depth
 * 2. ENRICHISSEMENT (BKY-LENS-2) : RAG pgvector + règles métier
 * 3. BUDGET (BKY-LENS-3) : tronquer à N articles/page, similarité min
 *
 * Override order: CLI (-Pbakery.augmentedContext.lens.xxx) > gradle.properties > DSL > site.yml > defaults
 *
 * Usage:
 * ```
 * bakery {
 *     augmentedContext {
 *         enabled = true
 *         lens {
 *             scope = LensScope.SUBGRAPH
 *             communities = listOf("bakery-gradle", "codebase-gradle")
 *             nodeTypes = listOf("file")
 *             edgeTypes = listOf("reference", "agent_reference")
 *             maxDepth = 2
 *             fileExtensions = listOf("adoc", "md", "html")
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
data class LensConfig(
    /** Scope de la lentille : SUBGRAPH (filtré), FULL (tout le graphe), SEMANTIC_ONLY (RAG seul) */
    var scope: LensScope = LensScope.SUBGRAPH,

    /** Communautés à inclure dans le sous-graphe (ex: ["bakery-gradle", "codebase-gradle"]) */
    var communities: List<String> = emptyList(),

    /** Types de nœuds à inclure (ex: ["file", "module", "class"]) */
    var nodeTypes: List<String> = listOf("file"),

    /** Types d'edges à inclure (ex: ["reference", "agent_reference"]) */
    var edgeTypes: List<String> = listOf("reference", "agent_reference"),

    /** Profondeur maximale de voisinage depuis la page courante (défaut: 2) */
    var maxDepth: Int = 2,

    /** Extensions de fichiers à retenir (ex: ["adoc", "md", "html"]) */
    var fileExtensions: List<String> = listOf("adoc", "md", "html"),

    /** Chemin vers le fichier graph.json (défaut: office/graph.json) */
    var graphFilePath: String = "office/graph.json",

    /** Règles métier éditoriales (BKY-LENS-2). */
    val rules: LensRules = LensRules(),

    /** Configuration RAG pgvector (BKY-LENS-2). */
    val rag: LensRagConfig = LensRagConfig()
) {
    /** DSL : bakery { augmentedContext { lens { rules { ... } } } } */
    fun rules(action: Action<LensRules>) {
        action.execute(rules)
    }

    /** DSL : bakery { augmentedContext { lens { rag { ... } } } } */
    fun rag(action: Action<LensRagConfig>) {
        action.execute(rag)
    }
}

/**
 * Configuration RAG pgvector — BKY-LENS-2.
 *
 * Contrôle les paramètres de similarité sémantique pour l'enrichissement.
 */
data class LensRagConfig(
    /** Active/désactive la recherche RAG (défaut: true). */
    var enabled: Boolean = true,

    /** Seuil de similarité minimale pour les résultats RAG (0.0–1.0, défaut: 0.7). */
    var similarityThreshold: Double = 0.7,

    /** Nombre maximum de résultats RAG à récupérer (top-K, défaut: 20). */
    var topK: Int = 20
)

/**
 * Scope de la lentille.
 * - SUBGRAPH : filtre le graphe par communautés/types/edges/depth
 * - FULL : utilise tout le graphe (pour debug ou petits workspaces)
 * - SEMANTIC_ONLY : ignore le graphe, utilise uniquement le RAG (BKY-LENS-2)
 */
enum class LensScope {
    SUBGRAPH,
    FULL,
    SEMANTIC_ONLY
}