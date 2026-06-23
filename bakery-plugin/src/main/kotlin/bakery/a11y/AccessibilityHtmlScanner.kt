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
