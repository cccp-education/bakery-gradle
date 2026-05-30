package bakery.article

import java.time.LocalDate

/**
 * Article structuré prêt à être injecté dans un site JBake.
 *
 * Contient toutes les métadonnées nécessaires pour créer un fichier
 * AsciiDoc dans `content/blog/YYYY/MM/article-slug.adoc`.
 *
 * @property titre Titre de l'article (niveau =)
 * @property slug Identifiant URL-friendly dérivé du titre
 * @property date Date de publication (défaut : aujourd'hui si non précisée dans la réponse LLM)
 * @property description Résumé / extrait (métadonnée :description:)
 * @property tags Tags de l'article (métadonnée :tags:)
 * @property body Corps de l'article en AsciiDoc (sans les métadonnées d'en-tête)
 */
data class ArticleOutput(
    val titre: String,
    val slug: String,
    val date: LocalDate,
    val description: String,
    val tags: List<String>,
    val body: String
)
