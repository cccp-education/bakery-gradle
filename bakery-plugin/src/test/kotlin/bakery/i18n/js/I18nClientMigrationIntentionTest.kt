package bakery.i18n.js

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-I18N-JS — US BKY-I18N-JS-3.
 *
 * Domain invariants of the `translateI18nClient` intention.
 */
class I18nClientMigrationIntentionTest {
    @Test
    fun `holds the source dirs reference language and targets`() {
        val intention =
            I18nClientMigrationIntention(
                sourceDirs = listOf("maquette/js"),
                referenceLanguage = "fr",
                targetLanguages = listOf("en", "it"),
                dryRun = false,
            )

        assertThat(intention.sourceDirs).containsExactly("maquette/js")
        assertThat(intention.referenceLanguage).isEqualTo("fr")
        assertThat(intention.targetLanguages).containsExactly("en", "it")
        assertThat(intention.dryRun).isFalse()
    }

    @Test
    fun `rejects an empty source dir list`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(sourceDirs = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a blank source dir`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(sourceDirs = listOf(" "))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects an unsupported reference language`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(sourceDirs = listOf("js"), referenceLanguage = "xx")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects an unsupported target language`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(sourceDirs = listOf("js"), targetLanguages = listOf("xx"))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects an empty target list`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(sourceDirs = listOf("js"), targetLanguages = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects the reference language as a target`() {
        assertThatThrownBy {
            I18nClientMigrationIntention(
                sourceDirs = listOf("js"),
                referenceLanguage = "fr",
                targetLanguages = listOf("fr", "en"),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
