package bakery.scaffold

import bakery.llm.LlmService

/**
 * Service domaine de scaffolding assiste par IA.
 *
 * Construit un prompt structure destine au LLM, appelle [LlmService],
 * parse la reponse JSON en [ScaffoldOutput].
 *
 * Clean Architecture : pure logique metier, zero dependance Gradle.
 * Pattern identique a [bakery.article.ArticleGenerator].
 */
class ScaffoldGenerator {

    /**
     * Genere une structure de site a partir d'une intention.
     *
     * Surcharge DDD : la [ScaffoldIntention] encapsule la description,
     * le type de site, la langue et le nom du projet pour guider le LLM.
     */
    suspend fun generate(intention: ScaffoldIntention, llm: LlmService): ScaffoldOutput {
        val prompt = buildPrompt(intention)
        val response = llm.complete(prompt)
        return parseResponse(response, intention)
    }

    /**
     * Construit le prompt systeme pour la generation de structure de site.
     *
     * Demande explicitement du JSON structure avec les champs obligatoires
     * pour permettre un parsing fiable.
     */
    internal fun buildPrompt(intention: ScaffoldIntention): String = """
        Tu es un expert en architecture de sites statiques JBake.
        Genere la structure JSON pour un site statique correspondant a la demande suivante.

        ${intention.toPromptContext()}

        TYPE DE SITE : ${intention.siteType.label}
        Les templates disponibles pour ce type sont :
        - blog : blog.thyme, post.thyme, page.thyme, archive.thyme, tags.thyme
        - portfolio : page.thyme, project-list.thyme, project-detail.thyme
        - documentation : page.thyme, section.thyme, api-doc.thyme, search.thyme
        - formation : page.thyme, session.thyme, module.thyme, quiz.thyme

        FORMAT DE REPONSE OBLIGATOIRE (JSON) :

        {
          "siteType": "blog",
          "projectName": "mon-site",
          "description": "Description concise du site",
          "templates": ["blog.thyme", "post.thyme", "page.thyme"],
          "metadata": {
            "title": "Titre du site",
            "description": "Description pour les metadonnees",
            "tags": ["tag1", "tag2"],
            "layout": "post",
            "language": "fr"
          }
        }

        CONSIGNES :
        - Le champ siteType DOIT etre un des types listes ci-dessus
        - Le champ projectName DOIT etre un slug URL-friendly (minuscules, tirets)
        - Le champ templates DOIT contenir uniquement des templates du type choisi
        - Le champ metadata.title DOIT etre clair et descriptif
        - Le champ metadata.tags DOIT contenir 3 a 5 tags pertinents
        - Reponds UNIQUEMENT avec le JSON, sans texte avant ou apres
    """.trimIndent()

    /**
     * Parse la reponse LLM en [ScaffoldOutput].
     *
     * Strategie resilient :
     * 1. Extrait le JSON de la reponse (meme si entoure de texte)
     * 2. Parse les champs obligatoires (siteType, projectName, description)
     * 3. Fallback : si parsing echoue, genere une structure minimale depuis l'intention
     */
    internal fun parseResponse(response: String, intention: ScaffoldIntention): ScaffoldOutput {
        if (response.isBlank()) {
            return fallbackOutput(intention)
        }

        return try {
            val jsonContent = extractJson(response)
            parseJsonOutput(jsonContent, intention)
        } catch (_: Exception) {
            fallbackOutput(intention)
        }
    }

