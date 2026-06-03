package bakery.firebase

/**
 * Result of Firebase configuration validation.
 *
 * Captures errors (blocking issues) and warnings (suggestions).
 * A valid configuration has zero errors; warnings are informational only.
 */
data class FirebaseValidationResult(
    val errors: List<ValidationIssue> = emptyList(),
    val warnings: List<ValidationIssue> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty()
    
    fun addError(issue: ValidationIssue): FirebaseValidationResult =
        copy(errors = errors + issue)
    
    fun addWarning(issue: ValidationIssue): FirebaseValidationResult =
        copy(warnings = warnings + issue)
    
    fun merge(other: FirebaseValidationResult): FirebaseValidationResult =
        copy(errors = errors + other.errors, warnings = warnings + other.warnings)
}

data class ValidationIssue(
    val field: String,
    val message: String,
    val severity: IssueSeverity = IssueSeverity.ERROR
)

enum class IssueSeverity { ERROR, WARNING }
