package bakery.lens

import contracts.context.ChannelType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * BKY-LENS-2.2 — Tests unitaires pour AugmentedContextResolver (résolution JSON).
 *
 * Méthodologie : DDD/TDD baby steps — chaque test compile ET passe AVANT de passer au suivant.
 *
 * Fixture : fichiers composite-context.json valides/invalides/inexistants.
 */
class AugmentedContextResolverTest {

    private lateinit var resolver: AugmentedContextResolver

    @TempDir
    lateinit var tempDir: File

    // ─── Fixture JSON manuel (évite les problèmes de sérialisation data class) ───
    private fun validCompositeContextJson(): String = """{
        "eagerSection": "Eager content: rules and governance.",
        "ragSection": "RAG content: similarity scores.",
        "graphifySection": "Graphify: structural relations.",
        "docsSection": "Docs: corpus documentation.",
        "config": {
            "totalTokenBudget": 8000,
            "budgetEagerLazy": 0.40,
            "budgetRag": 0.30,
            "budgetGraphify": 0.20,
            "budgetDocs": 0.10,
            "budgetOverhead": 0.0
        }
    }""".trimIndent()

    private fun emptyChannelsJson(): String = """{
        "eagerSection": "content",
        "ragSection": "",
        "graphifySection": "",
        "docsSection": "",
        "config": {
            "totalTokenBudget": 8000,
            "budgetEagerLazy": 0.40,
            "budgetRag": 0.30,
            "budgetGraphify": 0.20,
            "budgetDocs": 0.10,
            "budgetOverhead": 0.0
        }
    }""".trimIndent()

    @BeforeEach
    fun setUp() {
        resolver = AugmentedContextResolver()
    }

    // ──────────────────────────────────────
    // LENS-2.2 : resolve()
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedContextResolver — resolve()")
    inner class Resolve {

        @Test
        @DisplayName("resolve() avec fichier composite-context.json valide → CompositeContext")
        fun `resolve returns CompositeContext for valid file`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val result = resolver.resolve(contextFile.absolutePath)

            assertThat(result).isNotNull
            assertThat(result!!.eagerSection).isEqualTo("Eager content: rules and governance.")
            assertThat(result.ragSection).isEqualTo("RAG content: similarity scores.")
            assertThat(result.graphifySection).isEqualTo("Graphify: structural relations.")
            assertThat(result.docsSection).isEqualTo("Docs: corpus documentation.")
            assertThat(result.config.totalTokenBudget).isEqualTo(8000)
        }

        @Test
        @DisplayName("resolve() avec fichier inexistant → null")
        fun `resolve returns null for non-existent file`() {
            val result = resolver.resolve("/nonexistent/composite-context.json")
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("resolve() avec fichier invalide → null")
        fun `resolve returns null for invalid file`() {
            val invalidFile = File(tempDir, "invalid.json")
            invalidFile.writeText("{ invalid json }")

            val result = resolver.resolve(invalidFile.absolutePath)
            assertThat(result).isNull()
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : extractChannels()
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedContextResolver — extractChannels()")
    inner class ExtractChannels {

        @Test
        @DisplayName("extractChannels() retourne les canaux non vides")
        fun `extractChannels returns non-empty channels`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val context = resolver.resolve(contextFile.absolutePath)!!
            val channels = resolver.extractChannels(context)

            assertThat(channels).isNotEmpty
            assertThat(channels.keys).containsAnyOf(
                ChannelType.EAGER,
                ChannelType.RAG,
                ChannelType.GRAPHIFY,
                ChannelType.DOCS
            )
        }

        @Test
        @DisplayName("extractChannels() omet les canaux vides")
        fun `extractChannels omits empty channels`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(emptyChannelsJson())

            val context = resolver.resolve(contextFile.absolutePath)!!
            val channels = resolver.extractChannels(context)

            assertThat(channels.keys).containsExactly(ChannelType.EAGER)
            assertThat(channels).doesNotContainKey(ChannelType.RAG)
            assertThat(channels).doesNotContainKey(ChannelType.GRAPHIFY)
        }

        @Test
        @DisplayName("extractChannels() contient le bon contenu")
        fun `extractChannels has correct content`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val context = resolver.resolve(contextFile.absolutePath)!!
            val channels = resolver.extractChannels(context)

            assertThat(channels[ChannelType.EAGER]).contains("Eager content")
            assertThat(channels[ChannelType.RAG]).contains("similarity")
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : extractChannelsFromPath()
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedContextResolver — extractChannelsFromPath()")
    inner class ExtractChannelsFromPath {

        @Test
        @DisplayName("extractChannelsFromPath() avec fichier valide → canaux")
        fun `extractChannelsFromPath returns channels for valid file`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val channels = resolver.extractChannelsFromPath(contextFile.absolutePath)

            assertThat(channels).isNotEmpty
            assertThat(channels.keys).containsAnyOf(
                ChannelType.EAGER,
                ChannelType.RAG,
                ChannelType.GRAPHIFY,
                ChannelType.DOCS
            )
        }

        @Test
        @DisplayName("extractChannelsFromPath() avec fichier inexistant → emptyMap")
        fun `extractChannelsFromPath returns emptyMap for non-existent file`() {
            val channels = resolver.extractChannelsFromPath("/nonexistent/composite-context.json")
            assertThat(channels).isEmpty()
        }

        @Test
        @DisplayName("extractChannelsFromPath() avec fichier invalide → emptyMap")
        fun `extractChannelsFromPath returns emptyMap for invalid file`() {
            val invalidFile = File(tempDir, "invalid.json")
            invalidFile.writeText("{ invalid json }")

            val channels = resolver.extractChannelsFromPath(invalidFile.absolutePath)
            assertThat(channels).isEmpty()
        }
    }

