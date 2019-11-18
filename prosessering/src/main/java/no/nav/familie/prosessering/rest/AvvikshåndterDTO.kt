package no.nav.familie.ks.mottak.app.rest

import no.nav.familie.prosessering.domene.Avvikstype

data class AvvikshåndterDTO (
    val avvikstype: Avvikstype,
    val årsak: String
)
