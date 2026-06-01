package bakery

/**
 * DSL configuration for Theme system — BKY-JB-6.
 *
 * Usage:
 * ```
 * bakery {
 *     theme {
 *         mode = "auto"
 *         primaryColor = "#0d6efd"
 *         secondaryColor = "#6c757d"
 *         fontFamily = "Inter"
 *         logoUrl = "/img/logo.png"
 *         faviconUrl = "/img/favicon.ico"
 *     }
 * }
 * ```
 */
open class ThemeDsl(
    var mode: String = "auto",
    var primaryColor: String = "#0d6efd",
    var secondaryColor: String = "#6c757d",
    var fontFamily: String = "",
    var logoUrl: String = "",
    var faviconUrl: String = "",
)