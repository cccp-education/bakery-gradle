package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BakedLanguagesTest {
    @TempDir
    lateinit var bakeRoot: File

    private fun write(
        relative: String,
        content: String,
    ): File =
        bakeRoot
            .resolve(relative)
            .also {
                it.parentFile.mkdirs()
                it.writeText(content)
            }

    @Test
    fun `discovers the reference and every variant carrying an index`() {
        write("index.html", "<html lang=\"fr\"><body/></html>")
        write("en/index.html", "<html lang=\"en\"><body/></html>")
        write("ja/index.html", "<html lang=\"ja\"><body/></html>")

        assertThat(BakedLanguages.discover(bakeRoot)).containsExactly("en", "fr", "ja")
    }

    @Test
    fun `ignores a directory without an index`() {
        write("index.html", "<html lang=\"fr\"></html>")
        bakeRoot.resolve("assets").mkdirs()

        assertThat(BakedLanguages.discover(bakeRoot)).containsExactly("fr")
    }

    @Test
    fun `lowercases a regional language code`() {
        write("pt/index.html", "<html lang=\"pt-BR\"></html>")

        assertThat(BakedLanguages.langOf(bakeRoot.resolve("pt/index.html"))).isEqualTo("pt-br")
    }

    @Test
    fun `a missing index is unknown`() {
        assertThat(BakedLanguages.langOf(bakeRoot.resolve("missing.html"))).isNull()
    }
}
