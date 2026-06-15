package bakery.firebase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class ValidationIssue(
    val field: String,
    val message: String,
    val severity: IssueSeverity = IssueSeverity.ERROR
)

enum class IssueSeverity { ERROR, WARNING }
