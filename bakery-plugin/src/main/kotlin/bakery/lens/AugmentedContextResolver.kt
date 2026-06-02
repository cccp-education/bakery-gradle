package bakery.lens

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.context.ContextChannel
import java.io.File

/**
 * Résolveur de contexte augmenté — BKY-LENS-2.
 *
 * Lit le fichier `composite-context.json` (sortie de runner-gradle N3)
 * et extrait les canaux EAGER/RAG/GRAPHIFY/DOCS pour l'enrichissement
 * du scoring hybride dans [AugmentedArticlesService].
 *
 * Architecture :
 * ```
 * composite-context.json (runner-gradle N3)
 *     ↓ AugmentedContextResolver.resolve()
 *     ├── EAGER : gouvernance déterministe (.adoc)
 *     ├── RAG   : similarité vectorielle (pgvector)
 *     ├── GRAPHIFY : relations structurelles (graphe)
 *     └── DOCS  : corpus documentaire (codex pgvector)
 *     ↓
 *     AugmentedArticlesService.score() → articles enrichis
 * ```
 *
 * Contrat DAG : bakery (N2) importe codebase-contracts (N0).
 */
class AugmentedContextResolver {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Résout un contexte composite depuis un fichier JSON.
     *
     * @param contextFilePath Chemin vers le fichier composite-context.json
     * @return CompositeContext si le fichier existe et est valide, null sinon
     */
    fun resolve(contextFilePath: String): CompositeContext? {
        val file = File(contextFilePath)
        if (!file.exists()) return null
        return try {
            objectMapper.readValue(file, CompositeContext::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extrait les canaux de contexte d'un CompositeContext.
     *
     * Retourne une map de ChannelType → contenu textuel du canal.
     * Les canaux vides ou absents sont omis.
     *
     * @param context Contexte composite à analyser
     * @return Map des types de canaux vers leur contenu
     */
    fun extractChannels(context: CompositeContext): Map<ChannelType, String> {
        val channels = context.toChannels()
        return channels
            .filter { it.contentNonEmpty }
            .associate { it.type to it.content }
    }

    /**
     * Extrait les canaux de contexte depuis un chemin de fichier.
     * Convenience method combinant [resolve] et [extractChannels].
     *
     * @param contextFilePath Chemin vers composite-context.json
     * @return Map des types de canaux vers leur contenu, ou emptyMap si fichier absent/invalide
     */
    fun extractChannelsFromPath(contextFilePath: String): Map<ChannelType, String> {
        val context = resolve(contextFilePath) ?: return emptyMap()
        return extractChannels(context)
    }

    /**
     * Retourne les sections disponibles dans un contexte composite.
     * Utile pour le debug et le diagnostic.
     */
    fun availableSections(contextFilePath: String): Set<ChannelType> {
        return extractChannelsFromPath(contextFilePath).keys
    }
}

/**
 * Types de canaux extraits du contexte composite.
 * Alias pour faciliter l'usage dans le package lens.
 */
typealias ChannelType = contracts.context.ChannelType