    // ──────────────────────────────────────
    // LENS-2.2 : availableSections()
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.2 : AugmentedContextResolver — availableSections()")
    inner class AvailableSections {

        @Test
        @DisplayName("availableSections() retourne les sections disponibles")
        fun `availableSections returns available sections`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val sections = resolver.availableSections(contextFile.absolutePath)

            assertThat(sections).isNotEmpty
            assertThat(sections).containsAnyOf(
                ChannelType.EAGER,
                ChannelType.RAG,
                ChannelType.GRAPHIFY,
                ChannelType.DOCS
            )
        }

        @Test
        @DisplayName("availableSections() avec fichier inexistant → emptySet")
        fun `availableSections returns emptySet for non-existent file`() {
            val sections = resolver.availableSections("/nonexistent/composite-context.json")
            assertThat(sections).isEmpty()
        }

        @Test
        @DisplayName("availableSections() ne contient pas les canaux vides")
        fun `availableSections does not include empty channel types`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(emptyChannelsJson())

            val sections = resolver.availableSections(contextFile.absolutePath)

            assertThat(sections).containsExactly(ChannelType.EAGER)
        }
    }

    // ──────────────────────────────────────
    // LENS-2.3 : Boundary cases Resolver
    // ──────────────────────────────────────

    @Nested
    @DisplayName("LENS-2.3 : AugmentedContextResolver — Boundary cases")
    inner class BoundaryCases {

        @Test
        @DisplayName("resolve() avec fichier vide → null")
        fun `resolve returns null for empty file`() {
            val emptyFile = File(tempDir, "empty.json")
            emptyFile.writeText("")

            val result = resolver.resolve(emptyFile.absolutePath)
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("extractChannels avec toutes les sections vides → map vide")
        fun `extractChannels with all empty sections returns empty map`() {
            val allEmptyJson = """{
                "eagerSection": "",
                "ragSection": "",
                "graphifySection": "",
                "docsSection": "",
                "config": {
                    "totalTokenBudget": 8000,
                    "budgetEagerLazy": 0.40,
                    "budgetRag": 0.30,
                    "budgetGraphify": 0.20,
                    "budgetDocs": 0.10,
                    "budgetOverhead": 0.0
                }
            }""".trimIndent()
            val contextFile = File(tempDir, "all-empty.json")
            contextFile.writeText(allEmptyJson)

            val context = resolver.resolve(contextFile.absolutePath)!!
            val channels = resolver.extractChannels(context)

            assertThat(channels).isEmpty()
        }

        @Test
        @DisplayName("extractChannelsFromPath cohérent avec resolve + extractChannels")
        fun `extractChannelsFromPath consistent with resolve and extractChannels`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val pathResult = resolver.extractChannelsFromPath(contextFile.absolutePath)
            val resolveResult = resolver.resolve(contextFile.absolutePath)!!
            val manualResult = resolver.extractChannels(resolveResult)

            assertThat(pathResult).isEqualTo(manualResult)
        }

        @Test
        @DisplayName("availableSections cohérent avec extractChannelsFromPath")
        fun `availableSections consistent with extractChannelsFromPath`() {
            val contextFile = File(tempDir, "composite-context.json")
            contextFile.writeText(validCompositeContextJson())

            val sections = resolver.availableSections(contextFile.absolutePath)
            val channels = resolver.extractChannelsFromPath(contextFile.absolutePath)

            assertThat(sections).isEqualTo(channels.keys)
        }
    }
}
