package bakery.kgraph

/**
 * Service de construction du graphe d'articles connexes.
 *
 * Stratégie de scoring MVP :
 * - Tags partagés : +1.0 par tag commun
 * - Mots-clés du titre : +0.2 par mot significatif commun (>3 caractères, hors stop words)
 *
 * Les stop words (mots vides structurels) sont filtrés pour éviter les faux positifs
 * basés sur des termes génériques comme "Article", "Introduction", "Guide".
 *
 * Utilisation :
 * ```
 * val service = RelatedArticlesService()
 * val graph = service.buildGraph(articles)
 * val output = service.toSuggestions(graph)
 * ```
 */
class RelatedArticlesService {

    /**
     * Mots vides filtrés du score titre.
     * Mots structurels ou génériques qui n'indiquent pas une affinité thématique.
     */
    internal val stopWords: Set<String> = setOf(
        "the", "and", "for", "with", "from", "this", "that", "are", "was",
        "were", "been", "have", "has", "had", "not", "but", "all", "any",
        "can", "its", "our", "your", "their", "his", "her", "about",
        "into", "over", "after", "before", "between", "through", "during",
        "without", "within", "along", "across", "behind", "beyond",
        "more", "less", "very", "just", "also", "well", "still", "already",
        "get", "got", "go", "come", "make", "take", "know", "think",
        "give", "find", "tell", "ask", "show", "try", "keep", "seem",
        "need", "start", "begin", "end", "work", "part", "place", "case",
        "week", "day", "year", "thing", "world", "life", "hand", "point",
        "fact", "right", "left", "side", "line", "turn", "way", "mean",
        "using", "what", "when", "where", "why", "how", "who", "which",
        // Mots génériques fréquents dans les titres
        "article", "introduction", "guide", "tutorial", "getting", "started",
        "new", "best", "top", "complete", "ultimate", "beginner", "advanced",
        "simple", "quick", "easy", "step", "steps", "tips", "tricks",
    )

    /**
     * Construit le graphe [RelatedArticlesGraph] à partir d'une liste d'articles.
     * Calcule les arêtes entre chaque paire d'articles en fonction des tags
     * et des mots du titre partagés.
     */
    fun buildGraph(articles: List<ArticleNode>): RelatedArticlesGraph {
        if (articles.size < 2) {
            return RelatedArticlesGraph(articles, emptyList())
        }

        val edges = mutableListOf<ArticleEdge>()

        for (i in articles.indices) {
            for (j in i + 1 until articles.size) {
                val a = articles[i]
                val b = articles[j]
                val score = computeScore(a, b)
                if (score > 0.0) {
                    val reasons = buildReasons(a, b)
                    edges.add(
                        ArticleEdge(
                            sourceUrl = a.url,
                            targetUrl = b.url,
                            score = score,
                            reasons = reasons
                        )
                    )
                }
            }
        }

        return RelatedArticlesGraph(articles, edges)
    }

    /**
     * Calcule le score de similarité entre deux articles.
     */
    internal fun computeScore(a: ArticleNode, b: ArticleNode): Double {
        val tagScore = a.tags.intersect(b.tags.toSet()).size.toDouble()
        val titleScore = titleOverlap(a.title, b.title)
        return tagScore + titleScore
    }

    /**
     * Calcule le nombre de mots significatifs communs entre deux titres.
     * Mots de 3 caractères ou plus, en minuscules, dédoublonnés, hors stop words.
     * Chaque mot commun = +0.2 point.
     */
    internal fun titleOverlap(titleA: String, titleB: String): Double {
        val wordsA = meaningfulWords(titleA)
        val wordsB = meaningfulWords(titleB)
        return wordsA.intersect(wordsB).size * 0.2
    }

    private fun meaningfulWords(title: String): Set<String> =
        title.lowercase().split(Regex("\\W+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()

    /**
     * Génère les raisons d'une relation entre deux articles.
     * Format : "tag:{tagname}" pour chaque tag partagé,
     * "keyword:{word}" pour chaque mot de titre commun.
     */
    internal fun buildReasons(a: ArticleNode, b: ArticleNode): List<String> {
        val reasons = mutableListOf<String>()

        val sharedTags = a.tags.intersect(b.tags.toSet())
        sharedTags.forEach { tag -> reasons.add("tag:$tag") }

        val wordsA = meaningfulWords(a.title)
        val wordsB = meaningfulWords(b.title)
        val sharedWords = wordsA.intersect(wordsB)
        sharedWords.forEach { word -> reasons.add("keyword:$word") }

        return reasons
    }

    /**
     * Convertit un [RelatedArticlesGraph] en [RelatedArticlesOutput] contenant
     * les suggestions pour chaque article qui a au moins une relation.
     */
    fun toSuggestions(graph: RelatedArticlesGraph): RelatedArticlesOutput {
        val articlesByUrl = graph.articles.associateBy { it.url }

        val suggestions = mutableMapOf<String, List<RelatedArticleSuggestion>>()

        for (node in graph.articles) {
            val edges = graph.relatedTo(node.url)
            if (edges.isEmpty()) continue

            val related = edges.map { edge ->
                val targetUrl = if (edge.sourceUrl == node.url) edge.targetUrl else edge.sourceUrl
                val targetArticle = articlesByUrl[targetUrl]
                RelatedArticleSuggestion(
                    url = targetUrl,
                    title = targetArticle?.title ?: "",
                    score = edge.score,
                    reasons = edge.reasons
                )
            }

            suggestions[node.url] = related
        }

        return RelatedArticlesOutput(suggestions = suggestions)
    }
}
