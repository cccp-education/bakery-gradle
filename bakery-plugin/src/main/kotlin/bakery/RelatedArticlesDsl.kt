package bakery

/**
 * DSL configuration for Related Articles (Knowledge Graph) — BKY-BKG.
 *
 * Controls whether the related-articles KG component is enabled in the site,
 * how many suggestions to display per article, and where to find the
 * related-articles.json graph file.
 *
 * Usage:
 * ```
 * bakery {
 *     relatedArticles {
 *         enabled = true
 *         maxResults = 4
 *         heading = "Articles connexes"
 *         graphFilePath = "build/bakery/related-articles.json"
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
    var graphFilePath: String = "build/bakery/related-articles.json",
)