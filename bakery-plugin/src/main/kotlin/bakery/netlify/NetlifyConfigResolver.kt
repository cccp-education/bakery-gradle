package bakery.netlify

import bakery.ConfigResolver

/**
 * Resolves [NetlifyConfig] through the documented cascade (D12):
 *
 * CLI (`-Pbakery.netlify.*`) > gradle.properties > DSL > `site.yml` > defaults.
 *
 * The token is out of scope by design (D6): only the *name* of its environment
 * variable is configuration.
 */
object NetlifyConfigResolver {
    private const val PREFIX = "bakery.netlify"

    fun resolve(
        props: Map<String, String>,
        dsl: NetlifyDsl,
        yaml: NetlifyConfig?,
        default: NetlifyConfig = NetlifyConfig(),
    ): NetlifyConfig {
        val yamlDeploy = yaml?.deploy ?: NetlifyDeployOptions()
        val yamlToml = yaml?.toml ?: NetlifyTomlOptions()
        val defaultDeploy = default.deploy
        val defaultToml = default.toml

        val excludes =
            ConfigResolver.resolveString(
                props,
                "$PREFIX.deploy",
                "exclude",
                dsl.exclude,
                yamlDeploy.exclude.joinToString(","),
                defaultDeploy.exclude.joinToString(","),
            )

        return NetlifyConfig(
            siteId = ConfigResolver.resolveString(props, PREFIX, "siteId", dsl.siteId, yaml?.siteId, default.siteId),
            tokenEnv = ConfigResolver.resolveString(props, PREFIX, "tokenEnv", dsl.tokenEnv, yaml?.tokenEnv, default.tokenEnv),
            verifyUrl = ConfigResolver.resolveString(props, PREFIX, "verifyUrl", dsl.verifyUrl, yaml?.verifyUrl, default.verifyUrl),
            deploy =
                NetlifyDeployOptions(
                    exclude = parseCsv(excludes),
                    message =
                        ConfigResolver.resolveString(
                            props,
                            "$PREFIX.deploy",
                            "message",
                            dsl.message,
                            yamlDeploy.message,
                            defaultDeploy.message,
                        ),
                    force =
                        ConfigResolver.resolveBoolean(
                            props,
                            "$PREFIX.deploy",
                            "force",
                            dsl.force,
                            yamlDeploy.force,
                            defaultDeploy.force,
                        ),
                    dryRun =
                        ConfigResolver.resolveBoolean(
                            props,
                            "$PREFIX.deploy",
                            "dryRun",
                            dsl.dryRun,
                            yamlDeploy.dryRun,
                            defaultDeploy.dryRun,
                        ),
                ),
            toml =
                NetlifyTomlOptions(
                    enabled =
                        ConfigResolver.resolveBoolean(
                            props,
                            "$PREFIX.toml",
                            "enabled",
                            dsl.tomlEnabled,
                            yamlToml.enabled,
                            defaultToml.enabled,
                        ),
                    publishDir =
                        ConfigResolver.resolveString(
                            props,
                            "$PREFIX.toml",
                            "publishDir",
                            dsl.tomlPublishDir,
                            yamlToml.publishDir,
                            defaultToml.publishDir,
                        ),
                    notFound =
                        ConfigResolver.resolveString(
                            props,
                            "$PREFIX.toml",
                            "notFound",
                            dsl.tomlNotFound,
                            yamlToml.notFound,
                            defaultToml.notFound,
                        ),
                    securityHeaders =
                        ConfigResolver.resolveBoolean(
                            props,
                            "$PREFIX.toml",
                            "securityHeaders",
                            dsl.tomlSecurityHeaders,
                            yamlToml.securityHeaders,
                            defaultToml.securityHeaders,
                        ),
                    redirects =
                        ConfigResolver.resolveBoolean(
                            props,
                            "$PREFIX.toml",
                            "redirects",
                            dsl.tomlRedirects,
                            yamlToml.redirects,
                            defaultToml.redirects,
                        ),
                ),
        )
    }

    private fun parseCsv(value: String): List<String> =
        value
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
