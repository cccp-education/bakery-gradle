package bakery.contact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Contact form security configuration (EPIC BKY-CONTACT-SEC).
 *
 * Pure DDD domain type — no I/O, no Gradle coupling. Consumed by the
 * site.yml `contact:` section through Jackson mapping on [bakery.SiteConfiguration].
 *
 * Absent section (null) means: no contact form scaffold at all (backward compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ContactSecConfig(
    val enabled: Boolean = false,
    val endpointUrl: String = "",
    val firestoreCollection: String = "contacts",
    val turnstile: TurnstileConfig? = null,
    val maxHpFields: Int = 1,
    val minRenderTimeMs: Int = 2500,
    val rateLimit: RateLimitConfig? = null,
    val dailyGlobalCap: Int = 50,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TurnstileConfig(
    val siteKey: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RateLimitConfig(
    val perHour: Int = 3,
    val perDay: Int = 30,
)