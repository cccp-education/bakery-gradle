package bakery

/**
 * DSL configuration for Related Articles (Knowledge Graph) — BKY-BKG.
 *
 * Controls whether the related-articles KG component is enabled in the site
 * and how many suggestions to display per article.
 *
 * Usage:
 * ```
 * bakery {
 *     relatedArticles {
 *         enabled = true
 *         maxResults = 4
 *         heading = "Articles connexes"
 *     }
 * }
 * ```
 *
 * Override order: CLI (-PrelatedArticlesEnabled=true) > gradle.properties > DSL > site.yml > defaults
 */
open class RelatedArticlesDsl(
    var enabled: Boolean = false,
    var maxResults: Int = 4,
    var heading: String = "Articles connexes",
)