package bakery.a11y

data class AccessibilityFinding(
    val selector: String,
    val rule: String,
    val pass: Boolean,
    val message: String
)

data class AccessibilityReport(
    val findings: List<AccessibilityFinding>,
    val passedCount: Int,
    val failedCount: Int
) {
    val isCompliant: Boolean get() = failedCount == 0
}

fun audit(findings: List<AccessibilityFinding>): AccessibilityReport {
    val passed = findings.count { it.pass }
    val failed = findings.size - passed
    return AccessibilityReport(
        findings = findings,
        passedCount = passed,
        failedCount = failed
    )
}
