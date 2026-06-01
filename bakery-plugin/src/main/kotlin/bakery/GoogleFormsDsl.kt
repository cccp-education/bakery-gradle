package bakery

/**
 * DSL configuration for Google Forms embed — BKY-JB-3.
 *
 * Usage:
 * ```
 * bakery { googleForms { formId = "1ABCDEF-x1234567890" } }
 * ```
 */
open class GoogleFormsDsl(
    var formId: String = "",
    var width: String = "640",
    var height: String = "800",
    var allowMultiple: Boolean = false,
)
