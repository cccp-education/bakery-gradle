package bakery.netlify

/**
 * DSL surface for the Netlify target:
 *
 * ```
 * bakery {
 *   netlify {
 *     siteId = "…"
 *     tokenEnv = "NETLIFY_AUTH_TOKEN"
 *   }
 * }
 * ```
 *
 * Every field mirrors [NetlifyConfig]; the cascade is applied by
 * [NetlifyConfigResolver] (CLI > gradle.properties > DSL > site.yml > default).
 */
open class NetlifyDsl {
    var siteId: String = ""
    var tokenEnv: String = "NETLIFY_AUTH_TOKEN"
    var verifyUrl: String = ""

    /** Comma-separated extra exclusions. */
    var exclude: String = ""
    var message: String = "bakery deploy"
    var force: Boolean = false
    var dryRun: Boolean = false

    var tomlEnabled: Boolean = false
    var tomlPublishDir: String = ""
    var tomlNotFound: String = "/404.html"
    var tomlSecurityHeaders: Boolean = true
    var tomlRedirects: Boolean = false
}
