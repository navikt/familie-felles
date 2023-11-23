package no.nav.familie.http.util

import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

object UriUtil {
    fun uri(
        base: URI,
        path: String,
    ): URI =
        UriComponentsBuilder
            .fromUri(base)
            .pathSegment(path)
            .build()
            .toUri()

    fun uri(
        base: URI,
        path: String,
        query: String,
    ): URI =
        UriComponentsBuilder
            .fromUri(base)
            .pathSegment(path)
            .query(query)
            .build()
            .toUri()
}
