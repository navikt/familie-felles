package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.domene.Task

data class RestTask(val task: Task,
                    val journalpostId: String?,
                    val saksnummer: String?,
                    val søkerFødselsnummer: String)

