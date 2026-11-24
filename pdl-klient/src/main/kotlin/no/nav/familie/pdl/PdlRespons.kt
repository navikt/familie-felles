package no.nav.familie.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlRespons<T>(
    val data: T? = null,
    val errors: List<PdlFeil> = emptyList(),
    val extensions: PdlExtensions? = null,
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlExtensions(
    val warnings: List<PdlAdvarsel>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlAdvarsel(
    val query: String? = null,
    val id: String? = null,
    val code: String? = null,
    val message: String? = null,
    val details: Any? = null,
)
