package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PagefindTemplateWiringTest {
    private fun resource(path: String): String =
        requireNotNull(javaClass.classLoader.getResource(path)) { "missing resource '$path'" }
            .readText()

    @Test
    fun `the default header no longer pulls the Pagefind UI stylesheet`() {
        val header = resource("site/templates/header.thyme")

        assertThat(header).doesNotContain("_pagefind")
        assertThat(header).doesNotContain("pagefind-ui.css")
    }

    @Test
    fun `the default footer loads the headless adapter and not PagefindUI`() {
        val footer = resource("site/templates/footer.thyme")

        assertThat(footer).contains("js/search-pagefind.js")
        assertThat(footer).doesNotContain("PagefindUI")
        assertThat(footer).doesNotContain("_pagefind")
    }

    @Test
    fun `the default search modal keeps the form anchors the chrome contracts translate`() {
        val modal = resource("site/templates/blog_section.thyme")

        assertThat(modal).contains("id=\"pagefind-search-form\"")
        assertThat(modal).contains("type=\"search\"")
        assertThat(modal).contains("class=\"form-control\"")
        assertThat(modal).contains("placeholder=")
        assertThat(modal).contains("type=\"submit\"")
        assertThat(modal).contains("class=\"btn btn-primary\"")
        assertThat(modal).contains("id=\"pagefind-results\"")
        assertThat(modal).contains("pagefind/pagefind.js")
    }

    @Test
    fun `the default blog page carries the same search surface`() {
        val blog = resource("site/templates/blog.thyme")

        assertThat(blog).contains("id=\"pagefind-search-form\"")
        assertThat(blog).contains("id=\"pagefind-results\"")
        assertThat(blog).contains("pagefind/pagefind.js")
        assertThat(blog).doesNotContain("_pagefind")
    }

    @Test
    fun `the adapter is shipped in the two prototype and publication copies`() {
        val publication = resource("site/assets/js/search-pagefind.js")
        val prototype = resource("maquette/js/search-pagefind.js")

        assertThat(publication).isEqualTo(prototype)
        assertThat(publication).contains("runSearch")
        assertThat(publication).contains("data-pagefind-script")
    }
}
