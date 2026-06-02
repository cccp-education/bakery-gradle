package bakery.kgraph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Résout les articles connexes pour un slug/article donné à partir du fichier
 * `related-articles.json` produit par la tâche `collectRelatedArticles`.
 */
class RelatedArticlesResolver(
    private val relatedArticlesFile: File,
    private val maxResults: Int = 3
) {
    private val mapper = jacksonObjectMapper()

    private val suggestions: Map<String, List<RelatedArticleSuggestion>> by lazy {
        if (!relatedArticlesFile.exists()) {
            emptyMap()
        } else {
            val output: RelatedArticlesOutput = mapper.readValue(relatedArticlesFile)
            output.suggestions
        }
    }

    /**
     * Résout les suggestions d'articles connexes pour un slug donné.
     * Le slug peut être une URL relative (ex: "/blog/2024/kotlin.html")
     * ou un chemin partiel (ex: "kotlin.html").
     * Retourne une liste vide si aucun article connexe n'est trouvé.
     */
    fun resolve(slug: String): List<RelatedArticleSuggestion> {
        if (suggestions.isEmpty()) return emptyList()

        // Cherche d'abord par correspondance exacte
        suggestions[slug]?.let { return it.take(maxResults) }

        // Fallback : cherche par suffixe (fin de l'URL)
        for ((key, value) in suggestions) {
            if (key.endsWith(slug) || slug.endsWith(key) || key.contains(slug)) {
                return value.take(maxResults)
            }
        }

        return emptyList()
    }

    /**
     * Retourne true si le fichier de données existe et contient des suggestions.
     */
    fun hasSuggestions(): Boolean = suggestions.isNotEmpty()

    /**
     * Retourne le nombre total d'articles avec des suggestions.
     */
    fun articleCount(): Int = suggestions.size

    companion object {
        fun load(file: File, maxResults: Int = 3): RelatedArticlesResolver =
            RelatedArticlesResolver(file, maxResults)
    }
}
