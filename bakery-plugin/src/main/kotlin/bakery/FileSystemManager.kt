package bakery

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bakery.RepositoryConfiguration.Companion.CNAME
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gradle.api.Project
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.jar.JarFile
import kotlin.text.Charsets.UTF_8

typealias FsError = String
typealias FsResult = Either<FsError, Unit>

object FileSystemManager {
    val String.isYmlUri: Boolean
        get() = runCatching {
            val uri = URI(this)
            uri.path?.endsWith(".yml", ignoreCase = true) ?: false
        }.getOrDefault(false)

    fun copyResourceDirectory(resourcePath: String, targetDir: File, project: Project): FsResult {
        val resource = BakeryPlugin::class.java.classLoader.getResource(resourcePath)

        project.logger.info("Attempting to copy resource: $resourcePath")
        project.logger.info("Resource URL: $resource")

        if (resource == null) {
            val errorMsg = "Resource directory not found: $resourcePath"
            project.logger.error(errorMsg)
            return errorMsg.left()
        }

        return copyFromResourceUrl(resourcePath, targetDir, project, resource)
    }

    internal fun copyFromResourceUrl(resourcePath: String, targetDir: File, project: Project, resource: java.net.URL): FsResult {
        return when (resource.protocol) {
            "jar" -> copyFromJarUrl(resourcePath, targetDir, project, resource)
            "file" -> copyFromFileSystemUrl(resourcePath, targetDir, project, resource)
            else -> {
                val errorMsg = "Unsupported resource protocol: ${resource.protocol}"
                project.logger.error(errorMsg)
                errorMsg.left()
            }
        }
    }

    internal fun copyFromJarUrl(
        resourcePath: String,
        targetDir: File,
        project: Project,
        jarUrl: java.net.URL
    ): FsResult = try {
        JarFile(File(jarUrl.toURI())).use { jar ->
            val normalizedPath = resourcePath.removeSuffix("/") + "/"
            val destDir = targetDir.resolve(resourcePath)
            var copiedCount = 0

            jar.entries()
                .asSequence()
                .filter { entry ->
                    entry.name.startsWith(normalizedPath) &&
                            !entry.isDirectory &&
                            entry.name != normalizedPath
                }.forEach { entry ->
                    val relativePath = entry.name.removePrefix(normalizedPath)
                    val targetFile = destDir.resolve(relativePath)

                    @Suppress("LoggingSimilarMessage")
                    project.logger.info("Copying: ${entry.name} -> ${targetFile.absolutePath}")

                    targetFile.parentFile.mkdirs()

                    jar.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    copiedCount++
                }
            project.logger.lifecycle("✓ Copied $copiedCount files from $resourcePath to ${destDir.absolutePath}")
        }
        Unit.right()
    } catch (e: Exception) {
        project.logger.error("Error copying from JAR: ${e.message}", e)
        (e.message ?: "Unknown error copying from JAR").left()
    }

    internal fun copyFromFileSystemUrl(
        resourcePath: String,
        targetDir: File,
        project: Project,
        resource: java.net.URL
    ): FsResult = try {
        val sourceDir = File(resource.toURI())
        val destDir = targetDir.resolve(resourcePath)

        project.logger.info("Source: ${sourceDir.absolutePath}")
        project.logger.info("Destination: ${destDir.absolutePath}")

        if (!sourceDir.exists())
            return "Source directory does not exist: ${sourceDir.absolutePath}".left()
        if (!sourceDir.isDirectory)
            return "Source is not a directory: ${sourceDir.absolutePath}".left()
        destDir.parentFile.mkdirs()
        val copiedCount = sourceDir
            .walkTopDown()
            .filter { it.isFile }
            .count { sourceFile ->
                val relativePath = sourceFile.relativeTo(sourceDir).path
                val targetFile = destDir.resolve(relativePath)
                project.logger.info("Copying: ${sourceFile.absolutePath} -> ${targetFile.absolutePath}")
                targetFile.parentFile.mkdirs()
                sourceFile.copyTo(targetFile, overwrite = true)
                true
            }
        project.logger.lifecycle("✓ Copied $copiedCount files from $resourcePath to ${destDir.absolutePath}")
        Unit.right()
    } catch (e: Exception) {
        project.logger.error("Error copying from file system: ${e.message}", e)
        (e.message ?: "Unknown error copying from file system").left()
    }

    // Publishing logic
    fun createRepoDir(path: String, logger: Logger): File = path.let(::File).apply {
        if (exists() && !isDirectory)
            if (delete()) logger.info("$name exists as file and successfully deleted.")
            else throw "$name exists and must be a directory".run(::IOException)

        if (exists())
            if (deleteRecursively()) logger.info("$name exists as directory and successfully deleted.")
            else throw "$name exists as a directory and cannot be deleted".run(::IOException)

        if (!exists()) logger.info("$name does not exist.")
        else throw IOException("$name must not exist anymore.")

        if (!exists()) {
            if (mkdirs()) logger.info("$name as directory successfully created.")
            else throw IOException("$name as directory cannot be created.")
        }
    }

    fun copyBakedFilesToRepo(
        bakeDirPath: String, repoDir: File, logger: Logger
    ): FsResult = try {
        bakeDirPath.also { "bakeDirPath : $it".let(logger::info) }.let(::File).apply {
            copyRecursively(repoDir, true)
            deleteRecursively()
        }.run {
            if (!exists()) logger.info("$name directory successfully deleted.")
            else throw IOException("$name must not exist.")
        }
        Unit.right()
    } catch (e: Exception) {
        (e.message ?: "An error occurred during file copy.").left()
    }

    val yamlMapper: ObjectMapper by lazy {
        YAMLFactory()
            .let(::ObjectMapper)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .registerKotlinModule()
    }

    fun SiteConfiguration.createCnameFile(project: Project) {
        project.layout.buildDirectory.get()
            .asFile
            .resolve(bake.destDirPath)
            .resolve(CNAME).run {
                if (exists()) delete()
                if (bake.cname.isNotBlank()) {
                    apply(File::createNewFile)
                        .writeText(bake.cname, UTF_8)
                }
            }
    }


    fun Project.from(configPath: String): SiteConfiguration = read(file(configPath))

    fun Project.read(configFile: File): SiteConfiguration = try {
        yamlMapper.readValue(configFile)
    } catch (e: Exception) {
        logger.error("Failed to read site configuration from ${configFile.absolutePath}", e)
        // Return a default/empty configuration to avoid build failure
        SiteConfiguration()
    }
}