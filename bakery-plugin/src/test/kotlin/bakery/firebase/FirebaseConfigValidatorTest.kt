package bakery.firebase

import bakery.FirebaseAuthConfig
import bakery.FirebaseContactFormConfig
import bakery.FirebaseCollection
import bakery.FirebaseField
import bakery.FirebaseProjectInfo
import bakery.FirebaseCallableFunction
import bakery.FirebaseCallableParam
import bakery.FirebaseFirestoreSchema
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirebaseConfigValidatorTest {

    @Nested
    inner class AuthConfigValidation {

        @Test
        fun `valid auth config passes all checks`() {
            val config = FirebaseAuthConfig(
                apiKey = "AIzaSyB-abc123",
                authDomain = "my-project.firebaseapp.com",
                projectId = "my-project"
            )
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertTrue(result.isValid)
            assertEquals(0, result.errors.size)
        }

        @Test
        fun `blank apiKey is an error`() {
            val config = FirebaseAuthConfig(apiKey = "", authDomain = "p.firebaseapp.com", projectId = "p")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(1, result.errors.size)
            assertEquals("apiKey", result.errors[0].field)
        }

        @Test
        fun `non-Firebase apiKey is a warning`() {
            val config = FirebaseAuthConfig(apiKey = "wrong-key", authDomain = "p.firebaseapp.com", projectId = "p")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(0, result.errors.size)
            assertEquals(1, result.warnings.size)
            assertEquals("apiKey", result.warnings[0].field)
        }

        @Test
        fun `blank authDomain is an error`() {
            val config = FirebaseAuthConfig(apiKey = "AIzaSyB-abc", authDomain = "", projectId = "p")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(1, result.errors.size)
            assertEquals("authDomain", result.errors[0].field)
        }

        @Test
        fun `non-firebaseapp authDomain is a warning`() {
            val config = FirebaseAuthConfig(apiKey = "AIzaSyB-abc", authDomain = "example.com", projectId = "p")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(1, result.warnings.size)
            assertEquals("authDomain", result.warnings[0].field)
        }

        @Test
        fun `blank projectId is an error`() {
            val config = FirebaseAuthConfig(apiKey = "AIzaSyB-abc", authDomain = "p.firebaseapp.com", projectId = "")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(1, result.errors.size)
            assertEquals("projectId", result.errors[0].field)
        }

        @Test
        fun `authDomain without projectId is a warning`() {
            val config = FirebaseAuthConfig(apiKey = "AIzaSyB-abc", authDomain = "other-project.firebaseapp.com", projectId = "my-project")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(1, result.warnings.size)
            assertTrue(result.warnings[0].message.contains("projectId"))
        }

        @Test
        fun `multiple errors accumulate`() {
            val config = FirebaseAuthConfig(apiKey = "", authDomain = "", projectId = "")
            val result = FirebaseConfigValidator.validateAuthConfig(config)
            assertEquals(3, result.errors.size)
        }
    }

    @Nested
    inner class ContactConfigValidation {

        private val validContactConfig = FirebaseContactFormConfig(
            project = FirebaseProjectInfo(projectId = "my-project", apiKey = "AIzaSyB-abc123"),
            firestore = FirebaseFirestoreSchema(
                contacts = FirebaseCollection(name = "contacts", fields = listOf(FirebaseField("name", "string")), rulesEnabled = true),
                messages = FirebaseCollection(name = "messages", fields = listOf(FirebaseField("text", "string")), rulesEnabled = true)
            ),
            callable = FirebaseCallableFunction(name = "sendMessage", params = listOf(FirebaseCallableParam("message", "string")))
        )

        @Test
        fun `valid contact config passes all checks`() {
            val result = FirebaseConfigValidator.validateContactConfig(validContactConfig)
            assertTrue(result.isValid)
            assertEquals(0, result.errors.size)
        }

        @Test
        fun `blank projectId is an error`() {
            val config = validContactConfig.copy(project = FirebaseProjectInfo(projectId = "", apiKey = "AIzaSyB-abc"))
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(1, result.errors.size)
            assertEquals("project.projectId", result.errors[0].field)
        }

        @Test
        fun `blank apiKey is an error`() {
            val config = validContactConfig.copy(project = FirebaseProjectInfo(projectId = "p", apiKey = ""))
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(1, result.errors.size)
            assertEquals("project.apiKey", result.errors[0].field)
        }

        @Test
        fun `non-Firebase apiKey is a warning`() {
            val config = validContactConfig.copy(project = FirebaseProjectInfo(projectId = "p", apiKey = "wrong-key"))
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(0, result.errors.size)
            assertEquals(1, result.warnings.size)
        }

        @Test
        fun `blank contacts collection name is an error`() {
            val config = validContactConfig.copy(
                firestore = validContactConfig.firestore.copy(
                    contacts = FirebaseCollection(name = "", fields = listOf(FirebaseField("name", "string")), rulesEnabled = true)
                )
            )
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(1, result.errors.filter { it.field == "firestore.contacts.name" }.size)
        }

        @Test
        fun `blank messages collection name is an error`() {
            val config = validContactConfig.copy(
                firestore = validContactConfig.firestore.copy(
                    messages = FirebaseCollection(name = "", fields = listOf(FirebaseField("text", "string")), rulesEnabled = true)
                )
            )
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(1, result.errors.filter { it.field == "firestore.messages.name" }.size)
        }

        @Test
        fun `empty fields lists are warnings`() {
            val config = validContactConfig.copy(
                firestore = validContactConfig.firestore.copy(
                    contacts = FirebaseCollection(name = "contacts", fields = emptyList(), rulesEnabled = true),
                    messages = FirebaseCollection(name = "messages", fields = emptyList(), rulesEnabled = true)
                )
            )
            val result = FirebaseConfigValidator.validateContactConfig(config)
            assertEquals(2, result.warnings.size)
            assertTrue(result.isValid) // warnings don't block validity
        }
    }

    @Nested
    inner class ValidationResultComposition {

        @Test
        fun `merge combines errors and warnings`() {
            val r1 = FirebaseValidationResult(
                errors = listOf(ValidationIssue("a", "err a")),
                warnings = listOf(ValidationIssue("b", "warn b"))
            )
            val r2 = FirebaseValidationResult(
                errors = listOf(ValidationIssue("c", "err c")),
                warnings = listOf(ValidationIssue("d", "warn d"))
            )
            val merged = r1.merge(r2)
            assertEquals(2, merged.errors.size)
            assertEquals(2, merged.warnings.size)
        }

        @Test
        fun `isValid is false when errors exist`() {
            val result = FirebaseValidationResult(errors = listOf(ValidationIssue("x", "fail")))
            assertEquals(false, result.isValid)
        }

        @Test
        fun `isValid is true with only warnings`() {
            val result = FirebaseValidationResult(warnings = listOf(ValidationIssue("y", "suggestion")))
            assertEquals(true, result.isValid)
        }
    }
}
