package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InjectLangSwitchFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `injectLangSwitch injects fragment into menu dot thyme for 2 languages`() {
        createProjectWithFixture(2)
        val result = GradleRunner.create()
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

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        assertThat(enMenu.exists()).isTrue()
        val enContent = enMenu.readText()
        assertThat(enContent).contains("data-lang=\"fr\"")
        assertThat(enContent).contains("data-lang=\"en\"")
    }

    @Test
    fun `injectLangSwitch from EN subdir links to FR root correctly`() {
        createProjectWithFixture(2)
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        assertThat(enContent).contains("../index.html")
    }

    @Test
    fun `injectLangSwitch from FR root links to EN subdir correctly`() {
        createProjectWithFixture(2)
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        val frContent = frMenu.readText()
        assertThat(frContent).contains("en/index.html")
    }

    @Test
    fun `injectLangSwitch does not create self-loop on EN page`() {
        createProjectWithFixture(2)
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        val enAnchor = enContent.substringBefore("data-lang=\"en\"").substringAfterLast("<a ")
        assertThat(enAnchor).doesNotContain("en/index.html")
    }

    @Test
    fun `injectLangSwitch is idempotent on second invocation`() {
        createProjectWithFixture(2)
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val frMenu = projectDir.resolve("site/templates/menu.thyme")
        val firstContent = frMenu.readText()

        val result2 = GradleRunner.create()
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
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "transform")
            .build()

        println("DEBUG OUTPUT:\n${result.output}")
        assertThat(result.output).contains("injectLangSwitch")
    }

    @Test
    fun `injectLangSwitch with 3 languages links EN subdir to AR subdir`() {
        createProjectWithFixture(3)
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("injectLangSwitch")
            .build()

        val enMenu = projectDir.resolve("site/en/templates/menu.thyme")
        val enContent = enMenu.readText()
        assertThat(enContent).contains("../ar/index.html")
    }

    private fun createProjectWithFixture(langCount: Int) {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "inject-lang-switch-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
        """.trimIndent())

        val siteDir = projectDir.resolve("site")
        siteDir.resolve("templates").mkdirs()
        siteDir.resolve("content").mkdirs()

        val menuThyme = """
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
        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [$langsYaml]
        """.trimIndent())
    }
}