package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CHE-I18N-22 US-4 — the CLI can drive the content-migration parallelism.
 *
 * The pilot decision bounds the concurrency to two Ollama providers (two
 * parallel device keys), never three articles at once. Before this US the
 * option was absent: the CLI could not reach the `parallelism` field.
 */
class ContentI18nParallelismFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the CLI accepts a parallelism of two`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "migrateContentI18n",
                    "--contentI18nSource=content/blog",
                    "--contentI18nOutput=content-i18n",
                    "--contentI18nSourceLang=fr",
                    "--contentI18nTargetLangs=en",
                    "--contentI18nParallelism=2",
                    "--contentI18nDryRun=true",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `the CLI rejects a parallelism above two`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "migrateContentI18n",
                    "--contentI18nSource=content/blog",
                    "--contentI18nOutput=content-i18n",
                    "--contentI18nSourceLang=fr",
                    "--contentI18nTargetLangs=en",
                    "--contentI18nParallelism=3",
                    "--contentI18nDryRun=true",
                ).buildAndFail()

        assertThat(result.output).contains("Parallélisme")
    }

    private fun createSite() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "cheroliv-parallelism"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: "jbake"
              destDirPath: "bake"
              cname: "cheroliv.com"
            """.trimIndent(),
        )

        projectDir.resolve("jbake/content/blog/2026").mkdirs()
        projectDir.resolve("jbake/content/blog/2026/0001_post.adoc").writeText(
            """= Titre
:jbake-type: post
:jbake-status: published

Un article.
""",
        )
    }
}
