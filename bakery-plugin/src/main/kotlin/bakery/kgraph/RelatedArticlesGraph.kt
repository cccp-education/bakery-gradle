package bakery.kgraph

import java.time.Instant

/**
 * Noeud du graphe : un article du site statique bakery.
 *
 * @property url  Chemin relatif de l'article (ex: /blog/2024/kotlin.html)
 * @property title Titre de l'article
 * @property date Date ISO de publication (optionnel)
 * @property tags Mots-clés associés — issu de :jbake-tags: (optionnel)
 * @property description Description courte — issu de :jbake-description: (optionnel, pour TF-IDF)
 * @property author Auteur (optionnel)
 */
data class ArticleNode(
    val url: String,
    val title: String,
    val date: String = "",
    val tags: List<String> = emptyList(),
    val description: String = "",
    val author: String = ""
)

/**
 * Arête du graphe : relation entre deux articles.
 *
 * @property sourceUrl URL de l'article source
 * @property targetUrl URL de l'article cible
 * @property score Poids de la relation (plus haut = plus connexe)
 * @property reasons Liste des raisons de la relation (tag:xxx, reference, keyword:xxx)
 */
data class ArticleEdge(
    val sourceUrl: String,
    val targetUrl: String,
    val score: Double,
    val reasons: List<String> = emptyList()
)

/**
 * Graphe complet des articles d'un site bakery.
 * Permet de trouver les articles connexes à un article donné.
 *
 * @property articles Tous les articles du site
 * @property edges Toutes les relations entre articles
 */
data class RelatedArticlesGraph(
    val articles: List<ArticleNode>,
    val edges: List<ArticleEdge>
) {
    /**
     * Retourne les [maxResults] articles les plus connexes à l'article d'URL [url].
     * Les arêtes sont orientées source→target ; on cherche dans les deux sens.
     */
    fun relatedTo(url: String, maxResults: Int = 3): List<ArticleEdge> =
        edges.filter { it.sourceUrl == url || it.targetUrl == url }
            .sortedByDescending { it.score }
            .take(maxResults)
}

/**
 * Suggestion d'article connexe pour un article source.
 *
 * @property url URL de l'article connexe suggéré
 * @property title Titre de l'article connexe
 * @property score Score de pertinence
 * @property reasons Raisons de la suggestion
 */
data class RelatedArticleSuggestion(
    val url: String,
    val title: String,
    val score: Double,
    val reasons: List<String> = emptyList()
)

/**
 * Résumé d'un article blog pour la section blog_articles du JSON de sortie.
 * Contient les métadonnées JBake utiles pour le template Thymeleaf.
 *
 * @property title Titre de l'article
 * @property description Description courte (:jbake-description:)
 * @property tags Mots-clés (:jbake-tags:)
 * @property date Date ISO de publication
 * @property author Auteur
 */
data class BlogArticleSummary(
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val date: String = "",
    val author: String = ""
)

/**
 * Format de sortie du graphe d'articles connexes.
 * Sérialisé en JSON par la tâche collectRelatedArticles.
 *
 * @property version Version du format
 * @property generatedAt Timestamp ISO de génération
 * @property blogArticles Map URL → résumé des métadonnées JBake pour chaque article
 * @property suggestions Map URL source → listes de suggestions
 */
data class RelatedArticlesOutput(
    val version: String = "1.0",
    val generatedAt: String = Instant.now().toString(),
    val blogArticles: Map<String, BlogArticleSummary> = emptyMap(),
    val suggestions: Map<String, List<RelatedArticleSuggestion>> = emptyMap()
)
