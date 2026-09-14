package bakery.a11y

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccessibilityAuditTaskTest {
    @TempDir
    lateinit var tempDir: File

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TaskRegistration {
        @Test
        fun `task is registered with correct group and description`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y")
                    .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()

            assertNotNull(task)
            assertEquals("audit", task.group)
            assertTrue(task.description!!.contains("accessibilité"))
        }

        @Test
        fun `task initializes properties with defaults`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y-defaults")
                    .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()

            assertEquals("build/reports/accessibility-audit.json", task.reportPath.get())
            assertEquals("AA", task.conformanceLevel.get())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveAuditDir {
        @Test
        fun `uses configured audit directory when set`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y-dir")
                    .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            val configuredDir = tempDir.resolve("custom-bake")
            configuredDir.mkdirs()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(configuredDir)

            assertEquals(configuredDir, task.resolveAuditDir())
        }

        @Test
        fun `falls back to build slash bake when auditDir is not set`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y-fallback")
                    .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            val expected =
                project.layout.buildDirectory.asFile
                    .get()
                    .resolve("bake")

            assertEquals(expected, task.resolveAuditDir())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RegistrarAuditDirResolution {
        /**
         * BKY-A11Y-2 — Le registrar `accessibilityAudit` doit pointer l'`auditDir`
         * sur le répertoire où `bake` écrit réellement, c'est-à-dire
         * `layout.buildDirectory/<destDirPath>` (cf. `configureBakeTask`), et NON
         * `layout.projectDirectory/<destDirPath>`. Sans ce fix, `publishSite`
         * échoue en validation Gradle (`Input file does not exist : <engine>/bake`).
         */
        @Test
        fun `registrar resolves auditDir to the bake output under buildDirectory`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y-registrar")
                    .build()
            project.pluginManager.apply("java-base")

            val extension = project.extensions.create("bakery", bakery.BakeryExtension::class.java)
            val site =
                bakery.SiteConfiguration(
                    bake = bakery.BakeConfiguration(srcPath = "jbake", destDirPath = "bake"),
                )

            with(bakery.a11y.AccessibilityTaskRegistrar) {
                project.registerAccessibilityAuditTask(extension, site)
            }

            val task = project.tasks.getByName("accessibilityAudit") as AccessibilityAuditTask
            val expected = project.layout.buildDirectory.dir("bake").get().asFile

            assertEquals(expected, task.resolveAuditDir())
        }

        /**
         * BKY-A11Y-2 — Un `auditDir` explicite au DSL reste résolu relativement au
         * répertoire projet (comportement documenté, ex: `auditDir = "build/bake"`).
         */
        @Test
        fun `registrar honours explicit DSL auditDir against projectDirectory`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(tempDir)
                    .withName("test-a11y-registrar-dsl")
                    .build()
            project.pluginManager.apply("java-base")

            val extension = project.extensions.create("bakery", bakery.BakeryExtension::class.java)
            extension.a11y.auditDir.set("build/custom-bake")
            val site =
                bakery.SiteConfiguration(
                    bake = bakery.BakeConfiguration(srcPath = "jbake", destDirPath = "bake"),
                )

            with(bakery.a11y.AccessibilityTaskRegistrar) {
                project.registerAccessibilityAuditTask(extension, site)
            }

            val task = project.tasks.getByName("accessibilityAudit") as AccessibilityAuditTask
            val expected = project.layout.projectDirectory.dir("build/custom-bake").asFile

            assertEquals(expected, task.resolveAuditDir())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExecuteAudit {
        @TempDir
        lateinit var auditTempDir: File

        @Test
        fun `audits html files and writes json report`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-execute")
                    .build()
            project.pluginManager.apply("java-base")

            val bakedDir = auditTempDir.resolve("build/bake").apply { mkdirs() }
            bakedDir.resolve("index.html").writeText(
                """<p style="color: #000000; background-color: #FFFFFF;">OK</p>""",
                Charsets.UTF_8,
            )

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(bakedDir)
            task.reportPath.set("build/reports/a11y-test.json")

            task.executeAudit()

            val reportFile = auditTempDir.resolve("build/reports/a11y-test.json")
            assertTrue(reportFile.exists())
            val content = reportFile.readText(Charsets.UTF_8)
            assertThat(content).contains("\"compliant\": true")
            assertThat(content).contains("\"passedCount\": 1")
            assertThat(content).contains("\"failedCount\": 0")
        }

        @Test
        fun `fails when audit directory does not exist`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-missing-dir")
                    .build()
            project.pluginManager.apply("java-base")

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(File("/nonexistent/bake"))

            assertThrows<IllegalStateException> {
                task.executeAudit()
            }
        }

        @Test
        fun `reports non-compliant for failing contrast`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-fail")
                    .build()
            project.pluginManager.apply("java-base")

            val bakedDir = auditTempDir.resolve("build/bake").apply { mkdirs() }
            bakedDir.resolve("index.html").writeText(
                """<p style="color: #FFA500; background-color: #FFFFFF;">Weak</p>""",
                Charsets.UTF_8,
            )

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(bakedDir)
            task.reportPath.set("build/reports/a11y-fail.json")

            task.executeAudit()

            val reportFile = auditTempDir.resolve("build/reports/a11y-fail.json")
            assertTrue(reportFile.exists())
            val content = reportFile.readText(Charsets.UTF_8)
            assertThat(content).contains("\"compliant\": false")
            assertThat(content).contains("\"failedCount\": 1")
        }

        @Test
        fun `warns when no html files found`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-empty")
                    .build()
            project.pluginManager.apply("java-base")

            val bakedDir = auditTempDir.resolve("build/bake").apply { mkdirs() }

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(bakedDir)
            task.reportPath.set("build/reports/a11y-empty.json")

            task.executeAudit()

            val reportFile = auditTempDir.resolve("build/reports/a11y-empty.json")
            assertTrue(reportFile.exists())
            val content = reportFile.readText(Charsets.UTF_8)
            assertThat(content).contains("\"compliant\": true")
            assertThat(content).contains("\"passedCount\": 0")
            assertThat(content).contains("\"failedCount\": 0")
            assertThat(content).contains("\"findings\":")
        }

        @Test
        fun `fails when non compliant and failOnNonCompliant is true`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-gate")
                    .build()
            project.pluginManager.apply("java-base")

            val bakedDir = auditTempDir.resolve("build/bake").apply { mkdirs() }
            bakedDir.resolve("index.html").writeText(
                """<p style="color: #FFA500; background-color: #FFFFFF;">Weak</p>""",
                Charsets.UTF_8,
            )

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(bakedDir)
            task.reportPath.set("build/reports/a11y-gate.json")
            task.failOnNonCompliant.set(true)

            assertThrows<org.gradle.api.GradleException> {
                task.executeAudit()
            }
        }

        @Test
        fun `does not fail when non compliant and failOnNonCompliant is false`() {
            val project =
                ProjectBuilder
                    .builder()
                    .withProjectDir(auditTempDir)
                    .withName("test-a11y-no-gate")
                    .build()
            project.pluginManager.apply("java-base")

            val bakedDir = auditTempDir.resolve("build/bake").apply { mkdirs() }
            bakedDir.resolve("index.html").writeText(
                """<p style="color: #FFA500; background-color: #FFFFFF;">Weak</p>""",
                Charsets.UTF_8,
            )

            val task = project.tasks.register("accessibilityAudit", AccessibilityAuditTask::class.java).get()
            (task.auditDir as org.gradle.api.file.DirectoryProperty).set(bakedDir)
            task.reportPath.set("build/reports/a11y-no-gate.json")
            task.failOnNonCompliant.set(false)

            task.executeAudit()

            val reportFile = auditTempDir.resolve("build/reports/a11y-no-gate.json")
            assertTrue(reportFile.exists())
            val content = reportFile.readText(Charsets.UTF_8)
            assertThat(content).contains("\"compliant\": false")
        }
    }
}
