package bakery.llm

import contracts.i18n.OllamaConfig
import contracts.i18n.OllamaDeviceKey
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * T-I18N-BAKERY US-2 — the caller-side LLM pool config lives in `site.yml`
 * (`ollama:` section). This resolver turns that section into an effective
 * [IaConfig] so the caller does not have to mutate the shared runner build.
 *
 * Rules: the explicit DSL wins (an enabled IaConfig is never overridden), an
 * absent `ollama:` section leaves the DSL untouched, and a present `ollama:`
 * section activates the rotating pool with its model, port range and timeout.
 */
class IaConfigResolverTest {
    private fun ollama(
        model: String = "nemotron-3-super:cloud",
        portStart: Int = 11437,
        portEnd: Int = 11465,
        timeoutSeconds: Long = 300,
        keys: List<OllamaDeviceKey> = listOf(OllamaDeviceKey(keyName = "ollama-11437", privateKey = "ssh-ed25519-fake")),
    ) = OllamaConfig(
        deviceKeys = keys,
        model = model,
        portStart = portStart,
        portEnd = portEnd,
        timeoutSeconds = timeoutSeconds,
    )

    @Test
    fun `an absent ollama section leaves the dsl config untouched`() {
        val dsl = IaConfig()

        val resolved = IaConfigResolver.resolve(dsl, null)

        assertEquals(dsl, resolved)
        assertFalse(resolved.enabled)
        assertNull(resolved.portRange)
    }

    @Test
    fun `an ollama section activates the rotating pool`() {
        val resolved = IaConfigResolver.resolve(IaConfig(), ollama())

        assertTrue(resolved.enabled)
        assertEquals(11437..11465, resolved.portRange)
    }

    @Test
    fun `the pool model and port range come from the ollama section`() {
        val resolved = IaConfigResolver.resolve(IaConfig(), ollama(model = "gemma4:31b-cloud", portStart = 11440, portEnd = 11450))

        assertEquals("gemma4:31b-cloud", resolved.modelName)
        assertEquals(11440..11450, resolved.portRange)
        assertEquals("http://localhost:11440", resolved.baseUrl)
    }

    @Test
    fun `the timeout comes from the ollama section`() {
        val resolved = IaConfigResolver.resolve(IaConfig(), ollama(timeoutSeconds = 900))

        assertEquals(Duration.ofSeconds(900), resolved.timeout)
    }

    @Test
    fun `an explicitly enabled dsl config wins over the ollama section`() {
        val dsl =
            IaConfig(
                baseUrl = "http://localhost:11464",
                modelName = "gpt-oss:120b-cloud",
                enabled = true,
            )

        val resolved = IaConfigResolver.resolve(dsl, ollama())

        assertEquals(dsl, resolved)
        assertNull(resolved.portRange)
    }

    @Test
    fun `an explicitly enabled dsl config wins even without an ollama section`() {
        val dsl = IaConfig(enabled = true, modelName = "gpt-oss:120b-cloud")

        val resolved = IaConfigResolver.resolve(dsl, null)

        assertEquals(dsl, resolved)
        assertTrue(resolved.enabled)
    }

    @Test
    fun `an ollama section without device keys still activates the local pool`() {
        val resolved = IaConfigResolver.resolve(IaConfig(), ollama(keys = emptyList()))

        assertTrue(resolved.enabled)
        assertEquals(11437..11465, resolved.portRange)
    }
}
