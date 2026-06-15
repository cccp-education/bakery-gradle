package bakery.article

import bakery.BakeryConstants

/**
 * Intention de génération d'article — modèle domaine DDD.
 *
 * Capture le pourquoi et le comment d'un article avant sa génération :
 * - *topic* : sujet obligatoire
 * - *ton* : registre stylistique (informatif, technique, pédagogique, convaincre)
 * - *audience* : public cible (grand public, développeur, formateur)
 * - *keywords* : termes clés pour le SEO et le RAG
 * - *lang* : langue de l'article (fr, en, zh, hi, es, ar, bn, pt, ru, ur)
 *
 * Usage :
 * ```
 * val intention = ArticleIntention(
 *     topic = "Kotlin Coroutines",
 *     ton = ArticleTon.TECHNIQUE,
 *     audience = ArticleAudience.DEVELOPPEUR,
 *     keywords = listOf("coroutines", "async", "flow"),
 *     lang = "en"
 * )
 * ```
 */
data class ArticleIntention(
    val topic: String,
    val ton: ArticleTon = ArticleTon.INFORMATIF,
    val audience: ArticleAudience = ArticleAudience.GENERAL,
    private val rawKeywords: List<String> = emptyList(),
    val lang: String = "fr"
) {
    init {
        require(topic.isNotBlank()) { "Le sujet (topic) est obligatoire pour générer un article." }
        require(lang in BakeryConstants.SUPPORTED_LANGS) { "Langue '$lang' non supportée. Utilisez : ${BakeryConstants.SUPPORTED_LANGS.joinToString()}." }
    }

    /** Mots-clés nettoyés : trimmés, non-vides, dédupliqués. */
    val keywords: List<String> = rawKeywords
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    /**
     * Génère un contexte textuel injectable dans le prompt LLM.
     *
     * Format structuré pour guider le modèle vers le ton, l'audience
     * et les mots-clés attendus.
     */
    fun toPromptContext(): String = buildString {
        appendLine("Sujet : $topic")
        appendLine("Ton : ${ton.label}")
        appendLine("Public cible : ${audience.label}")
        appendLine("Langue : $lang")
        if (keywords.isNotEmpty()) {
            appendLine("Mots-clés : ${keywords.joinToString(", ")}")
        }
    }
}

/**
 * Registre stylistique de l'article.
 *
 * - INFORMATIF : neutre, factuel, vulgarisation
 * - TECHNIQUE : précis, détaillé, code-centric
 * - PEDAGOGIQUE : progressif, exemples, exercices
 * - CONVAINCRE : argumenté, comparaisons, avis tranché
 */
enum class ArticleTon(val label: String) {
    INFORMATIF("informatif"),
    TECHNIQUE("technique"),
    PEDAGOGIQUE("pédagogique"),
    CONVAINCRE("convaincre")
}

/**
 * Public cible de l'article.
 *
 * - GENERAL : grand public, pas de prérequis technique
 * - DEVELOPPEUR : professionnel logiciel, familiarité code/outils
 * - FORMATEUR : enseignant, familiarité pédagogie et contenu
 */
enum class ArticleAudience(val label: String) {
    GENERAL("grand public"),
    DEVELOPPEUR("développeur"),
    FORMATEUR("formateur")
}