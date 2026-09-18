package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CHE-I18N-22 US-4/US-8 — the CLI can drive the content-migration parallelism.
 *
 * The pilot decision S-044 raises the ceiling to five Ollama providers (the pool
 * spans 11437-11465), replacing the earlier two-provider bound. Concurrency is
 * across articles; one article is still translated block-by-block, sequentially.
 */
class ContentI18nParallelismFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the CLI accepts the five-provider ceiling`() {
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
                    "--contentI18nParallelism=5",
                    "--contentI18nDryRun=true",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

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
    fun `the CLI rejects a parallelism above five`() {
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
                    "--contentI18nParallelism=6",
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
