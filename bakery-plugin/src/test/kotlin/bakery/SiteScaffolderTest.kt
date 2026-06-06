package bakery

import bakery.SiteScaffolder.resolveAndValidateSiteTarget
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

    @Nested
    inner class SiteTypeTest {

        @Test
        fun `fromString should return BLOG for blog`() {
            assertThat(SiteType.fromString("blog")).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `fromString should return BASIC for basic`() {
            assertThat(SiteType.fromString("basic")).isEqualTo(SiteType.BASIC)
        }

        @Test
        fun `fromString should be case insensitive`() {
            assertThat(SiteType.fromString("BLOG")).isEqualTo(SiteType.BLOG)
            assertThat(SiteType.fromString("BASIC")).isEqualTo(SiteType.BASIC)
        }

        @Test
        fun `fromString should throw for unknown type`() {
            assertThatThrownBy { SiteType.fromString("unknown") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("inconnu")
        }

        @Test
        fun `fromStringOrDefault should return BLOG for null`() {
            assertThat(SiteType.fromStringOrDefault(null)).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `fromStringOrDefault should return BLOG for blank`() {
            assertThat(SiteType.fromStringOrDefault("")).isEqualTo(SiteType.BLOG)
            assertThat(SiteType.fromStringOrDefault("   ")).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `fromStringOrDefault should return BLOG for unknown type`() {
            assertThat(SiteType.fromStringOrDefault("nonsense")).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `fromStringOrDefault should resolve valid types`() {
            assertThat(SiteType.fromStringOrDefault("basic")).isEqualTo(SiteType.BASIC)
        }

        @Test
        fun `alias should return correct string`() {
            assertThat(SiteType.BLOG.alias).isEqualTo("blog")
            assertThat(SiteType.BASIC.alias).isEqualTo("basic")
        }
    }

    @Nested
    inner class ResolveAndValidateSiteTargetTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `should resolve and validate when both sitesBaseDir and siteName are set`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            project.extensions.add("bakery", mockBakeryExtension("office/sites", "my-company", null))
            val target = project.resolveAndValidateSiteTarget()
            assertThat(target).isEqualTo(tempDir.resolve("office/sites/my-company"))
            assertThat(target.exists()).isFalse
        }

        @Test
        fun `should resolve to siteName without validation when only siteName is set`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            project.extensions.add("bakery", mockBakeryExtension(null, "mysite", null))
            val target = project.resolveAndValidateSiteTarget()
            assertThat(target).isEqualTo(tempDir.resolve("mysite"))
        }

        @Test
        fun `should throw when sitesBaseDir is set but siteName is absent`() {
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            project.extensions.add("bakery", mockBakeryExtension("office/sites", null, null))
            assertThatThrownBy { project.resolveAndValidateSiteTarget() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("siteName must be defined")
        }
    }

    @Nested
    inner class ResolveSiteTypeTest {

        @TempDir
        lateinit var tempDir: File

        @Test
        fun `resolveSiteType should return BLOG by default`() {
            val ext = mockBakeryExtension(null, null, null)
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `resolveSiteType should return BLOG when siteType is blank`() {
            val ext = mockBakeryExtension(null, null, "")
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `resolveSiteType should return BASIC when siteType is basic`() {
            val ext = mockBakeryExtension(null, null, "basic")
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BASIC)
        }

        @Test
        fun `resolveSiteType should be case insensitive`() {
            val ext = mockBakeryExtension(null, null, "BASIC")
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BASIC)
        }

        @Test
        fun `resolveSiteType should fallback to BLOG for unknown type`() {
            val ext = mockBakeryExtension(null, null, "nonsense")
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BLOG)
        }

        @Test
        fun `resolveSiteType should return BLOG when siteType property is null`() {
            val ext = mockBakeryExtension(null, null, null)
            org.mockito.kotlin.whenever(ext.siteType).thenReturn(null)
            assertThat(SiteScaffolder.resolveSiteType(ext)).isEqualTo(SiteType.BLOG)
        }
    }

    companion object {
        private fun bakeryExtension(sitesBaseDir: String?, siteName: String?): BakeryExtension =
            mockBakeryExtension(sitesBaseDir, siteName, null)

        @Suppress("UNCHECKED_CAST")
        private fun mockBakeryExtension(
            sitesBaseDir: String?,
            siteName: String?,
            siteType: String?
        ): BakeryExtension {
            val ext = org.mockito.kotlin.mock<BakeryExtension>()
            val sbMock = org.mockito.kotlin.mock<org.gradle.api.provider.Property<String>>()
            val snMock = org.mockito.kotlin.mock<org.gradle.api.provider.Property<String>>()
            val stMock = org.mockito.kotlin.mock<org.gradle.api.provider.Property<String>>()
            org.mockito.kotlin.whenever(sbMock.orNull).thenReturn(sitesBaseDir)
            org.mockito.kotlin.whenever(snMock.orNull).thenReturn(siteName)
            org.mockito.kotlin.whenever(stMock.orNull).thenReturn(siteType)
            org.mockito.kotlin.whenever(ext.sitesBaseDir).thenReturn(sbMock)
            org.mockito.kotlin.whenever(ext.siteName).thenReturn(snMock)
            org.mockito.kotlin.whenever(ext.siteType).thenReturn(stMock)
            return ext
        }
    }
}
