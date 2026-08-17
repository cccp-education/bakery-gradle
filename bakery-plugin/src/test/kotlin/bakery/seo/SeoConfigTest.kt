package bakery.seo

import bakery.FileSystemManager
import bakery.SiteConfiguration
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EPIC BKY-SEO-1 — Unit tests for the SEO domain (bakery.seo).
 *
 * Covers SeoConfig / Person defaults and the Jackson mapping of the `seo:`
 * section in site.yml. Mirrors the DnsConfigTest pattern (BKY-DNS-1).
 *
 * Methodology: DDD/TDD baby steps — each test compiles AND passes before
 * moving to the next one.
 */
class SeoConfigTest {

    @Nested
    @DisplayName("SeoConfig defaults")
    inner class SeoConfigDefaults {
        @Test
        @DisplayName("defaults: empty siteName/brand, defaultOgImage, inLanguage fr")
        fun `defaults are empty strings and fr inLanguage`() {
            val config = SeoConfig()

            assertThat(config.siteName).isEqualTo("")
            assertThat(config.brand).isEqualTo("")
            assertThat(config.defaultOgImage).isEqualTo("")
            assertThat(config.twitterHandle).isNull()
            assertThat(config.websiteUrl).isNull()
            assertThat(config.person).isNull()
            assertThat(config.inLanguage).isEqualTo("fr")
        }

        @Test
        @DisplayName("equality is structural")
        fun `equality is structural`() {
            val a =
                SeoConfig(
                    siteName = "Example",
                    brand = "Example",
                    defaultOgImage = "example-default.png",
                    twitterHandle = "@example",
                    websiteUrl = "https://example.com",
                    person = Person(name = "Jane", url = "https://example.com/about"),
                    inLanguage = "en",
                )
            val b = a.copy()

            assertThat(a).isEqualTo(b)
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
        }
    }

    @Nested
    @DisplayName("Person defaults")
    inner class PersonDefaults {
        @Test
        @DisplayName("default Person has empty name, null url/jobTitle, empty sameAs")
        fun `default person has empty fields`() {
            val person = Person()

            assertThat(person.name).isEqualTo("")
            assertThat(person.url).isNull()
            assertThat(person.jobTitle).isNull()
            assertThat(person.sameAs).isEmpty()
        }

        @Test
        @DisplayName("Person with sameAs list is preserved")
        fun `person with sameAs list is preserved`() {
            val person =
                Person(
                    name = "Jane Doe",
                    url = "https://example.com/about",
                    jobTitle = "Software Engineer",
                    sameAs = listOf("https://github.com/jane", "https://twitter.com/jane"),
                )

            assertThat(person.name).isEqualTo("Jane Doe")
            assertThat(person.sameAs).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Jackson mapping — seo: section in site.yml")
    inner class JacksonMapping {
        @Test
        @DisplayName("seo section with all fields is parsed into SeoConfig")
        fun `seo section is parsed`() {
            val yaml =
                """
                language: fr
                supportedLanguages: [fr, en]
                seo:
                  siteName: Example
                  brand: Example
                  defaultOgImage: example-default.png
                  twitterHandle: "@example"
                  websiteUrl: "https://example.com"
                  inLanguage: fr
                  person:
                    name: Jane Doe
                    url: "https://example.com/about"
                    jobTitle: Software Engineer
                    sameAs:
                      - "https://github.com/jane"
                      - "https://twitter.com/jane"
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.seo).isNotNull
            val seo = config.seo!!
            assertThat(seo.siteName).isEqualTo("Example")
            assertThat(seo.brand).isEqualTo("Example")
            assertThat(seo.defaultOgImage).isEqualTo("example-default.png")
            assertThat(seo.twitterHandle).isEqualTo("@example")
            assertThat(seo.websiteUrl).isEqualTo("https://example.com")
            assertThat(seo.inLanguage).isEqualTo("fr")
            assertThat(seo.person).isNotNull
            assertThat(seo.person!!.name).isEqualTo("Jane Doe")
            assertThat(seo.person!!.url).isEqualTo("https://example.com/about")
            assertThat(seo.person!!.jobTitle).isEqualTo("Software Engineer")
            assertThat(seo.person!!.sameAs).hasSize(2)
            assertThat(seo.person!!.sameAs[0]).isEqualTo("https://github.com/jane")
        }

        @Test
        @DisplayName("seo section without optional fields is parsed with nulls")
        fun `seo section without optionals is parsed`() {
            val yaml =
                """
                seo:
                  siteName: Example
                  brand: Example
                  defaultOgImage: example-default.png
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.seo).isNotNull
            assertThat(config.seo!!.twitterHandle).isNull()
            assertThat(config.seo!!.websiteUrl).isNull()
            assertThat(config.seo!!.person).isNull()
            assertThat(config.seo!!.inLanguage).isEqualTo("fr")
        }

        @Test
        @DisplayName("absent seo section maps to null (backward compat, no injection)")
        fun `absent seo section maps to null`() {
            val yaml =
                """
                language: fr
                bake:
                  cname: example.com
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.seo).isNull()
        }

        @Test
        @DisplayName("unknown fields inside seo section are ignored")
        fun `unknown fields inside seo section are ignored`() {
            val yaml =
                """
                seo:
                  siteName: Example
                  brand: Example
                  defaultOgImage: example-default.png
                  unexpectedField: whatever
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)

            assertThat(config.seo).isNotNull
            assertThat(config.seo!!.siteName).isEqualTo("Example")
        }
    }
}