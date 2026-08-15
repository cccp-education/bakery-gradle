package bakery.dns

/**
 * Merges DNS configuration from the 4 sources
 * (CLI > YAML > Props > ENV).
 *
 * Pattern: capsule `mergeAudioPostConfig` (CapsuleConfigMerger). String
 * fields use isNotBlank() as the "explicitly set" heuristic — an explicit
 * blank never overrides a non-blank lower-priority source. Boolean fields
 * have no "blank" concept: YAML/props always beat ENV. Records are a
 * native YAML list: CLI cannot express them (YAGNI), YAML wins over
 * props/ENV, empty lists fall through.
 */
object DnsConfigMerger {

    fun merge(
        env: DnsConfig,
        props: DnsConfig,
        yaml: DnsConfig?,
        cli: Map<String, Any?>,
    ): DnsConfig =
        DnsConfig(
            provider = mergeStr(cli, "dns.provider", yaml?.provider, props.provider, env.provider),
            domain = mergeStr(cli, "dns.domain", yaml?.domain, props.domain, env.domain),
            records = mergeRecords(yaml?.records, props.records, env.records),
            dryRun = mergeBoolean(cli, "dns.dryRun", yaml?.dryRun, props.dryRun),
            credentials = mergeCredentials(cli, yaml?.credentials, props.credentials, env.credentials),
        )

    private fun mergeCredentials(
        cli: Map<String, Any?>,
        yaml: OvhCredentials?,
        props: OvhCredentials,
        env: OvhCredentials,
    ): OvhCredentials =
        OvhCredentials(
            applicationKey = mergeStr(cli, "dns.credentials.applicationKey", yaml?.applicationKey, props.applicationKey, env.applicationKey),
            applicationSecret = mergeStr(cli, "dns.credentials.applicationSecret", yaml?.applicationSecret, props.applicationSecret, env.applicationSecret),
            consumerKey = mergeStr(cli, "dns.credentials.consumerKey", yaml?.consumerKey, props.consumerKey, env.consumerKey),
        )

    // ─── Generic merge helpers (CLI > YAML > Props > ENV) ────────

    private fun mergeStr(
        cli: Map<String, Any?>,
        key: String,
        yaml: String?,
        props: String,
        env: String,
    ): String {
        val cliValue = cli[key]?.toString()
        if (!cliValue.isNullOrBlank()) return cliValue
        if (!yaml.isNullOrBlank()) return yaml
        if (props.isNotBlank()) return props
        return env
    }

    private fun mergeBoolean(
        cli: Map<String, Any?>,
        key: String,
        yaml: Boolean?,
        props: Boolean,
    ): Boolean {
        cli.cliBoolean(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    private fun mergeRecords(
        yaml: List<DnsRecord>?,
        props: List<DnsRecord>,
        env: List<DnsRecord>,
    ): List<DnsRecord> {
        if (!yaml.isNullOrEmpty()) return yaml
        if (props.isNotEmpty()) return props
        return env
    }

    private fun Map<String, Any?>.cliBoolean(key: String): Boolean? =
        this[key]?.let { (it as? Boolean) ?: it.toString().toBoolean() }
}