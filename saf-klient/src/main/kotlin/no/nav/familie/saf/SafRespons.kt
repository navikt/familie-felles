package no.nav.familie.saf

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SafRespons<T>(
    val data: T? = null,
    val errors: List<SafFeil> = emptyList(),
) {
    override fun toString(): String = "SafRespons"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SafFeil(
    val message: String? = null,
    val locations: List<Map<String, Int>>? = null,
    val path: List<Any?>? = null,
    val extensions: SafFeilExtensions? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SafFeilExtensions(
    val code: String? = null,
    val details: Map<String, Any?>? = null,
    val classification: String? = null,
)
