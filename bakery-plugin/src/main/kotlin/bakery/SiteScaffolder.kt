package bakery

import org.gradle.api.Project
import java.io.File

/**
 * Type de site JBake à scaffolding.
 * - **blog** (défaut) : site/blog classique avec articles, tags, archives
 * - **basic** : site minimal (index, about, contact)
 */
enum class SiteType(val alias: String) {
    BLOG("blog"),
    BASIC("basic");

    companion object {
        /** Résout un nom de type depuis une string, insensible à la casse. */
        fun fromString(value: String): SiteType =
            entries.firstOrNull { it.alias.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Site type inconnu : '$value'. Types supportés : ${entries.joinToString(", ") { it.alias }}"
                )

        /** Résoud un nom de type avec fallback vers BLOG. */
        fun fromStringOrDefault(value: String?): SiteType =
            if (value.isNullOrBlank()) BLOG
            else runCatching { fromString(value) }.getOrDefault(BLOG)
    }
}

object SiteScaffolder {

    fun resolveSiteTargetDir(
        bakeryExtension: BakeryExtension,
        projectDir: File
    ): File {
        val sitesBaseDir = bakeryExtension.sitesBaseDir.orNull
        val siteName = bakeryExtension.siteName.orNull

        return when {
            sitesBaseDir != null && siteName != null ->
                projectDir.resolve(sitesBaseDir).resolve(siteName)
            sitesBaseDir != null && siteName == null ->
                throw IllegalStateException("siteName must be defined when sitesBaseDir is set")
            siteName != null ->
                projectDir.resolve(siteName)
            else ->
                projectDir
        }
    }

    fun validateSiteTargetDoesNotExist(targetDir: File) {
        require(!targetDir.exists()) {
            "Site directory '${targetDir.absolutePath}' already exists"
        }
    }

    fun Project.resolveAndValidateSiteTarget(): File {
        val bakeryExtension = extensions.getByType(BakeryExtension::class.java)
        val targetDir = resolveSiteTargetDir(bakeryExtension, projectDir)

        if (targetDir != projectDir) {
            validateSiteTargetDoesNotExist(targetDir)
        }

        return targetDir
    }

    /** Résoud le type de site depuis l'extension bakery. */
    fun resolveSiteType(bakeryExtension: BakeryExtension): SiteType {
        val rawType = bakeryExtension.siteType.orNull
        return SiteType.fromStringOrDefault(rawType)
    }
}
