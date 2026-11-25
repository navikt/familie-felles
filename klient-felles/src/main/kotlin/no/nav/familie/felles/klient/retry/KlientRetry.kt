package no.nav.familie.felles.klient.retry

import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.web.client.RestClientResponseException

/**
 * Gjentar et kall ved HTTP 502, 503 og 504, med konstant ventetid og jitter mellom forsøkene.
 *
 * Bare feilen på toppnivå vurderes. Feilen kan komme fra tjenesten eller fra en interceptor på `RestClient`, for
 * eksempel tokenhenting. Hvert nytt forsøk kjører hele kallet på nytt, så interceptorene må tåle å bli kjørt flere
 * ganger. Bruk bare for kall uten bivirkninger.
 *
 * Spring `RetryTemplate` logger hvert mislykkede forsøk med exception og stacktrace på DEBUG-nivå. For HTTP-feil kan
 * meldingen inneholde responsbodyen. Ikke slå på DEBUG for `org.springframework.core.retry` i produksjon.
 */
class KlientRetry(
    retryKonfigurasjon: KlientRetryProperties,
) {
    internal val retryTemplate =
        with(retryKonfigurasjon) {
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

    fun <T> utfør(kall: () -> T): T =
        try {
            retryTemplate.execute { kall() }
        } catch (feil: RetryException) {
            throw feil.cause
        }

    private companion object {
        val MIDLERTIDIGE_FEIL_STATUSER = setOf(502, 503, 504)
    }
}
