package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Component

@Component
interface RestTaskMapper {

    fun toDto(task: Task): RestTask
}
