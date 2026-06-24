package bakery.pivot

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextTranslatableClassifierTest {

    @Test
    fun `natural language text is translatable`() {
        assertTrue(TextTranslatableClassifier.isTranslatable("Ce guide vous permet d'etre operationnel."))
        assertTrue(TextTranslatableClassifier.isTranslatable(" avec une partition de "))
        assertTrue(TextTranslatableClassifier.isTranslatable(" (recommande) :"))
        assertTrue(TextTranslatableClassifier.isTranslatable("Laissez la cle USB inseree"))
    }

    @Test
    fun `pure numbers and dashes are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable("1"))
        assertFalse(TextTranslatableClassifier.isTranslatable("2"))
        assertFalse(TextTranslatableClassifier.isTranslatable("3"))
        assertFalse(TextTranslatableClassifier.isTranslatable("-"))
    }

    @Test
    fun `sizes with units are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable("~8 Go"))
        assertFalse(TextTranslatableClassifier.isTranslatable("8 Go"))
        assertFalse(TextTranslatableClassifier.isTranslatable("reste (~48 Go)"))
        assertFalse(TextTranslatableClassifier.isTranslatable("Reste de la cle (~48 Go sur 64 Go)"))
    }

    @Test
    fun `locale codes are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable(" : fr_FR.UTF-8"))
        assertFalse(TextTranslatableClassifier.isTranslatable("fr_FR.UTF-8"))
    }

    @Test
    fun `product and desktop names are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable(" : Xubuntu XFCE"))
        assertFalse(TextTranslatableClassifier.isTranslatable("firefox"))
        assertFalse(TextTranslatableClassifier.isTranslatable("terminator"))
        assertFalse(TextTranslatableClassifier.isTranslatable("Ollama"))
        assertFalse(TextTranslatableClassifier.isTranslatable("ext4"))
        assertFalse(TextTranslatableClassifier.isTranslatable("ext4/squashfs"))
    }

    @Test
    fun `comma-separated package lists are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable("nmap, iperf3, wireshark, tshark, traceroute, dnsutils, net-tools, whois, iptraf-ng, iftop, nload"))
        assertFalse(TextTranslatableClassifier.isTranslatable("curl, wget, git, vim, jq, rsync"))
        assertFalse(TextTranslatableClassifier.isTranslatable("docker-ce, docker-ce-cli, containerd.io, docker-buildx-plugin, docker-compose-plugin"))
        assertFalse(TextTranslatableClassifier.isTranslatable("SDKMAN + JDK 25 (Temurin)"))
    }

    @Test
    fun `technical command parameters in parentheses are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable(" (bs=4M, conv=fsync)"))
    }

    @Test
    fun `technical comma separator is not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable(", "))
    }

    @Test
    fun `human language with FR in parentheses IS translatable`() {
        assertTrue(TextTranslatableClassifier.isTranslatable(" : FR (AZERTY)"))
        assertTrue(TextTranslatableClassifier.isTranslatable("System A (lecture seule)"))
    }

    @Test
    fun `short technical words are not translatable`() {
        assertFalse(TextTranslatableClassifier.isTranslatable("reste"))
    }
}