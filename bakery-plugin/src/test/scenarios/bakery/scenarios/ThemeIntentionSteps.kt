package bakery.scenarios

import io.cucumber.java.en.Given
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Cucumber steps for BKY-IA-2: Theme Intention — Sélection variante + injection thème étendue.
 *
 * Cascade : CLI (-P) > gradle.properties > DSL > site.yml > preset (variante) > defaults
 *
 * Note: Steps `When I am executing the task '...'` and `Then the file ... should contain ...`
 * are shared from [ConfigResolverSteps] and [MinimalSteps].
 */
class ThemeIntentionSteps(private val world: BakeryWorld) {

    @Given("the bakery DSL defines theme.accentColor as {string}")
    fun bakeryDslDefinesThemeAccentColor(accentColor: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(UTF_8)
        val updatedContent = if (content.contains("theme {")) {
            // Add accentColor to existing theme block
            content.replace(
                Regex("(theme\\s*\\{)"),
                "$1\n        accentColor = \"$accentColor\""
            )
        } else {
            // Add a new theme block inside bakery { } — insert before the last closing brace
            val lastBraceIndex = content.lastIndexOf('}')
            if (lastBraceIndex >= 0) {
                content.substring(0, lastBraceIndex) +
                    "\n    theme {\n        accentColor = \"$accentColor\"\n    }\n}"
            } else {
                content
            }
        }
        buildFile.writeText(updatedContent, UTF_8)
    }

    @Given("the bakery DSL defines theme.variant as {string}")
    fun bakeryDslDefinesThemeVariant(variant: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val buildFile = projectDir.resolve("build.gradle.kts")
        val content = buildFile.readText(UTF_8)
        val updatedContent = if (content.contains("theme {")) {
            // Add variant to existing theme block
            content.replace(
                Regex("(theme\\s*\\{)"),
                "$1\n        variant = \"$variant\""
            )
        } else {
            // Add a new theme block inside bakery { } — insert before the last closing brace
            val lastBraceIndex = content.lastIndexOf('}')
            if (lastBraceIndex >= 0) {
                content.substring(0, lastBraceIndex) +
                    "\n    theme {\n        variant = \"$variant\"\n    }\n}"
            } else {
                content
            }
        }
        buildFile.writeText(updatedContent, UTF_8)
    }

    @Given("gradle.properties with bakery.theme.variant = {string}")
    fun gradlePropertiesWithThemeVariant(variant: String) {
        val projectDir = world.projectDir ?: throw IllegalStateException("Project dir not initialized")
        val gradleProps = projectDir.resolve("gradle.properties")
        val existing = if (gradleProps.exists()) gradleProps.readText(UTF_8) else ""
        gradleProps.writeText(existing + "\nbakery.theme.variant=$variant\n", UTF_8)
    }
}