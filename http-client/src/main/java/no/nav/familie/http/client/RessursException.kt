package no.nav.familie.http.client

import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

class RessursException(val ressurs: Ressurs<Any>,
                       cause: RestClientResponseException,
                       val httpStatus: HttpStatus = HttpStatus.valueOf(cause.rawStatusCode)) : RuntimeException(cause)
