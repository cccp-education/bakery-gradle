package bakery.llm

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmServiceTest {

    @Test
    fun `complete returns generated text for a prompt`() = runBlocking {
        val fake = FakeLlmService("Bonjour, je suis un assistant IA.")
        val result = fake.complete("Présente-toi en une phrase.")
        assertEquals("Bonjour, je suis un assistant IA.", result)
    }

    @Test
    fun `complete records prompts received for audit trail`() = runBlocking {
        val fake = FakeLlmService("OK")
        fake.complete("Première question")
        fake.complete("Seconde question")
        assertEquals(listOf("Première question", "Seconde question"), fake.promptsReceived)
    }

    @Test
    fun `complete works with empty prompt and empty response`() = runBlocking {
        val fake = FakeLlmService("")
        val result = fake.complete("")
        assertEquals("", result)
    }
}
