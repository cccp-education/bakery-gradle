package bakery.contact

import bakery.FileSystemManager
import bakery.SiteConfiguration
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ContactSecConfigTest {

    @Nested
    @DisplayName("defaults")
    inner class Defaults {
        @Test
        fun `defaults are disabled with empty endpoint`() {
            val config = ContactSecConfig()
            assertThat(config.enabled).isFalse
            assertThat(config.endpointUrl).isEqualTo("")
            assertThat(config.firestoreCollection).isEqualTo("contacts")
            assertThat(config.maxHpFields).isEqualTo(1)
            assertThat(config.minRenderTimeMs).isEqualTo(2500)
            assertThat(config.dailyGlobalCap).isEqualTo(50)
            assertThat(config.turnstile).isNull()
            assertThat(config.rateLimit).isNull()
        }

        @Test
        fun `equality is structural`() {
            val a = ContactSecConfig(enabled = true, endpointUrl = "x", turnstile = TurnstileConfig("k"))
            val b = a.copy()
            assertThat(a).isEqualTo(b)
        }
    }

    @Nested
    @DisplayName("Jackson mapping")
    inner class JacksonMapping {
        @Test
        fun `contact section is parsed`() {
            val yaml =
                """
                contact:
                  enabled: true
                  endpointUrl: "https://script.example.com/exec"
                  firestoreCollection: contacts
                  turnstile:
                    siteKey: "0xTESTKEY"
                  rateLimit:
                    perHour: 5
                    perDay: 50
                  dailyGlobalCap: 100
                """.trimIndent()

            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)
            assertThat(config.contact).isNotNull
            val c = config.contact!!
            assertThat(c.enabled).isTrue
            assertThat(c.endpointUrl).isEqualTo("https://script.example.com/exec")
            assertThat(c.turnstile).isNotNull
            assertThat(c.turnstile!!.siteKey).isEqualTo("0xTESTKEY")
            assertThat(c.rateLimit).isNotNull
            assertThat(c.rateLimit!!.perHour).isEqualTo(5)
            assertThat(c.dailyGlobalCap).isEqualTo(100)
        }

        @Test
        fun `absent contact section maps to null`() {
            val yaml = "language: fr"
            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)
            assertThat(config.contact).isNull()
        }

        @Test
        fun `unknown fields are ignored`() {
            val yaml =
                """
                contact:
                  enabled: true
                  unexpected: whatever
                """.trimIndent()
            val config = FileSystemManager.yamlMapper.readValue<SiteConfiguration>(yaml)
            assertThat(config.contact!!.enabled).isTrue
        }
    }
}