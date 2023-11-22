package no.nav.familie.webflux.client

import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

class RessursException(
    val ressurs: Ressurs<Any>,
    cause: WebClientResponseException,
    val httpStatus: HttpStatus = HttpStatus.valueOf(cause.rawStatusCode),
) : RuntimeException(cause)
