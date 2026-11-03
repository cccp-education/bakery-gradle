package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PagefindTemplateWiringTest {
    private fun resource(path: String): String =
        requireNotNull(javaClass.classLoader.getResource(path)) { "missing resource '$path'" }
            .readText()

    @Test
    fun `the default header points at the bundle Pagefind actually writes`() {
        val header = resource("site/templates/header.thyme")

        assertThat(header).contains("pagefind/")
        assertThat(header).doesNotContain("_pagefind")
    }

    @Test
    fun `the default footer points at the bundle Pagefind actually writes`() {
        val footer = resource("site/templates/footer.thyme")

        assertThat(footer).contains("pagefind/")
        assertThat(footer).doesNotContain("_pagefind")
    }
}
