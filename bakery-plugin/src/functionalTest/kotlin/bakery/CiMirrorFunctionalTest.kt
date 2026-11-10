@file:Suppress("FunctionName")

package bakery

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.GradleRunner.create
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class CiMirrorFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `pushCiMirror pushes the site source and the workflow overlay to the target repo`() {
        val bareRemoteDir = projectDir.resolve("remote.git").apply { mkdirs() }
        Git
            .init()
            .setBare(true)
            .setDirectory(bareRemoteDir)
            .call()
            .close()
        val remoteUri = bareRemoteDir.toURI().toString()

        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement.repositories.gradlePluginPortal()
            rootProject.name = "ci-mirror-ft"
            """.trimIndent(),
            UTF_8,
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("education.cccp.bakery") }
            bakery { configPath = file("office/sites/demo/site.yml").absolutePath }
            """.trimIndent() + "\n",
            UTF_8,
        )

        val siteDir = projectDir.resolve("office/sites/demo").apply { mkdirs() }
        siteDir.resolve("jbake").mkdirs()
        siteDir.resolve("jbake/index.adoc").writeText("= Demo", UTF_8)
        siteDir.resolve("gradlew").writeText("#!/bin/sh\n", UTF_8)
        assertThat(siteDir.resolve("gradlew").setExecutable(true)).isTrue()
        siteDir.resolve("maquette").mkdirs()
        siteDir.resolve(".agents/sessions").mkdirs()
        siteDir.resolve(".agents/sessions/s1.adoc").writeText("governance", UTF_8)
        siteDir.resolve("build").mkdirs()
        siteDir.resolve("build/stale.html").writeText("stale", UTF_8)
        siteDir.resolve(".ci-mirror/.github/workflows").mkdirs()
        siteDir.resolve(".ci-mirror/.github/workflows/ci.yml").writeText("name: CI", UTF_8)

        siteDir.resolve("site.yml").writeText(
            """
            bake:
              srcPath: "jbake"
              destDirPath: "build/bake"
              cname: "demo"
            pushCiMirror:
              from: "ci-mirror"
              to: "ci-mirror-push"
              repo:
                name: "ci"
                repository: "$remoteUri"
                credentials:
                  username: "ciUser"
                  password: "ciPass"
              branch: "main"
              message: "sync demo"
            """.trimIndent() + "\n",
            UTF_8,
        )

        val result =
            create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("pushCiMirror", "--stacktrace")
                .build()

        assertThat(result.task(":pushCiMirror")?.outcome).isEqualTo(SUCCESS)

        val cloneDir = projectDir.resolve("verify").apply { mkdirs() }
        Git
            .cloneRepository()
            .setURI(remoteUri)
            .setDirectory(cloneDir)
            .call()
            .use { _ -> }

        assertThat(cloneDir.resolve("sites/demo/jbake/index.adoc")).exists().hasContent("= Demo")
        assertThat(cloneDir.resolve("sites/demo/gradlew").canExecute()).isTrue()
        assertThat(cloneDir.resolve(".github/workflows/ci.yml")).exists().hasContent("name: CI")
        assertThat(cloneDir.resolve("sites/demo/site.yml")).doesNotExist()
        assertThat(cloneDir.resolve("sites/demo/.agents/sessions/s1.adoc")).doesNotExist()
        assertThat(cloneDir.resolve("sites/demo/build/stale.html")).doesNotExist()
    }
}
