package bakery.search

/**
 * BKY-SEARCH-1 — the single rule of the Pagefind bundle layout.
 *
 * Pagefind 1.6 writes its bundle to `pagefind/` at the bake root. The scaffold
 * templates historically referenced `_pagefind/` (Pagefind < 1.5), which is a
 * 404 against the current output and would also be dropped by GitHub Pages'
 * Jekyll pass when no `.nojekyll` marker is published. One directory name, one
 * script entry point and one entry file are shared by the templates, the
 * runtime adapter and the guards, so the three cannot drift.
 *
 * The bundle holds **one independent index per language**. Pagefind reads the
 * page's `<html lang>` and loads only that index at runtime: there is no
 * monolithic common index to download.
 *
 * A consumer page may live below the site root (`/en/blog/post.html`); the
 * `rootpath` argument is the relative path back to the language tree root
 * (`""`, `"../"`, `"../../"`), never a leading slash.
 *
 * Pure rule: string composition only, no I/O, no Gradle, no template engine.
 */
object SearchBundle {
    /** Directory Pagefind writes, relative to the bake root. */
    const val DIRECTORY: String = "pagefind"

    /** Headless JS API entry point, loaded lazily by the runtime adapter. */
    const val SCRIPT: String = "pagefind.js"

    /** Entry file listing the per-language indexes and their page counts. */
    const val ENTRY: String = "pagefind-entry.json"

    /** DOM id of the result container the adapter fills. */
    const val RESULTS_ID: String = "pagefind-results"

    /** DOM id of the search form the chrome contract keeps translating. */
    const val FORM_ID: String = "pagefind-search-form"

    /** `pagefind/` from the given page root. */
    fun directoryPath(rootpath: String = ""): String = "${rootBase(rootpath)}$DIRECTORY/"

    /** `pagefind/pagefind.js` from the given page root. */
    fun scriptPath(rootpath: String = ""): String = "${rootBase(rootpath)}$DIRECTORY/$SCRIPT"

    /** `pagefind/pagefind-entry.json` from the given page root. */
    fun entryPath(rootpath: String = ""): String = "${rootBase(rootpath)}$DIRECTORY/$ENTRY"

    private fun rootBase(rootpath: String): String = if (rootpath.isBlank()) "" else rootpath.trimEnd('/') + "/"
}
