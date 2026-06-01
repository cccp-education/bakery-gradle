package bakery

/**
 * DSL configuration for Analytics + Newsletter — BKY-JB-5.
 *
 * Usage:
 * ```
 * bakery {
 *     analytics {
 *         provider = "plausible"
 *         domain = "my-site.com"
 *         scriptSrc = "https://plausible.io/js/script.js"
 *     }
 *     newsletter {
 *         enabled = true
 *         provider = "mailchimp"
 *         endpoint = "https://mailchimp.us1.list-manage.com/subscribe/post?u=xxx&id=yyy"
 *     }
 * }
 * ```
 */
open class AnalyticsDsl(
    var provider: String = "",
    var domain: String = "",
    var scriptSrc: String = "",
)

open class NewsletterDsl(
    var enabled: Boolean = false,
    var provider: String = "",
    var endpoint: String = "",
)