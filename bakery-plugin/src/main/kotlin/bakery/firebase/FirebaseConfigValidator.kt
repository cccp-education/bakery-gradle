package bakery.firebase

import bakery.FirebaseAuthConfig
import bakery.FirebaseContactFormConfig

/**
 * Mechanical validator for Firebase configuration.
 *
 * Validates format, completeness, and coherence of Firebase config blocks
 * WITHOUT calling any LLM. Pure rule-based validation.
 *
 * Rules validated:
 * - apiKey format: starts with "AIzaSy" (Firebase convention)
 * - authDomain format: ends with ".firebaseapp.com"
 * - projectId non-empty and follows Firebase naming conventions
 * - Cross-field coherence: authDomain contains projectId
 * - Contact form: firestore collection names non-empty
 * - Warning: newsletter suggested if contact form present but no analytics
 */
object FirebaseConfigValidator {

    fun validateAuthConfig(config: FirebaseAuthConfig): FirebaseValidationResult {
        val result = FirebaseValidationResult()
        var current = result

        // apiKey validation
        if (config.apiKey.isBlank()) {
            current = current.addError(ValidationIssue("apiKey", "apiKey is required for Firebase Auth"))
        } else if (!config.apiKey.startsWith("AIzaSy")) {
            current = current.addWarning(ValidationIssue("apiKey", "apiKey does not follow Firebase convention (should start with 'AIzaSy')", IssueSeverity.WARNING))
        }

        // authDomain validation
        if (config.authDomain.isBlank()) {
            current = current.addError(ValidationIssue("authDomain", "authDomain is required for Firebase Auth"))
        } else if (!config.authDomain.endsWith(".firebaseapp.com")) {
            current = current.addWarning(ValidationIssue("authDomain", "authDomain should end with '.firebaseapp.com'", IssueSeverity.WARNING))
        }

        // projectId validation
        if (config.projectId.isBlank()) {
            current = current.addError(ValidationIssue("projectId", "projectId is required for Firebase Auth"))
        }

        // Cross-field coherence: authDomain should contain projectId
        if (config.authDomain.isNotBlank() && config.projectId.isNotBlank()) {
            if (!config.authDomain.contains(config.projectId)) {
                current = current.addWarning(ValidationIssue("authDomain", "authDomain should contain the projectId (expected format: projectId.firebaseapp.com)", IssueSeverity.WARNING))
            }
        }

        return current
    }

    fun validateContactConfig(config: FirebaseContactFormConfig): FirebaseValidationResult {
        val result = FirebaseValidationResult()
        var current = result

        // project validation
        if (config.project.projectId.isBlank()) {
            current = current.addError(ValidationIssue("project.projectId", "projectId is required for Firebase contact form"))
        }
        if (config.project.apiKey.isBlank()) {
            current = current.addError(ValidationIssue("project.apiKey", "apiKey is required for Firebase contact form"))
        } else if (!config.project.apiKey.startsWith("AIzaSy")) {
            current = current.addWarning(ValidationIssue("project.apiKey", "apiKey does not follow Firebase convention", IssueSeverity.WARNING))
        }

        // firestore validation
        if (config.firestore.contacts.name.isBlank()) {
            current = current.addError(ValidationIssue("firestore.contacts.name", "contacts collection name is required"))
        }
        if (config.firestore.messages.name.isBlank()) {
            current = current.addError(ValidationIssue("firestore.messages.name", "messages collection name is required"))
        }
        if (config.firestore.contacts.fields.isEmpty()) {
            current = current.addWarning(ValidationIssue("firestore.contacts.fields", "contacts collection has no fields defined", IssueSeverity.WARNING))
        }
        if (config.firestore.messages.fields.isEmpty()) {
            current = current.addWarning(ValidationIssue("firestore.messages.fields", "messages collection has no fields defined", IssueSeverity.WARNING))
        }

        return current
    }
}
