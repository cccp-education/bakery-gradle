package bakery.i18n

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.text.Charsets.UTF_8

/**
 * BKY-LANG-NAV-8 — proves the frozen chrome translations are reusable with zero
 * LLM call (Ink Economy Law).
 *
 * The i18n golden masters (`i18n-fixtures/{site}/post-migration/templates/`) were
 * migrated once (S-132/S-150) and their `#{key}` templates are byte-identical to
 * the real site templates. Their EN values are frozen in
 * `translations_en.properties`. `jbake-core:2.7.0` has no MessageResolver, so a
 * site that wants a deployable EN variant must materialise literal templates from
 * that bundle — exactly what [MessageBundleResolver] does, once, at build time.
 *
 * This test replays that materialisation on the two real fixtures and asserts:
 *   1. every `#{key}` of every template resolves (no silently dropped text);
 *   2. the homepage marketing copy is present in EN — the homepage lives in
 *      `index.thyme` behind `index.N` keys, so the whole EN landing page is
 *      obtained without translating a single content article.
 */
class FrozenBundleReuseIntegrationTest {
    @Test
    fun `cccp-education golden master resolves every key with the frozen bundle`() {
        val result = resolveFixture("cccp-education")

        assertThat(result.unresolvedKeys)
            .describedAs("frozen bundle must cover every key of the migrated templates")
            .isEmpty()
        assertThat(result.resolved["index.thyme"])
            .describedAs("the EN homepage must be materialised from the frozen chrome")
            .contains("Common Content Creator Proletarian")
            .contains("Discover the plugins")
            .contains("100% Open Source bricks")
    }

    @Test
    fun `magic-stick golden master resolves every key with the frozen bundle`() {
        val result = resolveFixture("magic-stick")

        assertThat(result.unresolvedKeys)
            .describedAs("frozen bundle must cover every key of the migrated templates")
            .isEmpty()
        assertThat(result.resolved["index.thyme"])
            .describedAs("the EN homepage must be materialised from the frozen chrome")
            .contains("Standardized portable environment on bootable USB key.")
            .contains("A/B Architecture")
            .contains("Back to top")
    }

    private data class FixtureResolution(
        val resolved: Map<String, String>,
        val unresolvedKeys: List<String>,
    )

    private fun resolveFixture(siteId: String): FixtureResolution {
        val loader = javaClass.classLoader
        val translationsUrl =
            loader.getResource("i18n-fixtures/$siteId/translations_en.properties")
                ?: error("translations_en.properties absent for $siteId")
        val bundle = Properties()
        translationsUrl.openStream().use { bundle.load(it) }
        val bundleMap = bundle.map { it.key.toString() to it.value.toString() }.toMap()

        val templatesDir =
            loader.getResource("i18n-fixtures/$siteId/post-migration/templates")
                ?: error("post-migration templates absent for $siteId")
        val dir = java.io.File(templatesDir.toURI())

        val resolved = linkedMapOf<String, String>()
        val unresolved = mutableListOf<String>()
        dir
            .walkTopDown()
            .filter { it.isFile && it.extension == "thyme" }
            .forEach { file ->
                val result = MessageBundleResolver.resolve(file.readText(UTF_8), bundleMap)
                resolved[file.name] = result.content
                unresolved += result.unresolvedKeys
            }
        return FixtureResolution(resolved, unresolved)
    }
}
