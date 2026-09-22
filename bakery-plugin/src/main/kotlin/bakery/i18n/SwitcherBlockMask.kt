package bakery.i18n

/**
 * CHE-I18N-QUALITY — masks the deterministic language-switcher block out of a
 * template before it is sent to the model.
 *
 * The block lives between explicit markers (written by the site's
 * `LangSwitchBlockInjector`) and lists every language in its own name. It is
 * generated markup, re-injected after each translation — never author prose.
 * Sending it to the LLM wastes the metered budget and corrupts the large
 * `menu.thyme` skeleton (`ja` was rejected on every run).
 *
 * [mask] replaces each marked block by an opaque placeholder; [unmask] restores
 * them byte for byte after the translated skeleton has been substituted. A
 * template without markers is a strict no-op.
 *
 * Pure domain: no I/O, no LLM, no Gradle.
 */
object SwitcherBlockMask {

    const val START_MARKER = "<!-- che-i18n:lang-switcher:start -->"
    const val END_MARKER = "<!-- che-i18n:lang-switcher:end -->"

    private const val PLACEHOLDER_PREFIX = "@@CHE_I18N_SWITCHER_"
    private const val PLACEHOLDER_SUFFIX = "@@"

    data class Masked(
        val text: String,
        val blocks: List<String>,
    )

    fun mask(template: String): Masked {
        if (!template.contains(START_MARKER)) return Masked(template, emptyList())

        val blocks = mutableListOf<String>()
        val builder = StringBuilder(template.length)
        var cursor = 0
        while (true) {
            val start = template.indexOf(START_MARKER, cursor)
            if (start < 0) {
                builder.append(template, cursor, template.length)
                break
            }
            val end = template.indexOf(END_MARKER, start)
            if (end < 0) {
                builder.append(template, cursor, template.length)
                break
            }
            val blockEnd = end + END_MARKER.length
            builder.append(template, cursor, start)
            builder.append(placeholder(blocks.size))
            blocks += template.substring(start, blockEnd)
            cursor = blockEnd
        }
        return Masked(builder.toString(), blocks)
    }

    fun unmask(
        text: String,
        blocks: List<String>,
    ): String {
        if (blocks.isEmpty()) return text
        var output = text
        blocks.forEachIndexed { index, block ->
            output = output.replace(placeholder(index), block)
        }
        return output
    }

    private fun placeholder(index: Int): String = "$PLACEHOLDER_PREFIX$index$PLACEHOLDER_SUFFIX"
}
