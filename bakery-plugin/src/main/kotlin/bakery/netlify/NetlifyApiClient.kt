package bakery.netlify

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/** A Netlify deployment, reduced to what bakery needs. */
data class NetlifyDeploy(
    val id: String,
    val state: String,
    val url: String,
    val errorMessage: String = "",
)

sealed interface NetlifyApiResult {
    data class Success(
        val deploy: NetlifyDeploy,
    ) : NetlifyApiResult

    data class Failure(
        val message: String,
        val status: Int? = null,
    ) : NetlifyApiResult
}

/**
 * Netlify API client (US-4).
 *
 * The token is held in a plain class (never a `data class`) so it cannot leak
 * through an auto-generated `toString`. Every failure message is built by this
 * class and never includes the token.
 *
 * Polling is bounded and injectable (`sleeper`) so tests run instantly.
 */
class NetlifyApiClient(
    private val http: NetlifyHttp,
    private val token: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val pollAttempts: Int = 20,
    private val pollDelayMillis: Long = 1500,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
) {
    private val mapper = jacksonObjectMapper()

    fun deploy(
        siteId: String,
        zip: ByteArray,
    ): NetlifyApiResult =
        when (val created = createDeploy(siteId, zip)) {
            is NetlifyApiResult.Failure -> created
            is NetlifyApiResult.Success -> awaitReady(created.deploy)
        }

    fun createDeploy(
        siteId: String,
        zip: ByteArray,
    ): NetlifyApiResult {
        val response = http.postZip("$baseUrl/sites/$siteId/deploys", token, zip)
        if (response.status !in 200..299) return failure(response)
        val deploy = parseDeploy(response.body) ?: return NetlifyApiResult.Failure("Netlify returned an unreadable deploy payload")
        return NetlifyApiResult.Success(deploy)
    }

    fun awaitReady(deploy: NetlifyDeploy): NetlifyApiResult {
        var current = deploy
        repeat(pollAttempts) {
            when (current.state) {
                "ready" -> return NetlifyApiResult.Success(current)
                "error", "rejected" -> {
                    val detail =
                        current.errorMessage
                            .takeIf { it.isNotBlank() }
                            ?.let { ": $it" }
                            .orEmpty()
                    return NetlifyApiResult.Failure(
                        "Netlify deployment ${current.id} ended in state '${current.state}'$detail",
                    )
                }
            }
            val response = http.get("$baseUrl/deploys/${current.id}", token)
            if (response.status !in 200..299) return failure(response)
            current = parseDeploy(response.body) ?: current
            if (current.state == "ready") return NetlifyApiResult.Success(current)
            sleeper(pollDelayMillis)
        }
        return NetlifyApiResult.Failure(
            "Netlify deployment ${current.id} did not become ready after $pollAttempts polls (state '${current.state}')",
        )
    }

    private fun parseDeploy(body: String): NetlifyDeploy? =
        runCatching {
            val node = mapper.readTree(body)
            NetlifyDeploy(
                id = node.path("id").asText(""),
                state = node.path("state").asText(""),
                url = firstNonBlank(node, "deploy_ssl_url", "ssl_url", "url"),
                errorMessage = node.path("error_message").asText(""),
            )
        }.getOrNull()

    private fun firstNonBlank(
        node: com.fasterxml.jackson.databind.JsonNode,
        vararg fields: String,
    ): String {
        for (field in fields) {
            val value = node.path(field).asText("")
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun failure(response: NetlifyHttpResponse): NetlifyApiResult.Failure {
        val detail =
            response.body
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(200)
        val message =
            when (response.status) {
                401, 403 -> "Netlify authentication failed (check the token in the configured environment variable)"
                404 -> "Netlify site or deploy not found (check netlify.siteId)"
                429 -> "Netlify rate limit reached — retry later"
                in 400..499 -> "Netlify rejected the deployment (HTTP ${response.status})"
                in 500..599 -> "Netlify is unavailable (HTTP ${response.status}) — retry later"
                else -> "Netlify API error (HTTP ${response.status})"
            }
        return NetlifyApiResult.Failure(
            message = if (detail.isBlank()) message else "$message: $detail",
            status = response.status,
        )
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://api.netlify.com/api/v1"
    }
}
