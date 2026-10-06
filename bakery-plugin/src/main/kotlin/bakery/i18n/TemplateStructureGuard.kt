package bakery.i18n

/**
 * CHE-I18N-UNIFY — structural sanity of a translated Thymeleaf template.
 *
 * An LLM translation can silently eat a tag's closing `">` (a real `hi` run
 * turned `<section … th:if="${…}">` into `<section … th:if="${…}`), which only
 * surfaced at bake time as a `RenderingException` — the corrupted output had
 * already been written.
 *
 * The guard tracks the tag skeleton across the **whole document**, not line by
 * line: a tag may legitimately open on one line and close its `>` many lines
 * later (`header.thyme` `th:block th:with="…`, `menu.thyme`, the `og:image`
 * link…). A tag is malformed only when the document ends while a tag is still
 * open (the eaten `">`) or inside an unterminated comment. Plain comparison
 * operators in text (`a &gt; b`) are not tag opens, and a `>` inside a
 * double-quoted attribute value does not close the tag.
 *
 * S-250 (bakery) — the previous line-local rule rejected **every reference
 * template carrying a multi-line tag** (13 of the 20 cheroliv.com templates),
 * which in turn blocked `id`/`ko`/`sr`/`fa` translation: the model output was
 * structurally sound, the guard was wrong. The whole-document scan accepts the
 * real corpus and still rejects the eaten-quote defect.
 *
 * Pure domain: no regex on the whole document, no Gradle, no LLM.
 */
object TemplateStructureGuard {

    private val RAW_TAGS = listOf("script", "style")

    fun isWellFormed(template: String): Boolean {
        var inTag = false
        var inQuotes = false
        var inComment = false
        var rawTag: String? = null

        for (line in template.lineSequence()) {
            if (rawTag != null) {
                if (line.contains("</$rawTag", ignoreCase = true)) rawTag = null
                continue
            }

            var i = 0
            while (i < line.length) {
                if (inComment) {
                    if (line.startsWith("-->", i)) {
                        inComment = false
                        i += 3
                    } else {
                        i++
                    }
                    continue
                }
                if (inTag) {
                    when (line[i]) {
                        '"' -> inQuotes = !inQuotes
                        '>' -> if (!inQuotes) {
                            inTag = false
                            inQuotes = false
                        }
                    }
                    i++
                    continue
                }
                if (line[i] == '<') {
                    if (line.startsWith("<!--", i)) {
                        inComment = true
                        i += 4
                        continue
                    }
                    val next = line.getOrNull(i + 1)
                    // `</…`, `<!…`, `<?…` and a bare `<` (text) are not element opens.
                    if (next != null && next != '/' && next != '!' && next != '?' && !next.isWhitespace()) {
                        inTag = true
                        inQuotes = false
                    }
                    i++
                    continue
                }
                i++
            }

            // A raw `<script>`/`<style>` body is never parsed as markup: skip it
            // until its closing tag. Only opened when the line leaves the body
            // open (a `</script>` on the same line is already balanced).
            if (!inTag && !inComment) {
                val lower = line.lowercase()
                for (tag in RAW_TAGS) {
                    val open = lower.indexOf("<$tag")
                    if (open < 0) continue
                    if (lower.indexOf("</$tag", open) >= 0) continue
                    rawTag = tag
                }
            }
        }
        return !inTag && !inComment
    }
}
