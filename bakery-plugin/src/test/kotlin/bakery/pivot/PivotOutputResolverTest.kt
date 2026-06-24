package bakery.pivot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Session 161 — UT pour [PivotOutputResolver].
 *
 * Baby step DDD : extraire la logique de résolution du chemin de sortie YAML
 * (défaut = `{inputSansExt}.pivot.yaml`) dans un concept pur, testable sans
 * Gradle. La convention output remplace la logique inline de PivotTaskRegistrar
 * (S159) qui calculait le défaut avant que la tâche ne soit créée.
 */
class PivotOutputResolverTest {

    @Test
    fun `default output derives from input basename`() {
        assertEquals(
            "article.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput("article.adoc")
        )
    }

    @Test
    fun `default output preserves directory of input`() {
        assertEquals(
            "site/content/article.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput("site/content/article.adoc")
        )
    }

    @Test
    fun `default output handles input without extension`() {
        assertEquals(
            "README.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput("README")
        )
    }

    @Test
    fun `default output handles dotfiles without stripping the stem`() {
        // ".adoc" seul (pas de stem) → fallback "pivot"
        assertEquals(
            "pivot.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput(".adoc")
        )
    }

    @Test
    fun `default output handles empty path`() {
        assertEquals(
            "pivot.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput("")
        )
    }

    @Test
    fun `default output handles multi-dot basename`() {
        assertEquals(
            "article.fr.pivot.yaml",
            PivotOutputResolver.resolveDefaultOutput("article.fr.adoc")
        )
    }

    @Test
    fun `resolveOutput keeps explicit output when provided`() {
        assertEquals(
            "out/custom.yaml",
            PivotOutputResolver.resolveOutput(
                input = "article.adoc",
                explicitOutput = "out/custom.yaml"
            )
        )
    }

    @Test
    fun `resolveOutput computes default when explicit output is blank`() {
        assertEquals(
            "article.pivot.yaml",
            PivotOutputResolver.resolveOutput(
                input = "article.adoc",
                explicitOutput = ""
            )
        )
    }

    @Test
    fun `resolveOutput computes default when explicit output is null`() {
        assertEquals(
            "article.pivot.yaml",
            PivotOutputResolver.resolveOutput(
                input = "article.adoc",
                explicitOutput = null
            )
        )
    }
}