package bakery.site

import bakery.FileSystemManager.yamlMapper
import bakery.lens.AugmentedContextDsl
import bakery.GitService.GIT_ATTRIBUTES_CONTENT
import bakery.ResolvedConfigs
import bakery.SiteConfiguration
import bakery.SiteType
import bakery.escapeJsonForJavaProperties
import bakery.injection.configInjectors
import bakery.injection.updateProperty
import java.io.File
import kotlin.text.Charsets.UTF_8

object GenerateSiteService {

    fun resourcePathForType(siteType: SiteType): String = when (siteType) {
        SiteType.BLOG -> "site"
        SiteType.BASIC -> "site-basic"
    }

    fun defaultSiteDescription(siteType: SiteType): String = when (siteType) {
        SiteType.BLOG -> "Blog JBake avec articles, tags et archives"
        SiteType.BASIC -> "Site statique minimal"
    }

    fun createAndConfigureSiteYml(
        siteYmlFile: File,
        siteType: SiteType
    ) {
        siteYmlFile.createNewFile()
        val resourcePath = resourcePathForType(siteType)
        SiteConfiguration(
            bake = bakery.BakeConfiguration(resourcePath, "bake"),
            pushPage = bakery.GitPushConfiguration(from = resourcePath, to = "cvs"),
            pushMaquette = bakery.GitPushConfiguration(from = "maquette", to = "cvs")
        ).run(yamlMapper::writeValueAsString)
            .run(siteYmlFile::writeText)
    }

    fun setupGitIgnore(targetDir: File): Boolean {
        val gitignore = targetDir.resolve(".gitignore")
        return if (!gitignore.exists()) {
            gitignore.createNewFile()
            gitignore.writeText(
                ".gradle\nbuild\n.kotlin\nsite.yml\n.idea\n" +
                    "*.iml\n*.ipr\n*.iws\nlocal.properties\n"
            )
            true
        } else if (!gitignore.readText(UTF_8).contains("site.yml")) {
            gitignore.appendText("\nsite.yml\n", UTF_8)
            true
        } else false
    }

    fun setupGitAttributes(targetDir: File): Boolean {
        val gitattributes = targetDir.resolve(".gitattributes")
        return if (!gitattributes.exists()) {
            gitattributes.createNewFile()
            gitattributes.writeText(GIT_ATTRIBUTES_CONTENT.trimIndent(), UTF_8)
            true
        } else false
    }

    fun injectConfigIntoJbakeProperties(
        targetDir: File,
        site: SiteConfiguration,
        resolvedConfigs: ResolvedConfigs,
        augmentedContext: AugmentedContextDsl? = null
    ): Boolean {
        val jbakeProps = targetDir.resolve(site.bake.srcPath)
            .resolve("jbake.properties")
        if (!jbakeProps.exists()) return false

        val lines = jbakeProps.readText(UTF_8).lines().toMutableList()

        configInjectors.values.forEach { it.inject(lines, resolvedConfigs.toResolver()) }

        if (augmentedContext != null && augmentedContext.enabled) {
            injectLensBudgetProperties(lines, targetDir, site, augmentedContext)
        }

        jbakeProps.writeText(lines.joinToString("\n"), UTF_8)
        return true
    }

    private fun injectLensBudgetProperties(
        lines: MutableList<String>,
        targetDir: File,
        site: SiteConfiguration,
        augmentedContext: AugmentedContextDsl
    ) {
        updateProperty(lines, "augmentedContextEnabled", augmentedContext.enabled.toString())
        updateProperty(lines, "lensBudgetMaxArticlesPerPage", augmentedContext.budget.maxArticlesPerPage.toString())
        updateProperty(lines, "lensBudgetMinSimilarity", augmentedContext.budget.minSimilarity.toString())

        val augmentedContextFile = targetDir.resolve("build/bakery/augmented-context.json")
        if (augmentedContextFile.exists()) {
            val contextData = escapeJsonForJavaProperties(
                augmentedContextFile.readText(UTF_8)
            )
            updateProperty(lines, "augmentedContextData", contextData)
        } else {
            updateProperty(lines, "augmentedContextData", "")
        }
    }
}