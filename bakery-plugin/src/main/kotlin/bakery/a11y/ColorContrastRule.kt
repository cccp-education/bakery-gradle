package bakery.a11y

import kotlin.math.pow

data class ContrastResult(
    val ratio: Double,
    val passAa: Boolean,
    val passAaa: Boolean
)

private const val AA_THRESHOLD = 4.5
private const val AAA_THRESHOLD = 7.0

fun evaluateContrast(foregroundHex: String, backgroundHex: String): ContrastResult {
    val ratio = contrastRatio(foregroundHex, backgroundHex)
    return ContrastResult(
        ratio = ratio,
        passAa = ratio >= AA_THRESHOLD,
        passAaa = ratio >= AAA_THRESHOLD
    )
}

private fun contrastRatio(foregroundHex: String, backgroundHex: String): Double {
    val l1 = relativeLuminance(foregroundHex)
    val l2 = relativeLuminance(backgroundHex)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(hex: String): Double {
    val rgb = parseHex(hex)
    val r = luminanceComponent(rgb.first / 255.0)
    val g = luminanceComponent(rgb.second / 255.0)
    val b = luminanceComponent(rgb.third / 255.0)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun luminanceComponent(c: Double): Double =
    if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

private fun parseHex(hex: String): Triple<Int, Int, Int> {
    val normalized = hex.trimStart('#').uppercase()
    require(normalized.length == 6) { "Unsupported hex color: $hex" }
    return Triple(
        normalized.substring(0, 2).toInt(16),
        normalized.substring(2, 4).toInt(16),
        normalized.substring(4, 6).toInt(16)
    )
}
