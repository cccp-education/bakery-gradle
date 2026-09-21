package bakery.i18n

/**
 * CHE-I18N-UNIFY — structural sanity of a translated Thymeleaf template.
 *
 * An LLM translation can silently eat a tag's closing `">` (a real `hi` run
 * turned `<section … th:if="${…}">` into `<section … th:if="${…}`), which only
 * surfaced at bake time as a `RenderingException` — the corrupted output had
 * already been written.
 *
 * This guard checks the parseable skeleton line by line: every `<tag` opened on a
 * line must see its `>` on the same line, except inside a `<script>`/`<style>`
 * body. Plain comparison operators in text (`a &gt; b`) are not tag opens.
 *
 * Pure domain: no regex on the whole document, no Gradle, no LLM.
 */
object TemplateStructureGuard {

    fun isWellFormed(template: String): Boolean {
        var inRawBody = false
        var rawTag: String? = null

        for (line in template.lineSequence()) {
            if (inRawBody) {
                val closingTag = rawTag
                if (closingTag != null && line.lowercase().contains("</$closingTag")) {
                    inRawBody = false
                    rawTag = null
                }
                continue
            }

            val opener = rawOpener(line)
            if (opener != null) {
                inRawBody = true
                rawTag = opener
                continue
            }

            if (!isBalanced(line)) return false
        }
        return true
    }

    /** Returns `script`/`style` when the line opens such a body and never closes it. */
    private fun rawOpener(line: String): String? {
        val lower = line.lowercase()
        for (tag in listOf("script", "style")) {
            val open = lower.indexOf("<$tag")
            if (open < 0) continue
            if (lower.indexOf("</$tag", open) >= 0) continue
            return tag
        }
        return null
    }

    private fun isBalanced(line: String): Boolean {
        var index = 0
        while (true) {
            val open = line.indexOf('<', index)
            if (open < 0) return true
            val next = line.getOrNull(open + 1)
            // `</…`, `<!…` and a bare `<` (text) are not element opens.
            if (next == null || next == '/' || next == '!' || next == '?' || next.isWhitespace()) {
                index = open + 1
                continue
            }
            // A tag closes on a `>` *outside* a double-quoted attribute value:
            // `${#lists.size(x) > 1}` carries a comparison operator, not the tag
            // end — the eaten `">` case.
            val close = tagEnd(line, open)
            if (close < 0) return false
            index = close + 1
        }
    }

    private fun tagEnd(line: String, open: Int): Int {
        var inQuotes = false
        var i = open + 1
        while (i < line.length) {
            when (line[i]) {
                '"' -> inQuotes = !inQuotes
                '>' -> if (!inQuotes) return i
            }
            i++
        }
        return -1
    }
}
