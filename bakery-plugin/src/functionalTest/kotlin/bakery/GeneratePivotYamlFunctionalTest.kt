package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Session 159 — Tests fonctionnels pour generatePivotYaml.
 *
 * Valide que la tâche Gradle parse un fichier AsciiDoc et produit
 * un fichier YAML pivot (séparation structure/contenu éditorial).
 *
 * La tâche est disponible sans site.yml (mode scaffold only) car elle
 * n'est pas liée au pipeline JBake — c'est un utilitaire de transformation
 * AsciiDoc → YAML pivot.
 */
class GeneratePivotYamlFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `generatePivotYaml produces yaml file from adoc input`() {
        createProject()
        val adocFile = projectDir.resolve("input.adoc")
        adocFile.writeText("""
            title=Test pivot
            date=2026-01-01
            type=page
            status=published
            ~~~~~~

            == Titre principal

            Paragraphe de contenu.
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "-Pinput=input.adoc", "-Poutput=pivot.yaml")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val outputFile = projectDir.resolve("pivot.yaml")
        assertThat(outputFile.exists()).isTrue()
        val yaml = outputFile.readText()
        assertThat(yaml).contains("article:")
        assertThat(yaml).contains("frontmatter:")
        assertThat(yaml).contains("title: \"Test pivot\"")
        assertThat(yaml).contains("type: heading")
        assertThat(yaml).contains("text: \"Titre principal\"")
        assertThat(yaml).contains("type: paragraph")
        assertThat(yaml).contains("Paragraphe de contenu")
    }

    @Test
    fun `generatePivotYaml fails when input file is missing`() {
        createProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "-Pinput=nonexistent.adoc", "-Poutput=pivot.yaml")
            .buildAndFail()

        assertThat(result.output).contains("nonexistent.adoc")
        assertThat(result.output).contains("doesn't exist")
    }

    @Test
    fun `generatePivotYaml task is registered in generate group`() {
        createProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "generate")
            .build()

        assertThat(result.output).contains("generatePivotYaml")
    }

    private fun createProject() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "pivot-yaml-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
        """.trimIndent())
    }
}