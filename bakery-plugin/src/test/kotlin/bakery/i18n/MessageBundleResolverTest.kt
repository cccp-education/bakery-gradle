package bakery.i18n

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * BKY-LANG-NAV-8 — resolves a Thymeleaf template written with message keys
 * (`#{key}`) into a *full-swap* literal template, from a frozen bundle.
 *
 * `jbake-core:2.7.0` installs **no MessageResolver**: the `#{key}` bundles
 * `migrateToI18n` produces are never resolved at bake time (same reason
 * `translateTemplates` uses a full-template swap). A site that already owns a
 * frozen `messages_{lang}.properties` (the i18n golden masters) can therefore
 * materialise its translated templates **without any LLM call** — the values
 * were computed once and are reused (Ink Economy Law).
 *
 * Domain-pure: strings in, strings out. No I/O, no Gradle, no LLM.
 */
class MessageBundleResolverTest {
    private val bundle =
        mapOf(
            "menu.1" to "Home",
            "menu.8" to "Select interface theme",
            "index.1" to "Common Content Creator Proletarian.",
        )

    @Test
    fun `resolves a key used as a text node`() {
        val template = """<a th:text="#{menu.1}">Accueil</a>"""

        assertThat(MessageBundleResolver.resolve(template, bundle).content)
            .isEqualTo("""<a th:text="Home">Accueil</a>""")
    }

    @Test
    fun `resolves a key used inside an attribute expression`() {
        val template = """<button th:attr="aria-label=#{menu.8}">x</button>"""

        assertThat(MessageBundleResolver.resolve(template, bundle).content)
            .isEqualTo("""<button th:attr="aria-label=Select interface theme">x</button>""")
    }

    @Test
    fun `preserves an unknown key and reports it`() {
        val template = """<p th:text="#{missing.9}">?</p>"""

        val result = MessageBundleResolver.resolve(template, bundle)

        assertThat(result.content).isEqualTo(template)
        assertThat(result.unresolvedKeys).containsExactly("missing.9")
    }

    @Test
    fun `an empty bundle leaves the template untouched and reports every key`() {
        val template = """<a th:text="#{menu.1}">Accueil</a>"""

        val result = MessageBundleResolver.resolve(template, emptyMap())

        assertThat(result.content).isEqualTo(template)
        assertThat(result.unresolvedKeys).containsExactly("menu.1")
    }

    @Test
    fun `a template without any key is a strict no-op`() {
        val template = """<a href="about.html">About</a>"""

        val result = MessageBundleResolver.resolve(template, bundle)

        assertThat(result.content).isEqualTo(template)
        assertThat(result.unresolvedKeys).isEmpty()
    }

    @Test
    fun `resolving an already resolved template is idempotent`() {
        val template = """<a th:text="#{menu.1}">Accueil</a>"""

        val once = MessageBundleResolver.resolve(template, bundle).content
        val twice = MessageBundleResolver.resolve(once, bundle).content

        assertThat(twice).isEqualTo(once)
        assertThat(MessageBundleResolver.resolve(once, bundle).unresolvedKeys).isEmpty()
    }

    @Test
    fun `resolves every occurrence of the same key`() {
        val template = """<a th:text="#{menu.1}">x</a> <span th:text="#{menu.1}">y</span>"""

        assertThat(MessageBundleResolver.resolve(template, bundle).content)
            .isEqualTo("""<a th:text="Home">x</a> <span th:text="Home">y</span>""")
    }
}
