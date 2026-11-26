package no.nav.familie.saf

internal data class SafRequest(
    val query: String,
    val variables: Map<String, Any?>,
) {
    override fun toString(): String = "SafRequest"
}
