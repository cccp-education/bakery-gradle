package bakery.seo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * SEO injection configuration (EPIC BKY-SEO).
 *
 * Pure DDD domain type — no I/O, no Gradle coupling. Consumed by the
 * site.yml `seo:` section through Jackson mapping on [bakery.SiteConfiguration].
 *
 * Absent section (null) means: no SEO injection at all (backward compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SeoConfig(
    /** Site display name used in JSON-LD WebSite and OG tags. */
    val siteName: String = "",
    /** Brand name (may differ from siteName). */
    val brand: String = "",
    /** Default Open Graph image (absolute or root-relative) used when a page has no derived image. */
    val defaultOgImage: String = "",
    /** Twitter @handle for Twitter Card tags (optional). */
    val twitterHandle: String? = null,
    /** Canonical website URL (optional, used as base for canonical/OG resolution). */
    val websiteUrl: String? = null,
    /** Person metadata for JSON-LD Person node (optional). */
    val person: Person? = null,
    /** Default content language for JSON-LD `inLanguage` (ISO 639-1). */
    val inLanguage: String = "fr",
)

/**
 * Person metadata for JSON-LD `@graph` Person node.
 *
 * Pure DDD domain type — mirrors the cheroliv.com header.thyme JSON-LD
 * structure but parametrized by config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Person(
    /** Full display name. */
    val name: String = "",
    /** Canonical URL of the person/author page (optional). */
    val url: String? = null,
    /** Job title (optional). */
    val jobTitle: String? = null,
    /** `sameAs` list of profile URLs (GitHub, Twitter, LinkedIn, etc.). */
    val sameAs: List<String> = emptyList(),
)