package bakery

import bakery.FileSystemManager.yamlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * US-61a — Tâche de vérification du mapping YAML ↔ Gradle properties sécurisé.
 *
 * Valide que `site.yml` se parse en `SiteConfiguration`, masque les secrets
 * dans les logs, et rapporte les erreurs de parsing ou de champs manquants.
 *
 * Pipeline :
 * ```
 * verifyConfigurationMapping → parse YAML → validate champs requis → log masqué
 * ```
 *
 * Usage CLI :
 * ```
 * ./gradlew verifyConfigurationMapping
 * ```
 */
@DisableCachingByDefault(because = "Validation YAML — résultat dépendant du fichier de configuration")
abstract class VerifyConfigurationMappingTask : DefaultTask() {

    /** Chemin vers le fichier de configuration YAML (input requis). */
    @get:Input
    abstract val configPath: Property<String>

    init {
        group = "verification"
        description = "Valide le mapping YAML site.yml → SiteConfiguration et masque les secrets"
    }

    @TaskAction
    fun verify() {
        val path = configPath.get()
        val configFile = File(path)

        if (!configFile.exists()) {
            throw GradleException("Configuration file not found: $path")
        }

        val site = try {
            yamlMapper.readValue<SiteConfiguration>(configFile)
        } catch (e: Exception) {
            throw GradleException(
                "Failed to parse configuration file '$path': ${e.message}",
                e
            )
        }

        validateRequiredFields(site)

        val maskedSummary = buildMaskedSummary(site)
        logger.lifecycle("[verifyConfigurationMapping] ✅ Configuration OK: $maskedSummary")
    }

    private fun validateRequiredFields(site: SiteConfiguration) {
        val missing = mutableListOf<String>()

        if (site.bake.srcPath.isBlank()) missing.add("bake.srcPath")
        if (site.bake.destDirPath.isBlank()) missing.add("bake.destDirPath")
        if (site.pushPage.from.isBlank()) missing.add("pushPage.from")
        if (site.pushPage.to.isBlank()) missing.add("pushPage.to")
        if (site.pushMaquette.from.isBlank()) missing.add("pushMaquette.from")
        if (site.pushMaquette.to.isBlank()) missing.add("pushMaquette.to")

        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing required configuration fields: ${missing.joinToString(", ")}"
            )
        }
    }

    internal fun buildMaskedSummary(site: SiteConfiguration): String {
        val srcPath = site.bake.srcPath
        val destDirPath = site.bake.destDirPath
        val cname = site.bake.cname
        val repo = site.pushPage.repo.name
        val branch = site.pushPage.branch

        val credentialsPassword = mask(site.pushPage.repo.credentials.password)
        val pushMaquettePassword = mask(site.pushMaquette.repo.credentials.password)
        val firebaseApiKey = mask(site.firebase?.project?.apiKey ?: "")

        return buildString {
            append("srcPath=$srcPath, destDirPath=$destDirPath")
            if (cname.isNotBlank()) append(", cname=$cname")
            append(", repo=$repo, branch=$branch")
            append(", credentials.password=$credentialsPassword")
            append(", pushMaquette.password=$pushMaquettePassword")
            if (firebaseApiKey.isNotBlank()) append(", firebase.apiKey=$firebaseApiKey")
        }
    }

    internal fun mask(value: String): String =
        if (value.isBlank()) "(not set)" else "***"
}
