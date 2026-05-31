package bakery.llm

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IaConfigTest {

    @Test
    fun `default config has standard Ollama URL`() {
        val config = IaConfig()
        assertEquals("http://localhost:11464", config.baseUrl)
    }

    @Test
    fun `default config has bakery-llm model name`() {
        val config = IaConfig()
        assertEquals("deepseek-v4-pro", config.modelName)
    }

    @Test
    fun `default config has 120 second timeout`() {
        val config = IaConfig()
        assertEquals(Duration.ofSeconds(120), config.timeout)
    }

    @Test
    fun `config exposes enabled flag true by default`() {
        val config = IaConfig()
        assertTrue(config.enabled)
    }

    @Test
    fun `config can be disabled via enabled property`() {
        val config = IaConfig(enabled = false)
        assertFalse(config.enabled)
    }

    @Test
    fun `config can override all parameters`() {
        val config = IaConfig(
            baseUrl = "https://ollama.prod.example.com",
            modelName = "codellama:7b",
            timeout = Duration.ofMinutes(5),
            enabled = true
        )
        assertEquals("https://ollama.prod.example.com", config.baseUrl)
        assertEquals("codellama:7b", config.modelName)
        assertEquals(Duration.ofMinutes(5), config.timeout)
        assertTrue(config.enabled)
    }

    @Test
    fun `mutating properties works after construction`() {
        val config = IaConfig()
        config.baseUrl = "http://custom:11463"
        config.modelName = "phi"
        config.timeout = Duration.ofSeconds(30)
        config.enabled = false
        assertEquals("http://custom:11463", config.baseUrl)
        assertEquals("phi", config.modelName)
        assertEquals(Duration.ofSeconds(30), config.timeout)
        assertEquals(false, config.enabled)
    }
}
