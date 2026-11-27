package no.nav.familie.oppgave.modell

import com.fasterxml.jackson.annotation.JsonAlias

enum class Prioritet {
    KRITISK,
    HOY,
    NORM,
    LAV,
}

enum class Oppgavestatus {
    OPPRETTET,
    AAPNET,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
}

enum class Oppgavestatuskategori {
    AAPEN,
    AVSLUTTET,
}

enum class Brukertype {
    PERSON,
    ARBEIDSGIVER,
    SAMHANDLER,
}

enum class Gruppe {
    FOLKEREGISTERIDENT,
    NPID,

    @JsonAlias("AKTORID")
    AKTOERID,
}

enum class MetadataKey {
    NORM_DATO,
    SOKNAD_ID,
    REVURDERINGSTYPE,
    KRAV_ID,
    MOTTATT_DATO,
    EKSTERN_HENVENDELSE_ID,
    SKANNET_DATO,
    RINA_SAKID,
    HJEMMEL,
    EKSTERN_ID,
    BEHANDLING_ID,
}

enum class Sorteringsrekkefolge {
    ASC,
    DESC,
}

enum class Sorteringsfelt {
    OPPRETTET_TIDSPUNKT,
    FRIST,
}
