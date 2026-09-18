package bakery.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * CHE-I18N-22 US-7b — the template delta planner decides, per template, whether
 * a language still needs it translated.
 *
 * The historical behaviour skipped a whole language as soon as its `templates/`
 * directory existed: the eight cheroliv.com variants that carry seven templates
 * byte-identical to the French reference could never converge. Ink Economy: only
 * a missing or still-French template is sent to the model; a translated copy is
 * preserved.
 */
class TemplateTranslationPlannerTest {

    @Test
    fun `a missing template is scheduled`() {
        val reference = mapOf("hero.thyme" to "<h1>Développeur</h1>")

        val plan = TemplateTranslationPlanner.plan(reference, emptyMap())

        assertEquals(listOf("hero.thyme"), plan)
    }

    @Test
    fun `a template byte-identical to the reference is scheduled`() {
        val reference = mapOf("hero.thyme" to "<h1>Développeur</h1>")
        val target = mapOf("hero.thyme" to "<h1>Développeur</h1>")

        val plan = TemplateTranslationPlanner.plan(reference, target)

        assertEquals(listOf("hero.thyme"), plan)
    }

    @Test
    fun `a translated template is preserved`() {
        val reference = mapOf("hero.thyme" to "<h1>Développeur</h1>")
        val target = mapOf("hero.thyme" to "<h1>Developer</h1>")

        val plan = TemplateTranslationPlanner.plan(reference, target)

        assertEquals(emptyList(), plan)
    }

    @Test
    fun `only the french copies of a partial variant are scheduled`() {
        val reference =
            mapOf(
                "hero.thyme" to "<h1>Développeur</h1>",
                "blog.thyme" to "<p>Derniers articles</p>",
                "menu.thyme" to "<a>Accueil</a>",
            )
        val target =
            mapOf(
                "hero.thyme" to "<h1>Developer</h1>",
                "blog.thyme" to "<p>Derniers articles</p>",
                "menu.thyme" to "<a>Home</a>",
            )

        val plan = TemplateTranslationPlanner.plan(reference, target)

        assertEquals(listOf("blog.thyme"), plan)
    }

    @Test
    fun `the plan is ordered as the reference walk`() {
        val reference =
            linkedMapOf(
                "a.thyme" to "<p>Une</p>",
                "b.thyme" to "<p>Deux</p>",
                "c.thyme" to "<p>Trois</p>",
            )
        val target = mapOf("b.thyme" to "<p>Two</p>")

        val plan = TemplateTranslationPlanner.plan(reference, target)

        assertEquals(listOf("a.thyme", "c.thyme"), plan)
    }

    @Test
    fun `a target-only template is never scheduled`() {
        val reference = mapOf("hero.thyme" to "<h1>Développeur</h1>")
        val target =
            mapOf(
                "hero.thyme" to "<h1>Developer</h1>",
                "extra.thyme" to "<p>Extra</p>",
            )

        val plan = TemplateTranslationPlanner.plan(reference, target)

        assertEquals(emptyList(), plan)
    }

    @Test
    fun `an empty reference yields no plan`() {
        assertEquals(emptyList(), TemplateTranslationPlanner.plan(emptyMap(), emptyMap()))
    }

    @Test
    fun `a forced language regenerates every template`() {
        val reference =
            linkedMapOf(
                "hero.thyme" to "<h1>Développeur</h1>",
                "header.thyme" to "<title>Cheroliv</title>",
            )
        val target =
            mapOf(
                "hero.thyme" to "<h1>Developer</h1>",
                "header.thyme" to "<title>Cheroliv</title>",
            )

        val plan = TemplateTranslationPlanner.plan(reference, target, force = true)

        assertEquals(listOf("hero.thyme", "header.thyme"), plan)
    }

    @Test
    fun `a forced language with no target still schedules every template`() {
        val reference = linkedMapOf("hero.thyme" to "<h1>Développeur</h1>")

        val plan = TemplateTranslationPlanner.plan(reference, emptyMap(), force = true)

        assertEquals(listOf("hero.thyme"), plan)
    }

    @Test
    fun `parallelism inside one to twenty-five is accepted`() {
        assertEquals(1, TemplateTranslationPlanner.requireParallelism(1))
        assertEquals(25, TemplateTranslationPlanner.requireParallelism(25))
    }

    @Test
    fun `parallelism outside one to twenty-five is rejected`() {
        assertFailsWith<IllegalArgumentException> { TemplateTranslationPlanner.requireParallelism(0) }
        assertFailsWith<IllegalArgumentException> { TemplateTranslationPlanner.requireParallelism(26) }
    }
}
