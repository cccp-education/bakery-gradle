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
     * Mots vides filtrés du score titre et description.
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
        // Mots vides français fréquents dans les descriptions
        "pour", "une", "avec", "dans", "sur", "que", "qui", "est", "sont",
        "par", "plus", "aux", "des", "les", "est", "son", "ses", "cette",
    )

    /**
     * Construit le graphe [RelatedArticlesGraph] à partir d'une liste d'articles.
     * Calcule les arêtes entre chaque paire d'articles en fonction des tags,
     * mots du titre partagés, co-occurrences de tags, et entités des descriptions.
     */
    fun buildGraph(articles: List<ArticleNode>): RelatedArticlesGraph {
        if (articles.size < 2) {
            return RelatedArticlesGraph(articles, emptyList())
        }

        // Build tag co-occurrence map
        val tagCooccurrences = buildTagCooccurrenceMap(articles)

        val edges = mutableListOf<ArticleEdge>()

        for (i in articles.indices) {
            for (j in i + 1 until articles.size) {
                val a = articles[i]
                val b = articles[j]
                val score = computeScore(a, b, tagCooccurrences)
                val reasons = buildReasons(a, b, tagCooccurrences)
                if (score > 0.0) {
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
     * Construit la map des co-occurrences de tags.
     * Clé : paire de tags triée (tag1, tag2) avec tag1 < tag2.
     * Valeur : nombre d'articles où les deux tags apparaissent ensemble.
     */
    internal fun buildTagCooccurrenceMap(articles: List<ArticleNode>): Map<Pair<String, String>, Int> {
        val counts = mutableMapOf<Pair<String, String>, Int>()
        for (article in articles) {
            val sortedTags = article.tags.sorted()
            for (i in sortedTags.indices) {
                for (j in i + 1 until sortedTags.size) {
                    val pair = Pair(sortedTags[i], sortedTags[j])
                    counts[pair] = counts.getOrDefault(pair, 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Calcule le score de similarité entre deux articles.
     * Score = tags partagés + titre overlap + co-occurrence de tags + entités description.
     */
    internal fun computeScore(a: ArticleNode, b: ArticleNode, tagCooccurrences: Map<Pair<String, String>, Int> = emptyMap()): Double {
        val tagScore = a.tags.intersect(b.tags.toSet()).size.toDouble()
        val titleScore = titleOverlap(a.title, b.title)
        val cooccurrenceScore = computeCooccurrenceScore(a, b, tagCooccurrences)
        val entityScore = entityOverlap(a.description, b.description)
        return tagScore + titleScore + cooccurrenceScore + entityScore
    }

    /**
     * Calcule le score de co-occurrence de tags entre deux articles.
     * Pour chaque paire de tags partagés, ajoute un bonus proportionnel
     * au nombre d'articles où ces tags co-occurrent.
     */
    internal fun computeCooccurrenceScore(
        a: ArticleNode,
        b: ArticleNode,
        tagCooccurrences: Map<Pair<String, String>, Int>
    ): Double {
        val sharedTags = a.tags.intersect(b.tags.toSet()).sorted()
        if (sharedTags.size < 2) return 0.0

        var score = 0.0
        for (i in sharedTags.indices) {
            for (j in i + 1 until sharedTags.size) {
                val pair = Pair(sharedTags[i], sharedTags[j])
                val count = tagCooccurrences[pair] ?: 0
                if (count >= 2) {
                    score += 0.3 * minOf(count, 5) // Bonus plafonné à 1.5
                }
            }
        }
        return score
    }

    /**
     * Calcule le score d'overlap d'entités entre deux descriptions.
     * Les entités sont les mots significatifs (hors stop words, >3 chars)
     * extraits des descriptions. Chaque entité commune = +0.3 point.
     */
    internal fun entityOverlap(descA: String, descB: String): Double {
        if (descA.isBlank() || descB.isBlank()) return 0.0
        val entitiesA = meaningfulWords(descA)
        val entitiesB = meaningfulWords(descB)
        return entitiesA.intersect(entitiesB).size * 0.3
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
     * "keyword:{word}" pour chaque mot de titre commun,
     * "cooccurrence:{tag1},{tag2}" pour chaque paire de tags co-occurrents,
     * "entity:{word}" pour chaque entité de description commune.
     */
    internal fun buildReasons(a: ArticleNode, b: ArticleNode, tagCooccurrences: Map<Pair<String, String>, Int> = emptyMap()): List<String> {
        val reasons = mutableListOf<String>()

        val sharedTags = a.tags.intersect(b.tags.toSet())
        sharedTags.forEach { tag -> reasons.add("tag:$tag") }

        val wordsA = meaningfulWords(a.title)
        val wordsB = meaningfulWords(b.title)
        val sharedWords = wordsA.intersect(wordsB)
        sharedWords.forEach { word -> reasons.add("keyword:$word") }

        // Co-occurrence reasons
        val sortedSharedTags = sharedTags.sorted()
        for (i in sortedSharedTags.indices) {
            for (j in i + 1 until sortedSharedTags.size) {
                val pair = Pair(sortedSharedTags[i], sortedSharedTags[j])
                val count = tagCooccurrences[pair] ?: 0
                if (count >= 2) {
                    reasons.add("cooccurrence:${sortedSharedTags[i]},${sortedSharedTags[j]}")
                }
            }
        }

        // Entity overlap reasons
        if (a.description.isNotBlank() && b.description.isNotBlank()) {
            val entitiesA = meaningfulWords(a.description)
            val entitiesB = meaningfulWords(b.description)
            val sharedEntities = entitiesA.intersect(entitiesB)
            sharedEntities.forEach { entity -> reasons.add("entity:$entity") }
        }

        return reasons
    }

    /**
     * Convertit un [RelatedArticlesGraph] en [RelatedArticlesOutput] contenant
     * les suggestions pour chaque article qui a au moins une relation,
     * et les métadonnées JBake de tous les articles dans blogArticles.
     */
    fun toSuggestions(graph: RelatedArticlesGraph): RelatedArticlesOutput {
        val articlesByUrl = graph.articles.associateBy { it.url }

        val blogArticles = graph.articles.associate { node ->
            node.url to BlogArticleSummary(
                title = node.title,
                description = node.description,
                tags = node.tags,
                date = node.date,
                author = node.author
            )
        }

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

        return RelatedArticlesOutput(
            blogArticles = blogArticles,
            suggestions = suggestions
        )
    }
}
