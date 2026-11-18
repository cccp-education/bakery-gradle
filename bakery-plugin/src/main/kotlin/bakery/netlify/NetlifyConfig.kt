package bakery.netlify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Netlify deployment configuration (EPIC BKY-NETLIFY).
 *
 * Pure DDD domain type — no I/O, no Gradle coupling. Consumed by the
 * `site.yml` `netlify:` section and the 4-layer cascade (site.yml < DSL <
 * gradle.properties < CLI) through [NetlifyConfigResolver].
 *
 * Secrets policy (D6): the authentication token is **never** a configuration
 * value. [tokenEnv] carries the *name* of the environment variable; the value
 * is resolved only at execution time and stays `@Internal`.
 *
 * Absent section (null) means: no Netlify target at all (backward compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NetlifyConfig(
    /** Netlify site id (never a secret). */
    val siteId: String = "",
    /** Name of the environment variable holding the token. */
    val tokenEnv: String = "NETLIFY_AUTH_TOKEN",
    /** Deployment options. */
    val deploy: NetlifyDeployOptions = NetlifyDeployOptions(),
    /** Optional live smoke URL (D9). */
    val verifyUrl: String = "",
    /** `netlify.toml` / platform files (D11). */
    val toml: NetlifyTomlOptions = NetlifyTomlOptions(),
) {
    val isConfigured: Boolean get() = siteId.isNotBlank()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NetlifyDeployOptions(
    /** Extra payload exclusions (CNAME is always excluded, D5). */
    val exclude: List<String> = emptyList(),
    /** Netlify deploy title. */
    val message: String = "bakery deploy",
    /** Ignore the last-deploy digest and upload again (D7). */
    val force: Boolean = false,
    /** Plan only — no network call (D8). */
    val dryRun: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NetlifyTomlOptions(
    /** Write `netlify.toml` + platform files. */
    val enabled: Boolean = false,
    /** Published directory inside the site repo (build-from-git mode). */
    val publishDir: String = "",
    /** 404 target. */
    val notFound: String = "/404.html",
    /** Generate the baseline security headers (`_headers`). */
    val securityHeaders: Boolean = true,
    /** Generate the SPA fallback (`_redirects`). */
    val redirects: Boolean = false,
)
