package bakery

import org.gradle.api.Project

object LensTaskRegistrar {

    internal fun Project.registerCollectSiteContextTask(
        site: SiteConfiguration,
        augmentedContext: bakery.lens.AugmentedContextDsl? = null
    ) {
        tasks.register("collectSiteContext") { task ->
            task.apply {
                group = BakeryConstants.COLLECT_GROUP
                description = "Collecte le contexte du site baké → build/bakery/metadata.json pour runner-gradle N3."
                dependsOn(BakeryConstants.BAKE_TASK)

                doLast {
                    val bakedDir = layout.buildDirectory.get().asFile.resolve(site.bake.destDirPath)
                    val outputDir = layout.buildDirectory.get().asFile.resolve("bakery")

                    logger.lifecycle("[collectSiteContext] Scanning baked dir: {}", bakedDir.absolutePath)

                    if (augmentedContext != null && augmentedContext.enabled) {
                        SiteContextCollector.collectWithAugmentedContext(bakedDir, outputDir, augmentedContext)
                        logger.lifecycle("[collectSiteContext] metadata.json with augmentedEntries written to: {}", outputDir.absolutePath)
                    } else {
                        SiteContextCollector.collect(bakedDir, outputDir)
                        logger.lifecycle("[collectSiteContext] metadata.json written to: {}", outputDir.absolutePath)
                    }
                }
            }
        }
    }

    /**
     * Enregistre la tâche `collectAugmentedContext` — Pattern LENTILLE.
     *
     * Orchestre le pipeline LENS :
     * 1. Charge composite-context.json (AugmentedContextResolver)
     * 2. Extrait le sous-graphe (SubgraphExtractor)
     * 3. Score les nœuds (AugmentedArticlesService)
     * 4. Applique le budget (LensBudget)
     * 5. Écrit le JSON augmenté → build/bakery/augmented-context.json
     *
     * Remplace `registerCollectRelatedArticlesTask` (BKG legacy, supprimé en LENS-3.3).
     */
    internal fun Project.registerCollectAugmentedContextTask(
        site: SiteConfiguration,
        augmentedContextDsl: bakery.lens.AugmentedContextDsl? = null
    ) {
        tasks.register("collectAugmentedContext") { task ->
            task.apply {
                group = BakeryConstants.COLLECT_GROUP
                description = "Collecte le contexte augmenté LENS (ségrégation + enrichissement + budget) → build/bakery/augmented-context.json."
                dependsOn("collectSiteContext")

                doLast {
                    if (augmentedContextDsl == null || !augmentedContextDsl.enabled) {
                        logger.info("[collectAugmentedContext] AugmentedContext désactivé. Skip.")
                        return@doLast
                    }

                    val outputDir = layout.buildDirectory.get().asFile.resolve("bakery")
                    val contextFile = projectDir.resolve(augmentedContextDsl.contextPath)
                    val graphFile = projectDir.resolve(augmentedContextDsl.lens.graphFilePath)

                    val service = bakery.lens.LensPipelineService()
                    val result = service.execute(augmentedContextDsl, contextFile, graphFile, outputDir)

                    logger.lifecycle(
                        "[collectAugmentedContext] {} nœuds scorés → {} après règles → {} après budget → {}",
                        result.totalCandidates, result.totalAfterRules, result.totalAfterBudget,
                        outputDir.resolve("augmented-context.json").absolutePath
                    )
                }
            }
        }
    }
}
