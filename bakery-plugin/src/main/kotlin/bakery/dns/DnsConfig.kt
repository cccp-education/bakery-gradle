package bakery.dns

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * DNS provisioning configuration (EPIC BKY-DNS).
 *
 * Pure DDD domain type — no I/O, no Gradle coupling. Consumed by the
 * site.yml `dns:` section, gradle.properties and CLI flags through
 * [DnsConfigMerger].
 *
 * Absent section (null) means: no provisioning at all (backward compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DnsConfig(
    /** DNS provider handle, e.g. `ovh`. */
    val provider: String = "ovh",
    /** Zone to manage, e.g. `talaria.school`. */
    val domain: String = "",
    /** Desired records (apex A x4 GitHub Pages IPs, www CNAME). */
    val records: List<DnsRecord> = emptyList(),
    /** When true, only validate/dry-run — never mutate the zone. */
    val dryRun: Boolean = true,
    /** OVH API credentials (gitignored, masked on toString). */
    val credentials: OvhCredentials = OvhCredentials(),
)