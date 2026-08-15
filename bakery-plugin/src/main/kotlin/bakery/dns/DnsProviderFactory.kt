package bakery.dns

/**
 * Resolves a [DnsProvider] from its handle (EPIC BKY-DNS-3).
 *
 * Pattern capsule `resolveAudioPostProcessor` (CapsuleManager): an
 * unknown or unavailable provider degrades to [NoOpDnsProvider] rather
 * than throwing. The OVH adapter is only returned when a real
 * [OvhDnsClient] is supplied (i.e. OVH credentials configured).
 */
object DnsProviderFactory {

    fun resolve(name: String, ovhClient: OvhDnsClient? = null): DnsProvider =
        when (name) {
            "ovh" -> ovhClient?.let { OvhDnsProvider(it) } ?: NoOpDnsProvider()
            else -> NoOpDnsProvider()
        }
}
