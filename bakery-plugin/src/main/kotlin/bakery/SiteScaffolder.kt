package bakery

import org.gradle.api.Project
import java.io.File

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
}
