package bakery.workspace

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * BKY-CI-ISOLATION (US-2c) — the injected published-catalog version is the only
 * source of truth; its absence must fail loudly.
 */
class PublishedCatalogVersionTest {
    @Test
    fun `an injected property is returned as-is`() {
        val resolved =
            PublishedCatalogVersion.require("bakery.publishedCatalog.bomVersion") { "0.0.58" }

        assertThat(resolved).isEqualTo("0.0.58")
    }

    @Test
    fun `an absent property fails explicitly instead of degrading`() {
        assertThatThrownBy {
            PublishedCatalogVersion.require("bakery.publishedCatalog.bomVersion") { null }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("bakery.publishedCatalog.bomVersion")
            .hasMessageContaining("dépôt voisin")
    }

    @Test
    fun `a blank property fails explicitly, never silently green`() {
        assertThatThrownBy {
            PublishedCatalogVersion.require("bakery.publishedCatalog.bomVersion") { "   " }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("bakery.publishedCatalog.bomVersion")
    }
}
