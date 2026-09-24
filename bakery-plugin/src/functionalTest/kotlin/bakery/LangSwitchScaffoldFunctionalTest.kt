@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner.create
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LANG-NAV-3 — the client-side language switcher must ship in a scaffolded
 * site (decision D7): a brand-new site must not be born with a switcher that
 * sends the visitor home. `generateSite` copies both resource trees verbatim,
 * so the module must exist under `maquette/js/` and `site/assets/js/`, and the
 * two copies must be byte-identical (the parity D6 is enforced at build here,
 * not only at test time).
 */
class LangSwitchScaffoldFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `a scaffolded blog site ships the switcher in both trees, byte-identical`() {
        createMinimalBakeryProject(projectDir, siteName = "lang-nav")

        create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val siteDir = projectDir.resolve("lang-nav")
        val maquetteModule = siteDir.resolve("maquette/js/lang-switch.js")
        val publicationModule = siteDir.resolve("site/assets/js/lang-switch.js")

        assertThat(maquetteModule).exists().isFile
        assertThat(publicationModule).exists().isFile
        assertThat(publicationModule.readText(UTF_8))
            .isEqualTo(maquetteModule.readText(UTF_8))

        // The contract surface travels with the module (behaviour, not just presence).
        val module = maquetteModule.readText(UTF_8)
        assertThat(module).contains("resolveLangPath")
        assertThat(module).contains("attachLangSwitch")
    }

    @Test
    fun `a scaffolded blog site is born with a page-aware model menu (D7)`() {
        createMinimalBakeryProject(projectDir, siteName = "lang-nav")

        create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val menu = projectDir.resolve("lang-nav/site/templates/menu.thyme")
        assertThat(menu).exists().isFile

        val content = menu.readText(UTF_8)
        assertThat(content).contains("lang-option")
        assertThat(content).contains("data-lang")
        assertThat(content).contains("content.uri")

        // D3 anti split-brain: the model carries the single-rule expression, not
        // a page-blind absolute `'/' + lang.code + '/'` (the S-228 constat bug).
        assertThat(content).contains("content.uri.lastIndexOf")
        assertThat(content).contains("config.site_language")
        assertThat(content).doesNotContain("'/' + ${'$'}{lang.code} + '/'")
    }

    @Test
    fun `a scaffolded basic site is born with a page-aware model menu (D7)`() {
        createMinimalBakeryProject(projectDir, siteName = "lang-nav", siteType = "basic")

        create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSite")
            .build()

        val menu = projectDir.resolve("lang-nav/site-basic/templates/menu.thyme")
        assertThat(menu).exists().isFile

        val content = menu.readText(UTF_8)
        assertThat(content).contains("lang-option")
        assertThat(content).contains("content.uri.lastIndexOf")
        assertThat(content).doesNotContain("'/' + ${'$'}{lang.code} + '/'")
    }

    private fun createMinimalBakeryProject(
        projectDir: File,
        siteName: String,
        siteType: String? = null,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement.repositories.gradlePluginPortal()
            rootProject.name = "lang-nav-test"
            """.trimIndent(),
            UTF_8,
        )
        val siteTypeLine = siteType?.let { "    siteType = \"$it\"\n" } ?: ""
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
                siteName = "$siteName"
            $siteTypeLine}
            """.trimIndent() + "\n",
            UTF_8,
        )
    }
}
