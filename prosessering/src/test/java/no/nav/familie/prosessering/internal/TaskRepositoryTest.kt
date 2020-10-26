package no.nav.familie.prosessering.internal

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.TestAppConfig
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import no.nav.familie.prosessering.task.TaskStep2
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringRunner

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestAppConfig::class])
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
class TaskRepositoryTest {

    @Autowired
    private lateinit var repository: TaskRepository


    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker uavhengig av status`() {
        val preCount = repository.finnTasksMedStatus(Status.values().toList(), PageRequest.of(0, 1000))
        val preCountFeilet = preCount.count { it.status == Status.FEILET }
        val preCountUbehandlet = preCount.count { it.status == Status.UBEHANDLET }

        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksMedStatus(Status.values().toList(), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(3 + preCount.size)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2 + preCountFeilet)
        Assertions.assertThat(alleTasks.count { it.status == Status.UBEHANDLET }).isEqualTo(1 + preCountUbehandlet)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut alle tasker gitt en status`() {
        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksMedStatus(listOf(Status.FEILET), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(2)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2)
    }

    @Test
    fun `finnTasksMedStatus - skal hente ut max 1 task gitt en status`() {
        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksMedStatus(listOf(Status.FEILET), PageRequest.of(0, 1))
        Assertions.assertThat(alleTasks.size).isEqualTo(1)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(1)
    }

    @Test
    fun `finnTasksDtoTilFrontend - skal hente ut alle tasker uavhengig av status`() {
        val preCount = repository.finnTasksDtoTilFrontend(Status.values().toList(), PageRequest.of(0, 1000))
        val preCountFeilet = preCount.count { it.status == Status.FEILET }
        val preCountUbehandlet = preCount.count { it.status == Status.UBEHANDLET }

        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksDtoTilFrontend(Status.values().toList(), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(3 + preCount.size)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2 + preCountFeilet)
        Assertions.assertThat(alleTasks.count { it.status == Status.UBEHANDLET }).isEqualTo(1 + preCountUbehandlet)
    }

    @Test
    fun `finnTasksDtoTilFrontend - skal hente ut alle tasker gitt en status`() {
        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksDtoTilFrontend(listOf(Status.FEILET), PageRequest.of(0, 1000))
        Assertions.assertThat(alleTasks.size).isEqualTo(2)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(2)
    }

    @Test
    fun `finnTasksDtoTilFrontend - skal hente ut max 1 task gitt en status`() {
        val ubehandletTask = Task.nyTask(TaskStep1.TASK_1, "{'a'='b'}")
        ubehandletTask.status = Status.UBEHANDLET
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        val feiletTask2 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask2.status = Status.FEILET

        repository.saveAndFlush(ubehandletTask)
        repository.saveAndFlush(feiletTask1)
        repository.saveAndFlush(feiletTask2)

        val alleTasks = repository.finnTasksDtoTilFrontend(listOf(Status.FEILET), PageRequest.of(0, 1))
        Assertions.assertThat(alleTasks.size).isEqualTo(1)
        Assertions.assertThat(alleTasks.count { it.status == Status.FEILET }).isEqualTo(1)
    }

    @Test
    fun `finnTasksMedStatus - skal hente task til frontend`() {
        MDC.put(MDCConstants.MDC_CALL_ID, "test")
        val feiletTask1 = Task.nyTask(TaskStep2.TASK_2, "{'a'='1'}")
        feiletTask1.status = Status.FEILET
        repository.saveAndFlush(feiletTask1)

        val tasks = repository.finnTasksDtoTilFrontend(listOf(Status.FEILET), PageRequest.of(0, 1000))

        Assertions.assertThat(tasks).hasSize(1)

        val task = tasks.first()
        val id = task.id
        val tasklogg = repository.finnTaskloggTilFrontend(id)

        Assertions.assertThat(task.callId).isEqualTo("test")
        Assertions.assertThat(tasklogg).hasSize(1)
        Assertions.assertThat(task.sistKjørt).isEqualTo(tasklogg.first().opprettetTidspunkt)

        MDC.remove(MDCConstants.MDC_CALL_ID)
    }


}
