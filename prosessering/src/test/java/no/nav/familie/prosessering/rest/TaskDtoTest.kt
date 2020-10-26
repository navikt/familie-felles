package no.nav.familie.prosessering.rest

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties


class TaskDtoTest {

    @Test
    fun `TaskDto vs Task`() {
        val dto = feltnavn(TaskDto::class, "antallLogger", "sistKjørt")
        val dao = feltnavn(Task::class, "logg")

        sjekkFelter(dto, dao)
    }

    @Test
    fun `TaskLoggDto vs TaskLogg`() {
        val dto = feltnavn(TaskloggDto::class)
        val dao = feltnavn(TaskLogg::class, "task")

        sjekkFelter(dto, dao)
    }

    private fun sjekkFelter(dto: Set<String>, dao: Set<String>) {
        val dtoFelterEtterSletting = dto.toMutableSet()
        dtoFelterEtterSletting.removeAll(dao)
        val daoFelterEtterSletting = dao.toMutableSet()
        daoFelterEtterSletting.removeAll(dto)

        if (dtoFelterEtterSletting.isNotEmpty() || daoFelterEtterSletting.isNotEmpty()) {
            throw IllegalStateException("Omappede felter: dto=$dtoFelterEtterSletting dao=$daoFelterEtterSletting")
        }
    }

    private fun feltnavn(kClass: KClass<*>, vararg ignore: String ) =
            kClass.declaredMemberProperties.map { it.name }.filterNot {setOf(*ignore).contains(it) }.toSet()
}
