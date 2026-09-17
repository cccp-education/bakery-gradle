package bakery.article

import bakery.llm.LlmService
import bakery.util.slugify
import java.time.LocalDate

/**
 * Service domaine de génération d'article assistée IA.
 *
 * Construit un prompt AsciiDoc structuré, appelle [LlmService],
 * parse la réponse en [ArticleOutput].
 *
 * Clean Architecture : pure logique métier, zéro dépendance Gradle.
 */
class ArticleGenerator {
    /**
     * Génère un article structuré à partir d'une intention riche.
     *
     * Surcharge DDD : l'[ArticleIntention] encapsule le sujet, le ton,
     * l'audience, les mots-clés et la langue pour un prompt contextualisé.
     *
     * @param intention Intention de génération (DDD)
     * @param llm Service de complétion LLM (Ollama en prod, FakeLlmService en test)
     * @return [ArticleOutput] structuré, prêt à injecter dans un site JBake
     */
    suspend fun generate(
        intention: ArticleIntention,
        llm: LlmService,
    ): ArticleOutput {
        val prompt = buildPrompt(intention)
        val response = llm.complete(prompt)
        return parseResponse(response, intention.topic)
    }

    /**
     * Génère un article structuré à partir d'un sujet.
     *
     * @param topic Sujet de l'article (ex: "Introduction à Kotlin")
     * @param llm Service de complétion LLM (Ollama en prod, FakeLlmService en test)
     * @return [ArticleOutput] structuré, prêt à injecter dans un site JBake
     */
    suspend fun generate(
        topic: String,
        llm: LlmService,
    ): ArticleOutput {
        val prompt = buildPrompt(topic)
        val response = llm.complete(prompt)
        return parseResponse(response, topic)
    }

    /**
     * Construit le prompt système pour la génération d'article.
     *
     * Demande explicitement le format AsciiDoc avec métadonnées structurées
     * pour permettre un parsing fiable.
     */
    internal fun buildPrompt(topic: String): String =
        """
        Rédige un article de blog en français sur le sujet suivant : "$topic".

        FORMAT DE RÉPONSE OBLIGATOIRE (AsciiDoc) :

        = Titre de l'article
        :description: Résumé en une phrase
        :tags: tag1, tag2, tag3
        :date: YYYY-MM-DD

        == Première section

        Contenu de la première section...

        == Deuxième section

        Contenu de la deuxième section...

        CONSIGNES :
        - La première ligne DOIT commencer par "= " (titre principal)
        - Les métadonnées :description:, :tags:, :date: sont OBLIGATOIRES
        - Le corps utilise des sections == (niveau 2) et === (niveau 3)
        - Inclus du contenu substantiel, pas de placeholders
        - 3 à 5 sections de contenu
        - Date au format YYYY-MM-DD (aujourd'hui si non précisée)
        """.trimIndent()

    /**
     * Construit le prompt enrichi depuis une [ArticleIntention].
     *
     * Inclut le ton, l'audience, les mots-clés et la langue pour guider
     * le LLM vers un contenu plus ciblé et pertinent.
     */
    internal fun buildPrompt(intention: ArticleIntention): String {
        val langInstruction = languageInstruction(intention.lang)

        val toneGuidance =
            when (intention.ton) {
                ArticleTon.INFORMATIF -> "Adopte un ton informatif : neutre et factuel, vulgarise si nécessaire."
                ArticleTon.TECHNIQUE -> "Adopte un ton technique : précis, détaillé, inclus du code quand c'est pertinent."
                ArticleTon.PEDAGOGIQUE -> "Adopte un ton pédagogique : progressif, avec des exemples et exercices."
                ArticleTon.CONVAINCRE -> "Adopte un ton convaincre : argumenté, avec des comparaisons et un avis tranché."
            }

        val audienceGuidance =
            when (intention.audience) {
                ArticleAudience.GENERAL -> "Le public cible est grand public : évite le jargon, explique les concepts."
                ArticleAudience.DEVELOPPEUR -> "Le public cible est développeur : tu peux utiliser du jargon technique et du code."
                ArticleAudience.FORMATEUR -> "Le public cible est formateur : inclus des objectifs pédagogiques et des points clés."
            }

        val keywordsInstruction =
            if (intention.keywords.isNotEmpty()) {
                "Intègre naturellement ces mots-clés dans le contenu : ${intention.keywords.joinToString(", ")}."
            } else {
                ""
            }

        return """
            Rédige un article de blog sur le sujet suivant : "${intention.topic}".

            $langInstruction
            $toneGuidance
            $audienceGuidance
            ${if (keywordsInstruction.isNotBlank()) keywordsInstruction else ""}

            FORMAT DE RÉPONSE OBLIGATOIRE (AsciiDoc) :

            = Titre de l'article
            :description: Résumé en une phrase
            :tags: tag1, tag2, tag3
            :date: YYYY-MM-DD

            == Première section

            Contenu de la première section...

            == Deuxième section

            Contenu de la deuxième section...

            CONSIGNES :
            - La première ligne DOIT commencer par "= " (titre principal)
            - Les métadonnées :description:, :tags:, :date: sont OBLIGATOIRES
            - Le corps utilise des sections == (niveau 2) et === (niveau 3)
            - Inclus du contenu substantiel, pas de placeholders
            - 3 à 5 sections de contenu
            - Date au format YYYY-MM-DD (aujourd'hui si non précisée)
            """.trimIndent()
    }

