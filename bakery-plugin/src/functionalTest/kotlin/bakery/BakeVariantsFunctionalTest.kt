package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-8 — `bakeVariants` bakes every deployable `i18n/{lang}` variant
 * into a `{lang}/` sub-directory, with the same JBake engine as the reference.
 *
 * The reference is baked at the root by `bake`; this task is the missing brick
 * of a full i18n rollout: a site whose variants live under `i18n/{lang}/`
 * (materialised from a frozen bundle, or LLM-translated) obtains a deployable
 * `{lang}/` tree ready to publish alongside `index.html`.
 *
 * An undeployable variant (missing a reference template) is skipped, never
 * baked — one missing template aborts the whole JBake render (S-049).
 */
class BakeVariantsFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the task is registered in the transform group`() {
        createSiteWithVariants("en")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "transform")
                .build()

        assertThat(result.output).contains("bakeVariants")
    }

    @Test
    fun `a deployable variant is baked into its language sub-directory`() {
        createSiteWithVariants("en")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("bakeVariants")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val enIndex = projectDir.resolve("build/bake/en/about.html")
        assertThat(enIndex).exists()
        assertThat(enIndex.readText())
            .describedAs("the EN variant must be rendered by JBake, not copied raw")
            .contains("English homepage")
            .doesNotContain("#{")
    }

    @Test
    fun `an undeployable variant is skipped, never baked`() {
        createSiteWithVariants("en", "de")
        // de is missing archive.thyme (a reference template that archive.thyme
        // renders via th:replace) → baking it would abort the whole render.
        projectDir.resolve("jbake/i18n/de/templates/archive.thyme").delete()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("bakeVariants")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/bake/en/about.html")).exists()
        assertThat(projectDir.resolve("build/bake/de")).doesNotExist()
    }

    @Test
    fun `the reference language is never baked as a variant`() {
        createSiteWithVariants("en")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("bakeVariants")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/bake/fr")).doesNotExist()
    }

    @Test
    fun `deploySite bakes the variants so the published tree carries them`() {
        createSiteWithVariants("en")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("deploySite", "--dry-run")
                .build()

        assertThat(result.output).contains(":bakeVariants SKIPPED")
    }

    @Test
    fun `baking waits for the i18n chain so the injected selector is rendered`() {
        createSiteWithVariants("en")

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("bake", "materializeTemplates", "injectLangSwitch", "--dry-run")
                .build()

        val bakeIndex = result.output.indexOf(":bake SKIPPED")
        val injectIndex = result.output.indexOf(":injectLangSwitch SKIPPED")
        assertThat(bakeIndex)
            .describedAs("bake must render the injected selector, so it runs after injectLangSwitch")
            .isGreaterThan(injectIndex)
    }

    private fun createSiteWithVariants(vararg languages: String) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "bake-variants-test"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )

        val jbake = projectDir.resolve("jbake")
        val referenceTemplates = jbake.resolve("templates").apply { mkdirs() }
        jbake.resolve("assets").mkdirs()
        jbake.resolve("content").mkdirs()
        jbake.resolve("content/about.adoc").writeText(
            """
            = Accueil
            :jbake-status: published
            :jbake-type: page

            French homepage
            """.trimIndent(),
        )
        jbake.resolve("jbake.properties").writeText(
            """
            site.host=https://example.org/
            render.index=false
            render.archive=false
            render.tags=false
            render.blog=false
            render.feed=false
            render.sitemap=false
            template.page.file=page.thyme
            """.trimIndent(),
        )

        referenceTemplates.resolve("page.thyme").writeText(
            """<!DOCTYPE html><html><body><main th:utext="${'$'}{content.body}">x</main></body></html>""",
        )
        listOf("archive", "tags", "blog", "index").forEach { name ->
            referenceTemplates.resolve("$name.thyme").writeText("<html><body>x</body></html>")
        }

        languages.forEach { language ->
            val variant = jbake.resolve("i18n/$language")
            variant.resolve("templates").mkdirs()
            variant.resolve("content").mkdirs()
            variant.resolve("content/about.adoc").writeText(
                """
                = Home
                :jbake-status: published
                :jbake-type: page

                English homepage
                """.trimIndent(),
            )
            referenceTemplates.listFiles().orEmpty().forEach { template ->
                variant
                    .resolve("templates")
                    .resolve(template.name)
                    .writeText(template.readText())
            }
        }

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: "jbake"
              destDirPath: "bake"
            language: fr
            supportedLanguages: [fr, ${languages.joinToString(", ")}]
            """.trimIndent(),
        )
    }
}
