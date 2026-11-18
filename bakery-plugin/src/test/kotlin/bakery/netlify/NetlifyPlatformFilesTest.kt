package bakery.netlify

import bakery.deploy.DeployPayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipInputStream

/**
 * EPIC BKY-NETLIFY (US-5/US-7) — deterministic archive and platform files.
 */
class NetlifyPlatformFilesTest {
    @TempDir
    lateinit var root: File

    private fun file(
        path: String,
        content: String,
    ) {
        root.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    @Test
    @DisplayName("the archive contains every payload file, deterministically")
    fun archiveIsDeterministic() {
        file("index.html", "home")
        file("en/index.html", "home en")
        val files = DeployPayload.collect(root)

        val first = NetlifyArchive.build(root, files)
        val second = NetlifyArchive.build(root, files)

        assertThat(first).isEqualTo(second)
        val entries = mutableListOf<String>()
        ZipInputStream(first.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += entry.name
                entry = zip.nextEntry
            }
        }
        assertThat(entries).containsExactly("en/index.html", "index.html")
    }

    @Test
    @DisplayName("netlify.toml is minimal and deterministic")
    fun tomlRendering() {
        val toml = NetlifyTomlRenderer.renderToml(NetlifyTomlOptions(enabled = true, publishDir = "public"), "fallback")

        assertThat(toml).contains("[build]")
        assertThat(toml).contains("publish = \"public\"")
        assertThat(toml).doesNotContain("[[redirects]]")
        assertThat(NetlifyTomlRenderer.renderToml(NetlifyTomlOptions(enabled = true), "fallback"))
            .contains("publish = \"fallback\"")
    }

    @Test
    @DisplayName("redirects are opt-in")
    fun redirectsAreOptIn() {
        val withRedirects =
            NetlifyTomlRenderer.renderToml(
                NetlifyTomlOptions(enabled = true, redirects = true, notFound = "/404.html"),
                "public",
            )

        assertThat(withRedirects).contains("[[redirects]]")
        assertThat(withRedirects).contains("to = \"/404.html\"")
        assertThat(NetlifyTomlRenderer.renderRedirects("/404.html")).isEqualTo("/*  /404.html  404\n")
    }

    @Test
    @DisplayName("security headers are rendered as a Netlify _headers file")
    fun headers() {
        val headers = NetlifyTomlRenderer.renderHeaders()

        assertThat(headers).startsWith("/*")
        assertThat(headers).contains("X-Content-Type-Options: nosniff")
        assertThat(headers).contains("X-Frame-Options: DENY")
    }
}
