package bakery.firebase

import bakery.FirebaseAuthConfig
import bakery.FirebaseContactFormConfig
import bakery.FirebaseField
import bakery.FirebaseCollection
import bakery.FirebaseProjectInfo
import bakery.FirebaseCallableFunction
import bakery.FirebaseCallableParam
import bakery.FirebaseFirestoreSchema
import bakery.llm.FakeLlmService
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ValidateFirebaseConfigTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(): ValidateFirebaseConfigTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .withName("test-firebase-validate")
            .build()
        project.pluginManager.apply("java-base")
        return project.tasks.register("validateFirebaseConfig", ValidateFirebaseConfigTask::class.java).get()
    }

    @Nested
    inner class MechanicalValidation {

        @Test
        fun `valid auth config passes validation`() {
            val result = FirebaseConfigValidator.validateAuthConfig(
                FirebaseAuthConfig(apiKey = "AIzaSyB-abc123", authDomain = "my-project.firebaseapp.com", projectId = "my-project")
            )
            assertTrue(result.isValid)
            assertEquals(0, result.errors.size)
        }

        @Test
        fun `missing fields produce errors`() {
            val result = FirebaseConfigValidator.validateAuthConfig(
                FirebaseAuthConfig(apiKey = "", authDomain = "", projectId = "")
            )
            assertEquals(3, result.errors.size)
            assertEquals(false, result.isValid)
        }
    }

    @Nested
    inner class LlmPromptBuilding {

        @Test
        fun `buildLlmPrompt includes auth config fields`() {
            val task = createTask()
            task.resolvedAuthConfig = FirebaseAuthConfig(
                apiKey = "AIzaSyB-test",
                authDomain = "test-project.firebaseapp.com",
                projectId = "test-project"
            )
            val prompt = task.buildLlmPrompt()
            assertTrue(prompt.contains("AIzaSyB-test"))
            assertTrue(prompt.contains("test-project.firebaseapp.com"))
            assertTrue(prompt.contains("test-project"))
            assertTrue(prompt.contains("Firebase Auth"))
            assertTrue(prompt.contains("JSON"))
        }

        @Test
        fun `buildLlmPrompt includes contact config fields`() {
            val task = createTask()
            val contactConfig = FirebaseContactFormConfig(
                project = FirebaseProjectInfo(projectId = "my-project", apiKey = "AIzaSyB-test"),
                firestore = FirebaseFirestoreSchema(
                    contacts = FirebaseCollection(name = "contacts", fields = listOf(FirebaseField("name", "string")), rulesEnabled = true),
                    messages = FirebaseCollection(name = "messages", fields = listOf(FirebaseField("text", "string")), rulesEnabled = true)
                ),
                callable = FirebaseCallableFunction(name = "sendMessage", params = listOf(FirebaseCallableParam("message", "string")))
            )
            task.resolvedContactConfig = contactConfig
            val prompt = task.buildLlmPrompt()
            assertTrue(prompt.contains("Firebase Contact Form"))
            assertTrue(prompt.contains("my-project"))
            assertTrue(prompt.contains("contacts"))
        }
    }

    @Nested
    inner class LlmResponseParsing {

        @Test
        fun `parseLlmResponse with errors and warnings`() {
            val task = createTask()
            val response = """{"errors": [{"field": "apiKey", "message": "Invalid format"}], "warnings": [{"field": "authDomain", "message": "Consider using custom domain"}]}"""
            val result = task.parseLlmResponse(response)
            assertEquals(1, result.errors.size)
            assertEquals(1, result.warnings.size)
            assertEquals("apiKey", result.errors[0].field)
            assertEquals("authDomain", result.warnings[0].field)
        }

        @Test
        fun `parseLlmResponse with valid config returns empty result`() {
            val task = createTask()
            val response = """{"errors": [], "warnings": []}"""
            val result = task.parseLlmResponse(response)
            assertTrue(result.isValid)
            assertEquals(0, result.errors.size)
            assertEquals(0, result.warnings.size)
        }
    }

    @Test
    fun `task is registered with correct group and description`() {
        val task = createTask()

        assertNotNull(task)
        assertTrue(task.group == "validate")
        val desc = task.description
        assertTrue(desc != null && desc.contains("Valide la cohérence"))
    }

    @Test
    fun `task validate method succeeds with valid config`() {
        val task = createTask()
        task.resolvedAuthConfig = FirebaseAuthConfig(
            apiKey = "AIzaSyB-test",
            authDomain = "test-project.firebaseapp.com",
            projectId = "test-project"
        )
        task.validate()
        // Should not throw
    }

    @Test
    fun `task validate method throws with invalid config`() {
        val task = createTask()
        task.resolvedAuthConfig = FirebaseAuthConfig(apiKey = "", authDomain = "", projectId = "")

        val exception = org.junit.jupiter.api.assertThrows<java.lang.IllegalStateException> {
            task.validate()
        }
        assertTrue(exception.message?.contains("Configuration Firebase invalide") == true)
    }

    @Test
    fun `task validate method reports success when no configs to validate`() {
        val task = createTask()
        // No configs set - should log "Aucune configuration Firebase trouvée" and return
        task.validate()
        // Should not throw
    }

    @Test
    fun `task with LLM mocked produces expected result`() {
        val task = createTask()
        task.resolvedAuthConfig = FirebaseAuthConfig(
            apiKey = "AIzaSyB-test",
            authDomain = "test-project.firebaseapp.com",
            projectId = "test-project"
        )
        task.llmService = FakeLlmService("""{"errors": [], "warnings": []}""")
        task.validateWithIa.set("true")
        task.validate()
        // Should not throw when LLM returns valid JSON with empty lists
    }
}
