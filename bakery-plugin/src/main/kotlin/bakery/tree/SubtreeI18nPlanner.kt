package bakery.tree

import bakery.tree.SiteNode.Article

data class ArticleModification(
    val path: String,
    val oldChecksum: String?,
    val newChecksum: String,
    val translatableSegmentCount: Int
)

data class I18nDelta(
    val modifiedArticles: List<ArticleModification>,
    val untouchedArticles: List<Article>,
    val updatedChecksums: Map<String, String>
) {
    fun isEmpty(): Boolean = modifiedArticles.isEmpty()
}

class SubtreeI18nPlanner(
    private val tree: SiteTree?,
    private val flatArticles: List<Article>?,
    private val beforeChecksums: Map<String, String>
) {
    constructor(tree: SiteTree, beforeChecksums: Map<String, String>) : this(tree, null, beforeChecksums)
    constructor(articles: List<Article>, beforeChecksums: Map<String, String>) : this(null, articles, beforeChecksums)

    fun computeDelta(
        afterChecksums: Map<String, String>,
        subtreePath: String? = null
    ): I18nDelta {
        val articles = scopedArticles(subtreePath)
        val modified = mutableListOf<ArticleModification>()
        val untouched = mutableListOf<Article>()

        for (article in articles) {
            val path = article.path
            val before = beforeChecksums[path]
            val after = afterChecksums[path]
            if (after == null) continue
            if (before == after) {
                untouched.add(article)
            } else {
                modified.add(ArticleModification(
                    path = path,
                    oldChecksum = before,
                    newChecksum = after,
                    translatableSegmentCount = article.content?.translatableSegments()?.size ?: 0
                ))
            }
        }

        return I18nDelta(modified, untouched, afterChecksums)
    }

    private fun scopedArticles(subtreePath: String?): List<Article> {
        if (tree != null) {
            return if (subtreePath != null) {
                tree.findSubtree(subtreePath)?.leaves() ?: emptyList()
            } else {
                tree.leaves()
            }
        }
        return flatArticles ?: emptyList()
    }
}