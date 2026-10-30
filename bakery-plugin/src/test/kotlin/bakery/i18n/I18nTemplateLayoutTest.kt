package bakery.i18n

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-OUT-1 — the single layout rule shared by every per-language Thymeleaf
 * file (menu, header): the reference lives in `templates/`, a variant in
 * `i18n/{lang}/templates/`, with the legacy `{lang}/templates/` still probed.
 *
 * The split-brain this kills: `injectLangSwitch` knew `i18n/{lang}/` while
 * `injectSeo` did not, so the same site layout was read two different ways.
 */
class I18nTemplateLayoutTest {

    @Test
    fun `the reference lives in the templates directory`() {
        assertThat(I18nTemplateLayout.relativeCandidates("header.thyme", "fr", "fr"))
            .containsExactly("templates/header.thyme")
    }

    @Test
    fun `a variant probes the i18n tree before the legacy tree`() {
        assertThat(I18nTemplateLayout.relativeCandidates("header.thyme", "en", "fr"))
            .containsExactly("i18n/en/templates/header.thyme", "en/templates/header.thyme")
    }

    @Test
    fun `the i18n layout wins when both exist`(
        @TempDir tempDir: File,
    ) {
        val i18n = tempDir.resolve("i18n/en/templates/header.thyme")
        val legacy = tempDir.resolve("en/templates/header.thyme")
        i18n.parentFile.mkdirs()
        legacy.parentFile.mkdirs()
        i18n.writeText("i18n")
        legacy.writeText("legacy")

        assertThat(I18nTemplateLayout.resolve(tempDir, "header.thyme", "en", "fr")?.readText())
            .isEqualTo("i18n")
    }

    @Test
    fun `a language without the file resolves to null`(
        @TempDir tempDir: File,
    ) {
        assertThat(I18nTemplateLayout.resolve(tempDir, "header.thyme", "en", "fr")).isNull()
    }

    @Test
    fun `a blank language is rejected`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            I18nTemplateLayout.relativeCandidates("header.thyme", " ", "fr")
        }
    }
}
