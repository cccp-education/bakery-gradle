package bakery.dns

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task exposing [DnsProvisioner] to the build (EPIC BKY-DNS-4).
 *
 * Reads the merged [DnsConfig] (site.yml dns: section) and reconciles the
 * zone through the provider resolved by [DnsProviderFactory]. The OVH
 * adapter is only wired when credentials are complete — otherwise the
 * factory degrades to [NoOpDnsProvider], making CI runs a guaranteed
 * dry-run no-op.
 *
 * Safety (backlog): dry-run by default (never mutates without an
 * explicit `dns.dryRun=false`), orphans never deleted unless
 * `--dns.allowDelete` is passed.
 *
 * Secrets policy: credentials are a non-hashed [Internal] input — they
 * never participate in up-to-date checks or build cache fingerprints.
 */
@DisableCachingByDefault(because = "DNS reconciliation — side effect on the live zone")
abstract class ProvisionDnsTask : DefaultTask() {

    /** Provider handle, e.g. `ovh`. */
    @get:Input
    abstract val providerName: Property<String>

    /** Zone to manage, e.g. `talaria.school`. */
    @get:Input
    abstract val domain: Property<String>

    /** Desired records. */
    @get:Input
    abstract val records: ListProperty<DnsRecord>

    /** When true, only validate — never mutate the zone. */
    @get:Input
    abstract val dryRun: Property<Boolean>

    /** When true, orphans are deleted (purge flag). */
    @get:Input
    abstract val allowDelete: Property<Boolean>

    /** OVH credentials — never hashed, never logged. */
    @get:Internal
    abstract val credentials: Property<OvhCredentials>

    @TaskAction
    fun provision() {
        val name = providerName.get()
        val zone = domain.get()
        if (zone.isBlank()) {
            logger.warn("[dns] domain is blank — skipping provisioning")
            return
        }

        val ovhClient =
            credentials
                .orNull
                ?.takeIf { it.isComplete() }
                ?.let { OvhDnsClient(it, JavaOvhHttp(it)) }
        val provider = DnsProviderFactory.resolve(name, ovhClient)
        val dryRunValue = dryRun.get()
        val result =
            DnsProvisioner(provider, zone)
                .reconcile(
                    desired = records.get(),
                    dryRun = dryRunValue,
                    allowDelete = allowDelete.get(),
                )

        logger.lifecycle(
            "[dns] provider={} domain={} dryRun={} noop={}",
            provider.name(),
            zone,
            if (dryRunValue) "true" else "false",
            result.noop,
        )
        logger.lifecycle(
            "[dns] plan={} applied={} skipped={}",
            result.plan.size,
            result.applied.size,
            result.skipped.size,
        )
        if (dryRunValue) {
            logger.lifecycle("[dns] dry-run — no change applied to the zone")
        }
        result.plan.forEach { change ->
            val action =
                when (change) {
                    is DnsChange.Create -> "create"
                    is DnsChange.Update -> "update"
                    is DnsChange.Delete -> "delete"
                }
            logger.lifecycle(
                "[dns]   {} {} {} -> {} (ttl {})",
                action,
                change.record.type,
                change.record.name,
                change.record.value,
                change.record.ttl,
            )
        }
    }
}

private fun OvhCredentials.isComplete(): Boolean =
    listOf(applicationKey, applicationSecret, consumerKey).all { it.isNotBlank() }