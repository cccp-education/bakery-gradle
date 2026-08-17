package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC BKY-CONTACT-SEC US-7 — Functional tests on the public fixture
 * `cheroliv-com-contact` (mocks only, no real identifiers).
 */
class ContactSecFixtureFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `scaffoldContactSec on cheroliv-com-contact fixture produces hardened form and rules`() {
        copyFixture()
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
                assertThat(content).contains("<!-- CONTACT-SEC: bakery -->")
                assertThat(content).contains("contact-form")
                assertThat(content).contains("hp_name")
                assertThat(content).contains("session_token")
                assertThat(content).contains("pow_nonce")
                assertThat(content).contains("cf-turnstile")
                assertThat(content).contains("0xTESTKEY")

                val rules = projectDir.resolve("site/firestore.rules")
                assertThat(rules.exists()).isTrue()
                assertThat(rules.readText()).contains("match /contacts/{docId}")
                assertThat(rules.readText()).contains("created_at == request.time")
            }
    }

    @Test
    fun `scaffoldContactSec on fixture is idempotent on second run`() {
        copyFixture()
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
                assertThat(frFooter.readText()).isEqualTo(firstContent)
            }
    }

    private fun copyFixture() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "contact-sec-fixture-test"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = "site.yml" }
            """.trimIndent(),
        )

        val fixtureRoot = File("src/test/resources/fixtures/cheroliv-com-contact")
        val siteDir = projectDir.resolve("site")
        siteDir.mkdirs()

        fixtureRoot.resolve("jbake").copyRecursively(siteDir, overwrite = true)
        val enSource = fixtureRoot.resolve("i18n/en")
        val enTarget = siteDir.resolve("en")
        enSource.copyRecursively(enTarget, overwrite = true)

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
              rateLimit:
                perHour: 3
                perDay: 30
            """.trimIndent(),
        )
    }
}