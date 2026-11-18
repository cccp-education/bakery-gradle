package bakery.netlify

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes the Netlify platform files (US-7).
 *
 * This is an output, never a source of truth: the direct API deploy ships the
 * baked tree; these files document the site and support the secondary
 * "Netlify builds from Git" mode. Disabled by default (`toml.enabled = false`).
 */
@DisableCachingByDefault(because = "Platform files generation — output-only, trivial")
abstract class GenerateNetlifyTomlTask : DefaultTask() {
    @get:Input
    abstract val platformFilesEnabled: Property<Boolean>

    @get:Input
    abstract val publishDir: Property<String>

    @get:Input
    abstract val notFound: Property<String>

    @get:Input
    abstract val securityHeaders: Property<Boolean>

    @get:Input
    abstract val redirects: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        if (!platformFilesEnabled.get()) {
            logger.lifecycle("[netlify] platform files disabled — nothing written")
            return
        }
        dir.resolve("netlify.toml").writeText(
            NetlifyTomlRenderer.renderToml(
                NetlifyTomlOptions(
                    enabled = true,
                    publishDir = publishDir.get(),
                    notFound = notFound.get(),
                    securityHeaders = securityHeaders.get(),
                    redirects = redirects.get(),
                ),
                fallbackPublishDir = "public",
            ),
        )
        if (securityHeaders.get()) {
            dir.resolve("_headers").writeText(NetlifyTomlRenderer.renderHeaders())
        }
        if (redirects.get()) {
            dir.resolve("_redirects").writeText(NetlifyTomlRenderer.renderRedirects(notFound.get()))
        }
        logger.lifecycle("[netlify] platform files written to ${dir.absolutePath}")
    }
}
