package bakery

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.text.Charsets.UTF_8

class SiteScaffolderTest {

    @Nested
    inner class ResolveSiteTargetDirTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `should resolve to baseDir siteName when both are defined`() {
            val target = SiteScaffolder.resolveSiteTargetDir(
                bakeryExtension("office/sites", "my-company"),
                tempDir
            )
            assertThat(target).isEqualTo(tempDir.resolve("office/sites/my-company"))
        }

        @Test
        fun `should resolve to projectDir siteName when only siteName is defined`() {
            val target = SiteScaffolder.resolveSiteTargetDir(
                bakeryExtension(null, "mysite"),
                tempDir
            )
            assertThat(target).isEqualTo(tempDir.resolve("mysite"))
        }

        @Test
        fun `should throw when sitesBaseDir is defined but siteName is absent`() {
            assertThatThrownBy {
                SiteScaffolder.resolveSiteTargetDir(
                    bakeryExtension("office/sites", null),
                    tempDir
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("siteName must be defined")
        }

        @Test
        fun `should fall back to projectDir when neither is defined`() {
            val target = SiteScaffolder.resolveSiteTargetDir(
                bakeryExtension(null, null),
                tempDir
            )
            assertThat(target).isEqualTo(tempDir)
        }

        @Test
        fun `should resolve with nested sites base directory`() {
            val target = SiteScaffolder.resolveSiteTargetDir(
                bakeryExtension("a/b/c/sites", "deep-site"),
                tempDir
            )
            assertThat(target).isEqualTo(tempDir.resolve("a/b/c/sites/deep-site"))
        }
    }

    @Nested
    inner class ValidateSiteTargetDoesNotExistTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `should pass when target directory does not exist`() {
            val target = tempDir.resolve("new-site")
            SiteScaffolder.validateSiteTargetDoesNotExist(target)
        }

        @Test
        fun `should throw when target directory already exists`() {
            val target = tempDir.resolve("existing-site")
            target.mkdirs()

            assertThatThrownBy {
                SiteScaffolder.validateSiteTargetDoesNotExist(target)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("already exists")
        }

        @Test
        fun `should pass when target is a file that does not exist`() {
            val target = tempDir.resolve("not-a-dir")
            SiteScaffolder.validateSiteTargetDoesNotExist(target)
        }
    }

    @Nested
    inner class BakeryExtensionScaffoldingPropertiesTest {

        @TempDir
        lateinit var tempDir: File

        private lateinit var project: Project

        @BeforeEach
        fun setUp() {
            project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        }

        @Test
        fun `extension should have sitesBaseDir property`() {
            val extension = project.extensions.create("bakery_test", BakeryExtension::class.java) as BakeryExtension
            val sitesBaseDir: Property<String> = extension.sitesBaseDir
            assertThat(sitesBaseDir).isNotNull
            assertThat(sitesBaseDir.isPresent).isFalse
            assertThat(sitesBaseDir.orNull).isNull()
        }

        @Test
        fun `extension should have siteName property`() {
            val extension = project.extensions.create("bakery_test2", BakeryExtension::class.java) as BakeryExtension
            val siteName: Property<String> = extension.siteName
            assertThat(siteName).isNotNull
            assertThat(siteName.isPresent).isFalse
            assertThat(siteName.orNull).isNull()
        }
    }

    companion object {
        private fun bakeryExtension(sitesBaseDir: String?, siteName: String?): BakeryExtension =
            mockBakeryExtension(sitesBaseDir, siteName)

        @Suppress("UNCHECKED_CAST")
        private fun mockBakeryExtension(sitesBaseDir: String?, siteName: String?): BakeryExtension {
            val ext = org.mockito.kotlin.mock<BakeryExtension>()
            val sbMock = org.mockito.kotlin.mock<org.gradle.api.provider.Property<String>>()
            val snMock = org.mockito.kotlin.mock<org.gradle.api.provider.Property<String>>()
            org.mockito.kotlin.whenever(sbMock.orNull).thenReturn(sitesBaseDir)
            org.mockito.kotlin.whenever(snMock.orNull).thenReturn(siteName)
            org.mockito.kotlin.whenever(ext.sitesBaseDir).thenReturn(sbMock)
            org.mockito.kotlin.whenever(ext.siteName).thenReturn(snMock)
            return ext
        }
    }
}