    /**
     * Builds the language instruction for the given language code.
     *
     * Single source of truth: the N0 contract [contracts.i18n.LanguageCatalog].
     * The instruction names the language (English name) and carries the ISO
     * code, so the LLM is explicitly told which of the 22 supported languages
     * to write in — no silent fallback to French.
     *
     * Unknown codes are defensive: the instruction still carries the code
     * (e.g. a future catalog entry not yet in the local classpath).
     */
    internal fun languageInstruction(code: String): String {
        val language = contracts.i18n.LanguageCatalog.findByCode(code)
        val name = language?.name ?: code
        val nativeName = language?.nativeName
        val nativeHint = if (nativeName != null && nativeName != name) " ($nativeName)" else ""
        return "Write the article in $name$nativeHint. The article language code is '$code'."
    }

    /**
     * Parse la réponse LLM en [ArticleOutput].
     *
     * Stratégie résiliente :
     * 1. Extrait le titre (ligne commençant par "= ")
     * 2. Extrait les métadonnées (:description:, :tags:, :date:)
     * 3. Extrait le corps (tout après la dernière métadonnée)
     * 4. Fallback : si parsing échoue, génère un article minimal depuis le topic
     */
    internal fun parseResponse(
        response: String,
        topic: String,
    ): ArticleOutput {
        if (response.isBlank()) {
            return fallbackArticle(topic)
        }

        val lines = response.lines()

        // 1. Extraire le titre
        val titre =
            lines
                .firstOrNull { it.trimStart().startsWith("= ") }
                ?.trimStart()
                ?.removePrefix("= ")
                ?.trim()
                ?: topic

        // 2. Extraire les métadonnées
        val description =
            extractMetadata(lines, ":description:")
                ?: "Article généré sur le sujet : $topic"
        val tags =
            extractMetadata(lines, ":tags:")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf(topic.slugify())
        val dateStr = extractMetadata(lines, ":date:")
        val date =
            try {
                dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
            } catch (_: Exception) {
                LocalDate.now()
            }

        // 3. Extraire le slug
        val slug = titre.slugify()

        // 4. Extraire le corps (tout après la dernière métadonnée)
        val lastMetaIndex = lines.indexOfLast { it.trimStart().startsWith(":") && it.contains(": ") }
        val bodyStartIndex = if (lastMetaIndex >= 0) lastMetaIndex + 1 else 0
        val body =
            lines
                .drop(bodyStartIndex)
                .dropWhile { it.isBlank() }
                .joinToString("\n")
                .trim()
                .ifBlank { topic }

        return ArticleOutput(
            titre = titre,
            slug = slug,
            date = date,
            description = description,
            tags = tags,
            body = body,
        )
    }

    /**
     * Génère un article de secours quand le LLM ne produit rien d'exploitable.
     */
    private fun fallbackArticle(topic: String): ArticleOutput {
        val slug = topic.slugify()
        return ArticleOutput(
            titre = topic,
            slug = slug,
            date = LocalDate.now(),
            description = "Article généré automatiquement sur le sujet : $topic",
            tags = listOf(slug),
            body =
                """
                == Introduction
                
                Article en cours de rédaction sur le sujet : $topic.
                
                [NOTE]
                ====
                Cet article a été généré automatiquement. Le service IA n'a pas produit
                de contenu structuré. Veuillez vérifier le prompt ou réessayer.
                ====
                """.trimIndent(),
        )
    }

    /**
     * Extrait une métadonnée AsciiDoc (:key: value) depuis une liste de lignes.
     */
    private fun extractMetadata(
        lines: List<String>,
        key: String,
    ): String? =
        lines
            .firstOrNull { it.trimStart().startsWith(key, ignoreCase = false) }
            ?.trimStart()
            ?.removePrefix(key)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
