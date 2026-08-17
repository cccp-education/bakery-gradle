package bakery.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ContactSecRendererTest {

    private val renderer = ContactSecRenderer()

    private val configWithTurnstile =
        ContactSecConfig(
            enabled = true,
            endpointUrl = "https://script.example.com/exec",
            firestoreCollection = "contacts",
            turnstile = TurnstileConfig(siteKey = "0xTESTKEY"),
            minRenderTimeMs = 2500,
            dailyGlobalCap = 50,
        )

    private val configNoTurnstile =
        ContactSecConfig(
            enabled = true,
            endpointUrl = "https://script.example.com/exec",
            firestoreCollection = "contacts",
            turnstile = null,
        )

    @Nested
    @DisplayName("footer fragment")
    inner class FooterFragment {
        @Test
        fun `renders form with correct endpoint action`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("action=\"https://script.example.com/exec\"")
            assertThat(fragment).contains("method=\"post\"")
        }

        @Test
        fun `renders honeypot hp_name hidden field`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("name=\"hp_name\"")
            assertThat(fragment).contains("display:none")
        }

        @Test
        fun `renders session_token hidden field`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("name=\"session_token\"")
        }

        @Test
        fun `renders ts_render and fp hidden fields`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("name=\"ts_render\"")
            assertThat(fragment).contains("name=\"fp\"")
        }

        @Test
        fun `renders pow_nonce and pow_challenge hidden fields`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("name=\"pow_nonce\"")
            assertThat(fragment).contains("name=\"pow_challenge\"")
        }

        @Test
        fun `renders turnstile div when siteKey is configured`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("cf-turnstile")
            assertThat(fragment).contains("data-sitekey=\"0xTESTKEY\"")
        }

        @Test
        fun `renders placeholder when turnstile is not configured`() {
            val fragment = renderer.renderFooterFragment(configNoTurnstile)
            assertThat(fragment).contains("turnstile disabled")
            assertThat(fragment).doesNotContain("data-sitekey")
        }

        @Test
        fun `contains CONTACT-SEC bakery markers for idempotent injection`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("<!-- CONTACT-SEC: bakery -->")
            assertThat(fragment).contains("<!-- /CONTACT-SEC: bakery -->")
        }

        @Test
        fun `renders submit button disabled by default`() {
            val fragment = renderer.renderFooterFragment(configWithTurnstile)
            assertThat(fragment).contains("disabled")
            assertThat(fragment).contains("contact-submit")
        }
    }

    @Nested
    @DisplayName("contact.js")
    inner class ContactJs {
        @Test
        fun `renders JS with fingerprint function`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("fingerprint")
            assertThat(js).contains("navigator.userAgent")
        }

        @Test
        fun `renders JS with PoW solver`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("solvePow")
            assertThat(js).contains("sha256")
            assertThat(js).contains("pow_nonce")
        }

        @Test
        fun `renders JS with turnstile getResponse call`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("turnstile.getResponse")
        }

        @Test
        fun `renders JS with honeypot check`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("hp_name")
            assertThat(js).contains("if (hpName.value) return")
        }

        @Test
        fun `renders JS with minRenderTimeMs check`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("minRenderTimeMs")
            assertThat(js).contains("2500")
        }

        @Test
        fun `renders JS with fetch POST to form action`() {
            val js = renderer.renderContactJs(configWithTurnstile)
            assertThat(js).contains("fetch(form.action")
            assertThat(js).contains("POST")
        }
    }

    @Nested
    @DisplayName("firestore rules")
    inner class FirestoreRules {
        @Test
        fun `generates rules with correct collection name`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("match /contacts/{docId}")
        }

        @Test
        fun `allows create only`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("allow create:")
            assertThat(rules).contains("allow read, update, delete: if false;")
        }

        @Test
        fun `honeypot must be empty`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("hp_name == ''")
        }

        @Test
        fun `created_at must match request time`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("created_at == request.time")
        }

        @Test
        fun `whitelisted fields are enforced`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("name")
            assertThat(rules).contains("email")
            assertThat(rules).contains("subject")
            assertThat(rules).contains("message")
            assertThat(rules).contains("session_token")
        }

        @Test
        fun `length caps are enforced`() {
            val rules = renderer.renderFirestoreRules(configWithTurnstile)
            assertThat(rules).contains("name.size() <= 200")
            assertThat(rules).contains("message.size() <= 5000")
        }
    }
}