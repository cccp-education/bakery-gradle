package bakery.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SearchBundleTest {
    @Test
    fun `the bundle lives in pagefind and never the historical underscore directory`() {
        assertThat(SearchBundle.DIRECTORY).isEqualTo("pagefind")
        assertThat(SearchBundle.scriptPath()).doesNotContain("_pagefind")
    }

    @Test
    fun `the script path is resolved from the site root`() {
        assertThat(SearchBundle.scriptPath()).isEqualTo("pagefind/pagefind.js")
    }

    @Test
    fun `the script path is resolved from a one level variant`() {
        assertThat(SearchBundle.scriptPath("../")).isEqualTo("../pagefind/pagefind.js")
    }

    @Test
    fun `the script path is resolved from a two level page`() {
        assertThat(SearchBundle.scriptPath("../../")).isEqualTo("../../pagefind/pagefind.js")
    }

    @Test
    fun `the entry path follows the same root rule`() {
        assertThat(SearchBundle.entryPath()).isEqualTo("pagefind/pagefind-entry.json")
        assertThat(SearchBundle.entryPath("../")).isEqualTo("../pagefind/pagefind-entry.json")
    }

    @Test
    fun `the directory path keeps its trailing slash`() {
        assertThat(SearchBundle.directoryPath()).isEqualTo("pagefind/")
        assertThat(SearchBundle.directoryPath("../../")).isEqualTo("../../pagefind/")
    }
}
