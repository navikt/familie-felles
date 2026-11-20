package no.nav.familie.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlRespons<T>(
    val data: T? = null,
    val errors: List<PdlFeil> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFeil(
    val message: String? = null,
    val extensions: PdlFeilExtensions? = null,
    val locations: List<Map<String, Int>>? = null,
    val path: List<Any?>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFeilExtensions(
    val code: String? = null,
    val details: Map<String, Any?>? = null,
    val classification: String? = null,
)
