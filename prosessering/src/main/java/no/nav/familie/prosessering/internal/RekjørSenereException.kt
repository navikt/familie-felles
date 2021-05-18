package no.nav.familie.prosessering.internal

import java.time.LocalDateTime

data class RekjørSenereException(val årsak: String, val triggerTid: LocalDateTime): RuntimeException()
