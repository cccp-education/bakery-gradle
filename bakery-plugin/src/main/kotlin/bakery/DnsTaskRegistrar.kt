package bakery

import bakery.dns.ProvisionDnsTask
import org.gradle.api.Project

/**
 * Registers DNS provisioning tasks (EPIC BKY-DNS-4).
 *
 * `provisionDns` is only registered when the site.yml `dns:` section is
 * present (backward compat — absent section = no provisioning, no task).
 * The task is chained before `deploySite` so a deployment never ships
 * before the zone matches the desired state.
 */
object DnsTaskRegistrar {
    internal fun Project.registerProvisionDnsTask(site: SiteConfiguration) {
        val dns = site.dns ?: return

        tasks.register("provisionDns", ProvisionDnsTask::class.java) { task ->
            task.group = BakeryConstants.DEPLOY_GROUP
            task.description = "Reconcile DNS zone records (OVH) against the desired state (dry-run by default)"
            task.providerName.set(dns.provider)
            task.domain.set(dns.domain)
            task.records.set(dns.records)
            task.dryRun.set(dns.dryRun)
            task.allowDelete.set(false)
            task.credentials.set(dns.credentials)
        }

        tasks.named("deploySite") { it.dependsOn("provisionDns") }
    }
}