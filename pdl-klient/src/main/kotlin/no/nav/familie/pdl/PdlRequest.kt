package no.nav.familie.pdl

@PublishedApi
internal class PdlRequest(
    val query: String,
    val variables: Map<String, Any?>,
)
