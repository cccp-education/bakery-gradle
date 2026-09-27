package bakery.workspace

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * BKY-CI-ISOLATION — the workspace root resolver (US-2a).
 *
 * The three workspace-aware guards (`FixtureAlignmentAuditorTest`,
 * `RealSiteI18nMigrationIntegrationTest`, `BakeryPluginPublicationTest`) must
 * resolve the local workspace through ONE seam so their behaviour is identical:
 * an explicit override (CI / isolated checkout) wins over the local default,
 * and a missing resource degrades to `null` instead of throwing.
 */
class WorkspaceRootTest {
    @Test
    fun `explicit property override wins over the local default`() {
        val resolved =
            WorkspaceRoot.resolve(
                property = { "/tmp/mirror" },
                environment = { "/tmp/env-mirror" },
                defaultRoot = "/home/cheroliv/workspace",
            )

        assertThat(resolved).isEqualTo(File("/tmp/mirror"))
    }

    @Test
    fun `environment override is used when the property is absent`() {
        val resolved =
            WorkspaceRoot.resolve(
                property = { null },
                environment = { "/tmp/env-mirror" },
                defaultRoot = "/home/cheroliv/workspace",
            )

        assertThat(resolved).isEqualTo(File("/tmp/env-mirror"))
    }

    @Test
    fun `local default applies when neither property nor environment is set`() {
        val resolved =
            WorkspaceRoot.resolve(
                property = { null },
                environment = { null },
                defaultRoot = "/home/cheroliv/workspace",
            )

        assertThat(resolved).isEqualTo(File("/home/cheroliv/workspace"))
    }

    @Test
    fun `blank overrides are ignored, never taken as a root`() {
        val resolved =
            WorkspaceRoot.resolve(
                property = { "   " },
                environment = { "" },
                defaultRoot = "/home/cheroliv/workspace",
            )

        assertThat(resolved).isEqualTo(File("/home/cheroliv/workspace"))
    }

    @Test
    fun `site jbake dir resolves under the root`(
        @org.junit.jupiter.api.io.TempDir tempDir: File,
    ) {
        val siteDir = tempDir.resolve("office/sites/cheroliv.com/jbake").apply { mkdirs() }

        assertThat(WorkspaceRoot.siteJbake(tempDir, "cheroliv.com"))
            .isEqualTo(siteDir)
    }

    @Test
    fun `site templates dir resolves under the jbake root`(
        @org.junit.jupiter.api.io.TempDir tempDir: File,
    ) {
        val templatesDir = tempDir.resolve("office/sites/cheroliv.com/jbake/templates").apply { mkdirs() }

        assertThat(WorkspaceRoot.siteTemplates(tempDir, "cheroliv.com"))
            .isEqualTo(templatesDir)
    }

    @Test
    fun `missing site templates degrade to null instead of throwing`(
        @org.junit.jupiter.api.io.TempDir tempDir: File,
    ) {
        assertThat(WorkspaceRoot.siteTemplates(tempDir, "absent.example")).isNull()
    }

    @Test
    fun `missing site jbake degrades to null instead of throwing`(
        @org.junit.jupiter.api.io.TempDir tempDir: File,
    ) {
        assertThat(WorkspaceRoot.siteJbake(tempDir, "absent.example")).isNull()
    }
}
