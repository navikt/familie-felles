package no.nav.familie.tilgangsmaskin

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

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

    open fun sjekkTilgangTilPersoner(
        personIdenter: List<String>,
        regeltype: Regeltype = Regeltype.KJERNE_REGELTYPE,
    ): List<TilgangsmaskinResultat> {
        val gyldigeIdenter = personIdenter.distinct().filter { it.isNotBlank() }
        if (gyldigeIdenter.isEmpty()) return emptyList()

        val resultaterPerIdent =
            gyldigeIdenter
                .chunked(MAKS_ANTALL_IDENTER_PER_KALL)
                .flatMap { sjekkTilgangTilPersonerIBolk(it, regeltype) }
                .associateBy { it.personIdent }

        return gyldigeIdenter.map { resultaterPerIdent[it] ?: manglendeSvar(it) }
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(personIdenter.map { BrukerIdOgRegelsettDto(brukerId = it, type = regeltype) })
                    .retrieve()
                    .body<TilgangsmaskinBulkResponsDto>()
            } catch (exception: Exception) {
                throw TilgangsmaskinException("Feil ved kall mot Tilgangsmaskinen: ${exception.message}", exception)
            }

        return respons?.resultater?.map { it.tilResultat() } ?: throw TilgangsmaskinException("Fikk tomt svar fra Tilgangsmaskinen")
    }

    private fun manglendeSvar(personIdent: String) =
        TilgangsmaskinResultat(
            personIdent = personIdent,
            harTilgang = false,
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            avvisningskode = Avvisningskode.UKJENT,
            begrunnelse = "Fikk ikke svar fra Tilgangsmaskinen for personen",
        )

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
        // Tilgangsmaskinen tillater maksimalt 1000 brukerId-er per bulk-forespørsel, og svarer 413 over det.
        const val MAKS_ANTALL_IDENTER_PER_KALL = 1000
    }
}
