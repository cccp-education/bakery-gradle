package bakery.dns

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * EPIC BKY-DNS-2 — Functional tests over a real local HTTP server.
 *
 * Exercises the production transport [JavaOvhHttp] (real
 * java.net.http.HttpClient) end to end against a stub OVH API: it
 * validates the HMAC signature header, the X-Ovh-* headers and the JSON
 * request/response round-trip. Zero real OVH network.
 */
class OvhDnsClientFunctionalTest {

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String,
    )

    private lateinit var server: HttpServer
    private val captured = ConcurrentLinkedQueue<CapturedRequest>()
    private val responses = ConcurrentLinkedQueue<Pair<Int, String>>()

    private val credentials =
        OvhCredentials(
            applicationKey = "functional-ak",
            applicationSecret = "functional-as",
            consumerKey = "functional-ck",
        )

    @BeforeEach
    fun setUp() {
        captured.clear()
        responses.clear()
        server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                executor = Executors.newSingleThreadExecutor()
                createContext("/") { exchange ->
                    val requestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    captured.add(
                        CapturedRequest(
                            method = exchange.requestMethod,
                            path = exchange.requestURI.toString(),
                            headers = exchange.requestHeaders.entries.associate { (k, v) ->
                                k.lowercase() to v.joinToString(",")
                            },
                            body = requestBody,
                        ),
                    )
                    val (status, body) = responses.poll() ?: (200 to "{}")
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(status, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()
                }
                start()
            }
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun baseUrl(): String = "http://127.0.0.1:${server.address.port}"

    private fun client(): OvhDnsClient =
        OvhDnsClient(credentials, JavaOvhHttp(credentials), baseUrl())

    private fun assertSigned(request: CapturedRequest) {
        val timestamp = request.headers["x-ovh-timestamp"]
        val signature = request.headers["x-ovh-signature"]
        assertThat(timestamp).isNotNull
        assertThat(signature).isNotNull
        assertThat(request.headers["x-ovh-application"]).isEqualTo("functional-ak")
        assertThat(request.headers["x-ovh-consumer"]).isEqualTo("functional-ck")
        val expected =
            OvhSignature.sign(
                applicationSecret = "functional-as",
                consumerKey = "functional-ck",
                method = request.method,
                url = baseUrl() + request.path,
                body = request.body,
                timestamp = timestamp!!,
            )
        assertThat(signature).isEqualTo(expected)
    }

    @Nested
    @DisplayName("signed requests")
    inner class SignedRequests {
        @Test
        @DisplayName("listRecordIds sends a signed GET and parses the ids")
        fun `listRecordIds sends signed GET and parses ids`() {
            responses.add(200 to "[123456, 123457]")

            val ids = client().listRecordIds("talaria.school")

            assertThat(ids).containsExactly(123456L, 123457L)
            val request = captured.single()
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/record")
            assertThat(request.body).isEmpty()
            assertSigned(request)
        }

        @Test
        @DisplayName("fieldType query is part of the signed url")
        fun `fieldType query is part of the signed url`() {
            responses.add(200 to "[]")

            client().listRecordIds("talaria.school", fieldType = "A")

            val request = captured.single()
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/record?fieldType=A")
            assertSigned(request)
        }

        @Test
        @DisplayName("createRecord sends a signed POST with the exact JSON body")
        fun `createRecord sends signed POST with exact body`() {
            responses.add(
                200 to """{"id":999001,"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600}""",
            )

            val id =
                client().createRecord(
                    "talaria.school",
                    DnsRecord(type = "A", name = "@", value = "185.199.108.153", ttl = 3600),
                )

            assertThat(id).isEqualTo(999001L)
            val request = captured.single()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/record")
            assertThat(request.body)
                .isEqualTo(
                    "{\"fieldType\":\"A\",\"subDomain\":\"\",\"target\":\"185.199.108.153\",\"ttl\":3600}",
                )
            assertSigned(request)
        }

        @Test
        @DisplayName("refreshZone sends a signed POST with an empty json object")
        fun `refreshZone sends signed POST with empty json`() {
            responses.add(200 to "")

            client().refreshZone("talaria.school")

            val request = captured.single()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/refresh")
            assertThat(request.body).isEqualTo("{}")
            assertSigned(request)
        }
    }

    @Nested
    @DisplayName("response mapping over real HTTP")
    inner class ResponseMapping {
        @Test
        @DisplayName("getRecord maps an OVH record back to a normalized DnsRecord")
        fun `getRecord maps OvhRecord to DnsRecord`() {
            responses.add(
                200 to """{"id":123456,"fieldType":"A","subDomain":"","target":"185.199.108.153","ttl":3600}""",
            )

            val record = client().getRecord("talaria.school", 123456L)

            assertThat(record.type).isEqualTo("A")
            assertThat(record.name).isEqualTo("@")
            assertThat(record.value).isEqualTo("185.199.108.153")
            assertThat(record.ttl).isEqualTo(3600)
        }

        @Test
        @DisplayName("updateRecord sends a signed PUT on the record path")
        fun `updateRecord sends signed PUT`() {
            responses.add(200 to "{}")

            client().updateRecord(
                "talaria.school",
                123456L,
                DnsRecord(type = "A", name = "@", value = "185.199.108.153"),
            )

            val request = captured.single()
            assertThat(request.method).isEqualTo("PUT")
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/record/123456")
            assertSigned(request)
        }

        @Test
        @DisplayName("deleteRecord sends a signed DELETE on the record path")
        fun `deleteRecord sends signed DELETE`() {
            responses.add(200 to "")

            client().deleteRecord("talaria.school", 123456L)

            val request = captured.single()
            assertThat(request.method).isEqualTo("DELETE")
            assertThat(request.path).isEqualTo("/domain/zone/talaria.school/record/123456")
            assertSigned(request)
        }
    }

    @Nested
    @DisplayName("error handling over real HTTP")
    inner class ErrorHandling {
        @Test
        @DisplayName("non-2xx response surfaces OvhDnsException with the status")
        fun `non 2xx surfaces OvhDnsException`() {
            responses.add(404 to "{\"message\":\"Not found\"}")

            assertThatThrownBy { client().listRecordIds("talaria.school") }
                .isInstanceOf(OvhDnsException::class.java)
                .hasMessageContaining("404")
        }
    }
}
