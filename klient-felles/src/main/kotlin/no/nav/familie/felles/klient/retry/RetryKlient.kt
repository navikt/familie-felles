package no.nav.familie.felles.klient.retry

import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.web.client.RestClientResponseException

/**
 * Felles grunnklasse for klienter som gjentar kall ved HTTP 502, 503 og 504, med konstant ventetid og jitter.
 *
 * Bare feilen på toppnivå vurderes. Hvert nytt forsøk kjører hele kallet på nytt, også interceptorene på `RestClient`.
 * Bruk [retryVedMidlertidigFeil] bare rundt kall uten bivirkninger.
 *
 * Spring `RetryTemplate` logger hvert mislykkede forsøk med exception og stacktrace på DEBUG-nivå. For HTTP-feil kan
 * meldingen inneholde responsbodyen. Ikke slå på DEBUG for `org.springframework.core.retry` i produksjon.
 */
abstract class RetryKlient(
    retryProperties: KlientRetryProperties,
) {
    internal val retryTemplate =
        with(retryProperties) {
            RetryTemplate(
                RetryPolicy
                    .builder()
                    .maxRetries(if (enabled) maxRetries else 0L)
                    .delay(delay)
                    .jitter(delay.dividedBy(2))
                    .predicate { it is RestClientResponseException && it.statusCode.value() in MIDLERTIDIGE_FEIL_STATUSER }
                    .build(),
            )
        }

    protected fun <T> retryVedMidlertidigFeil(kall: () -> T): T =
        try {
            retryTemplate.execute { kall() }
        } catch (feil: RetryException) {
            throw feil.cause
        }

    private companion object {
        val MIDLERTIDIGE_FEIL_STATUSER = setOf(502, 503, 504)
    }
}
