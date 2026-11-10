package bakery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CiMirrorPayloadTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `excludes governance secrets and build artifacts at any depth`() {
        listOf(
            ".agents/session.adoc",
            "build/out.html",
            "src/build/out.class",
            ".gradle/cache.bin",
            ".kotlin/sessions/x",
            ".idea/workspace.xml",
            ".ci-mirror/.github/workflows/ci.yml",
            ".github/workflows/ci.yml",
            "site.yml",
            "site.yml.bak-pre-11434",
            "local.properties",
            "graph.json",
            "module.iml",
            ".bakery-checksums.properties",
            ".bakery-block-checksums/content/blog/x.checksums",
        ).forEach { path ->
            assertThat(CiMirrorPayload.isExcluded(path)).`as`("excluded: $path").isTrue()
        }
    }

    @Test
    fun `keeps the site source`() {
        listOf(
            "jbake/index.adoc",
            "jbake/templates/footer.thyme",
            "maquette/css/styles.css",
            "src/main/kotlin/App.kt",
            "build.gradle.kts",
            "jbake.properties",
            "functions/index.js",
        ).forEach { path ->
            assertThat(CiMirrorPayload.isExcluded(path)).`as`("kept: $path").isFalse()
        }
    }

    @Test
    fun `assembles the site source and the overlay into distinct roots`() {
        val siteDir = tempDir.resolve("office/sites/demo").apply { mkdirs() }
        siteDir.resolve("jbake").mkdirs()
        siteDir.resolve("jbake/index.adoc").writeText("= Home")
        siteDir.resolve("maquette/css").mkdirs()
        siteDir.resolve("maquette/css/styles.css").writeText("body{}")
        siteDir.resolve("site.yml").writeText("bake: {}")
        siteDir.resolve(".agents/sessions").mkdirs()
        siteDir.resolve(".agents/sessions/s1.adoc").writeText("secret governance")
        siteDir.resolve("build").mkdirs()
        siteDir.resolve("build/stale.html").writeText("stale")
        siteDir.resolve(".ci-mirror/.github/workflows").mkdirs()
        siteDir.resolve(".ci-mirror/.github/workflows/ci.yml").writeText("name: CI")
        siteDir.resolve(".ci-mirror/README.adoc").writeText("= mirror")

        val payloadDir = tempDir.resolve("build/ci-mirror")
        val result = CiMirrorPayload.assemble(siteDir, payloadDir, siteDir.resolve(".ci-mirror"))

        assertThat(result.isRight()).isTrue()
        val summary =
            result.fold(
                ifLeft = { error -> throw AssertionError(error) },
                ifRight = { it },
            )
        assertThat(summary.siteFiles).isEqualTo(2)
        assertThat(summary.overlayFiles).isEqualTo(2)
        assertThat(payloadDir.resolve("sites/demo/jbake/index.adoc")).exists().hasContent("= Home")
        assertThat(payloadDir.resolve("sites/demo/maquette/css/styles.css")).exists()
        assertThat(payloadDir.resolve(".github/workflows/ci.yml")).exists().hasContent("name: CI")
        assertThat(payloadDir.resolve("README.adoc")).exists()
        assertThat(payloadDir.resolve("sites/demo/site.yml")).doesNotExist()
        assertThat(payloadDir.resolve("sites/demo/.agents/sessions/s1.adoc")).doesNotExist()
        assertThat(payloadDir.resolve("sites/demo/build/stale.html")).doesNotExist()
        assertThat(payloadDir.resolve("sites/demo/.ci-mirror")).doesNotExist()
    }

    @Test
    fun `resets a stale payload before assembling`() {
        val siteDir = tempDir.resolve("office/sites/demo").apply { mkdirs() }
        siteDir.resolve("jbake").mkdirs()
        siteDir.resolve("jbake/index.adoc").writeText("= Home")
        siteDir.resolve(".ci-mirror").mkdirs()
        siteDir.resolve(".ci-mirror/README.adoc").writeText("= mirror")
        val payloadDir = tempDir.resolve("build/ci-mirror").apply { mkdirs() }
        payloadDir.resolve("stale.txt").writeText("stale")

        CiMirrorPayload.assemble(siteDir, payloadDir, siteDir.resolve(".ci-mirror"))

        assertThat(payloadDir.resolve("stale.txt")).doesNotExist()
        assertThat(payloadDir.resolve("sites/demo/jbake/index.adoc")).exists()
    }

    @Test
    fun `preserves the executable bit of the payload scripts`() {
        val siteDir = tempDir.resolve("office/sites/demo").apply { mkdirs() }
        siteDir.resolve("jbake").mkdirs()
        siteDir.resolve("gradlew").writeText("#!/bin/sh\n")
        assertThat(siteDir.resolve("gradlew").setExecutable(true)).isTrue()
        siteDir.resolve(".ci-mirror").mkdirs()
        siteDir.resolve(".ci-mirror/README.adoc").writeText("= mirror")
        val payloadDir = tempDir.resolve("build/ci-mirror")

        CiMirrorPayload.assemble(siteDir, payloadDir, siteDir.resolve(".ci-mirror"))

        assertThat(payloadDir.resolve("sites/demo/gradlew").canExecute()).isTrue()
    }

    @Test
    fun `fails clearly when the overlay is missing`() {
        val siteDir = tempDir.resolve("office/sites/demo").apply { mkdirs() }

        val result =
            CiMirrorPayload.assemble(siteDir, tempDir.resolve("payload"), siteDir.resolve(".ci-mirror"))

        assertThat(result.isLeft()).isTrue()
        assertThat(result.fold(ifLeft = { it }, ifRight = { "" })).contains("overlay not found")
    }

    @Test
    fun `fails clearly when the site directory is missing`() {
        val overlay = tempDir.resolve("overlay").apply { mkdirs() }

        val result =
            CiMirrorPayload.assemble(tempDir.resolve("missing"), tempDir.resolve("payload"), overlay)

        assertThat(result.isLeft()).isTrue()
        assertThat(result.fold(ifLeft = { it }, ifRight = { "" })).contains("Site directory not found")
    }
}
