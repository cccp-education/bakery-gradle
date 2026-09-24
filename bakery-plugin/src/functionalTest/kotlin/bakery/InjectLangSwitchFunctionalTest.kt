package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LANG-NAV-2 — the injected `th:href` is page-aware (D4).
 *
 * Because `injectLangSwitch` injects into `menu.thyme`, a template shared by
 * every page of a language, the emitted link is a Thymeleaf expression in
 * `${content.rootpath}` / `${content.uri}` — never a page-blind `index.html`.
 */
class InjectLangSwitchFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `injectLangSwitch injects page-aware fragment into menu dot thyme for 2 languages`() {
        createProjectWithFixture(2)
        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("injectLangSwitch")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        assertThat(frMenu.exists()).isTrue()
        val frContent = frMenu.readText()
        assertThat(frContent).contains("data-lang=\"fr\"")
        assertThat(frContent).contains("data-lang=\"en\"")
        assertThat(frContent).contains("dropdown-menu")
        assertThat(frContent).contains("\${content.rootpath + 'en/' + content.uri}")

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        assertThat(enMenu.exists()).isTrue()
        val enContent = enMenu.readText()
        assertThat(enContent).contains("data-lang=\"fr\"")
        assertThat(enContent).contains("data-lang=\"en\"")
    }

    @Test
    fun `injectLangSwitch from EN subdir preserves the page when linking to FR root`() {
        createProjectWithFixture(2)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        assertThat(enContent).contains("|../\${content.rootpath}\${content.uri}|")
    }

    @Test
    fun `injectLangSwitch from FR root preserves the page when linking to EN subdir`() {
        createProjectWithFixture(2)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        val frContent = frMenu.readText()
        assertThat(frContent).contains("\${content.rootpath + 'en/' + content.uri}")
    }

    @Test
    fun `injectLangSwitch does not hardcode a page-blind index html link`() {
        createProjectWithFixture(2)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        val frContent = frMenu.readText()
        val switcherBlock = frContent.substringAfter("lang-switcher-container").substringAfter("<ul")
        assertThat(switcherBlock).doesNotContain("'index.html'")
        assertThat(switcherBlock).doesNotContain("en/index.html")
    }

    @Test
    fun `injectLangSwitch does not create self-loop on EN page`() {
        createProjectWithFixture(2)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        val enAnchor = enContent.substringBefore("data-lang=\"en\"").substringAfterLast("<a ")
        assertThat(enAnchor).doesNotContain("en/index.html")
        assertThat(enAnchor).contains("content.uri.lastIndexOf")
    }

    @Test
    fun `injectLangSwitch is idempotent on second invocation`() {
        createProjectWithFixture(2)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        val firstContent = frMenu.readText()

        val result2 =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("injectLangSwitch")
                .build()

        assertThat(result2.output).contains("BUILD SUCCESSFUL")
        val secondContent = frMenu.readText()
        assertThat(secondContent).isEqualTo(firstContent)
    }

    @Test
    fun `injectLangSwitch is registered in transform group`() {
        createProjectWithFixture(2)
        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--group", "transform")
                .build()

        assertThat(result.output).contains("injectLangSwitch")
    }

    @Test
    fun `injectLangSwitch with 3 languages links EN subdir to AR subdir page-aware`() {
        createProjectWithFixture(3)
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        assertThat(enContent).contains("|../\${content.rootpath}ar/\${content.uri}|")
    }

    private fun createProjectWithFixture(langCount: Int) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "inject-lang-switch-test"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )

        val siteDir = projectDir.resolve("site")
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("content").mkdirs()

        val menuThyme =
            """
            <html xmlns:th="http://www.thymeleaf.org">
            <body>
            <nav class="navbar">
                <div class="container">
                    <a class="navbar-brand" href="#">Test</a>
                    <div class="lang-switcher-container">
                    </div>
                </div>
            </nav>
            </body>
            </html>
            """.trimIndent()

        siteDir.resolve("templates/menu.thyme").writeText(menuThyme)
        siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")

        val langs = if (langCount == 3) listOf("fr", "en", "ar") else listOf("fr", "en")

        for (lang in langs) {
            if (lang == "fr") continue
            val langDir = siteDir.resolve(lang)
            langDir.resolve("templates").mkdirs()
            langDir.resolve("content").mkdirs()
            langDir.resolve("templates/menu.thyme").writeText(menuThyme)
            langDir.resolve("content/index.html").writeText("<h1>Hello $lang</h1>")
        }

        val langsYaml = langs.joinToString(", ")
        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [$langsYaml]
            """.trimIndent(),
        )
    }
}
