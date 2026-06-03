package bakery.lens

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File

/**
 * Service d'orchestration du pipeline LENTILLE (BKY-LENS-1→3).
 *
 * Déplace la logique métier du [doLast] de [SiteManager.registerCollectAugmentedContextTask]
 * dans un service testable isolément. Le [doLast] devient 3 lignes :
 *
 * ```kotlin
 * val service = LensPipelineService()
 * val result = service.execute(config, contextFile, graphFile, outputDir)
 * logger.lifecycle("... {} nœuds scorés ...", result.totalCandidates, ...)
 * ```
 *
 * Pipeline (6 étapes) :
 * 1. Charger composite-context.json ([AugmentedContextResolver])
 * 2. Extraire le sous-graphe ([SubgraphExtractor])
 * 3. RAG results (canal RAG du composite-context — vide en MVP0)
 * 4. Scoring hybride ([AugmentedArticlesService])
 * 5. Filtrage par règles + budget ([LensRules], [LensBudget])
 * 6. Sérialisation JSON ([jacksonObjectMapper])
 *
 * @see LensPipelineOutput Sortie structurée du pipeline
 */
class LensPipelineService {

    private val logger: Logger = Logging.getLogger(LensPipelineService::class.java)

    /**
     * Exécute le pipeline LENTILLE complet.
     *
     * @param augmentedContextDsl Configuration DSL du contexte augmenté
     * @param contextFile Fichier composite-context.json (peut ne pas exister)
     * @param graphFile Fichier graph.json (peut ne pas exister)
     * @param outputDir Répertoire de sortie pour augmented-context.json
     * @return [LensPipelineOutput] avec métriques et nœuds retenus
     */
    fun execute(
        augmentedContextDsl: AugmentedContextDsl,
        contextFile: File,
        graphFile: File,
        outputDir: File
    ): LensPipelineOutput {

        if (!augmentedContextDsl.enabled) {
            logger.info("[LensPipeline] AugmentedContext désactivé. Skip.")
            return LensPipelineOutput(
                budget = BudgetSummary(
                    maxArticlesPerPage = augmentedContextDsl.budget.maxArticlesPerPage,
                    minSimilarity = augmentedContextDsl.budget.minSimilarity
                ),
                scoredNodes = emptyList(),
                totalCandidates = 0,
                totalAfterRules = 0,
                totalAfterBudget = 0
            )
        }

        // 1. Charger composite-context.json
        val resolver = AugmentedContextResolver()
        val compositeContext = if (contextFile.exists()) {
            resolver.resolve(contextFile.absolutePath)
        } else {
            logger.info("[LensPipeline] composite-context.json non trouvé à {}. RAG désactivé.", contextFile.absolutePath)
            null
        }

        if (compositeContext == null && contextFile.exists()) {
            logger.info("[LensPipeline] composite-context.json trouvé mais invalide à {}.", contextFile.absolutePath)
        }

        // 2. Extraire le sous-graphe depuis graph.json
        val extractor = SubgraphExtractor()
        val subgraph = if (graphFile.exists()) {
            val fullGraph = extractor.loadGraph(graphFile.absolutePath)
            extractor.extract(fullGraph, augmentedContextDsl.lens)
        } else {
            logger.info("[LensPipeline] graph.json non trouvé à {}. Sous-graphe vide.", graphFile.absolutePath)
            SiteSubgraph(emptyList(), emptyList(), emptyList())
        }

        // 3. RAG results — le canal RAG du composite-context fournit le contenu textuel,
        //    pas des scores structurés. Les scores RAG viendront de pgvector via codebase-gradle.
        val ragResults: Map<String, Double> = emptyMap()
        if (compositeContext != null) {
            val channels = resolver.extractChannels(compositeContext)
            val ragContent = channels[contracts.context.ChannelType.RAG]
            if (ragContent != null) {
                logger.info("[LensPipeline] Canal RAG disponible ({} caractères). Scoring RAG via pgvector à venir.", ragContent.length)
            }
        }

        // 4. Scoring hybride
        val service = AugmentedArticlesService()
        val allScored = service.scoreAll(
            subgraph = subgraph,
            ragResults = ragResults,
            lensRules = augmentedContextDsl.lens.rules
        )

        // 5. Filtrer par règles + budget
        val filtered = service.applyRules(allScored, augmentedContextDsl.lens.rules)
        val budgeted = augmentedContextDsl.budget.apply(filtered)

        // 6. Construire la sortie
        val output = LensPipelineOutput(
            budget = BudgetSummary(
                maxArticlesPerPage = augmentedContextDsl.budget.maxArticlesPerPage,
                minSimilarity = augmentedContextDsl.budget.minSimilarity
            ),
            scoredNodes = budgeted,
            totalCandidates = allScored.size,
            totalAfterRules = filtered.size,
            totalAfterBudget = budgeted.size
        )

        // 7. Écrire le JSON
        outputDir.mkdirs()
        val mapper = jacksonObjectMapper()
        val jsonMap = mapOf(
            "version" to output.version,
            "pipeline" to output.pipeline,
            "budget" to mapOf(
                "maxArticlesPerPage" to output.budget.maxArticlesPerPage,
                "minSimilarity" to output.budget.minSimilarity
            ),
            "scoredNodes" to output.scoredNodes,
            "totalCandidates" to output.totalCandidates,
            "totalAfterRules" to output.totalAfterRules,
            "totalAfterBudget" to output.totalAfterBudget
        )
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputDir.resolve("augmented-context.json"), jsonMap)

        logger.info(
            "[LensPipeline] {} nœuds scorés → {} après règles → {} après budget → {}",
            output.totalCandidates, output.totalAfterRules, output.totalAfterBudget,
            outputDir.resolve("augmented-context.json").absolutePath
        )

        return output
    }
}
