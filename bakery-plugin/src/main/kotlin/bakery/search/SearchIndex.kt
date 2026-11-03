package bakery.search

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * BKY-SEARCH-1 — the per-language Pagefind index, as published by the engine.
 *
 * `pagefind-entry.json` is the only file the adapter needs to know which
 * language indexes exist. Pagefind 1.6 emits one entry per detected
 * `<html lang>`, each with its own hash, wasm module and page count:
 *
 * ```json
 * {"version":"1.6.0","languages":{"fr":{"hash":"fr_a99f8d72fa","wasm":"fr","page_count":464}}}
 * ```
 *
 * The model is deliberately narrower than the file: the characters catalogue
 * and any future field are ignored. The domain is pure — the JSON parser is
 * the only seam, and it returns [Either] instead of throwing so the caller (a
 * Gradle guard) can report a readable violation.
 */
data class SearchLanguage(
    val code: String,
    val hash: String,
    val wasm: String,
    val pageCount: Int,
) {
    init {
        require(code.isNotBlank()) { "code must not be blank" }
        require(hash.isNotBlank()) { "hash must not be blank" }
        require(wasm.isNotBlank()) { "wasm must not be blank" }
        require(pageCount >= 0) { "pageCount must not be negative" }
    }
}

data class SearchIndex(
    val version: String,
    val languages: List<SearchLanguage>,
) {
    init {
        require(version.isNotBlank()) { "version must not be blank" }
    }

    val languagesByCode: Map<String, SearchLanguage> get() = languages.associateBy { it.code }

    val codes: Set<String> get() = languagesByCode.keys
}

sealed interface SearchIndexError {
    data class Malformed(
        val reason: String,
    ) : SearchIndexError
}

object PagefindEntry {
    private val mapper = jacksonObjectMapper()

    fun parse(json: String): Either<SearchIndexError, SearchIndex> =
        Either
            .catch { mapper.readValue<EntryDto>(json).toDomain() }
            .fold(
                ifLeft = { SearchIndexError.Malformed(it.message ?: it.javaClass.simpleName).left() },
                ifRight = { it.right() },
            )

    private data class EntryDto(
        val version: String = "",
        val languages: Map<String, LanguageDto> = emptyMap(),
    )

    private data class LanguageDto(
        val hash: String = "",
        val wasm: String = "",
        @JsonProperty("page_count") val pageCount: Int = 0,
    )

    private fun EntryDto.toDomain(): SearchIndex =
        SearchIndex(
            version = version,
            languages =
                languages
                    .map { (code, dto) ->
                        SearchLanguage(
                            code = code,
                            hash = dto.hash,
                            wasm = dto.wasm,
                            pageCount = dto.pageCount,
                        )
                    }.sortedBy { it.code },
        )
}
