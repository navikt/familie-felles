package no.nav.familie.prosessering.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.Task.Companion.nyTask
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.task.TaskStep1
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*


class TaskWorkerTest {

    private val repository: TaskRepository = mockk(relaxed = true)

    private val taskStep1 = TaskStep1(repository)

    private val worker: TaskWorker = TaskWorker(repository, listOf(taskStep1))

    @Test
    fun `skal behandle task`() {
        val task1 =
                nyTask(TaskStep1.TASK_1, "{'a'='b'}").copy(id = 1)
        every { repository.findById(1) } returns Optional.of(task1)
        val copy = task1.copy(status = Status.BEHANDLER)
        every { repository.saveAndFlush(copy) } returns copy
        assertThat(task1.status).isEqualTo(Status.UBEHANDLET)

        worker.doActualWork(task1.id!!)

        verify { repository.saveAndFlush(copy) }
        assertThat(task1.status).isEqualTo(Status.FERDIG)
        assertThat(task1.logg).hasSize(3)
    }

}
