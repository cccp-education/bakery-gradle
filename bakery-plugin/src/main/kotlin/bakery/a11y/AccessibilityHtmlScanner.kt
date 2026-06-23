package bakery.a11y

fun scanInlineColors(html: String): List<AccessibilityFinding> {
    val tagRegex = Regex("""<(\w+)[^>]*style\s*=\s*"([^"]*)"[^>]*>""")
    return tagRegex.findAll(html).mapNotNull { match ->
        val tag = match.groupValues[1]
        val style = match.groupValues[2]
        val color = extractColor(style, "color")
        val background = extractColor(style, "background-color")
        if (color != null && background != null) {
            val result = evaluateContrast(color, background)
            AccessibilityFinding(
                selector = "$tag[style]",
                rule = "color-contrast",
                pass = result.passAa,
                message = if (result.passAa) "Contrast ${"%.2f".format(result.ratio)} >= 4.5" else "Contrast ${"%.2f".format(result.ratio)} < 4.5"
            )
        } else null
    }.toList()
}

private fun extractColor(style: String, property: String): String? {
    val regex = Regex("""$property\s*:\s*([#A-Za-z0-9]+)""")
    return regex.find(style)?.groupValues?.get(1)
}

fun scanStructural(html: String): List<AccessibilityFinding> {
    val findings = mutableListOf<AccessibilityFinding>()
    findings.addAll(scanImgAlt(html))
    findings.addAll(scanLinkAriaLabel(html))
    findings.addAll(scanHeadingOrder(html))
    return findings
}

private fun scanImgAlt(html: String): List<AccessibilityFinding> {
    val imgRegex = Regex("""<img\b[^>]*>""")
    return imgRegex.findAll(html).map { match ->
        val tag = match.value
        val hasAlt = Regex("""\balt\s*=\s*"[^"]*"""").containsMatchIn(tag) ||
            Regex("""\balt\s*=\s*'[^']*'""").containsMatchIn(tag)
        val altValue = Regex("""\balt\s*=\s*"([^"]*)"""").find(tag)?.groupValues?.get(1)
            ?: Regex("""\balt\s*=\s*'([^']*)'""").find(tag)?.groupValues?.get(1)
            ?: ""
        val pass = hasAlt && altValue.isNotBlank()
        AccessibilityFinding(
            selector = "img",
            rule = "img-alt",
            pass = pass,
            message = if (pass) "Image has alt text" else "Image missing or empty alt attribute"
        )
    }.toList()
}

private fun scanLinkAriaLabel(html: String): List<AccessibilityFinding> {
    val linkRegex = Regex("""<a\b[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
    return linkRegex.findAll(html).map { match ->
        val fullTag = match.groupValues[0]
        val inner = match.groupValues[1].trim()
        val textContent = inner.replace(Regex("""<[^>]+>"""), "").trim()
        val hasAriaLabel = Regex("""\baria-label\s*=\s*"[^"]*"""").containsMatchIn(fullTag) ||
            Regex("""\baria-label\s*=\s*'[^']*'""").containsMatchIn(fullTag)
        val pass = textContent.isNotBlank() || hasAriaLabel
        AccessibilityFinding(
            selector = "a",
            rule = "link-aria-label",
            pass = pass,
            message = if (pass) "Link has text or aria-label" else "Link without text and aria-label"
        )
    }.toList()
}

private fun scanHeadingOrder(html: String): List<AccessibilityFinding> {
    val headingRegex = Regex("""<h([1-6])\b[^>]*>""")
    val levels = headingRegex.findAll(html).map { it.groupValues[1].toInt() }.toList()
    val findings = mutableListOf<AccessibilityFinding>()
    for (i in 1 until levels.size) {
        val prev = levels[i - 1]
        val curr = levels[i]
        if (curr > prev + 1) {
            findings.add(
                AccessibilityFinding(
                    selector = "h$curr",
                    rule = "heading-order",
                    pass = false,
                    message = "Heading skip: h$prev followed by h$curr (expected h${prev + 1} or lower)"
                )
            )
        }
    }
    return findings
}
