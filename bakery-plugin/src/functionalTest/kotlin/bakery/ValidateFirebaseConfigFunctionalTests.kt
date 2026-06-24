@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests fonctionnels pour l'EPIC BKY-IA-3 — Validation Firebase Config.
 *
 * Les tests DSL (compilation, enregistrement de tâche) ont été migrés vers
 * `FirebaseAuthConfigUnitTest` (ProjectBuilder, in-process) dans le cadre de
 * BKY-PERF-6. Seuls les tests qui exécutent réellement `validateFirebaseConfig`
 * (exécution runtime avec validation métier) restent ici.
 */
class ValidateFirebaseConfigFunctionalTests {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `validateFirebaseConfig task runs with valid config`() {
        createProjectWithFirebaseAuth(
            apiKey = "AIzaSyB-valid-key",
            authDomain = "my-project.firebaseapp.com",
            projectId = "my-project"
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("validateFirebaseConfig")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("[validateFirebaseConfig]")
    }

    @Test
    fun `validateFirebaseConfig with invalid config fails`() {
        createProjectWithFirebaseAuth(
            apiKey = "",
            authDomain = "",
            projectId = ""
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("validateFirebaseConfig")
            .buildAndFail()

        assertThat(result.output).contains("Configuration Firebase invalide")
    }

    private fun createProjectWithFirebaseAuth(
        apiKey: String,
        authDomain: String,
        projectId: String
    ) {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement { repositories { gradlePluginPortal(); mavenLocal() } }
            rootProject.name = "firebase-validate-test"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("education.cccp.bakery") }
            bakery {
                configPath = file("site.yml").absolutePath
                firebaseAuth {
                    apiKey = "$apiKey"
                    authDomain = "$authDomain"
                    projectId = "$projectId"
                }
            }
        """.trimIndent())

        // Write a site.yml that triggers the "else" branch (existing site directories)
        projectDir.resolve("site.yml").writeText("""
            bake:
              srcPath: "site"
              destDirPath: "build/bake"
            pushPage:
              from: "site"
              to: "cvs"
              repo:
                name: "test-site"
                repository: "https://github.com/user/repo.git"
                credentials:
                  username: "user"
                  password: "token"
              branch: "main"
              message: "Deploy test"
            pushMaquette:
              from: "maquette"
              to: "cvs"
              repo:
                name: "test-maquette"
                repository: "https://github.com/user/maquette.git"
                credentials:
                  username: "user"
                  password: "token"
              branch: "main"
              message: "Deploy maquette"
        """.trimIndent())

        // Create directories so plugin takes the "else" branch
        projectDir.resolve("site").mkdirs()
        projectDir.resolve("maquette").mkdirs()
    }
}
