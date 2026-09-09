package bakery.i18n.path

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ContentSourcePathResolverTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `absolute source path is used as-is`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "content", "/tmp/external/blog")

        assertThat(resolved).isEqualTo(File("/tmp/external/blog"))
        assertThat(resolved.isAbsolute).isTrue()
    }

    @Test
    fun `relative source path without the srcPath prefix resolves against the content root`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "content", "blog/2026")

        assertThat(resolved).isEqualTo(projectDir.resolve("content/blog/2026"))
    }

    @Test
    fun `relative source path prefixed with the content root does not double the prefix`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "content", "content/blog/2026")

        assertThat(resolved).isEqualTo(projectDir.resolve("content/blog/2026"))
    }

    @Test
    fun `source path equal to the content root resolves to the content root`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "content", "content")

        assertThat(resolved).isEqualTo(projectDir.resolve("content"))
    }

    @Test
    fun `blank srcPath resolves the relative source against the project dir`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "", "blog")

        assertThat(resolved).isEqualTo(projectDir.resolve("blog"))
    }

    @Test
    fun `nested srcPath segments are matched as a whole prefix`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "jbake/content", "jbake/content/blog/2025")

        assertThat(resolved).isEqualTo(projectDir.resolve("jbake/content/blog/2025"))
    }

    @Test
    fun `a partial segment of the srcPath is not treated as a prefix`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "jb", "jbake/content")

        assertThat(resolved).isEqualTo(projectDir.resolve("jb/jbake/content"))
    }

    @Test
    fun `legacy content root relative path is preserved when srcPath differs`() {
        val resolved = ContentSourcePathResolver.resolve(projectDir, "jbake", "content/blog")

        assertThat(resolved).isEqualTo(projectDir.resolve("jbake/content/blog"))
    }
}