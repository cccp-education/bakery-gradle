package bakery.seo

/**
 * Injects a rendered SEO fragment into a Thymeleaf header template (EPIC BKY-SEO).
 *
 * If the header contains a `<!-- SEO: bakery --> ... <!-- /SEO: bakery -->` marker
 * block, it is replaced wholesale. Otherwise the fragment is injected right
 * after the opening `<head>` tag.
 *
 * Idempotent: re-injecting the same fragment on an already-injected header
 * yields the same output (the marker block is replaced, not re-added).
 */
class SeoInjector {
    fun inject(
        headerThyme: String,
        renderedFragment: String,
    ): String {
        val markerRegex =
            Regex(
                "<!-- SEO: bakery -->[\\s\\S]*?<!-- /SEO: bakery -->",
                RegexOption.MULTILINE,
            )
        val markerMatch = markerRegex.find(headerThyme)
        if (markerMatch != null) {
            return headerThyme.replace(markerMatch.value, renderedFragment)
        }
        if (headerThyme.contains(renderedFragment)) {
            return headerThyme
        }
        val headRegex = Regex("(<head>)(\\s*\\n?)")
        val headMatch = headRegex.find(headerThyme)
        if (headMatch != null) {
            val openingTag = headMatch.groupValues[1]
            val trailingWhitespace = headMatch.groupValues[2]
            return headerThyme.replace(headMatch.value, "$openingTag\n$renderedFragment$trailingWhitespace")
        }
        return renderedFragment + "\n" + headerThyme
    }
}