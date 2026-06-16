package bakery

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bakery.FileSystemManager.isYmlUri
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

object SiteManager {

    fun Project.createJBakeRuntimeConfiguration()
            : Configuration = configurations.create("jbakeRuntime").apply {
        description = "Classpath for running Jbake core directly"
        listOf(
            "org.jbake:jbake-core:2.7.0",
            "commons-configuration:commons-configuration:1.10",
            "org.asciidoctor:asciidoctorj-diagram:3.0.1",
            "org.asciidoctor:asciidoctorj-diagram-plantuml:1.2025.3"
        ).forEach { this@createJBakeRuntimeConfiguration.dependencies.add(name, it) }
    }

    fun Project.configureConfigPath(
        bakeryExtension: BakeryExtension,
        isGradlePropertiesEnabled: Boolean
    ): Either<String, Unit> {
        if (isGradlePropertiesEnabled) return Unit.right()

        val gradlePropertiesFile = layout.projectDirectory.asFile.resolve("gradle.properties")
        if (!gradlePropertiesFile.exists()) {
            val msg = "Nor dsl configuration like 'bakery { configPath = file(\"site.yml\").absolutePath }\n' " +
                    "or gradle properties file found"
            logger.info(msg)
            return msg.left()
        }

        val configPath = properties[BakeryConstants.BAKERY_CONFIG_PATH_KEY]?.toString()
        if (!configPath.isNullOrBlank() && configPath.isYmlUri) {
            bakeryExtension.configPath.set(configPath)
            logger.lifecycle("[bakery] Configuration loaded from gradle.properties: $configPath")
            return Unit.right()
        }

        val msg = "gradle.properties found but bakery.config.path is missing, blank, or not a YML URI"
        logger.info(msg)
        return msg.left()
    }

}
