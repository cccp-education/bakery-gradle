package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * CHE-I18N-22 US-4 — the CLI can exclude content subtrees from translation.
 *
 * The cheroliv.com corpus carries 72 draft articles under `content/draft/` that
 * are never deployed (the published variant has no `draft/`). Translating them
 * burns 39% of the metered LLM budget for nothing — the Ink Economy Law applied
 * to *what* is sent. The reference variant already excludes `draft`; this option
 * lets the CLI reach the same behaviour.
 */
class ContentI18nExcludePathsFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the CLI excludes the requested subtree from the translation set`() {
        createSite()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(
                    "migrateContentI18n",
                    "--contentI18nSource=content",
                    "--contentI18nOutput=content-i18n",
                    "--contentI18nSourceLang=fr",
                    "--contentI18nTargetLangs=en",
                    "--contentI18nExcludePaths=draft",
                    "--contentI18nDryRun=true",
                ).build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun createSite() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "cheroliv-exclude"
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
        projectDir.resolve("jbake/content/draft").mkdirs()
        projectDir.resolve("jbake/content/draft/brouillon.adoc").writeText(
            """= Brouillon
:jbake-type: post
:jbake-status: draft

Un brouillon en français.
""",
        )
    }
}
