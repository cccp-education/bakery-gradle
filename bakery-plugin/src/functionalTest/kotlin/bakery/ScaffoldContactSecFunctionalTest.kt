package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ScaffoldContactSecFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `scaffoldContactSec injects form into FR footer thyme`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()
            .let { result ->
                assertThat(result.output).contains("BUILD SUCCESSFUL")
                val frFooter = projectDir.resolve("site/templates/footer.thyme")
                assertThat(frFooter.exists()).isTrue()
                val content = frFooter.readText()
                assertThat(content).contains("contact-form")
                assertThat(content).contains("hp_name")
                assertThat(content).contains("session_token")
                assertThat(content).contains("cf-turnstile")
                assertThat(content).contains("data-sitekey=\"0xTESTKEY\"")
            }
    }

    @Test
    fun `scaffoldContactSec injects into EN footer thyme`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()

        val enFooter = projectDir.resolve("site/en/templates/footer.thyme")
        assertThat(enFooter.exists()).isTrue()
        val content = enFooter.readText()
        assertThat(content).contains("contact-form")
        assertThat(content).contains("hp_name")
    }

    @Test
    fun `scaffoldContactSec generates contact js for each language`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()

        val frJs = projectDir.resolve("site/assets/js/contact.js")
        assertThat(frJs.exists()).isTrue()
        assertThat(frJs.readText()).contains("fingerprint")
        assertThat(frJs.readText()).contains("solvePow")
        assertThat(frJs.readText()).contains("turnstile.getResponse")

        val enJs = projectDir.resolve("site/en/assets/js/contact.js")
        assertThat(enJs.exists()).isTrue()
    }

    @Test
    fun `scaffoldContactSec generates firestore rules`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()

        val rules = projectDir.resolve("site/firestore.rules")
        assertThat(rules.exists()).isTrue()
        val content = rules.readText()
        assertThat(content).contains("match /contacts/{docId}")
        assertThat(content).contains("allow create:")
        assertThat(content).contains("hp_name == ''")
        assertThat(content).contains("created_at == request.time")
    }

    @Test
    fun `scaffoldContactSec is idempotent`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()

        val frFooter = projectDir.resolve("site/templates/footer.thyme")
        val firstContent = frFooter.readText()

        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()
            .let { result ->
                assertThat(result.output).contains("BUILD SUCCESSFUL")
                val secondContent = frFooter.readText()
                assertThat(secondContent).isEqualTo(firstContent)
            }
    }

    @Test
    fun `scaffoldContactSec is no-op without contact config`() {
        createProjectWithoutContactConfig()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("scaffoldContactSec")
            .build()
            .let { result ->
                assertThat(result.output).contains("BUILD SUCCESSFUL")
                assertThat(result.output).contains("No contact config")
            }
    }

    @Test
    fun `scaffoldContactSec is registered in transform group`() {
        createProjectWithFixture()
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group", "transform")
            .build()
            .let { result ->
                assertThat(result.output).contains("scaffoldContactSec")
            }
    }

    private fun createProjectWithFixture() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "scaffold-contact-sec-test"
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

        val footerThyme =
            """
            <html xmlns:th="http://www.thymeleaf.org">
            <body>
            <footer>Old footer</footer>
            </body>
            </html>
            """.trimIndent()

        siteDir.resolve("templates/footer.thyme").writeText(footerThyme)
        siteDir.resolve("content/index.html").writeText("<h1>Hello FR</h1>")

        val enDir = siteDir.resolve("en")
        enDir.resolve("templates").mkdirs()
        enDir.resolve("content").mkdirs()
        enDir.resolve("templates/footer.thyme").writeText(footerThyme)
        enDir.resolve("content/index.html").writeText("<h1>Hello EN</h1>")

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr, en]
            contact:
              enabled: true
              endpointUrl: "https://script.example.com/exec"
              firestoreCollection: contacts
              turnstile:
                siteKey: "0xTESTKEY"
              minRenderTimeMs: 2500
              dailyGlobalCap: 50
            """.trimIndent(),
        )
    }

    private fun createProjectWithoutContactConfig() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "scaffold-contact-sec-noconfig-test"
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
        siteDir.resolve("templates/footer.thyme").writeText(
            """
            <body>
            <footer>No contact</footer>
            </body>
            """.trimIndent(),
        )

        projectDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: site
              destDirPath: build/output
            language: fr
            supportedLanguages: [fr]
            """.trimIndent(),
        )
    }
}