package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-4.
 *
 * Architecture guard: the client-side i18n translation must reach the model
 * only through the N0 [contracts.i18n.TranslationService] port — the domain
 * `bakery.i18n.js` never calls an LLM over HTTP directly (the talaria node
 * harness `tools/translate-i18n.mjs` did, and that is the debt this EPIC
 * removes).
 *
 * The guard scans the production sources of the domain and fails on any direct
 * HTTP surface (`/api/generate`, `java.net.http`, an HTTP client class…). It
 * is a text-level check — the domain has no network dependency at all.
 */
class I18nClientNoDirectHttpBoundaryTest {
    private val domainDir =
        File(System.getProperty("user.dir")).absoluteFile.resolve("src/main/kotlin/bakery/i18n/js")

    private val forbiddenTokens =
        listOf(
            "api/generate",
            "java.net.http",
            "java.net.URL",
            "HttpURLConnection",
            "HttpClient",
            "okhttp3",
            "fetch(",
            "localhost:114",
        )

    @Test
    fun `the domain sources exist`() {
        assertThat(domainDir).isDirectory()
        assertThat(domainDir.listFiles { file -> file.extension == "kt" }).isNotEmpty()
    }

    @Test
    fun `no domain source reaches an LLM over HTTP directly`() {
        val offenders =
            domainDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    val content = file.readText(UTF_8).lowercase()
                    forbiddenTokens
                        .filter { token -> content.contains(token.lowercase()) }
                        .map { token -> "${file.name} → $token" }
                }.toList()

        assertThat(offenders)
            .withFailMessage("Direct HTTP surface in bakery.i18n.js (LLM must stay behind the N0 port): $offenders")
            .isEmpty()
    }

    @Test
    fun `the translation reaches the model only through the N0 port`() {
        val task = domainDir.resolve("TranslateI18nClientTask.kt").readText(UTF_8)

        assertThat(task).contains("contracts.i18n.TranslationService")
        assertThat(task).contains("contracts.i18n.TranslationRequest")
        assertThat(task).contains("translationService.translate(")
    }
}
