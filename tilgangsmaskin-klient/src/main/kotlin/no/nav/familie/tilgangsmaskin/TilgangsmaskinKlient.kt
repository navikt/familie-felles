package no.nav.familie.tilgangsmaskin

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Klient mot Tilgangsmaskinen (populasjonstilgangskontroll).
 *
 * Bruker bulk-endepunktet med on-behalf-of-token, slik at NAV-identen til innlogget saksbehandler hentes
 * fra tokenet. Kallet kan derfor kun gjøres i saksbehandlerkontekst — i systemkontekst finnes det ingen
 * ansatt å sjekke tilgang for, og konsumenten må håndtere det selv før den kaller klienten.
 *
 * Konsumenten er ansvarlig for å sende inn en [RestClient] som legger på et OBO-token mot Tilgangsmaskinen
 * sitt scope. Klienten eier protokollen, ikke autentiseringen.
 */
open class TilgangsmaskinKlient(
    tilgangsmaskinUri: URI,
    private val restClient: RestClient,
) {
    private val bulkUri: URI =
        UriComponentsBuilder
            .fromUri(tilgangsmaskinUri)
            .pathSegment("api", "v1", "bulk", "obo")
            .build()
            .toUri()

    /**
     * Sjekker tilgang til [personIdenter].
     *
     * Returnerer ett resultat per element i [personIdenter], i samme rekkefølge. Svarer ikke Tilgangsmaskinen
     * for en ident, nektes tilgang — vi skal aldri gi tilgang basert på et ufullstendig svar.
     *
     * Tomme identer sendes ikke videre. Tilgangsmaskinen avviser hele forespørselen med 400 hvis én eneste
     * brukerId er tom, og da ville én dårlig ident nektet tilgang til alle de andre i samme bolk.
     *
     * Kaster [TilgangsmaskinException] hvis kallet feiler. Da returneres ingen resultater i det hele tatt,
     * og kalleren må behandle unntaket som manglende tilgang.
     */
    open fun sjekkTilgangTilPersoner(
        personIdenter: List<String>,
        regeltype: Regeltype = Regeltype.KJERNE_REGELTYPE,
    ): List<TilgangsmaskinResultat> {
        if (personIdenter.isEmpty()) return emptyList()

        val gyldigeIdenter = personIdenter.distinct().filter { it.isNotBlank() }

        val resultaterPerIdent =
            gyldigeIdenter
                .chunked(MAKS_ANTALL_IDENTER_PER_KALL)
                .flatMap { sjekkTilgangTilPersonerIBolk(it, regeltype) }
                .associateBy { it.personIdent }

        return personIdenter
            .map { personIdent ->
                resultaterPerIdent[personIdent]
                    ?: if (personIdent.isBlank()) ugyldigIdent(personIdent) else manglendeSvar(personIdent)
            }.also { loggAvvisninger(it) }
    }

    private fun sjekkTilgangTilPersonerIBolk(
        personIdenter: List<String>,
        regeltype: Regeltype,
    ): List<TilgangsmaskinResultat> {
        val respons =
            try {
                restClient
                    .post()
                    .uri(bulkUri)
                    .body(personIdenter.map { BrukerIdOgRegelsettDto(brukerId = it, type = regeltype) })
                    .retrieve()
                    .body<TilgangsmaskinBulkResponsDto>()
                    ?: throw TilgangsmaskinException("Fikk tomt svar fra Tilgangsmaskinen")
            } catch (tilgangsmaskinException: TilgangsmaskinException) {
                throw tilgangsmaskinException
            } catch (exception: Exception) {
                throw TilgangsmaskinException("Feil ved kall mot Tilgangsmaskinen: ${exception.message}", exception)
            }

        return respons.resultater.map { it.tilResultat() }
    }

    private fun manglendeSvar(personIdent: String) =
        TilgangsmaskinResultat(
            personIdent = personIdent,
            harTilgang = false,
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            avvisningskode = Avvisningskode.UKJENT,
            begrunnelse = "Fikk ikke svar fra Tilgangsmaskinen for personen",
        )

    private fun ugyldigIdent(personIdent: String) =
        TilgangsmaskinResultat(
            personIdent = personIdent,
            harTilgang = false,
            httpStatus = HttpStatus.BAD_REQUEST.value(),
            avvisningskode = Avvisningskode.UKJENT,
            begrunnelse = "Personidenten er tom og kan ikke sjekkes mot Tilgangsmaskinen",
        )

    private fun loggAvvisninger(resultater: List<TilgangsmaskinResultat>) {
        val avviste = resultater.filterNot { it.harTilgang }
        if (avviste.isEmpty()) return

        logger.info(
            "Tilgangsmaskinen avviste tilgang til {} person(er). Avvisningskoder: {}. TraceId: {}",
            avviste.size,
            avviste.mapNotNull { it.avvisningskode }.distinct(),
            avviste.mapNotNull { it.traceId }.distinct(),
        )
    }

    private fun TilgangsmaskinBulkResultatDto.tilResultat(): TilgangsmaskinResultat {
        val harTilgang = status == HttpStatus.NO_CONTENT.value()
        return TilgangsmaskinResultat(
            personIdent = brukerId,
            harTilgang = harTilgang,
            httpStatus = status,
            avvisningskode = if (harTilgang) null else detaljer?.avvisningskode() ?: Avvisningskode.UKJENT,
            begrunnelse = detaljer?.begrunnelse,
            traceId = detaljer?.traceId,
            kanOverstyres = detaljer?.kanOverstyres ?: false,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangsmaskinKlient::class.java)

        // Tilgangsmaskinen tillater maksimalt 1000 brukerId-er per bulk-forespørsel, og svarer 413 over det.
        const val MAKS_ANTALL_IDENTER_PER_KALL = 1000
    }
}