    /**
     * Extrait le bloc JSON d'une reponse qui peut contenir du texte avant/apres.
     */
    internal fun extractJson(response: String): String {
        val startIndex = response.indexOf('{')
        val endIndex = response.lastIndexOf('}')
        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            throw IllegalArgumentException("No JSON object found in response")
        }
        return response.substring(startIndex, endIndex + 1)
    }

    /**
     * Parse un bloc JSON en [ScaffoldOutput].
     */
    private fun parseJsonOutput(json: String, intention: ScaffoldIntention): ScaffoldOutput {
        // Simple JSON parsing without external library dependency
        // (bakery already uses Jackson but we keep this pure domain)
        val siteType = extractJsonValue(json, "siteType")
            ?.let { ScaffoldSiteType.fromStringOrDefault(it) }
            ?: intention.siteType

        val projectName = extractJsonValue(json, "projectName")
            ?.takeIf { it.isNotBlank() }
            ?: intention.projectName.ifBlank { intention.description.slugify() }

        val description = extractJsonValue(json, "description")
            ?: intention.description

        val templates = extractJsonArray(json, "templates")
            .ifEmpty { defaultTemplatesFor(siteType) }

        val metadataTitle = extractNestedJsonValue(json, "metadata", "title")
            ?: projectName

        val metadataDescription = extractNestedJsonValue(json, "metadata", "description")
            ?: description

        val metadataTags = extractNestedJsonArray(json, "metadata", "tags")
            .ifEmpty { listOf(intention.description.slugify()) }

        val metadataLayout = extractNestedJsonValue(json, "metadata", "layout")
            ?: "post"

        val metadataLanguage = extractNestedJsonValue(json, "metadata", "language")
            ?: intention.lang

        return ScaffoldOutput(
            siteType = siteType,
            projectName = projectName,
            description = description,
            templates = templates,
            metadata = ScaffoldMetadata(
                title = metadataTitle,
                description = metadataDescription,
                tags = metadataTags,
                layout = metadataLayout,
                language = metadataLanguage
            )
        )
    }

    /**
     * Templates par defaut selon le type de site.
     */
    private fun defaultTemplatesFor(siteType: ScaffoldSiteType): List<String> = when (siteType) {
        ScaffoldSiteType.BLOG -> listOf("blog.thyme", "post.thyme", "page.thyme")
        ScaffoldSiteType.PORTFOLIO -> listOf("page.thyme", "project-list.thyme", "project-detail.thyme")
        ScaffoldSiteType.DOC -> listOf("page.thyme", "section.thyme", "search.thyme")
        ScaffoldSiteType.FORMATION -> listOf("page.thyme", "session.thyme", "module.thyme")
    }

    private fun fallbackOutput(intention: ScaffoldIntention): ScaffoldOutput = ScaffoldOutput(
        siteType = intention.siteType,
        projectName = intention.projectName.ifBlank { intention.description.slugify() },
        description = intention.description,
        templates = defaultTemplatesFor(intention.siteType),
        metadata = ScaffoldMetadata(
            title = intention.projectName.ifBlank { intention.description },
            description = intention.description,
            tags = listOf(intention.description.slugify()),
            layout = "post",
            language = intention.lang
        )
    )

    // ── JSON parsing helpers (lightweight, no external deps) ──────────

    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"""
        val regex = Regex(pattern)
        val match = regex.find(json) ?: return null
        val afterKey = json.substring(match.range.last + 1).trimStart()
        return when {
            afterKey.startsWith('"') -> {
                val endQuote = afterKey.indexOf('"', 1)
                if (endQuote > 0) afterKey.substring(1, endQuote) else null
            }
            afterKey.startsWith('[') || afterKey.startsWith('{') -> null
            else -> {
                val end = afterKey.indexOfAny(charArrayOf(',', '}', '\n'))
                if (end > 0) afterKey.substring(0, end).trim().trim('"') else null
            }
        }
    }

    private fun extractJsonArray(json: String, key: String): List<String> {
        val pattern = """"$key"\s*:\s*\["""
        val regex = Regex(pattern)
        val match = regex.find(json) ?: return emptyList()
        val afterBracket = json.substring(match.range.last + 1)
        val endBracket = afterBracket.indexOf(']')
        if (endBracket < 0) return emptyList()
        val arrayContent = afterBracket.substring(0, endBracket)
        return arrayContent.split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
    }

    private fun extractNestedJsonValue(json: String, parent: String, key: String): String? {
        val parentPattern = """"$parent"\s*:\s*\{"""
        val parentRegex = Regex(parentPattern)
        val parentMatch = parentRegex.find(json) ?: return null
        val afterParentOpen = json.substring(parentMatch.range.last + 1)
        val parentClose = afterParentOpen.indexOf('}')
        if (parentClose < 0) return null
        val parentBlock = afterParentOpen.substring(0, parentClose + 1)
        return extractJsonValue(parentBlock, key)
    }

    private fun extractNestedJsonArray(json: String, parent: String, key: String): List<String> {
        val parentPattern = """"$parent"\s*:\s*\{"""
        val parentRegex = Regex(parentPattern)
        val parentMatch = parentRegex.find(json) ?: return emptyList()
        val afterParentOpen = json.substring(parentMatch.range.last + 1)
        val parentClose = afterParentOpen.indexOf('}')
        if (parentClose < 0) return emptyList()
        val parentBlock = afterParentOpen.substring(0, parentClose + 1)
        return extractJsonArray(parentBlock, key)
    }

    private fun String.slugify(): String {
        return this.lowercase()
            .replace(Regex("[éèêë]"), "e")
            .replace(Regex("[àâä]"), "a")
            .replace(Regex("[ùûü]"), "u")
            .replace(Regex("[ôö]"), "o")
            .replace(Regex("[îï]"), "i")
            .replace(Regex("[ç]"), "c")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}