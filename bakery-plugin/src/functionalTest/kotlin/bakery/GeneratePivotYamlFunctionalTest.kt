package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Session 159 — Tests fonctionnels pour generatePivotYaml.
 * Session 161 — Migration CLI `-Pinput`/`-Poutput` → options natives
 * `--input`/`--output` (annotation `@Option`).
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
    fun `generatePivotYaml produces yaml file from adoc input via --input option`() {
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
            .withArguments("generatePivotYaml", "--input=input.adoc", "--output=pivot.yaml")
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
    fun `generatePivotYaml derives default output path from --input when --output omitted`() {
        createProject()
        val adocFile = projectDir.resolve("article.adoc")
        adocFile.writeText("""
            title=Convention output
            date=2026-06-24
            type=page
            status=published
            ~~~~~~

            == Section

            Contenu.
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "--input=article.adoc")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        // Convention PivotOutputResolver : {stem}.pivot.yaml
        val defaultOutput = projectDir.resolve("article.pivot.yaml")
        assertThat(defaultOutput.exists()).isTrue()
        assertThat(defaultOutput.readText()).contains("article:")
    }

    @Test
    fun `generatePivotYaml fails when input file does not exist`() {
        createProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "--input=nonexistent.adoc", "--output=pivot.yaml")
            .buildAndFail()

        assertThat(result.output).contains("nonexistent.adoc")
        assertThat(result.output).contains("n'existe pas")
    }

    @Test
    fun `generatePivotYaml fails when --input option is missing`() {
        createProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml")
            .buildAndFail()

        assertThat(result.output).contains("--input")
        assertThat(result.output).contains("requis")
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

    @Test
    fun `generatePivotYaml does not rewrite identical output on second invocation`() {
        createProject()
        val adocFile = projectDir.resolve("article.adoc")
        adocFile.writeText("""
            title=Idempotence test
            date=2026-06-24
            type=page
            status=published
            ~~~~~~

            == Section unique

            Contenu stable.
        """.trimIndent())

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "--input=article.adoc", "--output=pivot.yaml")
            .build()

        val outputFile = projectDir.resolve("pivot.yaml")
        assertThat(outputFile.exists()).isTrue()
        val firstContent = outputFile.readText()
        assertThat(firstContent).contains("Section unique")

        val result2 = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generatePivotYaml", "--input=article.adoc", "--output=pivot.yaml")
            .build()

        assertThat(result2.output).contains("BUILD SUCCESSFUL")
        val secondContent = outputFile.readText()
        assertThat(secondContent).isEqualTo(firstContent)
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